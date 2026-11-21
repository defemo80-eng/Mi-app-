package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.UserAccountEntity
import com.example.ui.components.DeFittOfficialLogo
import com.example.ui.components.OfflineBadge
import com.example.ui.theme.CodGold
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeFittBlack
import com.example.ui.theme.DeFittOutline
import com.example.ui.theme.DeFittPureBlack
import com.example.ui.theme.DeFittSurface
import com.example.ui.theme.DeFittSurfaceElevated
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenContainer
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isLoadingSession: Boolean,
    hasActiveUser: Boolean,
    onNavigateToHome: () -> Unit,
    onNavigateToAuth: () -> Unit
) {
    LaunchedEffect(isLoadingSession, hasActiveUser) {
        if (!isLoadingSession) {
            delay(850L)
            if (hasActiveUser) onNavigateToHome() else onNavigateToAuth()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeFittPureBlack, DeFittBlack, DeFittSurface)
                )
            )
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            DeFittOfficialLogo(size = 132.dp)
            Text(
                text = "DeFitt",
                style = MaterialTheme.typography.displayLarge,
                color = TextWhite
            )
            Text(
                text = "Entrenamiento, avatar 3D y registro nutricional 100% local",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            OfflineBadge()
            Spacer(modifier = Modifier.height(12.dp))
            CircularProgressIndicator(
                color = NeonGreen,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    savedAccounts: List<UserAccountEntity>,
    authError: String?,
    generatedRecoveryCode: String?,
    onClearError: () -> Unit,
    onDismissRecoveryCode: () -> Unit,
    onLogin: (nickname: String, pin: String) -> Unit,
    onRegister: (name: String, nickname: String, ageText: String, pin: String) -> Unit,
    onRecover: (nickname: String, code: String, newPin: String) -> Unit,
    onImportBackup: (Uri) -> Unit
) {
    val context = LocalContext.current
    var mode by remember { mutableIntStateOf(if (savedAccounts.isEmpty()) 1 else 0) } // 0=Login, 1=Register, 2=Recover
    var name by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf(savedAccounts.firstOrNull()?.nickname ?: "") }
    var ageText by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var recoveryCodeInput by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportBackup(uri)
    }

    if (generatedRecoveryCode != null) {
        AlertDialog(
            onDismissRequest = onDismissRecoveryCode,
            containerColor = DeFittSurfaceElevated,
            title = {
                Text("Guarda tu código local de recuperación", color = NeonGreen)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Como DeFitt funciona 100% sin internet ni servidores, este código es la única forma de recuperar tu acceso si olvidas tu PIN.",
                        color = TextSecondary
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeFittBlack, RoundedCornerShape(12.dp))
                            .border(1.dp, CodGold, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = generatedRecoveryCode,
                            style = MaterialTheme.typography.headlineMedium,
                            color = CodGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("recovery_code_display")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismissRecoveryCode,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DeFittPureBlack),
                    modifier = Modifier.testTag("recovery_code_confirm_button")
                ) {
                    Text("Ya lo guardé")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeFittBlack)
            .systemBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DeFittOfficialLogo(size = 64.dp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Bienvenido a DeFitt",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextWhite
                    )
                    OfflineBadge()
                }
            }

            // Mode Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(0, "Entrar", "auth_tab_login"),
                    Triple(1, "Crear cuenta", "auth_tab_register"),
                    Triple(2, "Recuperar", "auth_tab_recover")
                ).forEach { (idx, label, tag) ->
                    FilterChip(
                        selected = mode == idx,
                        onClick = {
                            mode = idx
                            onClearError()
                        },
                        label = { Text(label, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonGreen,
                            selectedLabelColor = DeFittPureBlack,
                            containerColor = DeFittSurface,
                            labelColor = TextWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag(tag)
                    )
                }
            }

            if (savedAccounts.isNotEmpty() && mode == 0) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Cuentas en este dispositivo:", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        savedAccounts.forEach { acc ->
                            FilterChip(
                                selected = nickname.equals(acc.nickname, ignoreCase = true),
                                onClick = { nickname = acc.nickname },
                                label = { Text("${acc.nickname} (${acc.codBalance} COD)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonGreenContainer,
                                    selectedLabelColor = NeonGreen
                                )
                            )
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = DeFittSurface),
                border = BorderStroke(1.dp, DeFittOutline),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val tfColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = DeFittOutline,
                        focusedLabelColor = NeonGreen,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = NeonGreen
                    )

                    if (mode == 1) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nombre completo") },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = NeonGreen) },
                            singleLine = true,
                            colors = tfColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input")
                        )
                    }

                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("Apodo") },
                        leadingIcon = { Icon(Icons.Default.PersonAdd, null, tint = NeonGreen) },
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_nickname_input")
                    )

                    if (mode == 1) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Edad (años)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = tfColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_age_input")
                        )
                    }

                    if (mode == 2) {
                        OutlinedTextField(
                            value = recoveryCodeInput,
                            onValueChange = { recoveryCodeInput = it.uppercase() },
                            label = { Text("Código de recuperación (ej. ABCD-1234-EFGH)") },
                            leadingIcon = { Icon(Icons.Default.Key, null, tint = CodGold) },
                            singleLine = true,
                            colors = tfColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recover_code_input")
                        )
                    }

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it },
                        label = { Text(if (mode == 2) "Nuevo PIN o contraseña (mín. 4)" else "PIN o contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = NeonGreen) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_pin_input")
                    )

                    if (authError != null) {
                        Text(
                            text = authError,
                            color = DangerRed,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag("auth_error_text")
                        )
                    }

                    Button(
                        onClick = {
                            when (mode) {
                                0 -> onLogin(nickname, pin)
                                1 -> onRegister(name, nickname, ageText, pin)
                                else -> onRecover(nickname, recoveryCodeInput, pin)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = DeFittPureBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("auth_submit_button")
                    ) {
                        Icon(
                            imageVector = when (mode) {
                                1 -> Icons.Default.PersonAdd
                                2 -> Icons.Default.Restore
                                else -> Icons.Default.Lock
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = when (mode) {
                                0 -> "INICIAR SESIÓN LOCAL"
                                1 -> "CREAR CUENTA LOCAL"
                                else -> "RESTABLECER PIN LOCAL"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                border = BorderStroke(1.dp, CodGold.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CodGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("import_backup_auth_button")
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Importar copia de seguridad desde otro teléfono")
            }

            Text(
                text = "Todos tus datos, historial, avatar 3D y COD permanecen cifrados y guardados exclusivamente en este dispositivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
