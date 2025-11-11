package com.example.alphakids.ui.screens.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alphakids.domain.models.DiagnosticsReport
import com.example.alphakids.domain.models.ServiceStatus

@Composable
fun DiagnosticsRoute(
    modifier: Modifier = Modifier,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    DiagnosticsScreen(
        modifier = modifier,
        state = uiState,
        onRetry = { viewModel.refresh(force = true) }
    )
}

@Composable
fun DiagnosticsScreen(
    state: DiagnosticsUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> DiagnosticsLoading()
            state.isError -> DiagnosticsError(message = state.error?.message.orEmpty(), onRetry = onRetry)
            state.isSuccess -> state.report?.let { DiagnosticsSuccess(it, onRetry) }
        }
    }
}

@Composable
private fun DiagnosticsLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Text(
            text = "Ejecutando diagnósticos...",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun DiagnosticsError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Error en diagnóstico", style = MaterialTheme.typography.headlineSmall)
        Text(text = message, modifier = Modifier.padding(vertical = 16.dp))
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}

@Composable
private fun DiagnosticsSuccess(report: DiagnosticsReport, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(16.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Diagnóstico de servicios", style = MaterialTheme.typography.headlineSmall)
        StatusCard(title = "Auth", status = report.authStatus)
        StatusCard(title = "Firestore", status = report.firestoreStatus)
        StatusCard(title = "Storage", status = report.storageStatus)
        Text(text = "Versión: ${report.appVersionName}")
        Button(onClick = onRetry) { Text(text = "Actualizar") }
    }
}

@Composable
private fun StatusCard(title: String, status: ServiceStatus) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (status.isHealthy) "Estado: OK" else "Estado: ERROR",
                style = MaterialTheme.typography.bodyMedium
            )
            status.message?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
