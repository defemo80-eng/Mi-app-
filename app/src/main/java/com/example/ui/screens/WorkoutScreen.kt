package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.WorkoutRoutine
import com.example.ui.components.ExerciseAnimationView
import com.example.ui.theme.CodGold
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeFittBlack
import com.example.ui.theme.DeFittOutline
import com.example.ui.theme.DeFittPureBlack
import com.example.ui.theme.DeFittSurface
import com.example.ui.theme.DeFittSurfaceElevated
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    routine: WorkoutRoutine,
    onConfirmWorkout: (completed: Boolean) -> Unit,
    onBack: () -> Unit
) {
    var activeIndex by remember { mutableIntStateOf(0) }
    var isResting by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    val currentExercise = routine.exercises[activeIndex]
    var secondsLeft by remember(activeIndex, isResting) {
        mutableIntStateOf(if (isResting) currentExercise.restSeconds else currentExercise.durationSeconds)
    }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, activeIndex, isResting) {
        while (isRunning && secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
            if (secondsLeft == 0) {
                if (!isResting) {
                    isResting = true
                } else {
                    isResting = false
                    if (activeIndex < routine.exercises.lastIndex) {
                        activeIndex++
                    } else {
                        isRunning = false
                        showConfirmationDialog = true
                    }
                }
            }
        }
    }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            containerColor = DeFittSurfaceElevated,
            title = {
                Text(
                    text = "¿Realizaste tus ejercicios?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextWhite
                )
            },
            text = {
                Text(
                    text = "Tu confirmación registra la rutina en tu historial diario sin alterar artificialmente la forma de tu avatar.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        onConfirmWorkout(true)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreen,
                        contentColor = DeFittPureBlack
                    ),
                    modifier = Modifier.testTag("workout_confirm_yes_button")
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("SÍ", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        onConfirmWorkout(false)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DangerRed,
                        contentColor = TextWhite
                    ),
                    modifier = Modifier.testTag("workout_confirm_no_button")
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("NO", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        containerColor = DeFittBlack,
        topBar = {
            TopAppBar(
                title = { Text("Entrenamiento Personalizado", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("workout_back_button")) {
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
                .padding(padding)
                .systemBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeFittSurface),
                    border = BorderStroke(1.dp, DeFittOutline),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(routine.title, style = MaterialTheme.typography.titleLarge, color = NeonGreen)
                        Text(routine.targetNote, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(
                            "Dificultad: ${routine.difficulty} • ~${routine.estimatedMinutes} min",
                            style = MaterialTheme.typography.labelSmall,
                            color = CodGold
                        )
                    }
                }

                // Active Exercise Card + Animation + Live Timer
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeFittSurfaceElevated),
                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ejercicio ${activeIndex + 1} de ${routine.exercises.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonGreen
                            )
                            Text(
                                text = if (isResting) "DESCANSO" else currentExercise.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isResting) CodGold else TextSecondary
                            )
                        }

                        Text(
                            text = if (isResting) "Respira y descansa" else currentExercise.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextWhite
                        )

                        ExerciseAnimationView(
                            animType = currentExercise.animType,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(165.dp)
                        )

                        Text(
                            text = currentExercise.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = DeFittSurface),
                            border = BorderStroke(1.dp, CodGold.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.HealthAndSafety, "Seguridad", tint = CodGold, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Seguridad: ${currentExercise.safetyNote}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextWhite
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(currentExercise.prescriptionLabel, style = MaterialTheme.typography.titleMedium, color = TextWhite)
                                Text("Descanso: ${currentExercise.restSeconds}s • ${currentExercise.difficulty}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                            Text(
                                text = "${secondsLeft}s",
                                style = MaterialTheme.typography.displayLarge,
                                color = if (isResting) CodGold else NeonGreen
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { isRunning = !isRunning },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreen,
                                    contentColor = DeFittPureBlack
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("workout_play_pause_btn")
                            ) {
                                Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(if (isRunning) "Pausar" else "Iniciar tiempo")
                            }

                            OutlinedButton(
                                onClick = {
                                    isRunning = false
                                    isResting = false
                                    if (activeIndex < routine.exercises.lastIndex) {
                                        activeIndex++
                                    } else {
                                        showConfirmationDialog = true
                                    }
                                },
                                border = BorderStroke(1.dp, DeFittOutline),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("workout_next_btn")
                            ) {
                                Icon(Icons.Default.SkipNext, null)
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(if (activeIndex < routine.exercises.lastIndex) "Siguiente" else "Finalizar")
                            }
                        }
                    }
                }

                // Finish / Confirm routine directly
                Button(
                    onClick = { showConfirmationDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = DeFittPureBlack),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("workout_finish_confirm_btn")
                ) {
                    Text("Terminar rutina y confirmar", style = MaterialTheme.typography.labelLarge)
                }

                // Full list of exercises in routine
                Text("Plan completo de hoy", style = MaterialTheme.typography.titleLarge, color = TextWhite)
                routine.exercises.forEachIndexed { idx, ex ->
                    Card(
                        onClick = {
                            isRunning = false
                            isResting = false
                            activeIndex = idx
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (idx == activeIndex) DeFittSurfaceElevated else DeFittSurface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (idx == activeIndex) NeonGreen else DeFittOutline
                        ),
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${idx + 1}. ${ex.name}", style = MaterialTheme.typography.titleMedium, color = TextWhite)
                                Text("${ex.prescriptionLabel} • Descanso ${ex.restSeconds}s", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                            Text(ex.category, style = MaterialTheme.typography.labelSmall, color = NeonGreen)
                        }
                    }
                }
            }
        }
    }
}
