package com.example.alphakids.ui.screens.tutor.games

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraOCRScreen(
    assignmentId: String,
    targetWord: String,
    onBackClick: () -> Unit,
    onWordCompleted: () -> Unit,
    onFailedAttempt: () -> Unit,
    viewModel: CameraOCRViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val permissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(assignmentId, targetWord) {
        viewModel.initialize(assignmentId, targetWord)
    }

    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    val status = uiState.status
    val statusMessage = uiState.message
    val isScanning = status is CameraScanStatus.Scanning

    Box(modifier = Modifier.fillMaxSize()) {
        if (permissionState.status.isGranted) {
            CameraScanScreen(
                delimitedBox = true,
                scanOnButton = true,
                isScanning = isScanning,
                onScanRequest = { viewModel.startScan() },
                onTextDetected = { viewModel.onScanResult(it) },
                onScanError = { viewModel.onScanError(it) },
                overlayContent = {
                    CameraOverlayHeader(
                        targetWord = uiState.displayWord,
                        onBackClick = onBackClick
                    )
                    if (statusMessage != null) {
                        StatusMessage(
                            message = statusMessage,
                            isError = status is CameraScanStatus.Error,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 128.dp)
                                .padding(horizontal = 24.dp)
                        )
                    } else if (isScanning) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 128.dp)
                                .padding(horizontal = 24.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            )
        } else {
            PermissionDeniedContent(
                onBackClick = onBackClick,
                onRequestPermission = { permissionState.launchPermissionRequest() }
            )
        }
    }

    var lastStatus by remember { mutableStateOf<CameraScanStatus>(CameraScanStatus.Idle) }

    LaunchedEffect(status) {
        if (status is CameraScanStatus.Success) {
            WordHistoryStorage.saveCompletedWord(context, uiState.displayWord)
            onWordCompleted()
            viewModel.resetToIdle()
        } else if (status is CameraScanStatus.Error && status != lastStatus) {
            onFailedAttempt()
        }
        lastStatus = status
    }
}

@Composable
private fun BoxScope.CameraOverlayHeader(
    targetWord: String,
    onBackClick: () -> Unit
) {
    SmallTopAppBar(
        title = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Busca: $targetWord",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Presiona \"Escanear\" para reconocer",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Regresar",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.smallTopAppBarColors(
            containerColor = Color.Black.copy(alpha = 0.6f),
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        ),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
    )
}

@Composable
private fun StatusMessage(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = message,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PermissionDeniedContent(
    onBackClick: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Necesitamos permiso de cámara",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Activa el permiso para escanear la palabra.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Conceder permiso")
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onBackClick) {
            Text("Volver")
        }
    }
}
