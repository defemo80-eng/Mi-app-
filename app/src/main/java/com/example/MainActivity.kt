package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.DeFittRoute
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BodyScannerScreen
import com.example.ui.screens.EvolutionScreen
import com.example.ui.screens.FoodScannerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LogrosScreen
import com.example.ui.screens.PerfilScreen
import com.example.ui.screens.RegistroScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TiendaScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.screens.WorkoutScreen
import com.example.ui.theme.DeFittBlack
import com.example.ui.theme.DeFittPureBlack
import com.example.ui.theme.DeFittSurface
import com.example.ui.theme.DeFittTheme
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenContainer
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.DeFittViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeFittTheme {
                DeFittAppRoot()
            }
        }
    }
}

private data class BottomTab(
    val route: DeFittRoute,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
fun DeFittAppRoot(vm: DeFittViewModel = viewModel()) {
    val navController = rememberNavController()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val savedAccounts by vm.savedAccounts.collectAsStateWithLifecycle()
    val statusBanner by vm.statusBanner.collectAsStateWithLifecycle()
    val authError by vm.authError.collectAsStateWithLifecycle()
    val recoveryCode by vm.generatedRecoveryCode.collectAsStateWithLifecycle()
    val previewItemId by vm.previewItemId.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        vm.reconcileDayRollover()
        if (state.activeUser != null) {
            vm.startStepTrackingIfPermitted()
        }
    }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            vm.clearBanner()
        }
    }

    val tabs = remember {
        listOf(
            BottomTab(DeFittRoute.Home, "Inicio", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
            BottomTab(DeFittRoute.Registro, "Registro", Icons.Filled.EventNote, Icons.Outlined.EventNote, "nav_registro"),
            BottomTab(DeFittRoute.Tienda, "Tienda", Icons.Filled.ShoppingBag, Icons.Outlined.ShoppingBag, "nav_tienda"),
            BottomTab(DeFittRoute.Logros, "Logros", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_logros"),
            BottomTab(DeFittRoute.Perfil, "Perfil", Icons.Filled.Person, Icons.Outlined.Person, "nav_perfil")
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination
    val isTopLevelTab = tabs.any { tab -> currentDest?.hasRoute(tab.route::class) == true }
    val isWideScreen = LocalConfiguration.current.screenWidthDp >= 720

    val navigateTopTab: (DeFittRoute) -> Unit = { target ->
        navController.navigate(target) {
            popUpTo<DeFittRoute.Home> { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = DeFittBlack,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = DeFittSurface,
                    contentColor = NeonGreen
                )
            }
        },
        bottomBar = {
            if (isTopLevelTab && !isWideScreen) {
                NavigationBar(containerColor = DeFittSurface) {
                    tabs.forEach { tab ->
                        val selected = currentDest?.hasRoute(tab.route::class) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateTopTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonGreen,
                                selectedTextColor = NeonGreen,
                                indicatorColor = NeonGreenContainer,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize()) {
            if (isTopLevelTab && isWideScreen) {
                NavigationRail(
                    containerColor = DeFittSurface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    tabs.forEach { tab ->
                        val selected = currentDest?.hasRoute(tab.route::class) == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateTopTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = NeonGreen,
                                selectedTextColor = NeonGreen,
                                indicatorColor = NeonGreenContainer,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag(tab.tag)
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = DeFittRoute.Splash
                ) {
                    composable<DeFittRoute.Splash> {
                        SplashScreen(
                            isLoadingSession = state.isLoadingSession,
                            hasActiveUser = state.activeUser != null,
                            onNavigateToHome = {
                                navController.navigate(DeFittRoute.Home) {
                                    popUpTo<DeFittRoute.Splash> { inclusive = true }
                                }
                            },
                            onNavigateToAuth = {
                                navController.navigate(DeFittRoute.Auth) {
                                    popUpTo<DeFittRoute.Splash> { inclusive = true }
                                }
                            }
                        )
                    }

                    composable<DeFittRoute.Auth> {
                        AuthScreen(
                            savedAccounts = savedAccounts,
                            authError = authError,
                            generatedRecoveryCode = recoveryCode,
                            onClearError = { vm.clearAuthError() },
                            onDismissRecoveryCode = {
                                vm.dismissRecoveryCode()
                                navController.navigate(DeFittRoute.Home) {
                                    popUpTo<DeFittRoute.Auth> { inclusive = true }
                                }
                            },
                            onLogin = { nick, pin ->
                                vm.login(nick, pin) {
                                    navController.navigate(DeFittRoute.Home) {
                                        popUpTo<DeFittRoute.Auth> { inclusive = true }
                                    }
                                }
                            },
                            onRegister = { name, nick, age, pin ->
                                vm.register(name, nick, age, pin) {
                                    // If recovery code dialog is shown, navigation happens on dismiss
                                }
                            },
                            onRecover = { nick, code, newPin ->
                                vm.recoverPin(nick, code, newPin) {
                                    // Navigation happens on recovery code confirmation
                                }
                            },
                            onImportBackup = { uri ->
                                vm.importBackupFromUri(context, uri) {
                                    navController.navigate(DeFittRoute.Home) {
                                        popUpTo<DeFittRoute.Auth> { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable<DeFittRoute.Home> {
                        HomeScreen(
                            state = state,
                            contentPadding = innerPadding,
                            onOpenWorkout = { navController.navigate(DeFittRoute.Workout) },
                            onOpenWallet = { navController.navigate(DeFittRoute.Wallet) },
                            onOpenBodyScanner = { navController.navigate(DeFittRoute.BodyScanner) },
                            onOpenFoodScanner = { navController.navigate(DeFittRoute.FoodScanner) },
                            onOpenEvolution = { navController.navigate(DeFittRoute.Evolution) },
                            onActivityPermissionGranted = { vm.startStepTrackingIfPermitted() }
                        )
                    }

                    composable<DeFittRoute.Registro> {
                        RegistroScreen(
                            state = state,
                            contentPadding = innerPadding,
                            onSelectDate = { vm.selectHistoryDate(it) },
                            onOpenFoodScanner = { navController.navigate(DeFittRoute.FoodScanner) },
                            onDeleteFood = { id, dateKey -> vm.deleteFood(id, dateKey) }
                        )
                    }

                    composable<DeFittRoute.Tienda> {
                        TiendaScreen(
                            state = state,
                            previewItemId = previewItemId,
                            contentPadding = innerPadding,
                            onPreviewItem = { vm.setPreviewStoreItem(it) },
                            onBuyItem = { vm.buyStoreItem(it) },
                            onToggleEquip = { vm.toggleEquip(it) }
                        )
                    }

                    composable<DeFittRoute.Logros> {
                        LogrosScreen(
                            state = state,
                            contentPadding = innerPadding,
                            onOpenEvolution = { navController.navigate(DeFittRoute.Evolution) }
                        )
                    }

                    composable<DeFittRoute.Perfil> {
                        PerfilScreen(
                            state = state,
                            contentPadding = innerPadding,
                            onUpdateProfile = { name, age, level, goal ->
                                vm.updateProfile(name, age, level, goal)
                            },
                            onOpenBodyScanner = { navController.navigate(DeFittRoute.BodyScanner) },
                            onToggleNotifications = { vm.setNotificationsEnabled(it) },
                            onToggleStepService = { vm.setStepServiceEnabled(it) },
                            onExportBackup = { uri -> vm.exportBackupToUri(context, uri) },
                            onImportBackup = { uri -> vm.importBackupFromUri(context, uri) {} },
                            onOpenAppSettings = { vm.openAppSettings(context) },
                            onLogout = {
                                vm.logout {
                                    navController.navigate(DeFittRoute.Auth) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            },
                            onDeleteAccount = {
                                vm.deleteActiveAccount {
                                    navController.navigate(DeFittRoute.Auth) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable<DeFittRoute.Workout> {
                        WorkoutScreen(
                            routine = state.dailyRoutine,
                            onConfirmWorkout = { completed -> vm.confirmWorkout(completed) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<DeFittRoute.FoodScanner> {
                        FoodScannerScreen(
                            onSaveFood = { meal, item, grams, conf, byAi ->
                                vm.logFood(meal, item, grams, conf, byAi)
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<DeFittRoute.BodyScanner> {
                        BodyScannerScreen(
                            currentAvatar = state.avatarParams,
                            equippedIds = state.equipped.map { it.itemId },
                            onSaveScan = { type, angles, sh, wa, hip, limb, pos, build, notes, h, skin, hairC, hairS ->
                                vm.saveScanResult(type, angles, sh, wa, hip, limb, pos, build, notes, h, skin, hairC, hairS)
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<DeFittRoute.Wallet> {
                        WalletScreen(
                            state = state,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<DeFittRoute.Evolution> {
                        EvolutionScreen(
                            state = state,
                            onOpenBodyScanner = { navController.navigate(DeFittRoute.BodyScanner) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
