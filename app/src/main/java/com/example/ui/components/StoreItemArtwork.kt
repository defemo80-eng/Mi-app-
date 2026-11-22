package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.StoreCategory
import com.example.domain.model.StoreItem
import com.example.ui.theme.DeFittOutline

/**
 * Custom offline studio product illustration for every single DeFitt Store item.
 * Replaces empty swatches with realistic, layered, category-specific garment & gear artwork.
 */
@Composable
fun StoreItemArtwork(
    item: StoreItem,
    modifier: Modifier = Modifier,
    size: Dp = 86.dp
) {
    val primary = parseColorSafe(item.primaryColorHex, Color(0xFF1A1E1A))
    val accent = parseColorSafe(item.accentColorHex, Color(0xFF39FF14))
    val highlight = Color.White.copy(alpha = 0.22f)
    val shadow = Color.Black.copy(alpha = 0.48f)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.16f),
                        Color(0xFF111711),
                        Color(0xFF070907)
                    )
                )
            )
            .border(1.dp, DeFittOutline, RoundedCornerShape(16.dp))
            .padding(6.dp)
            .testTag("store_artwork_${item.id}")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // Soft studio floor shadow
            drawOval(
                color = Color.Black.copy(alpha = 0.52f),
                topLeft = Offset(w * 0.16f, h * 0.84f),
                size = Size(w * 0.68f, h * 0.11f)
            )

            when (item.category) {
                StoreCategory.CAMISETAS -> drawShirt(w, h, primary, accent, highlight, shadow, item.styleVariant)
                StoreCategory.PANTALONES -> drawPants(w, h, primary, accent, highlight, shadow, item.styleVariant)
                StoreCategory.SUDADERAS -> drawHoodie(w, h, primary, accent, highlight, shadow, item.styleVariant)
                StoreCategory.ZAPATOS -> drawShoe(w, h, primary, accent, highlight, shadow, isAthletic = false, variant = item.styleVariant)
                StoreCategory.TENIS_DEPORTIVOS -> drawShoe(w, h, primary, accent, highlight, shadow, isAthletic = true, variant = item.styleVariant)
                StoreCategory.GORRAS -> drawCap(w, h, primary, accent, highlight, shadow)
                StoreCategory.GAFAS -> drawGlasses(w, h, primary, accent, highlight)
                StoreCategory.RELOJES -> drawWatch(w, h, primary, accent, highlight)
                StoreCategory.ACCESORIOS -> drawAccessory(w, h, primary, accent, highlight, item.styleVariant)
                StoreCategory.MOCHILAS -> drawBackpack(w, h, primary, accent, highlight, shadow)
                StoreCategory.CONJUNTOS_DEPORTIVOS -> drawTracksuit(w, h, primary, accent, highlight, shadow)
            }
        }
    }
}

private fun DrawScope.drawShirt(
    w: Float, h: Float, primary: Color, accent: Color, highlight: Color, shadow: Color, variant: Int
) {
    val isTank = variant == 2
    val bodyPath = Path().apply {
        if (isTank) {
            moveTo(w * 0.30f, h * 0.16f)
            lineTo(w * 0.38f, h * 0.16f)
            quadraticTo(w * 0.50f, h * 0.30f, w * 0.62f, h * 0.16f)
            lineTo(w * 0.70f, h * 0.16f)
            quadraticTo(w * 0.68f, h * 0.34f, w * 0.74f, h * 0.42f)
            lineTo(w * 0.72f, h * 0.84f)
            lineTo(w * 0.28f, h * 0.84f)
            lineTo(w * 0.26f, h * 0.42f)
            quadraticTo(w * 0.32f, h * 0.34f, w * 0.30f, h * 0.16f)
        } else {
            moveTo(w * 0.34f, h * 0.16f)
            quadraticTo(w * 0.50f, h * 0.26f, w * 0.66f, h * 0.16f)
            lineTo(w * 0.90f, h * 0.30f)
            lineTo(w * 0.83f, h * 0.47f)
            lineTo(w * 0.72f, h * 0.41f)
            lineTo(w * 0.73f, h * 0.84f)
            lineTo(w * 0.27f, h * 0.84f)
            lineTo(w * 0.28f, h * 0.41f)
            lineTo(w * 0.17f, h * 0.47f)
            lineTo(w * 0.10f, h * 0.30f)
        }
        close()
    }
    drawPath(bodyPath, Brush.linearGradient(listOf(primary.lighter(0.14f), primary, primary.darker(0.18f))))
    drawPath(bodyPath, accent.copy(alpha = 0.55f), style = Stroke(width = 2.2f, join = StrokeJoin.Round))

    // Collar trim
    drawArc(
        color = accent,
        startAngle = 15f,
        sweepAngle = 150f,
        useCenter = false,
        topLeft = Offset(w * 0.35f, h * 0.11f),
        size = Size(w * 0.30f, h * 0.14f),
        style = Stroke(width = 3.2f, cap = StrokeCap.Round)
    )

    // Athletic chest stripes / crest
    if (variant == 1) {
        drawLine(accent, Offset(w * 0.29f, h * 0.54f), Offset(w * 0.71f, h * 0.46f), strokeWidth = 4.5f, cap = StrokeCap.Round)
        drawLine(accent.copy(alpha = 0.5f), Offset(w * 0.29f, h * 0.60f), Offset(w * 0.71f, h * 0.52f), strokeWidth = 2.5f)
    } else {
        // DeFitt diamond chest crest
        val badge = Path().apply {
            moveTo(w * 0.57f, h * 0.34f)
            lineTo(w * 0.64f, h * 0.40f)
            lineTo(w * 0.57f, h * 0.46f)
            lineTo(w * 0.50f, h * 0.40f)
            close()
        }
        drawPath(badge, accent)
    }
    // Subtle fold lines
    drawLine(highlight, Offset(w * 0.38f, h * 0.38f), Offset(w * 0.36f, h * 0.76f), strokeWidth = 2f)
    drawLine(shadow, Offset(w * 0.66f, h * 0.45f), Offset(w * 0.65f, h * 0.80f), strokeWidth = 2f)
}

private fun DrawScope.drawHoodie(
    w: Float, h: Float, primary: Color, accent: Color, highlight: Color, shadow: Color, variant: Int
) {
    // Hood behind collar
    drawOval(
        color = primary.darker(0.15f),
        topLeft = Offset(w * 0.31f, h * 0.08f),
        size = Size(w * 0.38f, h * 0.24f)
    )
    val hoodiePath = Path().apply {
        moveTo(w * 0.32f, h * 0.20f)
        lineTo(w * 0.68f, h * 0.20f)
        lineTo(w * 0.90f, h * 0.38f)
        lineTo(w * 0.84f, h * 0.68f)
        lineTo(w * 0.74f, h * 0.66f)
        lineTo(w * 0.73f, h * 0.84f)
        lineTo(w * 0.27f, h * 0.84f)
        lineTo(w * 0.26f, h * 0.66f)
        lineTo(w * 0.16f, h * 0.68f)
        lineTo(w * 0.10f, h * 0.38f)
        close()
    }
    drawPath(hoodiePath, Brush.verticalGradient(listOf(primary.lighter(0.12f), primary, primary.darker(0.20f))))
    drawPath(hoodiePath, accent.copy(alpha = 0.6f), style = Stroke(width = 2.2f, join = StrokeJoin.Round))

    // Drawstrings
    drawLine(accent, Offset(w * 0.45f, h * 0.25f), Offset(w * 0.44f, h * 0.43f), strokeWidth = 2.6f, cap = StrokeCap.Round)
    drawLine(accent, Offset(w * 0.55f, h * 0.25f), Offset(w * 0.56f, h * 0.43f), strokeWidth = 2.6f, cap = StrokeCap.Round)
    if (variant == 1) {
        drawLine(accent, Offset(w * 0.50f, h * 0.23f), Offset(w * 0.50f, h * 0.83f), strokeWidth = 2.4f)
    }
    // Kangaroo pocket
    drawRoundRect(
        color = primary.darker(0.24f),
        topLeft = Offset(w * 0.33f, h * 0.58f),
        size = Size(w * 0.34f, h * 0.18f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = accent.copy(alpha = 0.45f),
        topLeft = Offset(w * 0.33f, h * 0.58f),
        size = Size(w * 0.34f, h * 0.18f),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = 1.6f)
    )
    drawLine(highlight, Offset(w * 0.20f, h * 0.40f), Offset(w * 0.17f, h * 0.60f), strokeWidth = 2f)
}

private fun DrawScope.drawPants(
    w: Float, h: Float, primary: Color, accent: Color, highlight: Color, shadow: Color, variant: Int
) {
    val isShorts = variant == 1
    val pantsPath = Path().apply {
        moveTo(w * 0.28f, h * 0.14f)
        lineTo(w * 0.72f, h * 0.14f)
        if (isShorts) {
            lineTo(w * 0.78f, h * 0.58f)
            lineTo(w * 0.54f, h * 0.58f)
            lineTo(w * 0.50f, h * 0.38f)
            lineTo(w * 0.46f, h * 0.58f)
            lineTo(w * 0.22f, h * 0.58f)
        } else {
            lineTo(w * 0.75f, h * 0.84f)
            lineTo(w * 0.56f, h * 0.84f)
            lineTo(w * 0.50f, h * 0.44f)
            lineTo(w * 0.44f, h * 0.84f)
            lineTo(w * 0.25f, h * 0.84f)
        }
        close()
    }
    drawPath(pantsPath, Brush.verticalGradient(listOf(primary.lighter(0.10f), primary, primary.darker(0.20f))))
    drawPath(pantsPath, accent.copy(alpha = 0.55f), style = Stroke(width = 2.0f, join = StrokeJoin.Round))

    // Waistband + drawstring
    drawRoundRect(
        color = accent.copy(alpha = 0.85f),
        topLeft = Offset(w * 0.28f, h * 0.13f),
        size = Size(w * 0.44f, h * 0.06f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Side athletic stripes
    val endY = if (isShorts) h * 0.56f else h * 0.81f
    drawLine(accent, Offset(w * 0.29f, h * 0.22f), Offset(if (isShorts) w * 0.24f else w * 0.27f, endY), strokeWidth = 3f)
    drawLine(accent, Offset(w * 0.71f, h * 0.22f), Offset(if (isShorts) w * 0.76f else w * 0.73f, endY), strokeWidth = 3f)
    drawLine(highlight, Offset(w * 0.37f, h * 0.30f), Offset(w * 0.36f, endY - h * 0.05f), strokeWidth = 1.8f)
}

private fun DrawScope.drawShoe(
    w: Float, h: Float, primary: Color, accent: Color, highlight: Color, shadow: Color, isAthletic: Boolean, variant: Int
) {
    // Sculpted Sneaker / Shoe profile
    val upperPath = Path().apply {
        moveTo(w * 0.16f, h * 0.66f)
        lineTo(w * 0.18f, h * 0.34f) // heel collar
        quadraticTo(w * 0.32f, h * 0.42f, w * 0.44f, h * 0.28f) // ankle dip & tongue
        lineTo(w * 0.62f, h * 0.44f) // vamp
        quadraticTo(w * 0.86f, h * 0.46f, w * 0.88f, h * 0.66f) // curved toe-box
        close()
    }
    drawPath(upperPath, Brush.linearGradient(listOf(primary.lighter(0.18f), primary, primary.darker(0.15f))))
    drawPath(upperPath, accent.copy(alpha = 0.65f), style = Stroke(width = 2f, join = StrokeJoin.Round))

    // Sculpted outsole / midsole
    val solePath = Path().apply {
        moveTo(w * 0.14f, h * 0.66f)
        lineTo(w * 0.89f, h * 0.66f)
        quadraticTo(w * 0.90f, h * 0.77f, w * 0.82f, h * 0.78f)
        lineTo(w * 0.16f, h * 0.78f)
        close()
    }
    drawPath(solePath, accent)
    if (isAthletic) {
        // Air cushion / neon midsole streak
        drawRoundRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(w * 0.22f, h * 0.69f),
            size = Size(w * 0.22f, h * 0.05f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        // Dynamic side speed vector
        drawLine(accent, Offset(w * 0.24f, h * 0.57f), Offset(w * 0.66f, h * 0.50f), strokeWidth = 4.2f, cap = StrokeCap.Round)
    } else {
        drawLine(highlight, Offset(w * 0.28f, h * 0.56f), Offset(w * 0.74f, h * 0.58f), strokeWidth = 2.4f)
    }

    // Laces
    for (i in 0..2) {
        val lx = w * (0.46f + i * 0.06f)
        val ly = h * (0.35f + i * 0.045f)
        drawLine(Color.White.copy(alpha = 0.9f), Offset(lx, ly), Offset(lx + w * 0.05f, ly + h * 0.02f), strokeWidth = 2.4f, cap = StrokeCap.Round)
    }
    if (variant == 1) {
        drawCircle(accent, radius = w * 0.03f, center = Offset(w * 0.24f, h * 0.45f))
    }
}

private fun DrawScope.drawCap(
    w: Float, h: Float, primary: Color, accent: Color, highlight: Color, shadow: Color
) {
    // 6-panel rounded crown
    val crown = Path().apply {
        moveTo(w * 0.18f, h * 0.58f)
        quadraticTo(w * 0.22f, h * 0.22f, w * 0.50f, h * 0.22f)
        quadraticTo(w * 0.78f, h * 0.22f, w * 0.78f, h * 0.58f)
        close()
    }
    drawPath(crown, Brush.verticalGradient(listOf(primary.lighter(0.18f), primary)))
    drawPath(crown, accent.copy(alpha = 0.6f), style = Stroke(width = 2f))

    // Top button + panel seam
    drawCircle(accent, radius = 4.5f, center = Offset(w * 0.50f, h * 0.22f))
    drawLine(highlight, Offset(w * 0.50f, h * 0.23f), Offset(w * 0.44f, h * 0.56f), strokeWidth = 1.8f)

    // Arched forward visor
    val visor = Path().apply {
        moveTo(w * 0.44f, h * 0.56f)
        quadraticTo(w * 0.74f, h * 0.50f, w * 0.92f, h * 0.64f)
        quadraticTo(w * 0.72f, h * 0.68f, w * 0.44f, h * 0.60f)
        close()
    }
    drawPath(visor, primary.darker(0.15f))
    drawPath(visor, accent, style = Stroke(width = 2.2f))

    // Front emblem
    drawCircle(accent, radius = w * 0.055f, center = Offset(w * 0.60f, h * 0.42f))
}

private fun DrawScope.drawGlasses(
    w: Float, h: Float, primary: Color, accent: Color, highlight: Color
) {
    // Temple arms
    drawLine(primary.lighter(0.2f), Offset(w * 0.16f, h * 0.40f), Offset(w * 0.30f, h * 0.30f), strokeWidth = 4f, cap = StrokeCap.Round)
    drawLine(primary.lighter(0.2f), Offset(w * 0.84f, h * 0.40f), Offset(w * 0.70f, h * 0.30f), strokeWidth = 4f, cap = StrokeCap.Round)
    // Dual sport shield lenses
    drawRoundRect(
        brush = Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.55f), primary)),
        topLeft = Offset(w * 0.13f, h * 0.40f),
        size = Size(w * 0.33f, h * 0.24f),
        cornerRadius = CornerRadius(14f, 14f)
    )
    drawRoundRect(
        brush = Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.55f), primary)),
        topLeft = Offset(w * 0.54f, h * 0.40f),
        size = Size(w * 0.33f, h * 0.24f),
        cornerRadius = CornerRadius(14f, 14f)
    )
    // Frame brow & bridge
    drawLine(primary.lighter(0.25f), Offset(w * 0.12f, h * 0.40f), Offset(w * 0.88f, h * 0.40f), strokeWidth = 5f, cap = StrokeCap.Round)
    drawLine(highlight, Offset(w * 0.19f, h * 0.46f), Offset(w * 0.32f, h * 0.56f), strokeWidth = 2.6f, cap = StrokeCap.Round)
    drawLine(highlight, Offset(w * 0.60f, h * 0.46f), Offset(w * 0.73f, h * 0.56f), strokeWidth = 2.6f, cap = StrokeCap.Round)
}

private fun DrawScope.drawWatch(
    w: Float, h: Float, primary: Color, accent: Color, highlight: Color
) {
    // Watch strap
    drawRoundRect(
        color = primary.lighter(0.12f),
        topLeft = Offset(w * 0.38f, h * 0.10f),
        size = Size(w * 0.24f, h * 0.78f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    // Bezel
    drawCircle(primary.darker(0.1f), radius = w * 0.23f, center = Offset(w * 0.50f, h * 0.49f))
    drawCircle(accent, radius = w * 0.23f, center = Offset(w * 0.50f, h * 0.49f), style = Stroke(width = 3.2f))
    // Glowing sport dial
    drawCircle(Color(0xFF0A100A), radius = w * 0.17f, center = Offset(w * 0.50f, h * 0.49f))
    drawArc(
        color = accent,
        startAngle = -90f,
        sweepAngle = 255f,
        useCenter = false,
        topLeft = Offset(w * 0.36f, h * 0.35f),
        size = Size(w * 0.28f, h * 0.28f),
        style = Stroke(width = 4f, cap = StrokeCap.Round)
    )
    drawLine(highlight, Offset(w * 0.42f, h * 0.38f), Offset(w * 0.49f, h * 0.44f), strokeWidth = 2.2f)
}

private fun DrawScope.drawAccessory(
    w: Float, h: Float, primary: Color, accent: Color, highlight: Color, variant: Int
) {
    if (variant == 1) {
        // Athletic Headband
        drawOval(
            color = primary.lighter(0.15f),
            topLeft = Offset(w * 0.14f, h * 0.34f),
            size = Size(w * 0.72f, h * 0.30f),
            style = Stroke(width = 16f)
        )
        drawOval(
            color = accent,
            topLeft = Offset(w * 0.14f, h * 0.34f),
            size = Size(w * 0.72f, h * 0.30f),
            style = Stroke(width = 3.5f)
        )
    } else {
        // Pair of Wristbands
        for (dx in listOf(-w * 0.14f, w * 0.14f)) {
            drawRoundRect(
                color = primary.lighter(0.12f),
                topLeft = Offset(w * 0.38f + dx, h * 0.26f),
                size = Size(w * 0.24f, h * 0.42f),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = accent,
                topLeft = Offset(w * 0.38f + dx, h * 0.40f),
                size = Size(w * 0.24f, h * 0.11f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawLine(highlight, Offset(w * 0.42f + dx, h * 0.30f), Offset(w * 0.42f + dx, h * 0.62f), strokeWidth = 2f)
        }
    }
}

private fun DrawScope.drawBackpack(
    w: Float, h: Float, primary: Color, accent: Color, highlight: Color, shadow: Color
) {
    // Top handle
    drawArc(
        color = accent,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(w * 0.40f, h * 0.10f),
        size = Size(w * 0.20f, h * 0.14f),
        style = Stroke(width = 4f, cap = StrokeCap.Round)
    )
    // Main ergonomic shell
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(primary.lighter(0.16f), primary, primary.darker(0.2f))),
        topLeft = Offset(w * 0.22f, h * 0.18f),
        size = Size(w * 0.56f, h * 0.66f),
        cornerRadius = CornerRadius(24f, 24f)
    )
    drawRoundRect(
        color = accent.copy(alpha = 0.6f),
        topLeft = Offset(w * 0.22f, h * 0.18f),
        size = Size(w * 0.56f, h * 0.66f),
        cornerRadius = CornerRadius(24f, 24f),
        style = Stroke(width = 2.2f)
    )
    // Front pocket + glowing reflective bar
    drawRoundRect(
        color = primary.darker(0.25f),
        topLeft = Offset(w * 0.28f, h * 0.48f),
        size = Size(w * 0.44f, h * 0.28f),
        cornerRadius = CornerRadius(14f, 14f)
    )
    drawLine(accent, Offset(w * 0.32f, h * 0.36f), Offset(w * 0.68f, h * 0.36f), strokeWidth = 4f, cap = StrokeCap.Round)
    drawLine(highlight, Offset(w * 0.28f, h * 0.26f), Offset(w * 0.28f, h * 0.44f), strokeWidth = 2.4f)
}

private fun DrawScope.drawTracksuit(
    w: Float, h: Float, primary: Color, accent: Color, highlight: Color, shadow: Color
) {
    // Jacket (left-top) + Jogger (right-bottom) layered composition
    val jacket = Path().apply {
        moveTo(w * 0.22f, h * 0.14f)
        lineTo(w * 0.54f, h * 0.14f)
        lineTo(w * 0.68f, h * 0.28f)
        lineTo(w * 0.62f, h * 0.48f)
        lineTo(w * 0.54f, h * 0.44f)
        lineTo(w * 0.54f, h * 0.60f)
        lineTo(w * 0.22f, h * 0.60f)
        lineTo(w * 0.22f, h * 0.44f)
        lineTo(w * 0.14f, h * 0.48f)
        lineTo(w * 0.08f, h * 0.28f)
        close()
    }
    drawPath(jacket, Brush.verticalGradient(listOf(primary.lighter(0.14f), primary)))
    drawPath(jacket, accent, style = Stroke(width = 2f))
    drawLine(accent, Offset(w * 0.38f, h * 0.15f), Offset(w * 0.38f, h * 0.59f), strokeWidth = 2.5f)

    // Matching Jogger
    val pants = Path().apply {
        moveTo(w * 0.48f, h * 0.44f)
        lineTo(w * 0.84f, h * 0.44f)
        lineTo(w * 0.86f, h * 0.86f)
        lineTo(w * 0.72f, h * 0.86f)
        lineTo(w * 0.66f, h * 0.62f)
        lineTo(w * 0.60f, h * 0.86f)
        lineTo(w * 0.46f, h * 0.86f)
        close()
    }
    drawPath(pants, Brush.verticalGradient(listOf(primary.lighter(0.10f), primary.darker(0.15f))))
    drawPath(pants, accent, style = Stroke(width = 2f))
    drawLine(accent, Offset(w * 0.82f, h * 0.47f), Offset(w * 0.84f, h * 0.84f), strokeWidth = 3f)
    drawLine(highlight, Offset(w * 0.28f, h * 0.24f), Offset(w * 0.28f, h * 0.52f), strokeWidth = 2f)
}

private fun parseColorSafe(hex: String, fallback: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

private fun Color.lighter(factor: Float): Color = Color(
    red = (red + (1f - red) * factor).coerceIn(0f, 1f),
    green = (green + (1f - green) * factor).coerceIn(0f, 1f),
    blue = (blue + (1f - blue) * factor).coerceIn(0f, 1f),
    alpha = alpha
)

private fun Color.darker(factor: Float): Color = Color(
    red = (red * (1f - factor)).coerceIn(0f, 1f),
    green = (green * (1f - factor)).coerceIn(0f, 1f),
    blue = (blue * (1f - factor)).coerceIn(0f, 1f),
    alpha = alpha
)
