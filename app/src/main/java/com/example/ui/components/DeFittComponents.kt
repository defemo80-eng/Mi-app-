package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.ExerciseAnimType
import com.example.ui.theme.CodGold
import com.example.ui.theme.CodGoldContainer
import com.example.ui.theme.DeFittOutline
import com.example.ui.theme.DeFittSurfaceElevated
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenContainer
import com.example.ui.theme.TextWhite
import kotlin.math.sin

@Composable
fun DeFittOfficialLogo(
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.defitt_logo),
        contentDescription = "Logo oficial de DeFitt",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    )
}

@Composable
fun OfflineBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(NeonGreenContainer, RoundedCornerShape(20.dp))
            .border(1.dp, NeonGreen.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = Icons.Default.WifiOff,
            contentDescription = "Modo 100% sin internet",
            tint = NeonGreen,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = "100% OFFLINE",
            style = MaterialTheme.typography.labelSmall,
            color = NeonGreen,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CodBalanceChip(
    balance: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(CodGoldContainer, RoundedCornerShape(20.dp))
            .border(1.dp, CodGold.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.MonetizationOn,
            contentDescription = "Moneda virtual COD",
            tint = CodGold,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = "$balance COD",
            style = MaterialTheme.typography.labelLarge,
            color = CodGold
        )
    }
}

@Composable
fun ExerciseAnimationView(
    animType: ExerciseAnimType,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "exercise_anim")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    Box(
        modifier = modifier
            .background(DeFittSurfaceElevated, RoundedCornerShape(18.dp))
            .border(1.dp, DeFittOutline, RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val floorY = h * 0.84f

            // Ground line
            drawLine(
                color = NeonGreen.copy(alpha = 0.4f),
                start = Offset(w * 0.12f, floorY),
                end = Offset(w * 0.88f, floorY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )

            val jointColor = NeonGreen
            val limbColor = TextWhite

            when (animType) {
                ExerciseAnimType.SQUATS -> {
                    val drop = phase * h * 0.16f
                    val headY = h * 0.22f + drop
                    val shoulderY = h * 0.34f + drop
                    val hipY = h * 0.56f + drop
                    val kneeX = cx + 22f + phase * 18f
                    val kneeY = h * 0.71f + drop * 0.3f
                    drawCircle(limbColor, 14f, Offset(cx, headY))
                    drawLine(limbColor, Offset(cx, shoulderY), Offset(cx - phase * 10f, hipY), 8f, StrokeCap.Round)
                    drawLine(limbColor, Offset(cx - phase * 10f, hipY), Offset(kneeX, kneeY), 8f, StrokeCap.Round)
                    drawLine(limbColor, Offset(kneeX, kneeY), Offset(cx + 8f, floorY - 4f), 8f, StrokeCap.Round)
                    drawLine(jointColor, Offset(cx, shoulderY), Offset(cx + 38f, shoulderY + 6f), 7f, StrokeCap.Round)
                }
                ExerciseAnimType.PUSHUPS -> {
                    val drop = phase * h * 0.13f
                    val footX = w * 0.22f
                    val handX = w * 0.74f
                    val shoulderY = floorY - 46f + drop
                    val hipY = floorY - 32f + drop * 0.6f
                    drawLine(limbColor, Offset(footX, floorY - 4f), Offset((footX + handX) / 2f, hipY), 8f, StrokeCap.Round)
                    drawLine(limbColor, Offset((footX + handX) / 2f, hipY), Offset(handX - 16f, shoulderY), 8f, StrokeCap.Round)
                    drawLine(jointColor, Offset(handX - 16f, shoulderY), Offset(handX, floorY - 4f), 7f, StrokeCap.Round)
                    drawCircle(limbColor, 13f, Offset(handX + 4f, shoulderY - 10f))
                }
                ExerciseAnimType.PLANK -> {
                    val breathe = sin(phase * Math.PI).toFloat() * 5f
                    val footX = w * 0.20f
                    val elbowX = w * 0.72f
                    val bodyY = floorY - 36f - breathe
                    drawLine(limbColor, Offset(footX, floorY - 4f), Offset(elbowX, bodyY), 9f, StrokeCap.Round)
                    drawLine(jointColor, Offset(elbowX, bodyY), Offset(elbowX, floorY - 4f), 8f, StrokeCap.Round)
                    drawLine(jointColor, Offset(elbowX, floorY - 4f), Offset(elbowX + 24f, floorY - 4f), 7f, StrokeCap.Round)
                    drawCircle(limbColor, 13f, Offset(elbowX + 14f, bodyY - 14f))
                }
                ExerciseAnimType.WALKING, ExerciseAnimType.COORDINATION -> {
                    val swing = (phase - 0.5f) * 38f
                    drawCircle(limbColor, 14f, Offset(cx, h * 0.22f))
                    drawLine(limbColor, Offset(cx, h * 0.34f), Offset(cx, h * 0.57f), 8f, StrokeCap.Round)
                    drawLine(jointColor, Offset(cx, h * 0.57f), Offset(cx + swing, floorY - 4f), 8f, StrokeCap.Round)
                    drawLine(limbColor, Offset(cx, h * 0.57f), Offset(cx - swing, floorY - 4f), 8f, StrokeCap.Round)
                    drawLine(jointColor, Offset(cx, h * 0.37f), Offset(cx - swing * 0.9f, h * 0.52f), 7f, StrokeCap.Round)
                    drawLine(limbColor, Offset(cx, h * 0.37f), Offset(cx + swing * 0.9f, h * 0.52f), 7f, StrokeCap.Round)
                }
                ExerciseAnimType.MOBILITY, ExerciseAnimType.STRETCH -> {
                    val armLift = phase * 42f
                    drawCircle(limbColor, 14f, Offset(cx, h * 0.24f))
                    drawLine(limbColor, Offset(cx, h * 0.36f), Offset(cx, h * 0.60f), 8f, StrokeCap.Round)
                    drawLine(limbColor, Offset(cx, h * 0.60f), Offset(cx - 18f, floorY - 4f), 8f, StrokeCap.Round)
                    drawLine(limbColor, Offset(cx, h * 0.60f), Offset(cx + 18f, floorY - 4f), 8f, StrokeCap.Round)
                    drawLine(jointColor, Offset(cx, h * 0.38f), Offset(cx - 34f, h * 0.42f - armLift), 7f, StrokeCap.Round)
                    drawLine(jointColor, Offset(cx, h * 0.38f), Offset(cx + 34f, h * 0.42f - armLift), 7f, StrokeCap.Round)
                }
            }
        }
    }
}
