package com.example.alphakids.ui.screens.tutor.games

import androidx.lifecycle.ViewModel
import com.example.alphakids.data.tts.TtsService
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed class CameraScanStatus {
    data object Idle : CameraScanStatus()
    data object Scanning : CameraScanStatus()
    data class Error(val reason: String) : CameraScanStatus()
    data object Success : CameraScanStatus()
}

data class CameraOCRUiState(
    val assignmentId: String = "",
    val targetWord: String = "",
    val displayWord: String = "",
    val detectedText: String = "",
    val status: CameraScanStatus = CameraScanStatus.Idle,
    val message: String? = null
)

@HiltViewModel
class CameraOCRViewModel @Inject constructor(
    private val ttsService: TtsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraOCRUiState())
    val uiState: StateFlow<CameraOCRUiState> = _uiState.asStateFlow()

    private var successSpoken = false

    fun initialize(assignmentId: String, targetWord: String) {
        val normalizedTarget = normalize(targetWord)
        if (
            _uiState.value.assignmentId == assignmentId &&
            _uiState.value.targetWord == normalizedTarget
        ) {
            return
        }
        successSpoken = false
        _uiState.value = CameraOCRUiState(
            assignmentId = assignmentId,
            targetWord = normalizedTarget,
            displayWord = targetWord,
            status = CameraScanStatus.Idle
        )
    }

    fun startScan() {
        if (_uiState.value.status is CameraScanStatus.Scanning) return
        successSpoken = false
        _uiState.update {
            it.copy(
                status = CameraScanStatus.Scanning,
                message = null,
                detectedText = ""
            )
        }
    }

    fun onScanResult(rawText: String) {
        if (_uiState.value.status !is CameraScanStatus.Scanning) return
        val normalizedDetected = normalize(rawText)
        val target = _uiState.value.targetWord
        val matched = normalizedDetected == target && target.isNotEmpty()
        val message = when {
            matched -> "¡Bien hecho! La palabra es ${_uiState.value.displayWord}"
            normalizedDetected.isBlank() -> "No se detectó texto"
            else -> "Detectado: $normalizedDetected"
        }
        _uiState.update {
            it.copy(
                detectedText = normalizedDetected,
                status = if (matched) CameraScanStatus.Success else CameraScanStatus.Error(message),
                message = message
            )
        }
        if (matched && !successSpoken) {
            successSpoken = true
            val utteranceId = buildString {
                append("camera_scan_")
                append(_uiState.value.assignmentId.ifBlank { target })
            }
            ttsService.speak(utteranceId, message)
        }
    }

    fun onScanError(reason: String) {
        if (_uiState.value.status !is CameraScanStatus.Scanning) return
        _uiState.update {
            it.copy(
                status = CameraScanStatus.Error(reason),
                message = reason
            )
        }
    }

    fun resetToIdle() {
        _uiState.update {
            it.copy(
                status = CameraScanStatus.Idle,
                message = null,
                detectedText = ""
            )
        }
    }

    private fun normalize(value: String): String {
        val cleaned = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("[^A-Za-z0-9 ]".toRegex(), " ")
            .trim()
            .replace("\\s+".toRegex(), " ")
        return cleaned.uppercase(Locale.getDefault())
    }
}
