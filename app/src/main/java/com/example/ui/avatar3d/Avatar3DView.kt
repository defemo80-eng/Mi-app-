package com.example.ui.avatar3d

import android.opengl.GLSurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.domain.model.AvatarParams
import com.example.domain.model.StoreCategory
import com.example.ui.theme.DeFittBlack
import com.example.ui.theme.DeFittOutline
import com.example.ui.theme.DeFittPureBlack
import com.example.ui.theme.DeFittSurfaceElevated
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import kotlin.math.abs
import kotlin.math.cos

@Composable
fun Avatar3DView(
    avatarParams: AvatarParams,
    equippedItemIds: List<String>,
    modifier: Modifier = Modifier,
    autoRotate: Boolean = false,
    showRotateHint: Boolean = true,
    showAngleControls: Boolean = true,
    testTag: String = "avatar_3d_view"
) {
    val isPreview = LocalInspectionMode.current
    val isRobolectric = remember {
        android.os.Build.FINGERPRINT.contains("robolectric", ignoreCase = true)
    }

    var yaw by remember { mutableFloatStateOf(16f) }
    var pitch by remember { mutableFloatStateOf(6f) }
    var autoSpin by remember(autoRotate) { mutableStateOf(autoRotate) }

    val equippedMap = remember(equippedItemIds) {
        Avatar3DRenderer.resolveEquippedMap(equippedItemIds)
    }

    Box(
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF132C10), Color(0xFF070C07), DeFittBlack)
                )
            )
            .border(1.dp, NeonGreen.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    autoSpin = false
                    yaw = (yaw + dragAmount.x * 0.72f) % 360f
                    pitch = (pitch - dragAmount.y * 0.25f).coerceIn(-12f, 26f)
                }
            }
    ) {
        if (isPreview || isRobolectric) {
            AvatarFallback2DCanvas(
                avatarParams = avatarParams,
                equippedMap = equippedMap,
                yawDegrees = yaw,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val renderer = remember { Avatar3DRenderer() }
            renderer.avatarParams = avatarParams
            renderer.equippedItems = equippedMap
            renderer.autoRotate = autoSpin
            if (!autoSpin) {
                renderer.targetYawDegrees = yaw
            }
            renderer.targetPitchDegrees = pitch

            var surfaceViewRef: GLSurfaceView? = remember { null }

            DisposableEffect(Unit) {
                onDispose {
                    surfaceViewRef?.onPause()
                }
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    GLSurfaceView(ctx).apply {
                        setEGLContextClientVersion(2)
                        setRenderer(renderer)
                        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                        surfaceViewRef = this
                    }
                },
                update = { view ->
                    renderer.avatarParams = avatarParams
                    renderer.equippedItems = equippedMap
                    renderer.autoRotate = autoSpin
                    if (!autoSpin) renderer.targetYawDegrees = yaw
                    renderer.targetPitchDegrees = pitch
                    view.requestRender()
                }
            )
        }

        if (showAngleControls) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .background(DeFittSurfaceElevated.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                    .border(1.dp, DeFittOutline, RoundedCornerShape(16.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnglePresetChip("Frente", !autoSpin && abs(yaw % 360f) < 25f) {
                    autoSpin = false
                    yaw = 0f
                    pitch = 6f
                }
                AnglePresetChip("Lado", !autoSpin && abs((yaw - 90f) % 360f) < 25f) {
                    autoSpin = false
                    yaw = 90f
                    pitch = 6f
                }
                AnglePresetChip("Espalda", !autoSpin && abs((yaw - 180f) % 360f) < 25f) {
                    autoSpin = false
                    yaw = 180f
                    pitch = 6f
                }
                AnglePresetChip("360°", autoSpin) {
                    autoSpin = !autoSpin
                }
            }
        } else if (showRotateHint) {
            Text(
                text = "Desliza para girar 360°",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .background(DeFittSurfaceElevated.copy(alpha = 0.78f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun AnglePresetChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) NeonGreen else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) DeFittPureBlack else TextWhite,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun AvatarFallback2DCanvas(
    avatarParams: AvatarParams,
    equippedMap: Map<StoreCategory, com.example.domain.model.StoreItem>,
    yawDegrees: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = size.minDimension / 240f
        val cosY = cos(Math.toRadians(yawDegrees.toDouble())).toFloat()
        val widthFactor = 0.45f + 0.55f * abs(cosY)
        val shoulderHalf = 30f * avatarParams.shoulderWidthRatio * scale * widthFactor
        val waistHalf = 20f * avatarParams.waistRatio * scale * widthFactor

        val skinColor = runCatching {
            Color(android.graphics.Color.parseColor(avatarParams.skinToneHex))
        }.getOrDefault(Color(0xFFD69E7A))
        val topItem = equippedMap[StoreCategory.CONJUNTOS_DEPORTIVOS]
            ?: equippedMap[StoreCategory.SUDADERAS]
            ?: equippedMap[StoreCategory.CAMISETAS]
        val pantsItem = equippedMap[StoreCategory.CONJUNTOS_DEPORTIVOS]
            ?: equippedMap[StoreCategory.PANTALONES]
        val topColor = topItem?.let {
            runCatching { Color(android.graphics.Color.parseColor(it.primaryColorHex)) }.getOrNull()
        } ?: Color(0xFF1A221A)
        val pantsColor = pantsItem?.let {
            runCatching { Color(android.graphics.Color.parseColor(it.primaryColorHex)) }.getOrNull()
        } ?: Color(0xFF151B18)

        // Floor shadow + neon ring
        drawOval(
            color = Color.Black.copy(alpha = 0.55f),
            topLeft = Offset(cx - 40f * scale, cy + 80f * scale),
            size = Size(80f * scale, 16f * scale)
        )
        drawOval(
            color = NeonGreen,
            topLeft = Offset(cx - 60f * scale, cy + 76f * scale),
            size = Size(120f * scale, 22f * scale),
            style = Stroke(width = 3f * scale)
        )
        if (equippedMap.containsKey(StoreCategory.MOCHILAS) && cosY < 0.3f) {
            drawRoundRect(
                color = Color(0xFF283028),
                topLeft = Offset(cx - 20f * scale, cy - 36f * scale),
                size = Size(40f * scale, 50f * scale),
                cornerRadius = CornerRadius(10f * scale, 10f * scale)
            )
        }
        // Head & Neck
        drawRoundRect(
            color = skinColor,
            topLeft = Offset(cx - 7f * scale, cy - 52f * scale),
            size = Size(14f * scale, 14f * scale),
            cornerRadius = CornerRadius(4f * scale, 4f * scale)
        )
        drawOval(
            color = skinColor,
            topLeft = Offset(cx - 16f * scale, cy - 86f * scale),
            size = Size(32f * scale, 38f * scale)
        )
        if (equippedMap.containsKey(StoreCategory.GORRAS)) {
            drawArc(
                color = NeonGreen,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - 18f * scale, cy - 90f * scale),
                size = Size(36f * scale, 22f * scale)
            )
        }
        // Torso & Legs
        drawRoundRect(
            color = topColor,
            topLeft = Offset(cx - shoulderHalf, cy - 40f * scale),
            size = Size(shoulderHalf * 2f, 52f * scale),
            cornerRadius = CornerRadius(12f * scale, 12f * scale)
        )
        drawRoundRect(
            color = pantsColor,
            topLeft = Offset(cx - waistHalf, cy + 10f * scale),
            size = Size(waistHalf * 2f, 70f * scale),
            cornerRadius = CornerRadius(8f * scale, 8f * scale)
        )
    }
}
