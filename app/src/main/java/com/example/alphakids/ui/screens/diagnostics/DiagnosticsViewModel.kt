package com.example.alphakids.ui.screens.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alphakids.domain.common.DomainResult
import com.example.alphakids.domain.usecases.HealthCheckUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val healthCheckUseCase: HealthCheckUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh(force: Boolean = true) {
        _uiState.value = DiagnosticsUiState(isLoading = true)
        viewModelScope.launch {
            when (val result = healthCheckUseCase(force)) {
                is DomainResult.Success -> {
                    _uiState.value = DiagnosticsUiState(
                        isLoading = false,
                        report = result.data
                    )
                }
                is DomainResult.Error -> {
                    _uiState.value = DiagnosticsUiState(
                        isLoading = false,
                        error = result.error
                    )
                }
            }
        }
    }
}
