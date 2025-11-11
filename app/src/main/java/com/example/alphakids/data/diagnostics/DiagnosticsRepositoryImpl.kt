package com.example.alphakids.data.diagnostics

import android.os.SystemClock
import com.example.alphakids.BuildConfig
import com.example.alphakids.data.common.FirebaseExceptionMapper
import com.example.alphakids.domain.common.DomainResult
import com.example.alphakids.domain.common.domainResultOf
import com.example.alphakids.domain.models.DiagnosticsReport
import com.example.alphakids.domain.models.ServiceStatus
import com.example.alphakids.domain.repository.DiagnosticsRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

class DiagnosticsRepositoryImpl @Inject constructor(
    private val firebaseApp: FirebaseApp,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : DiagnosticsRepository {

    override suspend fun runColdStartDiagnostics(force: Boolean): DomainResult<DiagnosticsReport> {
        return domainResultOf(FirebaseExceptionMapper::toDomainError) {
            ensureFirebaseInitialized()

            val authStatus = checkAuth()
            val firestoreStatus = checkWithRetry { checkFirestore() }
            val storageStatus = checkWithRetry { checkStorage() }

            DiagnosticsReport(
                authStatus = authStatus,
                firestoreStatus = firestoreStatus,
                storageStatus = storageStatus,
                appVersionName = BuildConfig.VERSION_NAME
            )
        }
    }

    private fun ensureFirebaseInitialized() {
        checkNotNull(firebaseApp) { "FirebaseApp no inicializado" }
    }

    private fun checkAuth(): ServiceStatus {
        val user = firebaseAuth.currentUser
        val message = if (user != null) "Usuario autenticado" else "Auth inicializado"
        return ServiceStatus(
            isHealthy = true,
            lastCheckedAtMillis = SystemClock.elapsedRealtime(),
            message = message
        )
    }

    private suspend fun checkFirestore(): ServiceStatus {
        val snapshot = firestore.collection("diagnostics")
            .limit(1)
            .get()
            .await()

        val message = if (snapshot.isEmpty) {
            "Firestore accesible"
        } else {
            "Firestore respondió con ${snapshot.size()} documentos"
        }

        return ServiceStatus(
            isHealthy = true,
            lastCheckedAtMillis = SystemClock.elapsedRealtime(),
            message = message
        )
    }

    private suspend fun checkStorage(): ServiceStatus {
        val result = storage.reference.list(1).await()
        val message = if (result.items.isEmpty()) {
            "Storage accesible"
        } else {
            "Storage contiene ${result.items.size} items"
        }

        return ServiceStatus(
            isHealthy = true,
            lastCheckedAtMillis = SystemClock.elapsedRealtime(),
            message = message
        )
    }

    private suspend fun checkWithRetry(block: suspend () -> ServiceStatus): ServiceStatus {
        var attempt = 0
        var delayMillis = INITIAL_DELAY
        var lastError: Throwable? = null

        while (attempt < MAX_ATTEMPTS) {
            try {
                return block()
            } catch (error: Throwable) {
                lastError = error
                attempt++
                if (attempt == MAX_ATTEMPTS) {
                    throw error
                }
                delay(delayMillis)
                delayMillis = (delayMillis * BACKOFF_FACTOR).coerceAtMost(MAX_DELAY)
            }
        }

        throw lastError ?: IllegalStateException("Fallo desconocido en diagnóstico")
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val INITIAL_DELAY = 200L
        const val MAX_DELAY = 1_500L
        const val BACKOFF_FACTOR = 2
    }
}
