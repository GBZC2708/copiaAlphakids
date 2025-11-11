package com.example.alphakids.ui.screens.student.assignments

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphakids.data.tts.TtsService
import com.example.alphakids.domain.models.Achievement
import com.example.alphakids.domain.models.WordAssignment
import com.example.alphakids.domain.usecases.CompleteAssignmentUseCase
import com.example.alphakids.domain.usecases.CreateAchievementUseCase
import com.example.alphakids.domain.usecases.DecrementAssignmentAttemptsUseCase
import com.example.alphakids.domain.usecases.ObservePendingAssignmentsUseCase
import com.example.alphakids.domain.usecases.ObserveStudentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val DEFAULT_ATTEMPTS = 3

@HiltViewModel
class StudentAssignmentsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeStudentUseCase: ObserveStudentUseCase,
    private val observePendingAssignmentsUseCase: ObservePendingAssignmentsUseCase,
    private val decrementAssignmentAttemptsUseCase: DecrementAssignmentAttemptsUseCase,
    private val completeAssignmentUseCase: CompleteAssignmentUseCase,
    private val createAchievementUseCase: CreateAchievementUseCase,
    private val ttsService: TtsService
) : ViewModel() {

    private val studentId: String = savedStateHandle.get<String>("studentId").orEmpty()

    private val _header = MutableStateFlow<StudentAssignmentsHeader?>(null)
    private val _headerLoaded = MutableStateFlow(false)
    private val _assignments = MutableStateFlow<List<StudentAssignmentItem>>(emptyList())
    private val _assignmentsLoaded = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _events = MutableSharedFlow<StudentAssignmentsEvent>()
    private val latestAssignments = MutableStateFlow<Map<String, WordAssignment>>(emptyMap())

    val events = _events.asSharedFlow()

    val uiState: StateFlow<StudentAssignmentsUiState> = combine(
        _header,
        _assignments,
        _headerLoaded,
        _assignmentsLoaded,
        _errorMessage
    ) { header, assignments, headerLoaded, assignmentsLoaded, error ->
        if (!headerLoaded || !assignmentsLoaded) {
            StudentAssignmentsUiState.Loading
        } else if (error != null) {
            StudentAssignmentsUiState.Error(header, error)
        } else if (header == null) {
            StudentAssignmentsUiState.Error(null, "No se encontró el estudiante.")
        } else if (assignments.isEmpty()) {
            StudentAssignmentsUiState.Empty(header, "No tienes asignaciones pendientes.")
        } else {
            StudentAssignmentsUiState.Success(header, assignments)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StudentAssignmentsUiState.Loading
    )

    init {
        if (studentId.isBlank()) {
            _headerLoaded.value = true
            _assignmentsLoaded.value = true
            _errorMessage.value = "No se encontró el estudiante."
        } else {
            viewModelScope.launch {
                observeStudentUseCase(studentId)
                    .catch { error ->
                        _headerLoaded.value = true
                        _errorMessage.value = error.message ?: "Error al cargar el estudiante."
                    }
                    .collect { student ->
                        _headerLoaded.value = true
                        if (student == null) {
                            _errorMessage.value = "No se encontró el estudiante."
                            _header.value = null
                        } else {
                            val fullName = listOf(student.nombre, student.apellido)
                                .filter { it.isNotBlank() }
                                .joinToString(" ")
                                .ifBlank { "Estudiante" }
                            val header = StudentAssignmentsHeader(
                                id = student.id,
                                fullName = fullName,
                                coins = student.coins.coerceAtLeast(0)
                            )
                            _header.value = header
                            if (_errorMessage.value?.contains("estudiante") == true) {
                                _errorMessage.value = null
                            }
                        }
                    }
            }

            viewModelScope.launch {
                observePendingAssignmentsUseCase(studentId)
                    .catch { error ->
                        _assignmentsLoaded.value = true
                        _errorMessage.value = error.message ?: "Error al cargar asignaciones."
                    }
                    .collect { assignments ->
                        _assignmentsLoaded.value = true
                        latestAssignments.value = assignments.associateBy { it.id }
                        _assignments.value = assignments
                            .filter { it.id.isNotBlank() }
                            .map { it.toUiItem() }
                        if (_assignments.value.isNotEmpty()) {
                            _errorMessage.value = null
                        }
                    }
            }
        }
    }

    fun onAssignmentScanFailed(assignmentId: String) {
        if (studentId.isBlank()) return
        viewModelScope.launch {
            val result = decrementAssignmentAttemptsUseCase(assignmentId)
            result.onSuccess { attempts ->
                val message = if (attempts > 0) {
                    "Intentos restantes: $attempts"
                } else {
                    "Sin intentos disponibles. Intenta nuevamente después."
                }
                _events.emit(StudentAssignmentsEvent.Message(message))
            }.onFailure { error ->
                val message = error.message ?: "No se pudo registrar el intento."
                _events.emit(StudentAssignmentsEvent.Message(message))
            }
        }
    }

    fun onAssignmentScanSucceeded(assignmentId: String) {
        if (studentId.isBlank()) return
        val assignment = latestAssignments.value[assignmentId]
        if (assignment == null) {
            viewModelScope.launch {
                _events.emit(StudentAssignmentsEvent.Message("Asignación no encontrada."))
            }
            return
        }
        viewModelScope.launch {
            val reward = assignment.rewardCoins.coerceAtLeast(0)
            val result = completeAssignmentUseCase(assignmentId, studentId, reward)
            result.onSuccess {
                val word = assignment.palabraTexto
                val message = if (reward > 0) {
                    "¡Excelente! Ganaste $reward monedas por \"$word\"."
                } else {
                    "¡Excelente! Completaste \"$word\"."
                }
                _events.emit(StudentAssignmentsEvent.Message(message))
                ttsService.speak("assignment_$assignmentId", message)
                createAchievement(assignmentId, word, reward)
            }.onFailure { error ->
                val message = error.message ?: "No se pudo completar la asignación."
                _events.emit(StudentAssignmentsEvent.Message(message))
                Log.e("StudentAssignmentsVM", "Error completing assignment", error)
            }
        }
    }

    fun retry() {
        _errorMessage.value = null
    }

    private fun createAchievement(assignmentId: String, word: String, reward: Int) {
        viewModelScope.launch {
            val achievement = Achievement(
                id = "assignment_$assignmentId",
                studentId = studentId,
                name = "Palabra completada",
                description = "Completaste \"$word\".",
                coins = reward.coerceAtLeast(0),
                createdAtMillis = System.currentTimeMillis()
            )
            createAchievementUseCase(achievement).onFailure {
                Log.e("StudentAssignmentsVM", "Error creating achievement", it)
            }
        }
    }

    private fun WordAssignment.toUiItem(): StudentAssignmentItem {
        val sanitizedWord = palabraTexto ?: ""
        return StudentAssignmentItem(
            id = id,
            word = sanitizedWord,
            maskedWord = maskWord(sanitizedWord),
            imageUrl = palabraImagenUrl,
            rewardCoins = rewardCoins.coerceAtLeast(0),
            attemptsLeft = (attemptsRemaining ?: DEFAULT_ATTEMPTS).coerceAtLeast(0)
        )
    }
}

data class StudentAssignmentsHeader(
    val id: String,
    val fullName: String,
    val coins: Int
)

data class StudentAssignmentItem(
    val id: String,
    val word: String,
    val maskedWord: String,
    val imageUrl: String?,
    val rewardCoins: Int,
    val attemptsLeft: Int
)

sealed interface StudentAssignmentsUiState {
    object Loading : StudentAssignmentsUiState
    data class Error(val header: StudentAssignmentsHeader?, val message: String) : StudentAssignmentsUiState
    data class Empty(val header: StudentAssignmentsHeader, val message: String) : StudentAssignmentsUiState
    data class Success(val header: StudentAssignmentsHeader, val assignments: List<StudentAssignmentItem>) : StudentAssignmentsUiState
}

sealed interface StudentAssignmentsEvent {
    data class Message(val text: String) : StudentAssignmentsEvent
}

private fun maskWord(value: String): String {
    if (value.isBlank()) return ""
    return buildString {
        value.forEach { char ->
            append(if (char.isWhitespace()) char else '*')
        }
    }
}
