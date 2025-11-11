package com.example.alphakids.ui.screens.tutor.dictionary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphakids.domain.models.Student
import com.example.alphakids.domain.models.TeacherDictionaryWord
import com.example.alphakids.domain.usecases.ObserveStudentUseCase
import com.example.alphakids.domain.usecases.ObserveTeacherDictionaryUseCase
import com.example.alphakids.domain.usecases.RewardDictionaryWordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StudentDictionaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeStudentUseCase: ObserveStudentUseCase,
    private val observeTeacherDictionaryUseCase: ObserveTeacherDictionaryUseCase,
    private val rewardDictionaryWordUseCase: RewardDictionaryWordUseCase
) : ViewModel() {

    private val studentId: String = savedStateHandle.get<String>("studentId").orEmpty()

    private val refreshTrigger = MutableStateFlow(0)
    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<String?>(null)
    private val selectedDifficulty = MutableStateFlow<String?>(null)
    private val selectedWordId = MutableStateFlow<String?>(null)
    private val locallyCompleted = MutableStateFlow<Set<String>>(emptySet())
    private val latestWords = MutableStateFlow<Map<String, TeacherDictionaryWord>>(emptyMap())

    private val _events = MutableSharedFlow<StudentDictionaryEvent>()
    val events = _events.asSharedFlow()

    // --- HEADER ---
    private val _headerFlow = MutableStateFlow<HeaderResult>(HeaderResult.Loading)
    private val headerFlow: StateFlow<HeaderResult> = _headerFlow.asStateFlow()

    init {
        if (studentId.isBlank()) {
            _headerFlow.value = HeaderResult.Error("No se encontró el estudiante.")
        } else {
            // observeStudentUseCase debe devolver Flow<Student?>
            viewModelScope.launch {
                observeStudentUseCase(studentId)
                    .map { student: Student? ->
                        val entity = student ?: return@map HeaderResult.Error("No se encontró el estudiante.")
                        val fullName = listOf(entity.nombre, entity.apellido)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                            .ifBlank { "Estudiante" }

                        HeaderResult.Data(
                            StudentDictionaryHeader(
                                studentId = entity.id,
                                teacherId = entity.idDocente,
                                fullName = fullName,
                                coins = entity.coins.coerceAtLeast(0)
                            )
                        )
                    }
                    .catch { e ->
                        emit(HeaderResult.Error(e.message ?: "Error al cargar el estudiante."))
                    }
                    .collect { result ->
                        _headerFlow.value = result
                    }
            }
        }
    }

    // TeacherId derivado del header (tipado y con estado inicial nulo)
    private val teacherIdFlow: StateFlow<String?> =
        headerFlow
            .map { (it as? HeaderResult.Data)?.header?.teacherId }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    // --- DICCIONARIO (combina con refreshTrigger para reintentar/actualizar) ---
    private val dictionaryFlow: StateFlow<List<TeacherDictionaryWord>> =
        combine(
            teacherIdFlow,
            refreshTrigger
        ) { teacherId: String?, _: Int -> teacherId }
            .flatMapLatest { teacherId ->
                if (teacherId.isNullOrBlank() || studentId.isBlank()) {
                    flowOf(emptyList())
                } else {
                    observeTeacherDictionaryUseCase(studentId, teacherId)
                }
            }
            .onEach { words ->
                latestWords.value = words.associateBy { it.id }
                val currentIds = words.map { it.id }.toSet()
                locallyCompleted.update { current -> current.filter { it in currentIds }.toSet() }
            }
            .catch { emit(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // --- Filtros tipados ---
    private data class FilterInputs(
        val query: String,
        val category: String?,
        val difficulty: String?,
        val selectedId: String?,
        val completedIds: Set<String>
    )

    private val filterInputs: StateFlow<FilterInputs> =
        combine(
            searchQuery,
            selectedCategory,
            selectedDifficulty,
            selectedWordId,
            locallyCompleted
        ) { query, category, difficulty, selectedId, completed ->
            FilterInputs(query, category, difficulty, selectedId, completed)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FilterInputs(
                query = "",
                category = null,
                difficulty = null,
                selectedId = null,
                completedIds = emptySet()
            )
        )

    // --- UI State ---
    val uiState: StateFlow<StudentDictionaryUiState> =
        combine(headerFlow, dictionaryFlow, filterInputs) { headerResult, words, inputs ->
            when (headerResult) {
                HeaderResult.Loading -> StudentDictionaryUiState.Loading
                is HeaderResult.Error -> StudentDictionaryUiState.Error(headerResult.message)
                is HeaderResult.Data -> {
                    val header = headerResult.header
                    val availableWords = words.filterNot { inputs.completedIds.contains(it.id) }

                    if (words.isEmpty()) {
                        return@combine StudentDictionaryUiState.Empty(
                            header = header,
                            message = "No hay palabras disponibles en tu diccionario."
                        )
                    }

                    val filtered = availableWords
                        .filter { word ->
                            val matchesQuery =
                                inputs.query.isBlank() || word.palabra.contains(inputs.query, ignoreCase = true)
                            val matchesCategory =
                                inputs.category.isNullOrBlank() || (word.categoria?.equals(inputs.category, ignoreCase = true) == true)
                            val matchesDifficulty =
                                inputs.difficulty.isNullOrBlank() || (word.dificultad?.equals(inputs.difficulty, ignoreCase = true) == true)
                            matchesQuery && matchesCategory && matchesDifficulty
                        }
                        .sortedBy { it.palabra.lowercase() }

                    val categories = availableWords
                        .mapNotNull { it.categoria?.takeIf { c -> c.isNotBlank() } }
                        .distinct()
                        .sorted()

                    val difficulties = availableWords
                        .mapNotNull { it.dificultad?.takeIf { d -> d.isNotBlank() } }
                        .distinct()
                        .sorted()

                    val items = filtered.map { word ->
                        StudentDictionaryWordItem(
                            id = word.id,
                            maskedWord = maskWord(word.palabra),
                            targetWord = word.palabra,
                            category = word.categoria,
                            difficulty = word.dificultad,
                            rewardCoins = word.rewardCoins.coerceAtLeast(0),
                            usage = word.uso,
                            isSelected = word.id == inputs.selectedId
                        )
                    }

                    val emptyMessage = if (items.isEmpty()) {
                        "No se encontraron palabras con los filtros seleccionados."
                    } else null

                    StudentDictionaryUiState.Success(
                        header = header,
                        items = items,
                        filters = DictionaryFiltersState(
                            searchQuery = inputs.query,
                            categories = categories,
                            selectedCategory = inputs.category,
                            difficulties = difficulties,
                            selectedDifficulty = inputs.difficulty
                        ),
                        emptyMessage = emptyMessage
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StudentDictionaryUiState.Loading
        )

    // --- Intents ---
    fun onSearchQueryChange(value: String) { searchQuery.value = value }

    fun onCategorySelected(value: String?) {
        selectedCategory.value = if (selectedCategory.value == value) null else value
    }

    fun onDifficultySelected(value: String?) {
        selectedDifficulty.value = if (selectedDifficulty.value == value) null else value
    }

    fun clearFilters() {
        searchQuery.value = ""
        selectedCategory.value = null
        selectedDifficulty.value = null
    }

    fun onWordSelected(wordId: String) { selectedWordId.value = wordId }

    fun retry() { refreshTrigger.update { it + 1 } }

    fun onWordScanFailed() {
        viewModelScope.launch {
            _events.emit(StudentDictionaryEvent.Message("No se reconoció la palabra. Inténtalo de nuevo."))
        }
    }

    fun onWordScanSucceeded(wordId: String) {
        val header = (headerFlow.value as? HeaderResult.Data)?.header ?: return
        val word = latestWords.value[wordId]
        if (word == null) {
            viewModelScope.launch {
                _events.emit(StudentDictionaryEvent.Message("La palabra ya no está disponible."))
            }
            return
        }
        viewModelScope.launch {
            // Optimistic UI
            locallyCompleted.update { it + wordId }
            selectedWordId.value = null

            val result = rewardDictionaryWordUseCase(
                studentId = header.studentId,
                teacherId = header.teacherId,
                wordId = wordId,
                rewardCoins = word.rewardCoins.coerceAtLeast(0)
            )

            if (result.isSuccess) {
                val reward = word.rewardCoins.coerceAtLeast(0)
                val message = if (reward > 0) "¡Excelente! Ganaste $reward monedas." else "¡Excelente trabajo!"
                _events.emit(StudentDictionaryEvent.Message(message))
            } else {
                // revertir si falla
                locallyCompleted.update { it - wordId }
                _events.emit(
                    StudentDictionaryEvent.Message(
                        result.exceptionOrNull()?.message ?: "Error al registrar tu progreso."
                    )
                )
            }
        }
    }
}

private fun maskWord(value: String): String =
    value.map { ch -> if (ch.isWhitespace()) ' ' else '*' }.joinToString("")

private sealed interface HeaderResult {
    data object Loading : HeaderResult
    data class Error(val message: String) : HeaderResult
    data class Data(val header: StudentDictionaryHeader) : HeaderResult
}

data class StudentDictionaryHeader(
    val studentId: String,
    val teacherId: String,
    val fullName: String,
    val coins: Int
)

data class DictionaryFiltersState(
    val searchQuery: String,
    val categories: List<String>,
    val selectedCategory: String?,
    val difficulties: List<String>,
    val selectedDifficulty: String?
)

data class StudentDictionaryWordItem(
    val id: String,
    val maskedWord: String,
    val targetWord: String,
    val category: String?,
    val difficulty: String?,
    val rewardCoins: Int,
    val usage: String?,
    val isSelected: Boolean
)

sealed interface StudentDictionaryUiState {
    data object Loading : StudentDictionaryUiState
    data class Error(val message: String) : StudentDictionaryUiState
    data class Empty(val header: StudentDictionaryHeader, val message: String) : StudentDictionaryUiState
    data class Success(
        val header: StudentDictionaryHeader,
        val items: List<StudentDictionaryWordItem>,
        val filters: DictionaryFiltersState,
        val emptyMessage: String?
    ) : StudentDictionaryUiState
}

sealed interface StudentDictionaryEvent {
    data class Message(val text: String) : StudentDictionaryEvent
}
