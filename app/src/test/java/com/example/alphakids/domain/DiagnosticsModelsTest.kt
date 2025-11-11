package com.example.alphakids.domain

import com.example.alphakids.domain.models.DiagnosticsReport
import com.example.alphakids.domain.models.ServiceStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiagnosticsModelsTest {

    @Test
    fun `allHealthy true when all services healthy`() {
        val report = DiagnosticsReport(
            authStatus = ServiceStatus(true, 0L),
            firestoreStatus = ServiceStatus(true, 0L),
            storageStatus = ServiceStatus(true, 0L),
            appVersionName = "1.0"
        )
        assertTrue(report.allHealthy)
    }

    @Test
    fun `allHealthy false when any service unhealthy`() {
        val report = DiagnosticsReport(
            authStatus = ServiceStatus(true, 0L),
            firestoreStatus = ServiceStatus(false, 0L),
            storageStatus = ServiceStatus(true, 0L),
            appVersionName = "1.0"
        )
        assertFalse(report.allHealthy)
    }
}
