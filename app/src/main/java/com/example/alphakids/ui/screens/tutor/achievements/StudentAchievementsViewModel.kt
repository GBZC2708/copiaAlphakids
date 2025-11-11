package com.example.alphakids.ui.screens.tutor.achievements

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphakids.domain.models.Achievement
import com.example.alphakids.domain.usecases.ObserveStudentAchievementsUseCase
import com.example.alphakids.domain.usecases.ObserveStudentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine

@HiltViewModel
class StudentAchievementsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeStudentUseCase: ObserveStudentUseCase,
    private val observeStudentAchievementsUseCase: ObserveStudentAchievementsUseCase
) : ViewModel() {

    private val studentId: String = savedStateHandle.get<String>("studentId").orEmpty()
    private val refreshTrigger = MutableStateFlow(0)

    private val headerFlow: StateFlow<HeaderResult> = if (studentId.isBlank()) {
        MutableStateFlow(HeaderResult.Error("No se encontró el estudiante."))
    } else {
        refreshTrigger
            .flatMapLatest {
                observeStudentUseCase(studentId)
                    .map<HeaderResult> { student ->
                        val entity = student ?: return@map HeaderResult.Error("No se encontró el estudiante.")
                        val fullName = listOf(entity.nombre, entity.apellido)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                            .ifBlank { "Estudiante" }
                        HeaderResult.Data(
                            StudentAchievementsHeader(
                                studentId = entity.id,
                                fullName = fullName,
                                coins = entity.coins.coerceAtLeast(0)
                            )
                        )
                    }
                    .onStart { emit(HeaderResult.Loading) }
                    .catch { emit(HeaderResult.Error(it.message ?: "Error al cargar el estudiante.")) }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HeaderResult.Loading
            )
    }

    private val achievementsFlow: StateFlow<AchievementsResult> = if (studentId.isBlank()) {
        MutableStateFlow(AchievementsResult.Error("No se encontró el estudiante."))
    } else {
        refreshTrigger
            .flatMapLatest {
                observeStudentAchievementsUseCase(studentId)
                    .map<AchievementsResult> { achievements ->
                        AchievementsResult.Data(achievements)
                    }
                    .onStart { emit(AchievementsResult.Loading) }
                    .catch { emit(AchievementsResult.Error(it.message ?: "Error al cargar los logros.")) }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AchievementsResult.Loading
            )
    }

    val uiState: StateFlow<StudentAchievementsUiState> = combine(headerFlow, achievementsFlow) { headerResult, achievementsResult ->
        when (headerResult) {
            HeaderResult.Loading -> StudentAchievementsUiState.Loading
            is HeaderResult.Error -> StudentAchievementsUiState.Error(null, headerResult.message)
            is HeaderResult.Data -> when (achievementsResult) {
                AchievementsResult.Loading -> StudentAchievementsUiState.Loading
                is AchievementsResult.Error -> StudentAchievementsUiState.Error(headerResult.header, achievementsResult.message)
                is AchievementsResult.Data -> {
                    val items = achievementsResult.items
                        .filter { it.id.isNotBlank() }
                        .map { it.toUiItem() }
                    if (items.isEmpty()) {
                        StudentAchievementsUiState.Empty(
                            header = headerResult.header,
                            message = "Aún no tienes logros desbloqueados."
                        )
                    } else {
                        StudentAchievementsUiState.Success(
                            header = headerResult.header,
                            achievements = items
                        )
                    }
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StudentAchievementsUiState.Loading
    )

    fun retry() {
        refreshTrigger.update { it + 1 }
    }

    private fun Achievement.toUiItem(): StudentAchievementItem {
        val normalizedName = name.ifBlank { "Logro" }
        val normalizedDescription = description.ifBlank { "" }
        val normalizedCoins = coins.coerceAtLeast(0)
        val normalizedMillis = createdAtMillis.takeIf { it > 0 }
        return StudentAchievementItem(
            id = id,
            title = normalizedName,
            description = normalizedDescription,
            coins = normalizedCoins,
            completedAtMillis = normalizedMillis,
            imageUrl = imageUrl
        )
    }
}

private sealed interface HeaderResult {
    object Loading : HeaderResult
    data class Error(val message: String) : HeaderResult
    data class Data(val header: StudentAchievementsHeader) : HeaderResult
}

private sealed interface AchievementsResult {
    object Loading : AchievementsResult
    data class Error(val message: String) : AchievementsResult
    data class Data(val items: List<Achievement>) : AchievementsResult
}

data class StudentAchievementsHeader(
    val studentId: String,
    val fullName: String,
    val coins: Int
)

data class StudentAchievementItem(
    val id: String,
    val title: String,
    val description: String,
    val coins: Int,
    val completedAtMillis: Long?,
    val imageUrl: String?
)

sealed interface StudentAchievementsUiState {
    object Loading : StudentAchievementsUiState
    data class Error(val header: StudentAchievementsHeader?, val message: String) : StudentAchievementsUiState
    data class Empty(val header: StudentAchievementsHeader, val message: String) : StudentAchievementsUiState
    data class Success(val header: StudentAchievementsHeader, val achievements: List<StudentAchievementItem>) : StudentAchievementsUiState
}
