package com.example.alphakids.ui.word.assign

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphakids.data.firebase.models.Estudiante
import com.example.alphakids.domain.models.Word
import com.example.alphakids.domain.models.WordAssignment
import com.example.alphakids.domain.repository.WordSortOrder
import com.example.alphakids.domain.usecases.CreateAssignmentUseCase
import com.example.alphakids.domain.usecases.GetCurrentUserUseCase
import com.example.alphakids.domain.usecases.GetFilteredWordsUseCase
import com.example.alphakids.domain.usecases.GetStudentsForDocenteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AssignmentUiState {
    object Idle : AssignmentUiState
    object Loading : AssignmentUiState
    data class Success(val message: String) : AssignmentUiState
    data class Error(val message: String) : AssignmentUiState
}

@HiltViewModel
class AssignWordViewModel @Inject constructor(
    private val createAssignmentUseCase: CreateAssignmentUseCase,
    private val getStudentsForDocenteUseCase: GetStudentsForDocenteUseCase,
    private val getFilteredWordsUseCase: GetFilteredWordsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AssignmentUiState>(AssignmentUiState.Idle)
    val uiState: StateFlow<AssignmentUiState> = _uiState.asStateFlow()

    private val _selectedStudentId = MutableStateFlow<String?>(null)
    val selectedStudentId: StateFlow<String?> = _selectedStudentId.asStateFlow()

    private val _wordSearchQuery = MutableStateFlow("")
    private val _wordFilterDifficulty = MutableStateFlow<String?>(null)
    val wordFilterDifficulty: StateFlow<String?> = _wordFilterDifficulty.asStateFlow()

    val students: StateFlow<List<Estudiante>> = getCurrentUserUseCase()
        .map { it?.uid }
        .flatMapLatest { docenteId ->
            if (docenteId != null) {
                getStudentsForDocenteUseCase(docenteId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val availableWords: StateFlow<List<Word>> = combine(
        getCurrentUserUseCase().map { it?.uid },
        _wordSearchQuery,
        _wordFilterDifficulty
    ) { docenteId, query, difficulty ->
        Triple(docenteId, query, difficulty)
    }.flatMapLatest { (docenteId, query, difficulty) ->
        if (docenteId != null) {
            getFilteredWordsUseCase(
                docenteId = docenteId,
                dificultad = difficulty,
                sortBy = WordSortOrder.TEXT_ASC
            ).map { words ->
                if (query.isBlank()) {
                    words
                } else {
                    words.filter { it.texto.contains(query, ignoreCase = true) }
                }
            }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectStudent(studentId: String?) {
        _selectedStudentId.value = studentId
    }

    fun setWordSearchQuery(query: String) {
        _wordSearchQuery.value = query
    }

    fun setWordFilterDifficulty(difficulty: String?) {
        _wordFilterDifficulty.value = if (_wordFilterDifficulty.value == difficulty) null else difficulty
    }

    fun createAssignment(word: Word) {
        viewModelScope.launch {
            _uiState.value = AssignmentUiState.Loading

            val docente = getCurrentUserUseCase().firstOrNull()
            val student = students.value.find { it.id == _selectedStudentId.value }

            if (docente == null || student == null) {
                _uiState.value = AssignmentUiState.Error("Faltan datos de usuario o estudiante.")
                return@launch
            }

            // Log para debug
            android.util.Log.d("AssignWordViewModel", "Creating assignment for student: ${student.id} (${student.nombre})")
            android.util.Log.d("AssignWordViewModel", "Word: ${word.texto} (${word.id})")
            android.util.Log.d("AssignWordViewModel", "Teacher: ${docente.uid}")

            val newAssignment = WordAssignment(
                id = "",
                idDocente = docente.uid,
                idEstudiante = student.id,
                idPalabra = word.id,
                palabraTexto = word.texto,
                palabraImagenUrl = word.imagenUrl,
                palabraAudioUrl = word.audioUrl,
                palabraDificultad = word.nivelDificultad,
                estudianteNombre = student.nombre,
                rewardCoins = word.rewardCoins,
                attemptsRemaining = 3,
                fechaAsignacionMillis = null,
                fechaLimiteMillis = null,
                estado = "PENDIENTE"
            )

            val result = createAssignmentUseCase(newAssignment)
            android.util.Log.d("AssignWordViewModel", "Assignment result: ${if (result.isSuccess) "SUCCESS" else "FAILURE"}")
            if (result.isFailure) {
                android.util.Log.e("AssignWordViewModel", "Assignment error: ${result.exceptionOrNull()?.message}")
            }

            _uiState.value = if (result.isSuccess) {
                AssignmentUiState.Success("Palabra asignada correctamente a ${student.nombre}.")
            } else {
                AssignmentUiState.Error(result.exceptionOrNull()?.message ?: "Error desconocido al asignar.")
            }
        }
    }

    fun resetUiState() {
        _uiState.value = AssignmentUiState.Idle
    }
}
