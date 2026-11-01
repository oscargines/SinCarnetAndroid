package com.oscar.sincarnet

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.oscar.sincarnet.presentation.R
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private const val CAN_CAMERA_LOG_TAG = "CanCameraScanner"
private const val REQUIRED_CANDIDATE_HITS = 2

private val CanGuide = CanGuideArea(
    leftFraction = 0.15f,
    topFraction = 0.35f,
    rightFraction = 0.85f,
    bottomFraction = 0.75f
)

/**
 * Diálogo a pantalla completa para leer el código CAN con la cámara.
 *
 * @param onDismiss Cierra el escáner sin modificar el código CAN
 * @param onCanDetected Devuelve el código CAN detectado (6 dígitos)
 */
@Composable
internal fun CanCameraScannerDialog(
    onDismiss: () -> Unit,
    onCanDetected: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            CanCameraScannerContent(
                onDismiss = onDismiss,
                onCanDetected = onCanDetected
            )
        }
    }
}

@Composable
private fun CanCameraScannerContent(
    onDismiss: () -> Unit,
    onCanDetected: (String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    var cameraError by remember { mutableStateOf<String?>(null) }
    var cameraSession by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            key(cameraSession) {
                CameraScannerPreview(
                    onCanDetected = onCanDetected,
                    onCameraError = { cameraError = it }
                )
            }
            ScannerOverlay(onClose = onDismiss)
        } else {
            PermissionRequiredContent(
                onGrantClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onCloseClick = onDismiss
            )
        }

        cameraError?.let { message ->
            CanCameraErrorDialog(
                message = message,
                onRetry = {
                    cameraError = null
                    cameraSession += 1
                },
                onClose = onDismiss
            )
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraScannerPreview(
    onCanDetected: (String) -> Unit,
    onCameraError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    val analysisRef = remember { AtomicReference<ImageAnalysis?>(null) }
    val isProcessing = remember { AtomicBoolean(false) }
    val isFinished = remember { AtomicBoolean(false) }
    val isDisposed = remember { AtomicBoolean(false) }

    var lastCandidate by remember { mutableStateOf<String?>(null) }
    var candidateHits by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        onDispose {
            isDisposed.set(true)
            analysisRef.get()?.clearAnalyzer()
            analysisRef.set(null)
            runCatching { cameraProviderFuture.get().unbindAll() }
            recognizer.close()
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                contentDescription = ctx.getString(R.string.can_camera_preview_description)
            }

            cameraProviderFuture.addListener({
                if (isDisposed.get()) return@addListener
                runCatching {
                    val provider = cameraProviderFuture.get()
                    val preview = CameraXPreview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setResolutionSelector(
                            ResolutionSelector.Builder()
                                .setResolutionStrategy(
                                    ResolutionStrategy(
                                        Size(1280, 720),
                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                    )
                                )
                                .build()
                        )
                        .build()

                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        analyzeCanFrame(
                            imageProxy = imageProxy,
                            recognizer = recognizer,
                            isProcessing = isProcessing,
                            isFinished = isFinished,
                            onCodeConfirmed = { code ->
                                if (code == lastCandidate) {
                                    candidateHits += 1
                                } else {
                                    lastCandidate = code
                                    candidateHits = 1
                                }
                                if (
                                    candidateHits >= REQUIRED_CANDIDATE_HITS &&
                                    isFinished.compareAndSet(false, true)
                                ) {
                                    onCanDetected(code)
                                }
                            },
                            onCandidateReset = {
                                lastCandidate = null
                                candidateHits = 0
                            }
                        )
                    }

                    analysisRef.set(analysis)
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                }.onFailure { error ->
                    Log.e(CAN_CAMERA_LOG_TAG, "No se pudo iniciar la cámara", error)
                    onCameraError(context.getString(R.string.can_camera_init_error))
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

@OptIn(ExperimentalGetImage::class)
private fun analyzeCanFrame(
    imageProxy: ImageProxy,
    recognizer: TextRecognizer,
    isProcessing: AtomicBoolean,
    isFinished: AtomicBoolean,
    onCodeConfirmed: (String) -> Unit,
    onCandidateReset: () -> Unit
) {
    if (isFinished.get() || !isProcessing.compareAndSet(false, true)) {
        imageProxy.close()
        return
    }

    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        isProcessing.set(false)
        imageProxy.close()
        return
    }

    val rotation = imageProxy.imageInfo.rotationDegrees
    val uprightWidth = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
    val uprightHeight = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height
    val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

    recognizer.process(inputImage)
        .addOnSuccessListener { text ->
            if (isFinished.get()) return@addOnSuccessListener

            val lines = text.textBlocks.flatMap { block ->
                block.lines.mapNotNull { line ->
                    val box = line.boundingBox ?: return@mapNotNull null
                    CanTextLine(
                        text = line.text,
                        left = box.left,
                        top = box.top,
                        right = box.right,
                        bottom = box.bottom
                    )
                }
            }

            val code = CanCodeDetector.detect(
                lines = lines,
                imageWidth = uprightWidth,
                imageHeight = uprightHeight,
                guideArea = CanGuide
            )

            if (code != null) {
                onCodeConfirmed(code)
            } else {
                onCandidateReset()
            }
        }
        .addOnFailureListener { error ->
            Log.w(CAN_CAMERA_LOG_TAG, "Error del OCR al leer el CAN", error)
        }
        .addOnCompleteListener {
            isProcessing.set(false)
            imageProxy.close()
        }
}

@Composable
private fun ScannerOverlay(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.can_camera_dialog_title),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.can_camera_instruction),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.8f)
                .aspectRatio(1.6f)
                .border(3.dp, Color.White, RoundedCornerShape(16.dp))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(color = Color.White)
            Text(
                text = stringResource(R.string.can_camera_searching),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            OutlinedButton(
                onClick = onClose,
                border = BorderStroke(1.dp, Color.White),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text(stringResource(R.string.can_camera_close))
            }
        }
    }
}

@Composable
private fun PermissionRequiredContent(
    onGrantClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.can_camera_permission_message),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = onGrantClick,
            border = BorderStroke(1.dp, Color.White),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text(stringResource(R.string.can_camera_grant_permission))
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onCloseClick) {
            Text(stringResource(R.string.can_camera_close), color = Color.White)
        }
    }
}

@Composable
private fun CanCameraErrorDialog(
    message: String,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    AccessibleAlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.can_camera_dialog_title)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.can_camera_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.can_camera_close))
            }
        }
    )
}
