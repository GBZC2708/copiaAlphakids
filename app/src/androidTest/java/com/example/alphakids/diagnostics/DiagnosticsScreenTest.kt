package com.example.alphakids.diagnostics

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.alphakids.domain.common.DomainError
import com.example.alphakids.domain.models.DiagnosticsReport
import com.example.alphakids.domain.models.ServiceStatus
import com.example.alphakids.ui.screens.diagnostics.DiagnosticsScreen
import com.example.alphakids.ui.screens.diagnostics.DiagnosticsUiState
import org.junit.Rule
import org.junit.Test

class DiagnosticsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loadingStateShowsProgress() {
        composeTestRule.setContent {
            DiagnosticsScreen(
                state = DiagnosticsUiState(isLoading = true),
                onRetry = {}
            )
        }

        composeTestRule.onNodeWithText("Ejecutando diagnósticos...").assertIsDisplayed()
    }

    @Test
    fun errorStateShowsMessage() {
        val fakeError = object : DomainError {
            override val message: String = "fallo"
            override val cause: Throwable? = null
        }
        composeTestRule.setContent {
            DiagnosticsScreen(
                state = DiagnosticsUiState(isLoading = false, error = fakeError),
                onRetry = {}
            )
        }

        composeTestRule.onNodeWithText("Error en diagnóstico").assertIsDisplayed()
        composeTestRule.onNodeWithText("fallo").assertIsDisplayed()
    }

    @Test
    fun successStateShowsStatuses() {
        val report = DiagnosticsReport(
            authStatus = ServiceStatus(true, 0L, "Auth listo"),
            firestoreStatus = ServiceStatus(true, 0L, "Firestore ok"),
            storageStatus = ServiceStatus(true, 0L, "Storage ok"),
            appVersionName = "1.0"
        )
        composeTestRule.setContent {
            DiagnosticsScreen(
                state = DiagnosticsUiState(isLoading = false, report = report),
                onRetry = {}
            )
        }

        composeTestRule.onNodeWithText("Diagnóstico de servicios").assertIsDisplayed()
        composeTestRule.onNodeWithText("Auth").assertIsDisplayed()
        composeTestRule.onNodeWithText("Versión: 1.0").assertIsDisplayed()
    }

    @Test
    fun retryButtonVisibleOnError() {
        val fakeError = object : DomainError {
            override val message: String = "reintenta"
            override val cause: Throwable? = null
        }
        composeTestRule.setContent {
            DiagnosticsScreen(
                state = DiagnosticsUiState(isLoading = false, error = fakeError),
                onRetry = {}
            )
        }

        composeTestRule.onNodeWithText("Reintentar").assertIsDisplayed()
    }

    @Test
    fun retryButtonVisibleOnSuccess() {
        val report = DiagnosticsReport(
            authStatus = ServiceStatus(true, 0L),
            firestoreStatus = ServiceStatus(true, 0L),
            storageStatus = ServiceStatus(true, 0L),
            appVersionName = "1.2"
        )
        composeTestRule.setContent {
            DiagnosticsScreen(
                state = DiagnosticsUiState(isLoading = false, report = report),
                onRetry = {}
            )
        }

        composeTestRule.onNodeWithText("Actualizar").assertIsDisplayed()
    }

    @Test
    fun statusCardShowsErrorState() {
        val report = DiagnosticsReport(
            authStatus = ServiceStatus(false, 0L, "sin auth"),
            firestoreStatus = ServiceStatus(true, 0L),
            storageStatus = ServiceStatus(true, 0L),
            appVersionName = "1.3"
        )
        composeTestRule.setContent {
            DiagnosticsScreen(
                state = DiagnosticsUiState(isLoading = false, report = report),
                onRetry = {}
            )
        }

        composeTestRule.onNodeWithText("Estado: ERROR").assertIsDisplayed()
    }
}
