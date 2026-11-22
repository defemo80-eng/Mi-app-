package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Timeline
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.engine.DateUtils
import com.example.domain.model.AvatarParams
import com.example.ui.avatar3d.Avatar3DView
import com.example.ui.components.CodBalanceChip
import com.example.ui.components.CodWalletFlowChart
import com.example.ui.components.HabitProgressRingsRow
import com.example.ui.components.MonthlyActivityLineChart
import com.example.ui.components.NutritionMacroDonutChart
import com.example.ui.components.WeeklyStepsBarChart
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

private data class AchievementSpec(
    val key: String,
    val title: String,
    val description: String,
    val xpReward: Int,
    val codReward: Int
)

private val ALL_ACHIEVEMENT_SPECS = listOf(
    AchievementSpec("FIRST_STEPS", "Primeros pasos", "Registra tus primeros 100 pasos válidos con DeFitt.", 40, 20),
    AchievementSpec("FIRST_WORKOUT", "Primera rutina completada", "Confirma tu primera rutina personalizada sin máquinas.", 60, 40),
    AchievementSpec("FIRST_SCAN", "Primer escaneo", "Calibra tu avatar 3D personal sin conexión.", 80, 50),
    AchievementSpec("FIRST_PURCHASE", "Primera compra", "Adquiere tu primer artículo en la tienda con COD.", 50, 25),
    AchievementSpec("DAILY_STREAK_3", "Constancia diaria", "Mantén actividad registrada durante 3 días.", 90, 60),
    AchievementSpec("FIRST_WEEK_ACTIVE", "Primera semana activa", "Completa 7 días con actividad física registrada.", 200, 150),
    AchievementSpec("STEP_GOAL_REACHED", "Meta de actividad alcanzada", "Alcanza tu meta diaria de pasos.", 100, 75)
)

@Composable
fun LogrosScreen(
    state: DeFittMainState,
    contentPadding: PaddingValues,
    onOpenEvolution: () -> Unit
) {
    val unlockedByKey = remember(state.achievements) { state.achievements.associateBy { it.achievementKey } }
    val user = state.activeUser
    var activeTab by remember { mutableIntStateOf(0) } // 0=Insignias, 1=Estadísticas

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
                Text("Logros y Estadísticas", style = MaterialTheme.typography.headlineMedium, color = TextWhite)
                Text("XP: ${user?.xpTotal ?: 0}", style = MaterialTheme.typography.titleMedium, color = NeonGreen)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    leadingIcon = { Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Insignias (${state.achievements.size}/${ALL_ACHIEVEMENT_SPECS.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonGreen,
                        selectedLabelColor = DeFittPureBlack,
                        containerColor = DeFittSurface,
                        labelColor = TextWhite
                    ),
                    modifier = Modifier.weight(1f).testTag("logros_tab_badges")
                )
                FilterChip(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    leadingIcon = { Icon(Icons.Default.Timeline, null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Estadísticas") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonGreen,
                        selectedLabelColor = DeFittPureBlack,
                        containerColor = DeFittSurface,
                        labelColor = TextWhite
                    ),
                    modifier = Modifier.weight(1f).testTag("logros_tab_stats")
                )
            }

            if (activeTab == 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeFittSurfaceElevated),
                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${state.achievements.size} de ${ALL_ACHIEVEMENT_SPECS.size} insignias",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextWhite
                            )
                            Text(
                                "Las insignias otorgan XP y COD, sin alterar artificialmente el cuerpo del avatar.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Button(
                            onClick = onOpenEvolution,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DeFittPureBlack),
                            modifier = Modifier.testTag("logros_open_evolution_btn")
                        ) {
                            Icon(Icons.Default.Timeline, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Evolución")
                        }
                    }
                }

                ALL_ACHIEVEMENT_SPECS.forEach { spec ->
                    val unlocked = unlockedByKey[spec.key]
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (unlocked != null) DeFittSurfaceElevated else DeFittSurface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (unlocked != null) CodGold.copy(alpha = 0.7f) else DeFittOutline
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (unlocked != null) Icons.Default.EmojiEvents else Icons.Default.Lock,
                                contentDescription = spec.title,
                                tint = if (unlocked != null) CodGold else TextMuted,
                                modifier = Modifier.size(30.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    spec.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (unlocked != null) TextWhite else TextSecondary
                                )
                                Text(spec.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                Text(
                                    text = if (unlocked != null) {
                                        "Desbloqueado el ${DateUtils.formatDisplayDate(DateUtils.dateKeyFromMillis(unlocked.unlockedAt))}"
                                    } else {
                                        "Recompensa: +${spec.codReward} COD • +${spec.xpReward} XP"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (unlocked != null) NeonGreen else CodGold
                                )
                            }
                        }
                    }
                }
            } else {
                val activeDaysCount = remember(state.allDailyLogs) {
                    state.allDailyLogs.count { it.steps > 0 || it.workoutsCompleted > 0 || it.caloriesTotal > 0f }
                }
                val completedWorkouts = remember(state.allWorkouts) {
                    state.allWorkouts.count { it.completed }
                }
                val today = state.todayLog

                HabitProgressRingsRow(
                    activeDaysCount = activeDaysCount,
                    completedWorkoutsCount = completedWorkouts,
                    scansCount = state.bodyScans.size,
                    unlockedAchievementsCount = state.achievements.size,
                    totalAchievementsCount = ALL_ACHIEVEMENT_SPECS.size
                )

                WeeklyStepsBarChart(
                    logs = state.allDailyLogs,
                    stepGoal = user?.dailyStepGoal ?: 6000
                )

                MonthlyActivityLineChart(logs = state.allDailyLogs)

                CodWalletFlowChart(transactions = state.transactions)

                NutritionMacroDonutChart(
                    proteinGrams = today?.proteinTotal ?: 0f,
                    carbsGrams = today?.carbsTotal ?: 0f,
                    fatsGrams = today?.fatsTotal ?: 0f,
                    sugarsGrams = today?.sugarsTotal ?: 0f,
                    fiberGrams = today?.fiberTotal ?: 0f,
                    calories = today?.caloriesTotal ?: 0f
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvolutionScreen(
    state: DeFittMainState,
    onOpenBodyScanner: () -> Unit,
    onBack: () -> Unit
) {
    val scans = state.bodyScans
    var selectedScanIndex by remember { mutableIntStateOf(0) }
    val selectedScan = scans.getOrNull(selectedScanIndex)

    val displayedAvatar = remember(selectedScan, state.avatarParams) {
        if (selectedScan == null) state.avatarParams
        else try {
            Json { ignoreUnknownKeys = true }.decodeFromString<AvatarParams>(selectedScan.avatarParamsJson)
        } catch (_: Exception) {
            state.avatarParams
        }
    }

    Scaffold(
        containerColor = DeFittBlack,
        topBar = {
            TopAppBar(
                title = { Text("MI EVOLUCIÓN", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("evolution_back_btn")) {
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
                Avatar3DView(
                    avatarParams = displayedAvatar,
                    equippedItemIds = state.equipped.map { it.itemId },
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    testTag = "evolution_avatar_3d"
                )

                if (scans.isNotEmpty()) {
                    Text("Historial de escaneos y avatares", style = MaterialTheme.typography.titleMedium, color = TextWhite)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        scans.take(4).forEachIndexed { idx, scan ->
                            Button(
                                onClick = { selectedScanIndex = idx },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedScanIndex == idx) NeonGreen else DeFittSurface,
                                    contentColor = if (selectedScanIndex == idx) DeFittPureBlack else TextWhite
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(scan.dateKey.takeLast(5), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Scan comparison card (strict honest reporting)
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeFittSurfaceElevated),
                    border = BorderStroke(1.dp, DeFittOutline),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Comparación de escaneos", style = MaterialTheme.typography.titleMedium, color = CodGold)
                        Text(state.scanComparison.summaryMessage, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        if (state.scanComparison.hasTwoScans) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Hombros: ${"%+.1f".format(state.scanComparison.shoulderDeltaPct)}%", style = MaterialTheme.typography.labelSmall, color = TextWhite)
                                Text("Contorno: ${"%+.1f".format(state.scanComparison.waistDeltaPct)}%", style = MaterialTheme.typography.labelSmall, color = TextWhite)
                                Text("Postura: ${"%+.1f".format(state.scanComparison.postureDeltaPct)}%", style = MaterialTheme.typography.labelSmall, color = TextWhite)
                            }
                        }
                    }
                }

                Button(
                    onClick = onOpenBodyScanner,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DeFittPureBlack),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("evolution_new_scan_btn")
                ) {
                    Text(
                        if (state.weeklyScanDue) "Realizar escaneo semanal ahora" else "Registrar nuevo escaneo corporal",
                        fontWeight = FontWeight.Bold
                    )
                }

                // Modern Weekly Steps & Monthly Activity Charts
                WeeklyStepsBarChart(
                    logs = state.allDailyLogs,
                    stepGoal = state.activeUser?.dailyStepGoal ?: 6000
                )

                MonthlyActivityLineChart(logs = state.allDailyLogs)

                val completedWorkouts = state.allWorkouts.count { it.completed }
                HabitProgressRingsRow(
                    activeDaysCount = state.allDailyLogs.count { it.steps > 0 || it.workoutsCompleted > 0 },
                    completedWorkoutsCount = completedWorkouts,
                    scansCount = scans.size,
                    unlockedAchievementsCount = state.achievements.size,
                    totalAchievementsCount = ALL_ACHIEVEMENT_SPECS.size
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    state: DeFittMainState,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = DeFittBlack,
        topBar = {
            TopAppBar(
                title = { Text("Monedero COD", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("wallet_back_btn")) {
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
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeFittSurfaceElevated),
                    border = BorderStroke(1.dp, CodGold),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Saldo acumulado", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                            CodBalanceChip(balance = state.activeUser?.codBalance ?: 0L)
                        }
                        Text(
                            "1 paso válido = 1 COD. El saldo es moneda virtual interna sin valor monetario real y nunca se reinicia a medianoche.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                Text("Historial de ganancias y compras", style = MaterialTheme.typography.titleLarge, color = TextWhite)

                if (state.transactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aún no hay movimientos COD registrados.", color = TextMuted)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.transactions, key = { it.id }) { tx ->
                            val isPositive = tx.amount >= 0
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DeFittSurface),
                                border = BorderStroke(1.dp, DeFittOutline),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.MonetizationOn,
                                            null,
                                            tint = if (isPositive) CodGold else DangerRed
                                        )
                                        Column {
                                            Text(tx.description, style = MaterialTheme.typography.bodyLarge, color = TextWhite)
                                            Text(
                                                "${DateUtils.formatDisplayDate(tx.dateKey)} ${DateUtils.formatTime(tx.timestamp)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isPositive) "+${tx.amount} COD" else "${tx.amount} COD",
                                            color = if (isPositive) SuccessGreen else DangerRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Saldo: ${tx.balanceAfter}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
