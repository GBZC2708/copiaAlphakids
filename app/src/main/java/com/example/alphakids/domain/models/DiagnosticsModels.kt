package com.example.alphakids.domain.models

data class ServiceStatus(
    val isHealthy: Boolean,
    val lastCheckedAtMillis: Long,
    val message: String? = null
)

data class DiagnosticsReport(
    val authStatus: ServiceStatus,
    val firestoreStatus: ServiceStatus,
    val storageStatus: ServiceStatus,
    val appVersionName: String
) {
    val allHealthy: Boolean
        get() = authStatus.isHealthy && firestoreStatus.isHealthy && storageStatus.isHealthy
}
