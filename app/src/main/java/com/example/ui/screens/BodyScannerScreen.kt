package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.ai.OfflineVisionEngines
import com.example.domain.ai.PoseAngleMetrics
import com.example.domain.ai.PoseAnalysisResult
import com.example.domain.model.AvatarParams
import com.example.ui.avatar3d.Avatar3DView
import com.example.ui.theme.CodGold
import com.example.ui.theme.DeFittBlack
import com.example.ui.theme.DeFittOutline
import com.example.ui.theme.DeFittPureBlack
import com.example.ui.theme.DeFittSurface
import com.example.ui.theme.DeFittSurfaceElevated
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BodyScannerScreen(
    currentAvatar: AvatarParams,
    equippedIds: List<String>,
    onSaveScan: (
        scanType: String,
        anglesCaptured: Int,
        shoulderWidthRatio: Float,
        waistRatio: Float,
        hipWidthRatio: Float,
        limbThicknessRatio: Float,
        postureAlignmentScore: Float,
        estimatedBuildLabel: String,
        notes: String,
        heightCm: Int?,
        skinToneHex: String?,
        hairColorHex: String?,
        hairStyle: Int?
    ) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val hasCameraFeature = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPerm = granted }

    var useCameraMode by remember { mutableStateOf(hasCameraFeature) }
    var useFrontCamera by remember { mutableStateOf(true) }
    var activeAngleIndex by remember { mutableIntStateOf(0) } // 0=Frontal, 1=Lateral, 2=Posterior
    val angles = listOf("Frontal", "Lateral", "Posterior")
    var capturedAngles by remember { mutableStateOf<Map<String, PoseAngleMetrics>>(emptyMap()) }
    var scannerMessage by remember {
        mutableStateOf(
            if (hasCameraFeature) {
                "Captura 3 ángulos (Frontal, Lateral y Posterior) para reconstruir tus proporciones reales en 3D."
            } else {
                "Este dispositivo no reporta hardware de cámara compatible para escaneo 3D. Usa la calibración local manual."
            }
        )
    }
    var countdown by remember { mutableIntStateOf(0) }
    var isProcessing by remember { mutableStateOf(false) }
    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }

    // Manual Calibration state (always transparent when used)
    var heightCm by remember { mutableIntStateOf(currentAvatar.heightCm) }
    var shoulderRatio by remember { mutableFloatStateOf(currentAvatar.shoulderWidthRatio) }
    var waistRatio by remember { mutableFloatStateOf(currentAvatar.waistRatio) }
    var hipRatio by remember { mutableFloatStateOf(currentAvatar.hipWidthRatio) }
    var limbRatio by remember { mutableFloatStateOf(currentAvatar.limbThicknessRatio) }
    var postureScore by remember { mutableFloatStateOf(currentAvatar.postureAlignment) }
    var selectedSkin by remember { mutableStateOf(currentAvatar.skinToneHex) }
    var selectedHairColor by remember { mutableStateOf(currentAvatar.hairColorHex) }
    var selectedHairStyle by remember { mutableIntStateOf(currentAvatar.hairStyle) }

    val skinOptions = listOf("#F2C9AC", "#E0AC8B", "#D69E7A", "#B67856", "#8D5534", "#5C3824")
    val hairColors = listOf("#1D1B1A", "#4A3222", "#8C5828", "#D4B46A", "#7A7A7A")
    val hairLabels = listOf("Corto", "Degradado", "Largo", "Rapado")

    val processPoseResult: (PoseAnalysisResult) -> Unit = { res ->
        when (res) {
            is PoseAnalysisResult.Success -> {
                capturedAngles = capturedAngles + (res.metrics.angleName to res.metrics)
                scannerMessage = "Ángulo ${res.metrics.angleName} capturado (${(res.metrics.averageConfidence * 100).toInt()}% confianza)."
                if (activeAngleIndex < angles.lastIndex) {
                    activeAngleIndex++
                }
            }
            is PoseAnalysisResult.Incomplete -> {
                scannerMessage = res.reason
            }
        }
        isProcessing = false
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isProcessing = true
            scope.launch {
                val res = OfflineVisionEngines.analyzeBodyPoseUri(context, uri, angles[activeAngleIndex])
                processPoseResult(res)
            }
        }
    }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000L)
            if (countdown == 1) {
                countdown = 0
                val cap = imageCaptureRef
                if (cap != null) {
                    isProcessing = true
                    cap.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bmp = rotateBitmap(image)
                                image.close()
                                scope.launch {
                                    val res = OfflineVisionEngines.analyzeBodyPose(bmp, angles[activeAngleIndex])
                                    processPoseResult(res)
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                isProcessing = false
                                scannerMessage = "Error de captura: ${exception.message}"
                            }
                        }
                    )
                }
            } else {
                countdown--
            }
        }
    }

    Scaffold(
        containerColor = DeFittBlack,
        topBar = {
            TopAppBar(
                title = { Text("Escáner Corporal 3D", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("body_scan_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeFittBlack)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mode toggle: Escáner por Cámara vs Calibración Local
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = useCameraMode,
                        onClick = { useCameraMode = true },
                        label = { Text("Escáner Cámara IA") },
                        leadingIcon = { Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonGreen,
                            selectedLabelColor = DeFittPureBlack
                        ),
                        modifier = Modifier.weight(1f).testTag("scan_mode_camera")
                    )
                    FilterChip(
                        selected = !useCameraMode,
                        onClick = { useCameraMode = false },
                        label = { Text("Calibración Local") },
                        leadingIcon = { Icon(Icons.Default.Tune, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonGreen,
                            selectedLabelColor = DeFittPureBlack
                        ),
                        modifier = Modifier.weight(1f).testTag("scan_mode_manual")
                    )
                }

                if (useCameraMode) {
                    // Angle selector chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        angles.forEachIndexed { idx, angle ->
                            val captured = capturedAngles.containsKey(angle)
                            FilterChip(
                                selected = activeAngleIndex == idx,
                                onClick = { activeAngleIndex = idx },
                                label = { Text(if (captured) "✓ $angle" else angle) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (captured) SuccessGreen else NeonGreen,
                                    selectedLabelColor = DeFittPureBlack,
                                    containerColor = DeFittSurface,
                                    labelColor = if (captured) SuccessGreen else TextWhite
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DeFittSurface)
                            .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!hasCameraFeature) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(Icons.Default.AccessibilityNew, null, tint = WarningAmber, modifier = Modifier.size(36.dp))
                                Text(
                                    "Cámara no disponible en este equipo. Cambia a Calibración Local para configurar tu avatar sin simular escaneos.",
                                    color = TextSecondary
                                )
                            }
                        } else if (hasCameraPerm) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { ctx -> PreviewView(ctx) },
                                update = { previewView ->
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
                                    cameraProviderFuture.addListener({
                                        try {
                                            val provider = cameraProviderFuture.get()
                                            val preview = Preview.Builder().build().also {
                                                it.setSurfaceProvider(previewView.surfaceProvider)
                                            }
                                            val capture = ImageCapture.Builder().build()
                                            imageCaptureRef = capture
                                            val selector = if (useFrontCamera) {
                                                CameraSelector.DEFAULT_FRONT_CAMERA
                                            } else {
                                                CameraSelector.DEFAULT_BACK_CAMERA
                                            }
                                            provider.unbindAll()
                                            provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                                        } catch (_: Exception) {
                                        }
                                    }, ContextCompat.getMainExecutor(previewView.context))
                                }
                            )

                            // Translucent posture alignment guide overlay
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cx = size.width / 2f
                                val h = size.height
                                val guideColor = NeonGreen.copy(alpha = 0.42f)
                                val headR = h * 0.085f
                                drawCircle(
                                    color = guideColor,
                                    radius = headR,
                                    center = Offset(cx, h * 0.17f),
                                    style = Stroke(width = 3f)
                                )
                                val torsoW = if (activeAngleIndex == 1) h * 0.11f else h * 0.22f
                                drawRoundRect(
                                    color = guideColor,
                                    topLeft = Offset(cx - torsoW / 2f, h * 0.27f),
                                    size = Size(torsoW, h * 0.34f),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
                                    style = Stroke(width = 2.5f)
                                )
                                drawLine(
                                    color = guideColor,
                                    start = Offset(cx, h * 0.08f),
                                    end = Offset(cx, h * 0.92f),
                                    strokeWidth = 1.5f,
                                    cap = StrokeCap.Round
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DeFittBlack.copy(alpha = 0.7f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Guía: ${angles[activeAngleIndex]} (${capturedAngles.size}/3)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonGreen
                                )
                            }

                            IconButton(
                                onClick = { useFrontCamera = !useFrontCamera },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(DeFittBlack.copy(alpha = 0.65f), CircleShape)
                            ) {
                                Icon(Icons.Default.Cameraswitch, "Cambiar cámara", tint = TextWhite)
                            }

                            if (countdown > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .background(DeFittBlack.copy(alpha = 0.75f), CircleShape)
                                        .border(2.dp, NeonGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$countdown", style = MaterialTheme.typography.displayLarge, color = NeonGreen)
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, null, tint = NeonGreen, modifier = Modifier.size(36.dp))
                                Text("Autoriza la cámara para detectar tu postura corporal localmente.", color = TextSecondary)
                                Button(
                                    onClick = { permLauncher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DeFittPureBlack),
                                    modifier = Modifier.testTag("body_grant_camera_btn")
                                ) {
                                    Text("Permitir cámara")
                                }
                            }
                        }
                    }

                    Text(scannerMessage, style = MaterialTheme.typography.bodyMedium, color = NeonGreen)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            enabled = hasCameraFeature && hasCameraPerm && !isProcessing && countdown == 0,
                            onClick = { countdown = 4 },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DeFittPureBlack),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("body_capture_angle_btn")
                        ) {
                            Text(
                                if (isProcessing) "Midiendo..." else "Capturar ${angles[activeAngleIndex]} (4s)",
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            enabled = !isProcessing,
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            border = BorderStroke(1.dp, DeFittOutline),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Foto cuerpo")
                        }
                    }

                    if (capturedAngles.isNotEmpty()) {
                        val metricsList = capturedAngles.values.toList()
                        val avgShoulder = metricsList.map { it.shoulderToTorsoRatio }.average().toFloat()
                        val avgHip = metricsList.map { it.hipToTorsoRatio }.average().toFloat()
                        val avgPosture = metricsList.map { it.postureAlignment }.average().toFloat()

                        val normShoulder = (avgShoulder / 0.72f).coerceIn(0.84f, 1.22f)
                        val normHip = (avgHip / 0.54f).coerceIn(0.86f, 1.18f)
                        val normWaist = ((normShoulder * 0.45f) + (normHip * 0.55f)).coerceIn(0.82f, 1.20f)

                        Button(
                            onClick = {
                                onSaveScan(
                                    "CAMERA_POSE_MULTI_ANGLE",
                                    capturedAngles.size,
                                    normShoulder,
                                    normWaist,
                                    normHip,
                                    1.0f,
                                    avgPosture,
                                    "Reconstrucción pose local (${capturedAngles.size}/3 ángulos)",
                                    "Ángulos: ${capturedAngles.keys.joinToString(", ")}. Representación aproximada, no medición médica.",
                                    heightCm,
                                    selectedSkin,
                                    selectedHairColor,
                                    selectedHairStyle
                                )
                                onBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = DeFittPureBlack),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_camera_scan_btn")
                        ) {
                            Icon(Icons.Default.CheckCircle, null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Generar y Guardar Avatar 3D (${capturedAngles.size}/3 ángulos)")
                        }
                    }

                    Text(
                        "Nota: El análisis se ejecuta 100% en tu teléfono mediante detección de puntos corporales. No es una medición médica exacta.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                } else {
                    // Transparent Manual Local Calibration Fallback
                    val previewAvatar = currentAvatar.copy(
                        heightCm = heightCm,
                        shoulderWidthRatio = shoulderRatio,
                        waistRatio = waistRatio,
                        hipWidthRatio = hipRatio,
                        limbThicknessRatio = limbRatio,
                        postureAlignment = postureScore,
                        skinToneHex = selectedSkin,
                        hairColorHex = selectedHairColor,
                        hairStyle = selectedHairStyle
                    )

                    Avatar3DView(
                        avatarParams = previewAvatar,
                        equippedItemIds = equippedIds,
                        modifier = Modifier.fillMaxWidth().height(230.dp),
                        testTag = "manual_calibration_avatar_3d"
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DeFittSurface),
                        border = BorderStroke(1.dp, DeFittOutline),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Calibración Local de Apariencia", style = MaterialTheme.typography.titleMedium, color = NeonGreen)
                            Text(
                                "Si tu dispositivo no puede completar un escaneo por cámara o iluminación, ajusta tu representación directamente.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )

                            LabeledSlider("Estatura: $heightCm cm", heightCm.toFloat(), 145f..205f) { heightCm = it.toInt() }
                            LabeledSlider("Amplitud hombros: ${(shoulderRatio * 100).toInt()}%", shoulderRatio, 0.85f..1.20f) { shoulderRatio = it }
                            LabeledSlider("Contorno medio: ${(waistRatio * 100).toInt()}%", waistRatio, 0.84f..1.22f) { waistRatio = it }
                            LabeledSlider("Cadera: ${(hipRatio * 100).toInt()}%", hipRatio, 0.86f..1.18f) { hipRatio = it }
                            LabeledSlider("Postura alineada: ${(postureScore * 100).toInt()}%", postureScore, 0.72f..1.00f) { postureScore = it }

                            Text("Tono de piel", style = MaterialTheme.typography.labelLarge, color = TextWhite)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                skinOptions.forEach { hex ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(hex)))
                                            .border(
                                                width = if (selectedSkin == hex) 2.5.dp else 1.dp,
                                                color = if (selectedSkin == hex) NeonGreen else DeFittOutline,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedSkin = hex }
                                    )
                                }
                            }

                            Text("Cabello", style = MaterialTheme.typography.labelLarge, color = TextWhite)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                hairLabels.forEachIndexed { idx, label ->
                                    FilterChip(
                                        selected = selectedHairStyle == idx,
                                        onClick = { selectedHairStyle = idx },
                                        label = { Text(label) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                hairColors.forEach { hex ->
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(hex)))
                                            .border(
                                                width = if (selectedHairColor == hex) 2.5.dp else 1.dp,
                                                color = if (selectedHairColor == hex) NeonGreen else DeFittOutline,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedHairColor = hex }
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    onSaveScan(
                                        "MANUAL_CALIBRATION",
                                        0,
                                        shoulderRatio,
                                        waistRatio,
                                        hipRatio,
                                        limbRatio,
                                        postureScore,
                                        "Calibración local manual (${heightCm} cm)",
                                        "Configuración personal directa sin simulación de cámara.",
                                        heightCm,
                                        selectedSkin,
                                        selectedHairColor,
                                        selectedHairStyle
                                    )
                                    onBack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DeFittPureBlack),
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_manual_calibration_btn")
                            ) {
                                Text("Guardar Avatar Personal", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextWhite)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = NeonGreen,
                activeTrackColor = NeonGreen,
                inactiveTrackColor = DeFittSurfaceElevated
            )
        )
    }
}

private fun rotateBitmap(image: ImageProxy): Bitmap {
    val bmp = image.toBitmap()
    val rot = image.imageInfo.rotationDegrees
    if (rot == 0) return bmp
    val m = Matrix().apply { postRotate(rot.toFloat()) }
    return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
}
