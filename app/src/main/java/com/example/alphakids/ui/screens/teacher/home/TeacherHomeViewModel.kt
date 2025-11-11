package com.example.alphakids.ui.screens.teacher.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphakids.data.firebase.models.Estudiante
import com.example.alphakids.domain.models.Word
import com.example.alphakids.domain.repository.WordSortOrder
import com.example.alphakids.domain.usecases.GetCurrentUserUseCase
import com.example.alphakids.domain.usecases.GetStudentsForDocenteUseCase
import com.example.alphakids.domain.usecases.GetWordsByDocenteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TeacherHomeHeader(val fullName: String)

data class TeacherHomeStats(
    val studentsCount: Int,
    val wordsCount: Int
)

data class TeacherHomeStudentItem(
    val id: String,
    val fullName: String,
    val grade: String,
    val section: String,
    val coins: Int
)

sealed interface TeacherHomeUiState {
    object Loading : TeacherHomeUiState
    data class Error(val header: TeacherHomeHeader?, val message: String) : TeacherHomeUiState
    data class Empty(val header: TeacherHomeHeader, val stats: TeacherHomeStats) : TeacherHomeUiState
    data class Success(
        val header: TeacherHomeHeader,
        val stats: TeacherHomeStats,
        val students: List<TeacherHomeStudentItem>,
        val searchQuery: String,
        val selectedGrade: String?,
        val availableGrades: List<String>
    ) : TeacherHomeUiState
}

@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getStudentsForDocenteUseCase: GetStudentsForDocenteUseCase,
    private val getWordsByDocenteUseCase: GetWordsByDocenteUseCase
) : ViewModel() {

    private val currentUserState: StateFlow<com.example.alphakids.domain.models.User?> =
        getCurrentUserUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    private val docenteIdFlow = currentUserState
        .map { it?.uid }
        .distinctUntilChanged()

    private val headerFlow: StateFlow<TeacherHomeHeader?> = currentUserState
        .map { user ->
            user?.let {
                val parts = listOf(it.nombre, it.apellido).filter { part -> part.isNotBlank() }
                TeacherHomeHeader(parts.joinToString(separator = " ").ifBlank { "Docente" })
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _searchQuery = MutableStateFlow("")
    private val _selectedGrade = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val studentsFlow: StateFlow<List<Estudiante>> = docenteIdFlow
        .flatMapLatest { docenteId ->
            if (docenteId.isNullOrEmpty()) {
                flowOf(emptyList())
            } else {
                getStudentsForDocenteUseCase(docenteId)
                    .catch { throwable ->
                        onStreamError(throwable.message ?: "Error al cargar estudiantes")
                        emit(emptyList())
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val wordsFlow: StateFlow<List<Word>> = docenteIdFlow
        .flatMapLatest { docenteId ->
            if (docenteId.isNullOrEmpty()) {
                flowOf(emptyList())
            } else {
                getWordsByDocenteUseCase(docenteId, WordSortOrder.TEXT_ASC)
                    .catch { throwable ->
                        onStreamError(throwable.message ?: "Error al cargar palabras")
                        emit(emptyList())
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow<TeacherHomeUiState>(TeacherHomeUiState.Loading)
    val uiState: StateFlow<TeacherHomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // En algunas versiones de coroutines, combine(vararg) usa un transform con Array<T>.
            combine(
                headerFlow,
                studentsFlow,
                wordsFlow,
                _searchQuery,
                _selectedGrade,
                _errorMessage
            ) { arr ->
                val header = arr[0] as TeacherHomeHeader?
                val students = arr[1] as List<Estudiante>
                val words = arr[2] as List<Word>
                val query = arr[3] as String
                val grade = arr[4] as String?
                val error = arr[5] as String?
                toUiState(header, students, words, query, grade, error)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onGradeSelected(grade: String) {
        _selectedGrade.value = if (_selectedGrade.value == grade) null else grade
    }

    fun retry() {
        _errorMessage.value = null
    }

    private fun toUiState(
        header: TeacherHomeHeader?,
        students: List<Estudiante>,
        words: List<Word>,
        query: String,
        grade: String?,
        error: String?
    ): TeacherHomeUiState {
        if (header == null) {
            return TeacherHomeUiState.Error(header = null, message = "No se encontró información del docente.")
        }

        if (error != null) {
            return TeacherHomeUiState.Error(header = header, message = error)
        }

        val mappedStudents = students.map { estudiante ->
            TeacherHomeStudentItem(
                id = estudiante.id,
                fullName = listOf(estudiante.nombre, estudiante.apellido)
                    .filter { it.isNotBlank() }
                    .joinToString(separator = " "),
                grade = estudiante.grado,
                section = estudiante.seccion,
                coins = estudiante.coins
            )
        }

        val stats = TeacherHomeStats(
            studentsCount = mappedStudents.size,
            wordsCount = words.size
        )

        if (mappedStudents.isEmpty()) {
            return TeacherHomeUiState.Empty(header = header, stats = stats)
        }

        val normalizedQuery = query.trim().lowercase()

        val filtered = mappedStudents.filter { student ->
            val matchesQuery = normalizedQuery.isEmpty() ||
                    student.fullName.lowercase().contains(normalizedQuery)
            val matchesGrade = grade.isNullOrEmpty() || student.grade == grade
            matchesQuery && matchesGrade
        }

        val availableGrades = mappedStudents
            .mapNotNull { it.grade.takeIf(String::isNotBlank) }
            .distinct()
            .sorted()

        return TeacherHomeUiState.Success(
            header = header,
            stats = stats,
            students = filtered,
            searchQuery = query,
            selectedGrade = grade,
            availableGrades = availableGrades
        )
    }

    fun onStreamError(message: String) {
        _errorMessage.value = message
    }
}
