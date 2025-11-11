package com.example.alphakids.ui.screens.diagnostics

import com.example.alphakids.domain.common.DomainError
import com.example.alphakids.domain.models.DiagnosticsReport

data class DiagnosticsUiState(
    val isLoading: Boolean = true,
    val report: DiagnosticsReport? = null,
    val error: DomainError? = null
) {
    val isError: Boolean get() = error != null
    val isSuccess: Boolean get() = report != null && !isLoading
}
