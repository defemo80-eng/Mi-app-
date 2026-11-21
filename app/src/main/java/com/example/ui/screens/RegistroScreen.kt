package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.engine.DateUtils
import com.example.domain.model.AvatarParams
import com.example.domain.model.MealType
import com.example.ui.avatar3d.Avatar3DView
import com.example.ui.theme.CodGold
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeFittBlack
import com.example.ui.theme.DeFittOutline
import com.example.ui.theme.DeFittPureBlack
import com.example.ui.theme.DeFittSurface
import com.example.ui.theme.DeFittSurfaceElevated
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.DeFittMainState
import kotlinx.serialization.json.Json
import java.util.Calendar

@Composable
fun RegistroScreen(
    state: DeFittMainState,
    contentPadding: PaddingValues,
    onSelectDate: (String) -> Unit,
    onOpenFoodScanner: () -> Unit,
    onDeleteFood: (entryId: String, dateKey: String) -> Unit
) {
    var mode by remember { mutableIntStateOf(0) } // 0=Detalle del día, 1=Calendario mensual
    val selectedDate = state.selectedHistoryDate
    val logsByDate = remember(state.allDailyLogs) { state.allDailyLogs.associateBy { it.dateKey } }
    val selectedLog = logsByDate[selectedDate]

    val calNow = remember { Calendar.getInstance() }
    var calYear by remember { mutableIntStateOf(calNow.get(Calendar.YEAR)) }
    var calMonth by remember { mutableIntStateOf(calNow.get(Calendar.MONTH)) }

    val snapshotAvatar = remember(selectedLog?.avatarSnapshotJson, state.avatarParams) {
        val raw = selectedLog?.avatarSnapshotJson.orEmpty()
        if (raw.isBlank()) state.avatarParams
        else try {
            Json { ignoreUnknownKeys = true }.decodeFromString<AvatarParams>(raw)
        } catch (_: Exception) {
            state.avatarParams
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeFittBlack)
            .padding(contentPadding),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("REGISTRO", style = MaterialTheme.typography.headlineMedium, color = TextWhite)
                Button(
                    onClick = onOpenFoodScanner,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DeFittPureBlack),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("registro_add_food_btn")
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Comida", fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = mode == 0,
                    onClick = { mode = 0 },
                    leadingIcon = { Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Historial del día") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonGreen,
                        selectedLabelColor = DeFittPureBlack
                    ),
                    modifier = Modifier.weight(1f).testTag("registro_tab_daily")
                )
                FilterChip(
                    selected = mode == 1,
                    onClick = { mode = 1 },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Calendario mensual") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonGreen,
                        selectedLabelColor = DeFittPureBlack
                    ),
                    modifier = Modifier.weight(1f).testTag("registro_tab_calendar")
                )
            }

            if (mode == 1) {
                // Monthly Calendar
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeFittSurface),
                    border = BorderStroke(1.dp, DeFittOutline),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (calMonth == 0) {
                                        calMonth = 11
                                        calYear--
                                    } else calMonth--
                                },
                                modifier = Modifier.testTag("calendar_prev_month")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior", tint = NeonGreen)
                            }
                            Text(
                                text = DateUtils.monthTitle(calYear, calMonth),
                                style = MaterialTheme.typography.titleLarge,
                                color = TextWhite
                            )
                            IconButton(
                                onClick = {
                                    if (calMonth == 11) {
                                        calMonth = 0
                                        calYear++
                                    } else calMonth++
                                },
                                modifier = Modifier.testTag("calendar_next_month")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mes siguiente", tint = NeonGreen)
                            }
                        }

                        // Weekday headers
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("L", "M", "X", "J", "V", "S", "D").forEach { d ->
                                Text(
                                    text = d,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        val grid = remember(calYear, calMonth) { DateUtils.monthDaysGrid(calYear, calMonth) }
                        val rows = grid.chunked(7)
                        val scanDates = remember(state.bodyScans) { state.bodyScans.map { it.dateKey }.toSet() }

                        rows.forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (i in 0 until 7) {
                                    val dateKey = week.getOrNull(i)
                                    if (dateKey == null) {
                                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                    } else {
                                        val log = logsByDate[dateKey]
                                        val hasActivity = log != null && (
                                            log.steps > 0 || log.caloriesTotal > 0f ||
                                                log.workoutsCompleted > 0 || scanDates.contains(dateKey)
                                            )
                                        val isSelected = dateKey == selectedDate
                                        val dayNum = dateKey.takeLast(2).toIntOrNull()?.toString() ?: ""

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    when {
                                                        isSelected -> NeonGreen
                                                        hasActivity -> DeFittSurfaceElevated
                                                        else -> DeFittBlack
                                                    }
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) NeonGreen else if (hasActivity) NeonGreen.copy(alpha = 0.4f) else DeFittOutline.copy(alpha = 0.4f),
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    onSelectDate(dateKey)
                                                    mode = 0
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = dayNum,
                                                    color = if (isSelected) DeFittPureBlack else TextWhite,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                                if (hasActivity) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isSelected) DeFittPureBlack
                                                                else if (scanDates.contains(dateKey)) CodGold
                                                                else NeonGreen
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("● Verde: actividad/comida", style = MaterialTheme.typography.labelSmall, color = NeonGreen)
                            Text("● Dorado: escaneo corporal", style = MaterialTheme.typography.labelSmall, color = CodGold)
                        }
                    }
                }
            }

            // Selected Date Full Snapshot
            Card(
                colors = CardDefaults.cardColors(containerColor = DeFittSurfaceElevated),
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = DateUtils.formatDisplayDate(selectedDate),
                            style = MaterialTheme.typography.titleLarge,
                            color = NeonGreen
                        )
                        Text(
                            text = if (selectedLog?.isClosed == true) "Día cerrado" else "En curso",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedLog?.isClosed == true) CodGold else SuccessGreen
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatBadge("Pasos", "${selectedLog?.steps ?: 0L}")
                        StatBadge("COD", "+${selectedLog?.codEarned ?: 0L}", CodGold)
                        StatBadge("Rutinas", "${selectedLog?.workoutsCompleted ?: 0}")
                        StatBadge("Calorías", "${(selectedLog?.caloriesTotal ?: 0f).toInt()}")
                    }

                    if (!selectedLog?.activitySummary.isNullOrBlank()) {
                        Text(
                            text = selectedLog!!.activitySummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Date-matched Avatar representation
            Text("Avatar en esta fecha", style = MaterialTheme.typography.titleMedium, color = TextWhite)
            Avatar3DView(
                avatarParams = snapshotAvatar,
                equippedItemIds = state.equipped.map { it.itemId },
                modifier = Modifier.fillMaxWidth().height(175.dp),
                showRotateHint = true,
                testTag = "history_avatar_3d"
            )

            // Nutritional Totals for Selected Date
            Card(
                colors = CardDefaults.cardColors(containerColor = DeFittSurface),
                border = BorderStroke(1.dp, DeFittOutline),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Restaurant, null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                        Text("Resumen nutricional aproximado", style = MaterialTheme.typography.titleMedium, color = TextWhite)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatBadge("Proteínas", "${"%.1f".format(selectedLog?.proteinTotal ?: 0f)}g")
                        StatBadge("Carbs", "${"%.1f".format(selectedLog?.carbsTotal ?: 0f)}g")
                        StatBadge("Grasas", "${"%.1f".format(selectedLog?.fatsTotal ?: 0f)}g")
                        StatBadge("Azúcares", "${"%.1f".format(selectedLog?.sugarsTotal ?: 0f)}g")
                    }
                }
            }

            // Meals grouped by section
            MealType.entries.forEach { meal ->
                val mealFoods = state.selectedDateFoods.filter { it.mealType == meal.name }
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeFittSurface),
                    border = BorderStroke(1.dp, DeFittOutline),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(meal.label, style = MaterialTheme.typography.titleMedium, color = NeonGreen)
                        if (mealFoods.isEmpty()) {
                            Text("Sin alimentos registrados en ${meal.label.lowercase()}.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        } else {
                            mealFoods.forEach { food ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${food.foodName} (${food.portionGrams}g)", color = TextWhite, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            "${food.calories.toInt()} kcal • P ${"%.1f".format(food.protein)}g • C ${"%.1f".format(food.carbs)}g • G ${"%.1f".format(food.fats)}g",
                                            color = TextSecondary,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    IconButton(onClick = { onDeleteFood(food.id, food.dateKey) }) {
                                        Icon(Icons.Default.DeleteOutline, "Eliminar", tint = DangerRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Workouts for Selected Date
            Card(
                colors = CardDefaults.cardColors(containerColor = DeFittSurface),
                border = BorderStroke(1.dp, DeFittOutline),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Entrenamientos del día", style = MaterialTheme.typography.titleMedium, color = NeonGreen)
                    if (state.selectedDateWorkouts.isEmpty()) {
                        Text("No hay rutinas registradas para esta fecha.", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        state.selectedDateWorkouts.forEach { w ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(w.routineTitle, color = TextWhite, style = MaterialTheme.typography.bodyLarge)
                                    Text("${w.difficulty} • ${w.durationMinutes} min", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                                }
                                Text(
                                    text = if (w.completed) "COMPLETADA" else "PENDIENTE",
                                    color = if (w.completed) SuccessGreen else DangerRed,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBadge(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = TextWhite
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
