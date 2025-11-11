package com.example.alphakids.ui.screens.tutor.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphakids.domain.models.Student
import com.example.alphakids.domain.usecases.ObserveStudentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map

@HiltViewModel
class StudentTabsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeStudentUseCase: ObserveStudentUseCase
) : ViewModel() {

    // ✅ Especificar el tipo para que compile
    private val studentId: String = savedStateHandle.get<String>("studentId").orEmpty()

    val uiState: StateFlow<StudentTabsUiState> =
        if (studentId.isBlank()) {
            MutableStateFlow(StudentTabsUiState.Error("No se encontró el perfil del estudiante."))
        } else {
            observeStudentUseCase(studentId)
                .map { student: Student? ->
                    if (student == null) {
                        StudentTabsUiState.Empty("No hay información disponible.")
                    } else {
                        val fullName = buildString {
                            if (student.nombre.isNotBlank()) append(student.nombre)
                            if (student.apellido.isNotBlank()) {
                                if (isNotEmpty()) append(' ')
                                append(student.apellido)
                            }
                        }.ifBlank { "Estudiante" }

                        StudentTabsUiState.Success(
                            StudentHeaderState(
                                id = student.id,
                                fullName = fullName,
                                coins = student.coins.coerceAtLeast(0)
                            )
                        )
                    }
                }
                .onStart { emit(StudentTabsUiState.Loading) }
                .catch { e ->
                    emit(StudentTabsUiState.Error(e.message ?: "Error al cargar el estudiante."))
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = StudentTabsUiState.Loading
                )
        }
}

data class StudentHeaderState(
    val id: String,
    val fullName: String,
    val coins: Int
)

sealed interface StudentTabsUiState {
    object Loading : StudentTabsUiState
    data class Error(val message: String) : StudentTabsUiState
    data class Empty(val message: String) : StudentTabsUiState
    data class Success(val header: StudentHeaderState) : StudentTabsUiState
}
