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
            val floorY = h * 0.86f
            val scale = (size.minDimension / 160f).coerceIn(0.65f, 1.5f)

            // Studio floor shadow + neon mat line
            drawOval(
                color = Color.Black.copy(alpha = 0.45f),
                topLeft = Offset(cx - 55f * scale, floorY - 6f * scale),
                size = androidx.compose.ui.geometry.Size(110f * scale, 12f * scale)
            )
            drawLine(
                color = NeonGreen.copy(alpha = 0.55f),
                start = Offset(w * 0.14f, floorY),
                end = Offset(w * 0.86f, floorY),
                strokeWidth = 3.5f * scale,
                cap = StrokeCap.Round
            )

            fun drawLimb(a: Offset, b: Offset, color: Color, thickness: Float) {
                drawLine(color, a, b, strokeWidth = thickness * scale, cap = StrokeCap.Round)
            }

            fun drawJoint(p: Offset, color: Color = NeonGreen, radius: Float = 4.5f) {
                drawCircle(color, radius = radius * scale, center = p)
            }

            val bodyColor = Color(0xFFE7ECE7)
            val accentColor = NeonGreen

            when (animType) {
                ExerciseAnimType.SQUATS -> {
                    val drop = phase * h * 0.17f
                    val hip = Offset(cx - phase * 14f * scale, h * 0.55f + drop)
                    val shoulder = Offset(cx + phase * 6f * scale, h * 0.34f + drop)
                    val head = Offset(shoulder.x + 2f * scale, shoulder.y - 18f * scale)
                    val knee = Offset(cx + 22f * scale + phase * 16f * scale, h * 0.72f + drop * 0.25f)
                    val ankle = Offset(cx + 6f * scale, floorY - 3f * scale)
                    val hand = Offset(shoulder.x + 36f * scale, shoulder.y + 6f * scale - phase * 8f * scale)

                    drawLimb(hip, knee, bodyColor, 10f)
                    drawLimb(knee, ankle, bodyColor, 8.5f)
                    drawLimb(shoulder, hip, bodyColor, 12f)
                    drawLimb(shoulder, hand, accentColor, 7f)
                    drawCircle(bodyColor, 12f * scale, head)
                    drawJoint(shoulder)
                    drawJoint(hip)
                    drawJoint(knee)
                }
                ExerciseAnimType.PUSHUPS -> {
                    val drop = phase * h * 0.13f
                    val ankle = Offset(w * 0.20f, floorY - 4f * scale)
                    val hand = Offset(w * 0.76f, floorY - 4f * scale)
                    val shoulder = Offset(w * 0.70f, floorY - 46f * scale + drop)
                    val hip = Offset(w * 0.45f, floorY - 26f * scale + drop * 0.55f)
                    val elbow = Offset(w * 0.73f - phase * 8f * scale, (shoulder.y + hand.y) / 2f)
                    val head = Offset(shoulder.x + 15f * scale, shoulder.y - 9f * scale)

                    drawLimb(ankle, hip, bodyColor, 9f)
                    drawLimb(hip, shoulder, bodyColor, 11.5f)
                    drawLimb(shoulder, elbow, accentColor, 7.5f)
                    drawLimb(elbow, hand, accentColor, 7f)
                    drawCircle(bodyColor, 11.5f * scale, head)
                    drawJoint(shoulder)
                    drawJoint(elbow)
                    drawJoint(hip)
                }
                ExerciseAnimType.PLANK -> {
                    val breathe = sin(phase * Math.PI).toFloat() * 4f * scale
                    val ankle = Offset(w * 0.18f, floorY - 4f * scale)
                    val elbow = Offset(w * 0.72f, floorY - 4f * scale)
                    val shoulder = Offset(w * 0.70f, floorY - 38f * scale - breathe)
                    val hip = Offset(w * 0.44f, floorY - 22f * scale - breathe * 0.7f)
                    val head = Offset(shoulder.x + 15f * scale, shoulder.y - 10f * scale)

                    drawLimb(ankle, hip, bodyColor, 9.5f)
                    drawLimb(hip, shoulder, bodyColor, 11.5f)
                    drawLimb(shoulder, elbow, accentColor, 8f)
                    drawLimb(elbow, Offset(elbow.x + 22f * scale, elbow.y), accentColor, 7f)
                    drawCircle(bodyColor, 11.5f * scale, head)
                    drawJoint(shoulder)
                    drawJoint(hip)
                }
                ExerciseAnimType.WALKING -> {
                    val swing = (phase - 0.5f) * 38f * scale
                    val shoulder = Offset(cx, h * 0.34f)
                    val hip = Offset(cx, h * 0.56f)
                    val kneeF = Offset(cx + swing * 0.65f, h * 0.71f - kotlin.math.abs(swing) * 0.12f)
                    val footF = Offset(cx + swing, floorY - 4f * scale)
                    val kneeB = Offset(cx - swing * 0.65f, h * 0.71f)
                    val footB = Offset(cx - swing, floorY - 4f * scale)
                    val handF = Offset(cx - swing * 0.85f, h * 0.50f)
                    val handB = Offset(cx + swing * 0.85f, h * 0.50f)

                    drawLimb(hip, kneeB, bodyColor.copy(alpha = 0.6f), 9f)
                    drawLimb(kneeB, footB, bodyColor.copy(alpha = 0.6f), 8f)
                    drawLimb(hip, kneeF, accentColor, 9.5f)
                    drawLimb(kneeF, footF, accentColor, 8.5f)
                    drawLimb(shoulder, hip, bodyColor, 12f)
                    drawLimb(shoulder, handB, bodyColor.copy(alpha = 0.7f), 7f)
                    drawLimb(shoulder, handF, accentColor, 7.5f)
                    drawCircle(bodyColor, 12f * scale, Offset(cx, h * 0.20f))
                    drawJoint(hip)
                    drawJoint(kneeF)
                }
                ExerciseAnimType.COORDINATION -> {
                    val lift = phase
                    val shoulder = Offset(cx, h * 0.34f)
                    val hip = Offset(cx, h * 0.56f)
                    val raisedKnee = Offset(cx + 22f * scale * lift, h * 0.70f - 20f * scale * lift)
                    val raisedFoot = Offset(raisedKnee.x - 4f * scale, raisedKnee.y + 18f * scale)
                    val supportFoot = Offset(cx - 10f * scale, floorY - 4f * scale)
                    val crossHand = Offset(cx + 16f * scale * lift, h * 0.54f - 8f * scale * lift)
                    val balanceHand = Offset(cx - 32f * scale, h * 0.44f - 10f * scale * lift)

                    drawLimb(hip, supportFoot, bodyColor, 9.5f)
                    drawLimb(hip, raisedKnee, accentColor, 9.5f)
                    drawLimb(raisedKnee, raisedFoot, accentColor, 8.5f)
                    drawLimb(shoulder, hip, bodyColor, 12f)
                    drawLimb(shoulder, crossHand, accentColor, 7.5f)
                    drawLimb(shoulder, balanceHand, bodyColor, 7f)
                    drawCircle(bodyColor, 12f * scale, Offset(cx, h * 0.20f))
                    drawJoint(raisedKnee)
                    drawJoint(crossHand)
                }
                ExerciseAnimType.MOBILITY, ExerciseAnimType.STRETCH -> {
                    val armLift = phase * 38f * scale
                    val sway = sin(phase * Math.PI).toFloat() * 8f * scale
                    val shoulder = Offset(cx + sway * 0.4f, h * 0.36f)
                    val hip = Offset(cx, h * 0.59f)
                    val leftHand = Offset(shoulder.x - 32f * scale, h * 0.42f - armLift)
                    val rightHand = Offset(shoulder.x + 32f * scale, h * 0.42f - armLift)

                    drawLimb(hip, Offset(cx - 18f * scale, floorY - 4f * scale), bodyColor, 9.5f)
                    drawLimb(hip, Offset(cx + 18f * scale, floorY - 4f * scale), bodyColor, 9.5f)
                    drawLimb(shoulder, hip, bodyColor, 12f)
                    drawLimb(shoulder, leftHand, accentColor, 7.5f)
                    drawLimb(shoulder, rightHand, accentColor, 7.5f)
                    drawCircle(bodyColor, 12f * scale, Offset(shoulder.x, h * 0.22f))
                    drawJoint(shoulder)
                    drawJoint(leftHand)
                    drawJoint(rightHand)
                }
            }
        }
    }
}
