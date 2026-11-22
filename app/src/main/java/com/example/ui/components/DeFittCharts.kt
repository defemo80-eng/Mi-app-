package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CodTransactionEntity
import com.example.data.local.entity.DailyLogEntity
import com.example.ui.theme.CodGold
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeFittOutline
import com.example.ui.theme.DeFittSurface
import com.example.ui.theme.DeFittSurfaceElevated
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite

@Composable
private fun rememberEntranceProgress(key: Any?): Float {
    var target by remember(key) { mutableFloatStateOf(0f) }
    LaunchedEffect(key) { target = 1f }
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "chart_entrance"
    )
    return progress
}

@Composable
fun WeeklyStepsBarChart(
    logs: List<DailyLogEntity>,
    stepGoal: Int,
    modifier: Modifier = Modifier
) {
    val chartLogs = remember(logs) { logs.sortedBy { it.dateKey }.takeLast(7) }
    val anim = rememberEntranceProgress(chartLogs.size to chartLogs.sumOf { it.steps })

    Card(
        colors = CardDefaults.cardColors(containerColor = DeFittSurface),
        border = BorderStroke(1.dp, DeFittOutline),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("chart_weekly_steps")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pasos semanales", style = MaterialTheme.typography.titleMedium, color = TextWhite)
                Text("Meta: $stepGoal", style = MaterialTheme.typography.labelSmall, color = CodGold)
            }

            if (chartLogs.isEmpty() || chartLogs.all { it.steps == 0L }) {
                Text(
                    text = "Aún no hay pasos registrados esta semana. Camina con DeFitt para ver tus barras diarias.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            } else {
                val maxVal = maxOf(
                    stepGoal.toFloat(),
                    chartLogs.maxOf { it.steps }.toFloat()
                ).coerceAtLeast(500f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                ) {
                    val goalY = size.height * (1f - (stepGoal / maxVal).coerceIn(0.05f, 0.95f))
                    drawLine(
                        color = CodGold.copy(alpha = 0.55f),
                        start = Offset(0f, goalY),
                        end = Offset(size.width, goalY),
                        strokeWidth = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                    )

                    val count = chartLogs.size
                    val slotW = size.width / count
                    val barW = (slotW * 0.5f).coerceIn(18f, 44f)

                    chartLogs.forEachIndexed { idx, log ->
                        val ratio = (log.steps / maxVal).coerceIn(0.04f, 1f) * anim
                        val barH = size.height * ratio
                        val left = idx * slotW + (slotW - barW) / 2f
                        val reachedGoal = log.steps >= stepGoal.toLong()
                        val brush = if (reachedGoal) {
                            Brush.verticalGradient(listOf(NeonGreen, SuccessGreen.copy(alpha = 0.7f)))
                        } else {
                            Brush.verticalGradient(listOf(NeonGreen.copy(alpha = 0.85f), Color(0xFF1B4D18)))
                        }
                        drawRoundRect(
                            brush = brush,
                            topLeft = Offset(left, size.height - barH),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(10f, 10f)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    chartLogs.forEach { log ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${log.steps}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (log.steps >= stepGoal) CodGold else NeonGreen,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = log.dateKey.takeLast(5),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyActivityLineChart(
    logs: List<DailyLogEntity>,
    modifier: Modifier = Modifier
) {
    val chartLogs = remember(logs) { logs.sortedBy { it.dateKey }.takeLast(30) }
    val anim = rememberEntranceProgress(chartLogs.size to chartLogs.sumOf { it.steps })

    Card(
        colors = CardDefaults.cardColors(containerColor = DeFittSurface),
        border = BorderStroke(1.dp, DeFittOutline),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("chart_monthly_activity")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Actividad mensual (últimos 30 días)", style = MaterialTheme.typography.titleMedium, color = TextWhite)
                val avgSteps = if (chartLogs.isNotEmpty()) chartLogs.sumOf { it.steps } / chartLogs.size else 0L
                if (chartLogs.isNotEmpty()) {
                    Text("Prom: $avgSteps/día", style = MaterialTheme.typography.labelSmall, color = NeonGreen)
                }
            }

            if (chartLogs.isEmpty()) {
                Text(
                    text = "Sin historial suficiente aún. Tus días cerrados aparecerán aquí como tendencia continua.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            } else {
                val maxSteps = chartLogs.maxOf { it.steps }.coerceAtLeast(1000L).toFloat()
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val padH = 12f
                    val usableH = size.height - padH * 2f
                    val points = chartLogs.mapIndexed { index, log ->
                        val x = if (chartLogs.size == 1) {
                            size.width / 2f
                        } else {
                            (index.toFloat() / (chartLogs.size - 1).toFloat()) * size.width
                        }
                        val norm = (log.steps / maxSteps).coerceIn(0f, 1f) * anim
                        val y = padH + usableH * (1f - norm)
                        Offset(x, y)
                    }

                    // Reference grid lines
                    for (step in 1..3) {
                        val gy = size.height * (step / 4f)
                        drawLine(
                            color = DeFittOutline.copy(alpha = 0.5f),
                            start = Offset(0f, gy),
                            end = Offset(size.width, gy),
                            strokeWidth = 1.5f
                        )
                    }

                    if (points.size == 1) {
                        drawCircle(NeonGreen.copy(alpha = 0.25f), radius = 20f, center = points.first())
                        drawCircle(NeonGreen, radius = 8f, center = points.first())
                    } else {
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 0 until points.lastIndex) {
                                val p0 = points[i]
                                val p1 = points[i + 1]
                                val midX = (p0.x + p1.x) / 2f
                                cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                            }
                        }
                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(points.last().x, size.height)
                            lineTo(points.first().x, size.height)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(NeonGreen.copy(alpha = 0.32f), Color.Transparent)
                            )
                        )
                        drawPath(
                            path = linePath,
                            color = NeonGreen,
                            style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                        )
                        points.forEachIndexed { idx, pt ->
                            if (idx == points.lastIndex || chartLogs[idx].steps == chartLogs.maxOf { it.steps }) {
                                drawCircle(DeFittSurfaceElevated, radius = 8f, center = pt)
                                drawCircle(
                                    color = if (idx == points.lastIndex) CodGold else NeonGreen,
                                    radius = 5.5f,
                                    center = pt
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = chartLogs.first().dateKey.takeLast(5),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "Máx: ${chartLogs.maxOf { it.steps }} pasos",
                        style = MaterialTheme.typography.labelSmall,
                        color = CodGold
                    )
                    Text(
                        text = chartLogs.last().dateKey.takeLast(5),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun NutritionMacroDonutChart(
    proteinGrams: Float,
    carbsGrams: Float,
    fatsGrams: Float,
    sugarsGrams: Float,
    fiberGrams: Float,
    calories: Float,
    modifier: Modifier = Modifier
) {
    val totalMacros = (proteinGrams + carbsGrams + fatsGrams + fiberGrams).coerceAtLeast(0f)
    val anim = rememberEntranceProgress(totalMacros to calories)

    val segments = remember(proteinGrams, carbsGrams, fatsGrams, fiberGrams) {
        listOf(
            Triple("Proteínas", proteinGrams, NeonGreen),
            Triple("Carbs", carbsGrams, CodGold),
            Triple("Grasas", fatsGrams, Color(0xFFFF8C42)),
            Triple("Fibra", fiberGrams, Color(0xFF4CD7D0))
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DeFittSurface),
        border = BorderStroke(1.dp, DeFittOutline),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("chart_nutrition_macros")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Distribución nutricional aproximada", style = MaterialTheme.typography.titleMedium, color = TextWhite)

            if (totalMacros <= 0.1f && calories <= 0.1f) {
                Text(
                    text = "Sin alimentos registrados para esta fecha. Registra comidas con el escáner o catálogo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(116.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(112.dp)) {
                            val strokeW = 20f
                            val arcSize = Size(size.width - strokeW, size.height - strokeW)
                            val arcTopLeft = Offset(strokeW / 2f, strokeW / 2f)

                            drawArc(
                                color = DeFittSurfaceElevated,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = strokeW)
                            )

                            if (totalMacros > 0f) {
                                var startAngle = -90f
                                segments.forEach { (_, grams, color) ->
                                    if (grams > 0f) {
                                        val sweep = (grams / totalMacros) * 360f * anim
                                        drawArc(
                                            color = color,
                                            startAngle = startAngle,
                                            sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                                            useCenter = false,
                                            topLeft = arcTopLeft,
                                            size = arcSize,
                                            style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                                        )
                                        startAngle += sweep
                                    }
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${calories.toInt()}",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text("kcal", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        segments.forEach { (name, grams, color) ->
                            val pct = if (totalMacros > 0f) ((grams / totalMacros) * 100f).toInt() else 0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                                    Text(name, style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                                }
                                Text(
                                    text = "${"%.1f".format(grams)}g ($pct%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Text(
                            text = "Azúcares estimados: ${"%.1f".format(sugarsGrams)}g",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CodWalletFlowChart(
    transactions: List<CodTransactionEntity>,
    modifier: Modifier = Modifier
) {
    val groupedDays = remember(transactions) {
        transactions
            .groupBy { it.dateKey }
            .toSortedMap()
            .entries
            .toList()
            .takeLast(7)
            .map { (date, list) ->
                val earned = list.filter { it.amount > 0 }.sumOf { it.amount }
                val spent = list.filter { it.amount < 0 }.sumOf { -it.amount }
                Triple(date, earned, spent)
            }
    }
    val anim = rememberEntranceProgress(transactions.size)

    Card(
        colors = CardDefaults.cardColors(containerColor = DeFittSurface),
        border = BorderStroke(1.dp, DeFittOutline),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("chart_cod_flow")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Movimientos COD por día", style = MaterialTheme.typography.titleMedium, color = TextWhite)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CodGold))
                        Text("Ganado", style = MaterialTheme.typography.labelSmall, color = CodGold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(DangerRed))
                        Text("Gastado", style = MaterialTheme.typography.labelSmall, color = DangerRed)
                    }
                }
            }

            if (groupedDays.isEmpty()) {
                Text(
                    text = "Aún no hay movimientos COD. Camina, logra insignias o compra ropa para ver el balance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            } else {
                val maxCod = groupedDays.maxOf { maxOf(it.second, it.third) }.coerceAtLeast(100L).toFloat()
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(125.dp)
                ) {
                    val slotW = size.width / groupedDays.size
                    val singleBarW = (slotW * 0.28f).coerceIn(10f, 24f)

                    groupedDays.forEachIndexed { idx, (_, earned, spent) ->
                        val center = idx * slotW + slotW / 2f
                        if (earned > 0L) {
                            val hEarned = (earned / maxCod).coerceIn(0.06f, 1f) * size.height * anim
                            drawRoundRect(
                                color = CodGold,
                                topLeft = Offset(center - singleBarW - 2f, size.height - hEarned),
                                size = Size(singleBarW, hEarned),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }
                        if (spent > 0L) {
                            val hSpent = (spent / maxCod).coerceIn(0.06f, 1f) * size.height * anim
                            drawRoundRect(
                                color = DangerRed,
                                topLeft = Offset(center + 2f, size.height - hSpent),
                                size = Size(singleBarW, hSpent),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    groupedDays.forEach { (date, _, _) ->
                        Text(
                            text = date.takeLast(5),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HabitProgressRingsRow(
    activeDaysCount: Int,
    completedWorkoutsCount: Int,
    scansCount: Int,
    unlockedAchievementsCount: Int,
    totalAchievementsCount: Int,
    modifier: Modifier = Modifier
) {
    val rings = listOf(
        Triple("Días activos", activeDaysCount, (activeDaysCount / 7f).coerceIn(0f, 1f) to NeonGreen),
        Triple("Rutinas", completedWorkoutsCount, (completedWorkoutsCount / 5f).coerceIn(0f, 1f) to SuccessGreen),
        Triple("Escaneos", scansCount, (scansCount / 4f).coerceIn(0f, 1f) to CodGold),
        Triple(
            "Insignias",
            unlockedAchievementsCount,
            (unlockedAchievementsCount / totalAchievementsCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) to Color(0xFF4CD7D0)
        )
    )
    val anim = rememberEntranceProgress(activeDaysCount + completedWorkoutsCount + scansCount + unlockedAchievementsCount)

    Card(
        colors = CardDefaults.cardColors(containerColor = DeFittSurface),
        border = BorderStroke(1.dp, DeFittOutline),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("chart_habit_rings")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Anillos de constancia real", style = MaterialTheme.typography.titleMedium, color = TextWhite)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                rings.forEach { (label, count, pair) ->
                    val (ratio, ringColor) = pair
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(60.dp)) {
                                val sw = 10f
                                drawArc(
                                    color = DeFittSurfaceElevated,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = Offset(sw / 2f, sw / 2f),
                                    size = Size(size.width - sw, size.height - sw),
                                    style = Stroke(width = sw)
                                )
                                if (ratio > 0f) {
                                    drawArc(
                                        color = ringColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f * ratio * anim,
                                        useCenter = false,
                                        topLeft = Offset(sw / 2f, sw / 2f),
                                        size = Size(size.width - sw, size.height - sw),
                                        style = Stroke(width = sw, cap = StrokeCap.Round)
                                    )
                                }
                            }
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
