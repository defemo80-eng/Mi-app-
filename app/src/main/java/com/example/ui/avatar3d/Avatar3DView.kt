package com.example.ui.avatar3d

import android.opengl.GLSurfaceView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.domain.model.AvatarParams
import com.example.domain.model.StoreCategory
import com.example.ui.theme.DeFittBlack
import com.example.ui.theme.DeFittSurfaceElevated
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextSecondary
import kotlin.math.cos

@Composable
fun Avatar3DView(
    avatarParams: AvatarParams,
    equippedItemIds: List<String>,
    modifier: Modifier = Modifier,
    autoRotate: Boolean = false,
    showRotateHint: Boolean = true,
    testTag: String = "avatar_3d_view"
) {
    val isPreview = LocalInspectionMode.current
    val isRobolectric = remember {
        android.os.Build.FINGERPRINT.contains("robolectric", ignoreCase = true)
    }

    var yaw by remember { mutableFloatStateOf(16f) }
    var pitch by remember { mutableFloatStateOf(8f) }

    val equippedMap = remember(equippedItemIds) {
        Avatar3DRenderer.resolveEquippedMap(equippedItemIds)
    }

    Box(
        modifier = modifier
            .testTag(testTag)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF10240E), DeFittBlack)
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    yaw = (yaw + dragAmount.x * 0.65f) % 360f
                    pitch = (pitch - dragAmount.y * 0.25f).coerceIn(-12f, 28f)
                }
            }
    ) {
        if (isPreview || isRobolectric) {
            // Safe deterministic Canvas projection during previews & Robolectric unit tests
            AvatarFallback2DCanvas(
                avatarParams = avatarParams,
                yawDegrees = yaw,
                hasBackpack = equippedMap.containsKey(StoreCategory.MOCHILAS),
                hasCap = equippedMap.containsKey(StoreCategory.GORRAS),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val renderer = remember { Avatar3DRenderer() }
            renderer.avatarParams = avatarParams
            renderer.equippedItems = equippedMap
            renderer.autoRotate = autoRotate
            if (!autoRotate) {
                renderer.yawDegrees = yaw
            }
            renderer.pitchDegrees = pitch

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
                    renderer.autoRotate = autoRotate
                    if (!autoRotate) renderer.yawDegrees = yaw
                    renderer.pitchDegrees = pitch
                    view.requestRender()
                }
            )
        }

        if (showRotateHint) {
            Text(
                text = "Desliza para girar 360°",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .background(DeFittSurfaceElevated.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun AvatarFallback2DCanvas(
    avatarParams: AvatarParams,
    yawDegrees: Float,
    hasBackpack: Boolean,
    hasCap: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = size.minDimension / 240f
        val cosY = cos(Math.toRadians(yawDegrees.toDouble())).toFloat()
        val shoulderHalf = 30f * avatarParams.shoulderWidthRatio * scale * (0.45f + 0.55f * kotlin.math.abs(cosY))
        val waistHalf = 20f * avatarParams.waistRatio * scale * (0.45f + 0.55f * kotlin.math.abs(cosY))

        drawOval(
            color = NeonGreen,
            topLeft = Offset(cx - 58f * scale, cy + 78f * scale),
            size = Size(116f * scale, 22f * scale),
            style = Stroke(width = 3f * scale)
        )
        if (hasBackpack && cosY < 0.2f) {
            drawRect(
                color = Color(0xFF283028),
                topLeft = Offset(cx - 18f * scale, cy - 35f * scale),
                size = Size(36f * scale, 48f * scale)
            )
        }
        drawCircle(
            color = Color(0xFFD69E7A),
            radius = 18f * scale,
            center = Offset(cx, cy - 68f * scale)
        )
        if (hasCap) {
            drawArc(
                color = NeonGreen,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - 19f * scale, cy - 88f * scale),
                size = Size(38f * scale, 22f * scale)
            )
        }
        drawRect(
            color = Color(0xFF1A221A),
            topLeft = Offset(cx - shoulderHalf, cy - 42f * scale),
            size = Size(shoulderHalf * 2f, 52f * scale)
        )
        drawRect(
            color = Color(0xFF151B18),
            topLeft = Offset(cx - waistHalf, cy + 10f * scale),
            size = Size(waistHalf * 2f, 72f * scale)
        )
    }
}
