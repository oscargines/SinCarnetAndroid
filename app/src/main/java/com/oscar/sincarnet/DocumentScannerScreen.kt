package com.oscar.sincarnet

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PointF
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private enum class ScanPhase { PREVIEW, ADJUST, SAVING }
private enum class ScannableDocument {
    IDENTITY,
    DRIVING_LICENSE,
    CIRCULATION_PERMIT,
    ITV_CARD
}

private enum class CaptureTarget {
    IDENTITY_FRONT,
    IDENTITY_BACK,
    DRIVING_LICENSE_FRONT,
    DRIVING_LICENSE_BACK,
    CIRCULATION_PERMIT_BACK,
    CIRCULATION_PERMIT_FRONT,
    ITV_CARD
}

private val ScanBarBg = Color(0xEE0D0D1A)
private val ScanAccent = Color(0xFFFFD600)
private val ScanButtonSave = Color(0xFF4A148C)
private const val SCAN_LOG_TAG = "DocumentScannerFlow"

@Composable
internal fun DocumentScannerScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onOpenPdf: (File) -> Unit,
    onSharePdf: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasIdentity by remember { mutableStateOf(false) }
    var hasDrivingLicense by remember { mutableStateOf(false) }
    var hasCirculationPermit by remember { mutableStateOf(false) }
    var hasItvCard by remember { mutableStateOf(false) }

    val scannedByType = remember { mutableStateMapOf<ScannableDocument, android.graphics.Bitmap>() }
    var identityFrontBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var identityBackBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var drivingFrontBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var drivingBackBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var circulationBackBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var circulationFrontBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var generatedPdf by remember { mutableStateOf<File?>(null) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var activeCapture by remember { mutableStateOf<CaptureTarget?>(null) }
    var pendingCapture by remember { mutableStateOf<CaptureTarget?>(null) }

    // Boton atras del sistema en el panel principal del escaner.
    BackHandler(enabled = activeCapture == null) {
        onBackClick()
    }

    fun isEnabled(doc: ScannableDocument): Boolean = when (doc) {
        ScannableDocument.IDENTITY -> hasIdentity
        ScannableDocument.DRIVING_LICENSE -> hasDrivingLicense
        ScannableDocument.CIRCULATION_PERMIT -> hasCirculationPermit
        ScannableDocument.ITV_CARD -> hasItvCard
    }

    fun removeIfDisabled(doc: ScannableDocument, enabled: Boolean) {
        if (!enabled) {
            when (doc) {
                ScannableDocument.IDENTITY -> {
                    identityFrontBitmap = null
                    identityBackBitmap = null
                }
                ScannableDocument.DRIVING_LICENSE -> {
                    drivingFrontBitmap = null
                    drivingBackBitmap = null
                }
                ScannableDocument.CIRCULATION_PERMIT -> {
                    circulationBackBitmap = null
                    circulationFrontBitmap = null
                }
                ScannableDocument.ITV_CARD -> scannedByType.remove(doc)
            }
            generatedPdf = null
        }
    }

    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text(stringResource(R.string.scan_error_title)) },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { errorMessage = null }) {
                    Text(stringResource(R.string.close_action))
                }
            }
        )
    }

    pendingCapture?.let {
        AlertDialog(
            onDismissRequest = { pendingCapture = null },
            title = { Text(stringResource(R.string.scan_capture_info_title)) },
            text = { Text(stringResource(R.string.scan_capture_info_message)) },
            confirmButton = {
                Button(onClick = {
                    activeCapture = pendingCapture
                    pendingCapture = null
                }) {
                    Text(stringResource(R.string.scan_open_camera_action))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingCapture = null }) {
                    Text(stringResource(R.string.cancel_action))
                }
            }
        )
    }

    if (activeCapture != null) {
        val currentDoc = activeCapture ?: return
        key(currentDoc) {
            CaptureDocumentScreen(
                modifier = modifier,
                title = when (currentDoc) {
                    CaptureTarget.IDENTITY_FRONT -> stringResource(R.string.scan_doc_identity_front_short)
                    CaptureTarget.IDENTITY_BACK -> stringResource(R.string.scan_doc_identity_back_short)
                    CaptureTarget.DRIVING_LICENSE_FRONT -> stringResource(R.string.scan_doc_driving_front_short)
                    CaptureTarget.DRIVING_LICENSE_BACK -> stringResource(R.string.scan_doc_driving_back_short)
                    CaptureTarget.CIRCULATION_PERMIT_BACK -> stringResource(R.string.scan_doc_circulation_back_short)
                    CaptureTarget.CIRCULATION_PERMIT_FRONT -> stringResource(R.string.scan_doc_circulation_front_short)
                    CaptureTarget.ITV_CARD -> stringResource(R.string.scan_doc_itv_short)
                },
                onBackClick = { activeCapture = null },
                onBitmapScanned = { bitmap ->
                    val target = activeCapture ?: return@CaptureDocumentScreen
                    Log.d(
                        SCAN_LOG_TAG,
                        "onBitmapScanned target=$target bitmap=${bitmap.width}x${bitmap.height}"
                    )
                    when (target) {
                        CaptureTarget.IDENTITY_FRONT -> {
                            identityFrontBitmap = bitmap
                            // Tras el anverso, obligamos captura de reverso.
                            activeCapture = CaptureTarget.IDENTITY_BACK
                            generatedPdf = null
                            Log.d(SCAN_LOG_TAG, "Identity front saved -> switching to IDENTITY_BACK")
                            return@CaptureDocumentScreen
                        }
                        CaptureTarget.IDENTITY_BACK -> {
                            identityBackBitmap = bitmap
                            Log.d(SCAN_LOG_TAG, "Identity back saved")
                        }
                        CaptureTarget.DRIVING_LICENSE_FRONT -> {
                            drivingFrontBitmap = bitmap
                            activeCapture = CaptureTarget.DRIVING_LICENSE_BACK
                            generatedPdf = null
                            Log.d(SCAN_LOG_TAG, "Driving front saved -> switching to DRIVING_LICENSE_BACK")
                            return@CaptureDocumentScreen
                        }
                        CaptureTarget.DRIVING_LICENSE_BACK -> {
                            drivingBackBitmap = bitmap
                            Log.d(SCAN_LOG_TAG, "Driving back saved")
                        }
                        CaptureTarget.CIRCULATION_PERMIT_BACK -> {
                            circulationBackBitmap = bitmap
                            activeCapture = CaptureTarget.CIRCULATION_PERMIT_FRONT
                            generatedPdf = null
                            Log.d(SCAN_LOG_TAG, "Circulation back saved -> switching to CIRCULATION_PERMIT_FRONT")
                            return@CaptureDocumentScreen
                        }
                        CaptureTarget.CIRCULATION_PERMIT_FRONT -> {
                            circulationFrontBitmap = bitmap
                            Log.d(SCAN_LOG_TAG, "Circulation front saved")
                        }
                        CaptureTarget.ITV_CARD -> scannedByType[ScannableDocument.ITV_CARD] = bitmap
                    }
                    generatedPdf = null
                    activeCapture = null
                    Log.d(SCAN_LOG_TAG, "Capture flow closed for target=$target")
                }
            )
        }
        return
    }

    val hasIdentityBothSides = identityFrontBitmap != null && identityBackBitmap != null
    val identityFrontDone = identityFrontBitmap != null
    val identityBackDone = identityBackBitmap != null
    val drivingFrontDone = drivingFrontBitmap != null
    val drivingBackDone = drivingBackBitmap != null
    val circulationBackDone = circulationBackBitmap != null
    val circulationFrontDone = circulationFrontBitmap != null
    val itvDone = scannedByType[ScannableDocument.ITV_CARD] != null
    val hasDrivingBothSides = drivingFrontBitmap != null && drivingBackBitmap != null
    val hasCirculationBothSides = circulationBackBitmap != null && circulationFrontBitmap != null
    val drivingReady = !hasDrivingLicense || hasDrivingBothSides
    val identityReady = !hasIdentity || hasIdentityBothSides
    val circulationReady = !hasCirculationPermit || hasCirculationBothSides
    val atLeastOneScanned =
        (scannedByType[ScannableDocument.ITV_CARD] != null) || hasIdentityBothSides || hasDrivingBothSides || hasCirculationBothSides
    val canGenerate = atLeastOneScanned && identityReady && drivingReady && circulationReady && !isGeneratingPdf
    val canShare = generatedPdf != null

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(15.dp))
            Text(
                text = stringResource(R.string.scan_documents_available_title),
                style = MaterialTheme.typography.titleMedium
            )

            SwitchRow(
                label = stringResource(R.string.scan_switch_identity),
                checked = hasIdentity,
                onCheckedChange = {
                    hasIdentity = it
                    removeIfDisabled(ScannableDocument.IDENTITY, it)
                }
            )
            SwitchRow(
                label = stringResource(R.string.scan_switch_driving),
                checked = hasDrivingLicense,
                onCheckedChange = {
                    hasDrivingLicense = it
                    removeIfDisabled(ScannableDocument.DRIVING_LICENSE, it)
                }
            )
            SwitchRow(
                label = stringResource(R.string.scan_switch_circulation),
                checked = hasCirculationPermit,
                onCheckedChange = {
                    hasCirculationPermit = it
                    removeIfDisabled(ScannableDocument.CIRCULATION_PERMIT, it)
                }
            )
            SwitchRow(
                label = stringResource(R.string.scan_switch_itv),
                checked = hasItvCard,
                onCheckedChange = {
                    hasItvCard = it
                    removeIfDisabled(ScannableDocument.ITV_CARD, it)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { pendingCapture = CaptureTarget.IDENTITY_FRONT },
                enabled = isEnabled(ScannableDocument.IDENTITY),
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.scan_doc_identity_button)) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (identityFrontDone) {
                    Text(
                        text = stringResource(R.string.scan_identity_front_done),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32)
                    )
                }
                if (identityFrontDone && identityBackDone) {
                    Spacer(modifier = Modifier.size(18.dp))
                }
                if (identityBackDone) {
                    Text(
                        text = stringResource(R.string.scan_identity_back_done),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Button(
                onClick = { pendingCapture = CaptureTarget.DRIVING_LICENSE_FRONT },
                enabled = isEnabled(ScannableDocument.DRIVING_LICENSE),
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.scan_doc_driving_button)) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (drivingFrontDone) {
                    Text(
                        text = stringResource(R.string.scan_driving_front_done),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32)
                    )
                }
                if (drivingFrontDone && drivingBackDone) {
                    Spacer(modifier = Modifier.size(18.dp))
                }
                if (drivingBackDone) {
                    Text(
                        text = stringResource(R.string.scan_driving_back_done),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Button(
                onClick = { pendingCapture = CaptureTarget.CIRCULATION_PERMIT_BACK },
                enabled = isEnabled(ScannableDocument.CIRCULATION_PERMIT),
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.scan_doc_circulation_button)) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (circulationBackDone) {
                    Text(
                        text = stringResource(R.string.scan_circulation_back_done),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32)
                    )
                }
                if (circulationBackDone && circulationFrontDone) {
                    Spacer(modifier = Modifier.size(18.dp))
                }
                if (circulationFrontDone) {
                    Text(
                        text = stringResource(R.string.scan_circulation_front_done),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Button(
                onClick = { pendingCapture = CaptureTarget.ITV_CARD },
                enabled = isEnabled(ScannableDocument.ITV_CARD),
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.scan_doc_itv_button)) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (itvDone) {
                    Text(
                        text = stringResource(R.string.scan_itv_done),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            Button(
                onClick = {
                    isGeneratingPdf = true
                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            val orderedScans = listOf(
                                ScannableDocument.ITV_CARD
                            ).mapNotNull { scannedByType[it] }
                            val numeroDiligencias = JuzgadoAtestadoStorage(context)
                                .loadCurrent()
                                .numeroDiligencias
                            DocumentScanUtils.saveScansToPdf(
                                context = context,
                                scans = orderedScans,
                                outputName = "documentos_escaneados_${System.currentTimeMillis()}.pdf",
                                numeroDiligencias = numeroDiligencias,
                                identityFront = identityFrontBitmap,
                                identityBack = identityBackBitmap,
                                identityTitle = context.getString(R.string.scan_identity_pdf_title),
                                drivingFront = drivingFrontBitmap,
                                drivingBack = drivingBackBitmap,
                                drivingTitle = context.getString(R.string.scan_driving_pdf_title),
                                circulationBack = circulationBackBitmap,
                                circulationFront = circulationFrontBitmap,
                                circulationTitle = context.getString(R.string.scan_circulation_pdf_title),
                                frontLabel = context.getString(R.string.scan_doc_front_label),
                                backLabel = context.getString(R.string.scan_doc_back_label)
                            )
                        }.onSuccess { file ->
                            withContext(Dispatchers.Main) {
                                generatedPdf = file
                                isGeneratingPdf = false
                                onOpenPdf(file)
                            }
                        }.onFailure { e ->
                            withContext(Dispatchers.Main) {
                                isGeneratingPdf = false
                                errorMessage = context.getString(
                                    R.string.scan_generate_pdf_error,
                                    e.message ?: context.getString(R.string.scan_unknown_error)
                                )
                            }
                        }
                    }
                },
                enabled = canGenerate,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ScanButtonSave)
            ) {
                if (isGeneratingPdf) {
                    Text(stringResource(R.string.scan_generating_pdf))
                } else {
                    Text(stringResource(R.string.scan_generate_pdf_action))
                }
            }

            Button(
                onClick = {
                    val file = generatedPdf ?: return@Button
                    onSharePdf(file)
                },
                enabled = canShare,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.scan_share_pdf_action)) }

            // Espacio para que el contenido no quede tapado por el boton sticky.
            Spacer(modifier = Modifier.height(84.dp))
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackIconButton(onClick = onBackClick)
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CaptureDocumentScreen(
    modifier: Modifier,
    title: String,
    onBackClick: () -> Unit,
    onBitmapScanned: (android.graphics.Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var phase by remember { mutableStateOf(ScanPhase.PREVIEW) }
    var capturedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var grayscaleEnabled by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var autoDetectApplied by remember { mutableStateOf<Boolean?>(null) }

    // Boton atras del sistema dentro del flujo de camara.
    // PREVIEW -> vuelve al panel de 6 botones.
    // ADJUST -> vuelve a previsualizacion de camara.
    // SAVING -> no permite salir hasta terminar procesamiento.
    BackHandler {
        when (phase) {
            ScanPhase.PREVIEW -> onBackClick()
            ScanPhase.ADJUST -> phase = ScanPhase.PREVIEW
            ScanPhase.SAVING -> Unit
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permLauncher.launch(Manifest.permission.CAMERA)
    }

    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text(stringResource(R.string.scan_error_title)) },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { errorMessage = null }) {
                    Text(stringResource(R.string.close_action))
                }
            }
        )
    }

    if (phase == ScanPhase.SAVING) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xCC000000)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = Color.White)
                Text(
                    stringResource(R.string.scan_saving_document_for, title),
                    color = Color.White
                )
            }
        }
        return
    }

    if (phase == ScanPhase.PREVIEW) {
        if (!hasCameraPermission) {
            PermissionDeniedContent(
                modifier = modifier,
                onGrantClick = { permLauncher.launch(Manifest.permission.CAMERA) },
                onBackClick = onBackClick
            )
            return
        }

        Column(modifier = modifier.fillMaxSize()) {
            ScannerHintBanner(stringResource(R.string.scan_hint_place_on_dark_background, title))

            Box(Modifier
                .fillMaxWidth()
                .weight(1f)) {
                val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        cameraProviderFuture.addListener({
                            runCatching {
                                val provider = cameraProviderFuture.get()
                                val preview = CameraXPreview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture
                                )
                            }.onFailure { e ->
                                errorMessage = context.getString(
                                    R.string.scan_init_camera_error,
                                    e.message ?: context.getString(R.string.scan_unknown_error)
                                )
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.99f }
                ) {
                    val mH = size.width * 0.05f
                    val mV = size.height * 0.08f
                    val l = mH
                    val t = mV
                    val r = size.width - mH
                    val b = size.height - mV
                    drawRect(color = Color(0x99000000))
                    drawRect(
                        Color.Transparent,
                        Offset(l, t),
                        Size(r - l, b - t),
                        blendMode = BlendMode.Clear
                    )
                    drawRect(
                        ScanAccent,
                        Offset(l, t),
                        Size(r - l, b - t),
                        style = Stroke(width = 3.dp.toPx())
                    )
                    val cLen = 22.dp.toPx()
                    val cStroke = 4.dp.toPx()
                    listOf(
                        Triple(l, t, Pair(1f, 1f)),
                        Triple(r, t, Pair(-1f, 1f)),
                        Triple(r, b, Pair(-1f, -1f)),
                        Triple(l, b, Pair(1f, -1f))
                    ).forEach { (cx, cy, dir) ->
                        drawLine(
                            ScanAccent,
                            Offset(cx, cy),
                            Offset(cx + dir.first * cLen, cy),
                            cStroke,
                            StrokeCap.Round
                        )
                        drawLine(
                            ScanAccent,
                            Offset(cx, cy),
                            Offset(cx, cy + dir.second * cLen),
                            cStroke,
                            StrokeCap.Round
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScanBarBg)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = {
                    val tempFile =
                        File(context.cacheDir, "scan_cap_${System.currentTimeMillis()}.jpg")
                    val outputOpts = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                    imageCapture.takePicture(
                        outputOpts,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(out: ImageCapture.OutputFileResults) {
                                val bmp = DocumentScanUtils.readBitmapWithExif(tempFile)
                                if (bmp != null) {
                                    capturedBitmap = bmp
                                    phase = ScanPhase.ADJUST
                                } else {
                                    errorMessage = context.getString(R.string.scan_read_captured_error)
                                }
                            }

                            override fun onError(exc: ImageCaptureException) {
                                errorMessage = context.getString(
                                    R.string.scan_capture_error,
                                    exc.message ?: context.getString(R.string.scan_unknown_error)
                                )
                            }
                        }
                    )
                }) { Text(stringResource(R.string.scan_capture_action)) }
                BackIconButton(onClick = onBackClick)
            }
        }
        return
    }

    val bitmap = capturedBitmap ?: run { phase = ScanPhase.PREVIEW; return }
    var containerWPx by remember { mutableFloatStateOf(0f) }
    var containerHPx by remember { mutableFloatStateOf(0f) }
    val imgScale = if (containerWPx > 0f && containerHPx > 0f) {
        minOf(containerWPx / bitmap.width, containerHPx / bitmap.height)
    } else 1f
    val imgDispW = bitmap.width * imgScale
    val imgDispH = bitmap.height * imgScale
    val imgOffX = (containerWPx - imgDispW) / 2f
    val imgOffY = (containerHPx - imgDispH) / 2f
    val corners = remember {
        mutableStateListOf(
            Offset(0f, 0f),
            Offset(0f, 0f),
            Offset(0f, 0f),
            Offset(0f, 0f)
        )
    }

    LaunchedEffect(bitmap, containerWPx, containerHPx) {
        if (containerWPx <= 0f || containerHPx <= 0f) return@LaunchedEffect

        val scale2 = minOf(containerWPx / bitmap.width, containerHPx / bitmap.height)
        val dw = bitmap.width * scale2
        val dh = bitmap.height * scale2
        val ox = (containerWPx - dw) / 2f
        val oy = (containerHPx - dh) / 2f

        val autoCorners = DocumentScanUtils.detectDocumentCornersOnDarkBackground(bitmap)
        if (autoCorners != null) {
            autoDetectApplied = true
            fun bmpToScreen(p: android.graphics.PointF): Offset {
                val sx = ox + (p.x / bitmap.width) * dw
                val sy = oy + (p.y / bitmap.height) * dh
                return Offset(
                    x = sx.coerceIn(0f, containerWPx),
                    y = sy.coerceIn(0f, containerHPx)
                )
            }
            corners[0] = bmpToScreen(autoCorners[0])
            corners[1] = bmpToScreen(autoCorners[1])
            corners[2] = bmpToScreen(autoCorners[2])
            corners[3] = bmpToScreen(autoCorners[3])
        } else {
            autoDetectApplied = false
            // Fallback robusto al comportamiento manual previo.
            val inset = 0.07f
            corners[0] = Offset(ox + dw * inset, oy + dh * inset)
            corners[1] = Offset(ox + dw * (1f - inset), oy + dh * inset)
            corners[2] = Offset(ox + dw * (1f - inset), oy + dh * (1f - inset))
            corners[3] = Offset(ox + dw * inset, oy + dh * (1f - inset))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScannerHintBanner(stringResource(R.string.scan_hint_adjust_corners, title))

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .onGloballyPositioned { coords ->
                    containerWPx = coords.size.width.toFloat()
                    containerHPx = coords.size.height.toFloat()
                }
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            if (containerWPx > 0f) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(corners[0].x, corners[0].y)
                        lineTo(corners[1].x, corners[1].y)
                        lineTo(corners[2].x, corners[2].y)
                        lineTo(corners[3].x, corners[3].y)
                        close()
                    }
                    drawPath(path, color = Color(0x55FFD600), style = Fill)
                    drawPath(path, color = ScanAccent, style = Stroke(width = 2.5.dp.toPx()))
                }

                val handleSizePx = with(density) { 44.dp.toPx() }
                corners.forEachIndexed { i, corner ->
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (corner.x - handleSizePx / 2).roundToInt(),
                                    (corner.y - handleSizePx / 2).roundToInt()
                                )
                            }
                            .size(with(density) { handleSizePx.toDp() })
                            .background(ScanAccent, CircleShape)
                            .pointerInput(i) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    corners[i] = Offset(
                                        (corners[i].x + dragAmount.x).coerceIn(0f, containerWPx),
                                        (corners[i].y + dragAmount.y).coerceIn(0f, containerHPx)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (i + 1).toString(),
                            color = Color.Black,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(ScanBarBg)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.scan_grayscale_effect_label),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
                Switch(checked = grayscaleEnabled, onCheckedChange = { grayscaleEnabled = it })
            }

            autoDetectApplied?.let { applied ->
                Text(
                    text = if (applied) {
                        stringResource(R.string.scan_auto_adjust_applied)
                    } else {
                        stringResource(R.string.scan_manual_adjust)
                    },
                    color = if (applied) Color(0xFFB9F6CA) else Color(0xFFFFE082),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        capturedBitmap = null
                        autoDetectApplied = null
                        phase = ScanPhase.PREVIEW
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) { Text(stringResource(R.string.scan_repeat_photo_action)) }

                Button(
                    onClick = {
                        Log.d(
                            SCAN_LOG_TAG,
                            "Save scan pressed title=$title phase=$phase bitmap=${bitmap.width}x${bitmap.height}"
                        )
                        phase = ScanPhase.SAVING
                        scope.launch(Dispatchers.IO) {
                            runCatching {
                                fun screenToBmp(s: Offset): PointF {
                                    val bx = ((s.x - imgOffX) / imgDispW * bitmap.width).coerceIn(
                                        0f,
                                        bitmap.width.toFloat()
                                    )
                                    val by = ((s.y - imgOffY) / imgDispH * bitmap.height).coerceIn(
                                        0f,
                                        bitmap.height.toFloat()
                                    )
                                    return PointF(bx, by)
                                }

                                val ordered =
                                    DocumentScanUtils.orderCorners(corners.map { screenToBmp(it) })
                                val outW = maxOf(
                                    DocumentScanUtils.distance(ordered[0], ordered[1]),
                                    DocumentScanUtils.distance(ordered[3], ordered[2])
                                ).toInt().coerceIn(200, 4000)
                                val outH = maxOf(
                                    DocumentScanUtils.distance(ordered[0], ordered[3]),
                                    DocumentScanUtils.distance(ordered[1], ordered[2])
                                ).toInt().coerceIn(200, 5000)

                                var result = DocumentScanUtils.applyPerspectiveTransform(
                                    bitmap,
                                    ordered,
                                    outW,
                                    outH
                                )
                                if (grayscaleEnabled) result =
                                    DocumentScanUtils.enhanceScanBitmap(result)
                                Log.d(
                                    SCAN_LOG_TAG,
                                    "Processing success title=$title out=${result.width}x${result.height}"
                                )
                                result
                            }.onSuccess { processedBitmap ->
                                withContext(Dispatchers.Main) {
                                    runCatching {
                                        onBitmapScanned(processedBitmap)
                                    }.onSuccess {
                                        Log.d(
                                            SCAN_LOG_TAG,
                                            "onBitmapScanned callback OK title=$title"
                                        )
                                    }.onFailure { callbackError ->
                                        Log.e(
                                            SCAN_LOG_TAG,
                                            "onBitmapScanned callback FAILED title=$title",
                                            callbackError
                                        )
                                        errorMessage = context.getString(
                                            R.string.scan_process_error,
                                            callbackError.message ?: context.getString(R.string.scan_unknown_error)
                                        )
                                        phase = ScanPhase.ADJUST
                                    }
                                }
                            }.onFailure { e ->
                                withContext(Dispatchers.Main) {
                                    Log.e(SCAN_LOG_TAG, "Scan processing FAILED title=$title", e)
                                    errorMessage = context.getString(
                                        R.string.scan_process_error,
                                        e.message ?: context.getString(R.string.scan_unknown_error)
                                    )
                                    phase = ScanPhase.ADJUST
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ScanButtonSave)
                ) { Text(stringResource(R.string.scan_save_scan_action)) }
            }
        }
    }
}

@Composable
private fun ScannerHintBanner(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(ScanBarBg)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PermissionDeniedContent(
    modifier: Modifier = Modifier,
    onGrantClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.scan_camera_permission_required),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrantClick) { Text(stringResource(R.string.scan_grant_camera_permission)) }
        Spacer(Modifier.height(12.dp))
        BackIconButton(onClick = onBackClick)
    }
}

@ComposePreview(showBackground = true, showSystemUi = true)
@Composable
private fun DocumentScannerScreenPreview() {
    MaterialTheme {
        DocumentScannerScreen(
            onBackClick = {},
            onOpenPdf = {},
            onSharePdf = {}
        )
    }
}

