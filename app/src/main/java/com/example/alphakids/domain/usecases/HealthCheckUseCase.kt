package com.example.alphakids.domain.usecases

import com.example.alphakids.domain.common.DomainResult
import com.example.alphakids.domain.models.DiagnosticsReport
import com.example.alphakids.domain.repository.DiagnosticsRepository
import javax.inject.Inject

class HealthCheckUseCase @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) {
    suspend operator fun invoke(force: Boolean = false): DomainResult<DiagnosticsReport> {
        return diagnosticsRepository.runColdStartDiagnostics(force)
    }
}
