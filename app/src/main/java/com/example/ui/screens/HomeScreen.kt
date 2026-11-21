package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.avatar3d.Avatar3DView
import com.example.ui.components.CodBalanceChip
import com.example.ui.components.DeFittOfficialLogo
import com.example.ui.components.OfflineBadge
import com.example.ui.theme.CodGold
import com.example.ui.theme.DeFittBlack
import com.example.ui.theme.DeFittOutline
import com.example.ui.theme.DeFittPureBlack
import com.example.ui.theme.DeFittSurface
import com.example.ui.theme.DeFittSurfaceElevated
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenContainer
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.DeFittMainState

@Composable
fun HomeScreen(
    state: DeFittMainState,
    contentPadding: PaddingValues,
    onOpenWorkout: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenBodyScanner: () -> Unit,
    onOpenFoodScanner: () -> Unit,
    onOpenEvolution: () -> Unit,
    onActivityPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    val user = state.activeUser
    val todayLog = state.todayLog
    val stepsToday = todayLog?.steps ?: 0L
    val stepGoal = (user?.dailyStepGoal ?: 6000).coerceAtLeast(1)
    val stepProgress = (stepsToday.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1f)

    var hasActivityPerm by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val activityPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasActivityPerm = granted
        if (granted) onActivityPermissionGranted()
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header: Logo de DeFitt + Saldo de COD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DeFittOfficialLogo(size = 46.dp)
                    Column {
                        Text(
                            text = "Hola, ${user?.nickname ?: "Atleta"}",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextWhite
                        )
                        OfflineBadge()
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onOpenWallet() }
                        .testTag("home_cod_wallet_chip")
                ) {
                    CodBalanceChip(balance = user?.codBalance ?: 0L)
                }
            }

            // Sensor or Permission notice (never fake steps!)
            if (!state.stepSensorAvailable) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeFittSurfaceElevated),
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.WarningAmber, null, tint = WarningAmber)
                        Text(
                            "Este dispositivo no reporta un sensor físico de pasos (TYPE_STEP_COUNTER).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else if (!hasActivityPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NeonGreenContainer),
                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Activa el sensor de actividad para contar pasos y ganar COD.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWhite,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { activityPermLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
                            modifier = Modifier.testTag("home_grant_activity_btn")
                        ) {
                            Text("ACTIVAR", color = NeonGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Hero Personal 3D Avatar in the center
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(255.dp)
            ) {
                Avatar3DView(
                    avatarParams = state.avatarParams,
                    equippedItemIds = state.equipped.map { it.itemId },
                    modifier = Modifier.fillMaxSize(),
                    testTag = "home_avatar_3d"
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(DeFittSurfaceElevated.copy(alpha = 0.85f))
                            .border(1.dp, DeFittOutline, RoundedCornerShape(14.dp))
                            .clickable { onOpenBodyScanner() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("home_body_scan_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, null, tint = NeonGreen, modifier = Modifier.size(15.dp))
                            Text(
                                text = if (state.avatarParams.calibratedViaCamera) "Recalibrar 3D" else "Escáner 3D",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextWhite
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(DeFittSurfaceElevated.copy(alpha = 0.85f))
                            .border(1.dp, DeFittOutline, RoundedCornerShape(14.dp))
                            .clickable { onOpenEvolution() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("home_evolution_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Timeline, null, tint = CodGold, modifier = Modifier.size(15.dp))
                            Text("Mi Evolución", style = MaterialTheme.typography.labelSmall, color = TextWhite)
                        }
                    }
                }

                if (state.weeklyScanDue) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CodGold.copy(alpha = 0.16f))
                            .border(1.dp, CodGold, RoundedCornerShape(12.dp))
                            .clickable { onOpenBodyScanner() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Escaneo semanal",
                            style = MaterialTheme.typography.labelSmall,
                            color = CodGold
                        )
                    }
                }
            }

            // 3. Brief Activity Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DeFittSurface),
                border = BorderStroke(1.dp, DeFittOutline),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null, tint = NeonGreen)
                            Text(
                                text = "$stepsToday / $stepGoal pasos",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextWhite,
                                modifier = Modifier.testTag("home_steps_text")
                            )
                        }
                        Text(
                            text = "+${todayLog?.codEarned ?: 0L} COD hoy",
                            style = MaterialTheme.typography.labelLarge,
                            color = CodGold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { stepProgress },
                        color = NeonGreen,
                        trackColor = DeFittSurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.LocalDining, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Text(
                                text = "${(todayLog?.caloriesTotal ?: 0f).toInt()} kcal aprox.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = "Rutinas hoy: ${todayLog?.workoutsCompleted ?: 0} • XP: ${user?.xpTotal ?: 0}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            // 4. Primary Action Button: ENTRENAMIENTO
            Button(
                onClick = onOpenWorkout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = DeFittPureBlack
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("home_workout_button")
            ) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = "ENTRENAMIENTO",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Secondary quick tools: Escáner de Comida & Monedero COD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenFoodScanner,
                    border = BorderStroke(1.dp, DeFittOutline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("home_food_scanner_button")
                ) {
                    Icon(Icons.Default.Restaurant, null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Escáner Comida", maxLines = 1)
                }

                OutlinedButton(
                    onClick = onOpenWallet,
                    border = BorderStroke(1.dp, CodGold.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CodGold),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("home_wallet_button")
                ) {
                    Icon(Icons.Default.MonetizationOn, null, tint = CodGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Monedero COD", maxLines = 1)
                }
            }
        }
    }
}
