package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.engine.DateUtils
import com.example.domain.model.ExperienceLevel
import com.example.ui.components.DeFittOfficialLogo
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
import com.example.ui.viewmodel.DeFittMainState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PerfilScreen(
    state: DeFittMainState,
    contentPadding: PaddingValues,
    onUpdateProfile: (name: String, age: Int, level: ExperienceLevel, stepGoal: Int) -> Unit,
    onOpenBodyScanner: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleStepService: (Boolean) -> Unit,
    onExportBackup: (Uri) -> Unit,
    onImportBackup: (Uri) -> Unit,
    onOpenAppSettings: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val context = LocalContext.current
    val user = state.activeUser

    var name by remember(user?.id, user?.name) { mutableStateOf(user?.name ?: "") }
    var ageText by remember(user?.id, user?.age) { mutableStateOf((user?.age ?: 25).toString()) }
    var stepGoalText by remember(user?.id, user?.dailyStepGoal) { mutableStateOf((user?.dailyStepGoal ?: 6000).toString()) }
    var level by remember(user?.id, user?.experienceLevel) {
        mutableStateOf(
            try {
                ExperienceLevel.valueOf(user?.experienceLevel ?: ExperienceLevel.PRINCIPIANTE.name)
            } catch (_: Exception) {
                ExperienceLevel.PRINCIPIANTE
            }
        )
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasActivityPerm by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
                    PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var hasNotifPerm by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPerm = it
    }
    val activityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasActivityPerm = it
        if (it) onToggleStepService(true)
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasNotifPerm = it
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) onExportBackup(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportBackup(uri)
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = DeFittSurfaceElevated,
            title = { Text("¿Eliminar cuenta local?", color = DangerRed) },
            text = {
                Text(
                    "Se borrarán todos los registros, historial, avatar, compras y COD de '${user?.nickname ?: ""}' en este teléfono. Exporta una copia antes si deseas conservarla.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = TextWhite),
                    modifier = Modifier.testTag("confirm_delete_account_btn")
                ) {
                    Text("Eliminar definitivamente")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = TextWhite)
                }
            }
        )
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DeFittOfficialLogo(size = 54.dp)
                Column {
                    Text("Perfil y Configuración", style = MaterialTheme.typography.headlineMedium, color = TextWhite)
                    Text(
                        "@${user?.nickname ?: ""} • Cuenta creada ${DateUtils.formatDisplayDate(DateUtils.dateKeyFromMillis(user?.createdAt ?: 0L))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Personal Data Editor
            Card(
                colors = CardDefaults.cardColors(containerColor = DeFittSurface),
                border = BorderStroke(1.dp, DeFittOutline),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Datos personales y meta", style = MaterialTheme.typography.titleMedium, color = NeonGreen)
                    val tfColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = DeFittOutline,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it.filter { c -> c.isDigit() } },
                            label = { Text("Edad") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = tfColors,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = stepGoalText,
                            onValueChange = { stepGoalText = it.filter { c -> c.isDigit() } },
                            label = { Text("Meta diaria pasos") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = tfColors,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Nivel de experiencia", style = MaterialTheme.typography.labelLarge, color = TextWhite)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExperienceLevel.entries.forEach { exp ->
                            FilterChip(
                                selected = level == exp,
                                onClick = { level = exp },
                                label = { Text(exp.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonGreen,
                                    selectedLabelColor = DeFittPureBlack
                                )
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onUpdateProfile(
                                name,
                                ageText.toIntOrNull() ?: 25,
                                level,
                                stepGoalText.toIntOrNull() ?: 6000
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DeFittPureBlack),
                        modifier = Modifier.fillMaxWidth().testTag("save_profile_btn")
                    ) {
                        Text("Guardar cambios", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Avatar Config
            OutlinedButton(
                onClick = onOpenBodyScanner,
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("profile_avatar_config_btn")
            ) {
                Text("Configurar / Recalibrar Avatar 3D")
            }

            // Permissions & Hardware Sensors
            Card(
                colors = CardDefaults.cardColors(containerColor = DeFittSurface),
                border = BorderStroke(1.dp, DeFittOutline),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Sensores, permisos y automatización", style = MaterialTheme.typography.titleMedium, color = NeonGreen)

                    PermissionRow(
                        icon = Icons.Default.CameraAlt,
                        title = "Cámara (Escáner corporal y comida)",
                        granted = hasCameraPerm,
                        onRequest = { cameraLauncher.launch(Manifest.permission.CAMERA) }
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        PermissionRow(
                            icon = Icons.Default.Sensors,
                            title = "Sensor de pasos (${if (state.stepSensorAvailable) "Disponible" else "No detectado"})",
                            granted = hasActivityPerm,
                            onRequest = { activityLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionRow(
                            icon = Icons.Default.NightlightRound,
                            title = "Notificaciones de servicio y avisos",
                            granted = hasNotifPerm,
                            onRequest = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Conteo en segundo plano", color = TextWhite)
                        Switch(
                            checked = state.stepServiceEnabled,
                            onCheckedChange = onToggleStepService,
                            colors = SwitchDefaults.colors(checkedThumbColor = DeFittPureBlack, checkedTrackColor = NeonGreen)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recordatorios locales", color = TextWhite)
                        Switch(
                            checked = state.notificationsEnabled,
                            onCheckedChange = onToggleNotifications,
                            colors = SwitchDefaults.colors(checkedThumbColor = DeFittPureBlack, checkedTrackColor = NeonGreen)
                        )
                    }

                    TextButton(onClick = onOpenAppSettings) {
                        Text("Abrir ajustes del sistema", color = TextSecondary)
                    }
                }
            }

            // Local Backup Export / Import (no server!)
            Card(
                colors = CardDefaults.cardColors(containerColor = DeFittSurface),
                border = BorderStroke(1.dp, CodGold.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Backup, null, tint = CodGold)
                        Text("Copias de seguridad sin internet", style = MaterialTheme.typography.titleMedium, color = CodGold)
                    }
                    Text(
                        "Exporta toda tu cuenta, avatar, COD e historial a un archivo local para trasladarlo a otro teléfono.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                exportLauncher.launch("defitt_backup_${user?.nickname ?: "cuenta"}.json")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CodGold, contentColor = DeFittPureBlack),
                            modifier = Modifier.weight(1f).testTag("export_backup_btn")
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Exportar")
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                            border = BorderStroke(1.dp, CodGold),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CodGold),
                            modifier = Modifier.weight(1f).testTag("import_backup_btn")
                        ) {
                            Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Importar")
                        }
                    }
                }
            }

            // Session & Delete Account
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onLogout,
                    border = BorderStroke(1.dp, DeFittOutline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("logout_btn")
                ) {
                    Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Cerrar sesión")
                }

                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("delete_account_btn")
                ) {
                    Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Eliminar cuenta")
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, null, tint = if (granted) SuccessGreen else TextSecondary, modifier = Modifier.size(18.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextWhite)
        }
        if (granted) {
            Text("Activo", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
        } else {
            TextButton(onClick = onRequest) {
                Text("Permitir", color = NeonGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}
