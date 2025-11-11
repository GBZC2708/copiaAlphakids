package com.example.alphakids.domain.repository

import com.example.alphakids.domain.common.DomainResult
import com.example.alphakids.domain.models.DiagnosticsReport

interface DiagnosticsRepository {
    suspend fun runColdStartDiagnostics(force: Boolean = false): DomainResult<DiagnosticsReport>
}
