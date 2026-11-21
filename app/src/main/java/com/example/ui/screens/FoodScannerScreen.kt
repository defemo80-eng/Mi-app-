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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.ai.OfflineVisionEngines
import com.example.domain.catalog.OfflineFoodCatalog
import com.example.domain.model.FoodCatalogItem
import com.example.domain.model.MealType
import com.example.ui.theme.CodGold
import com.example.ui.theme.DeFittBlack
import com.example.ui.theme.DeFittOutline
import com.example.ui.theme.DeFittPureBlack
import com.example.ui.theme.DeFittSurface
import com.example.ui.theme.DeFittSurfaceElevated
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FoodScannerScreen(
    onSaveFood: (MealType, FoodCatalogItem, Int, String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPerm = granted }

    var selectedMeal by remember { mutableStateOf(MealType.ALMUERZO) }
    var selectedFood by remember { mutableStateOf(OfflineFoodCatalog.items.first()) }
    var portionGrams by remember { mutableIntStateOf(selectedFood.defaultPortionGrams) }
    var detectedLabelsText by remember { mutableStateOf("Apunta la cámara o busca un alimento manualmente.") }
    var recognizedByAi by remember { mutableStateOf(false) }
    var confidenceLabel by remember { mutableStateOf("Selección manual") }
    var searchQuery by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isProcessing = true
            scope.launch {
                val res = OfflineVisionEngines.recognizeFoodFromUri(context, uri)
                selectedFood = res.matchedItem
                portionGrams = res.matchedItem.defaultPortionGrams
                recognizedByAi = res.recognizedByAi
                confidenceLabel = if (res.recognizedByAi) {
                    "IA local (${res.topConfidencePercent}%): ${res.detectedLabels.joinToString(", ")}"
                } else {
                    "Sin etiquetas claras; verifica el alimento"
                }
                detectedLabelsText = confidenceLabel
                isProcessing = false
            }
        }
    }

    Scaffold(
        containerColor = DeFittBlack,
        topBar = {
            TopAppBar(
                title = { Text("Escáner Local de Alimentos", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("food_back_button")) {
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
                // CameraX Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(DeFittSurface)
                        .border(1.dp, NeonGreen.copy(alpha = 0.45f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPerm) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }
                                        val capture = ImageCapture.Builder().build()
                                        imageCaptureRef = capture
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            capture
                                        )
                                    } catch (_: Exception) {
                                        // Emulator or hardware without back camera
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            }
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, null, tint = NeonGreen, modifier = Modifier.size(34.dp))
                            Text(
                                "Permite acceso a la cámara para identificar comida con el modelo local.",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(
                                onClick = { permLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DeFittPureBlack),
                                modifier = Modifier.testTag("food_grant_camera_btn")
                            ) {
                                Text("Permitir cámara")
                            }
                        }
                    }
                }

                // Capture & Photo Picker actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        enabled = hasCameraPerm && !isProcessing,
                        onClick = {
                            val cap = imageCaptureRef ?: return@Button
                            isProcessing = true
                            cap.takePicture(
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val bmp = imageProxyToBitmap(image)
                                        image.close()
                                        scope.launch {
                                            val res = OfflineVisionEngines.recognizeFoodFromBitmap(bmp)
                                            selectedFood = res.matchedItem
                                            portionGrams = res.matchedItem.defaultPortionGrams
                                            recognizedByAi = res.recognizedByAi
                                            confidenceLabel = if (res.recognizedByAi) {
                                                "IA local (${res.topConfidencePercent}%): ${res.detectedLabels.joinToString(", ")}"
                                            } else {
                                                "No se detectó alimento claro; ajusta manualmente"
                                            }
                                            detectedLabelsText = confidenceLabel
                                            isProcessing = false
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        isProcessing = false
                                        detectedLabelsText = "Error de captura: ${exception.message}"
                                    }
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DeFittPureBlack),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("food_capture_btn")
                    ) {
                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(if (isProcessing) "Analizando..." else "Escanear comida")
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        border = BorderStroke(1.dp, DeFittOutline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("food_gallery_btn")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Elegir foto")
                    }
                }

                // AI status + Disclaimer
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeFittSurfaceElevated),
                    border = BorderStroke(1.dp, DeFittOutline),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(detectedLabelsText, style = MaterialTheme.typography.bodyMedium, color = NeonGreen)
                        Text(
                            "Estimación aproximada. Una fotografía no permite conocer todos los ingredientes ni sustituye evaluación profesional.",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarningAmber
                        )
                    }
                }

                // Meal Type Selector
                Text("Sección del día", style = MaterialTheme.typography.titleMedium, color = TextWhite)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MealType.entries.forEach { meal ->
                        FilterChip(
                            selected = selectedMeal == meal,
                            onClick = { selectedMeal = meal },
                            label = { Text(meal.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonGreen,
                                selectedLabelColor = DeFittPureBlack,
                                containerColor = DeFittSurface,
                                labelColor = TextWhite
                            ),
                            modifier = Modifier.testTag("meal_chip_${meal.name}")
                        )
                    }
                }

                // Selected Food + Portion Adjustment + Nutritional Estimate
                val factor = portionGrams / 100f
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeFittSurface),
                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(selectedFood.name, style = MaterialTheme.typography.titleLarge, color = TextWhite)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Porción aproximada:", color = TextSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { portionGrams = (portionGrams - 25).coerceAtLeast(25) },
                                    modifier = Modifier.testTag("portion_minus_btn")
                                ) {
                                    Icon(Icons.Default.Remove, "Disminuir", tint = NeonGreen)
                                }
                                Text("${portionGrams} g", style = MaterialTheme.typography.titleMedium, color = CodGold)
                                IconButton(
                                    onClick = { portionGrams = (portionGrams + 25).coerceAtMost(2000) },
                                    modifier = Modifier.testTag("portion_plus_btn")
                                ) {
                                    Icon(Icons.Default.Add, "Aumentar", tint = NeonGreen)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NutrientPill("Calorías", "${(selectedFood.caloriesPer100g * factor).toInt()} kcal")
                            NutrientPill("Proteínas", "${"%.1f".format(selectedFood.proteinPer100g * factor)}g")
                            NutrientPill("Carbs", "${"%.1f".format(selectedFood.carbsPer100g * factor)}g")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NutrientPill("Grasas", "${"%.1f".format(selectedFood.fatsPer100g * factor)}g")
                            NutrientPill("Azúcares", "${"%.1f".format(selectedFood.sugarsPer100g * factor)}g")
                            NutrientPill("Fibra", "${"%.1f".format(selectedFood.fiberPer100g * factor)}g")
                        }

                        Button(
                            onClick = {
                                onSaveFood(selectedMeal, selectedFood, portionGrams, confidenceLabel, recognizedByAi)
                                onBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DeFittPureBlack),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_food_btn")
                        ) {
                            Text("Guardar en ${selectedMeal.label}", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Manual Search / Correction List
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Corregir o buscar alimento en catálogo local") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = NeonGreen) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = DeFittOutline,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("food_search_input")
                )

                OfflineFoodCatalog.search(searchQuery).take(10).forEach { item ->
                    Card(
                        onClick = {
                            selectedFood = item
                            portionGrams = item.defaultPortionGrams
                            recognizedByAi = false
                            confidenceLabel = "Corrección/selección manual"
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedFood.id == item.id) DeFittSurfaceElevated else DeFittSurface
                        ),
                        border = BorderStroke(1.dp, if (selectedFood.id == item.id) NeonGreen else DeFittOutline),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.name, color = TextWhite, style = MaterialTheme.typography.bodyLarge)
                            Text("${item.caloriesPer100g.toInt()} kcal/100g", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NutrientPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = TextWhite)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val bmp = image.toBitmap()
    val rot = image.imageInfo.rotationDegrees
    if (rot == 0) return bmp
    val matrix = Matrix().apply { postRotate(rot.toFloat()) }
    return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
}
