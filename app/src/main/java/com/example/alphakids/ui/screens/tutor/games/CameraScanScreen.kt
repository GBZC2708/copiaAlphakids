package com.example.alphakids.ui.screens.tutor.games

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun CameraScanScreen(
    delimitedBox: Boolean,
    scanOnButton: Boolean,
    isScanning: Boolean,
    onScanRequest: () -> Unit,
    onTextDetected: (String) -> Unit,
    onScanError: (String) -> Unit,
    modifier: Modifier = Modifier,
    overlayContent: @Composable BoxScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    val shouldAnalyze = remember { AtomicBoolean(false) }
    val analysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            recognizer.close()
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                cameraProvider = future.get()
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            shouldAnalyze.set(true)
        } else {
            shouldAnalyze.set(false)
        }
    }

    DisposableEffect(cameraProvider) {
        val provider = cameraProvider ?: return@DisposableEffect onDispose {}
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        analysis.clearAnalyzer()
        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
            if (!shouldAnalyze.compareAndSet(true, false)) {
                imageProxy.close()
                return@setAnalyzer
            }
            val mainExecutor = ContextCompat.getMainExecutor(context)
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                mainExecutor.execute {
                    onScanError("No se detectó imagen")
                }
                return@setAnalyzer
            }
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            recognizer.process(inputImage)
                .addOnSuccessListener { result ->
                    mainExecutor.execute {
                        onTextDetected(result.text)
                    }
                }
                .addOnFailureListener { error ->
                    mainExecutor.execute {
                        onScanError(error.message ?: "Error al escanear")
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        onDispose {
            analysis.clearAnalyzer()
            provider.unbindAll()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { previewView }
        )
        if (delimitedBox) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = Color.Black.copy(alpha = 0.6f), size = size)
                val width = size.width * 0.8f
                val height = size.height * 0.32f
                val left = (size.width - width) / 2f
                val top = (size.height - height) / 2f
                drawRect(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    blendMode = BlendMode.Clear
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(width = 4.dp.toPx())
                )
            }
        }
        overlayContent()
        if (scanOnButton) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onScanRequest,
                    enabled = !isScanning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    if (isScanning) {
                        Text(
                            text = " Escaneando...",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    } else {
                        Text("Escanear")
                    }
                }
            }
        }
    }
}
