package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DeFittApplication
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.BodyScanEntity
import com.example.data.local.entity.CodTransactionEntity
import com.example.data.local.entity.DailyLogEntity
import com.example.data.local.entity.EquippedItemEntity
import com.example.data.local.entity.FoodEntryEntity
import com.example.data.local.entity.StepHardwareStateEntity
import com.example.data.local.entity.StorePurchaseEntity
import com.example.data.local.entity.UserAccountEntity
import com.example.data.local.entity.WorkoutLogEntity
import com.example.data.repository.AuthResult
import com.example.data.repository.PurchaseResult
import com.example.domain.engine.AvatarEvolutionEngine
import com.example.domain.engine.DateUtils
import com.example.domain.engine.WorkoutGenerator
import com.example.domain.hardware.StepTrackerManager
import com.example.domain.model.AvatarParams
import com.example.domain.model.ExperienceLevel
import com.example.domain.model.FoodCatalogItem
import com.example.domain.model.MealType
import com.example.domain.model.ScanComparison
import com.example.domain.model.StoreItem
import com.example.domain.model.WorkoutRoutine
import com.example.service.StepCounterForegroundService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeFittMainState(
    val isLoadingSession: Boolean = true,
    val activeUser: UserAccountEntity? = null,
    val avatarParams: AvatarParams = AvatarParams(),
    val todayLog: DailyLogEntity? = null,
    val allDailyLogs: List<DailyLogEntity> = emptyList(),
    val stepState: StepHardwareStateEntity? = null,
    val transactions: List<CodTransactionEntity> = emptyList(),
    val todayFoods: List<FoodEntryEntity> = emptyList(),
    val selectedDateFoods: List<FoodEntryEntity> = emptyList(),
    val selectedDateWorkouts: List<WorkoutLogEntity> = emptyList(),
    val allWorkouts: List<WorkoutLogEntity> = emptyList(),
    val bodyScans: List<BodyScanEntity> = emptyList(),
    val purchases: List<StorePurchaseEntity> = emptyList(),
    val equipped: List<EquippedItemEntity> = emptyList(),
    val achievements: List<AchievementEntity> = emptyList(),
    val selectedHistoryDate: String = DateUtils.todayKey(),
    val notificationsEnabled: Boolean = true,
    val stepServiceEnabled: Boolean = true,
    val stepSensorAvailable: Boolean = true,
    val weeklyScanDue: Boolean = false,
    val daysSinceLastScan: Int = 0,
    val scanComparison: ScanComparison = AvatarEvolutionEngine.compareScans(null, null),
    val dailyRoutine: WorkoutRoutine = WorkoutGenerator.generateDailyRoutine(25, ExperienceLevel.PRINCIPIANTE, 0L)
)

@OptIn(ExperimentalCoroutinesApi::class)
class DeFittViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DeFittApplication
    private val repo = app.repository
    val stepTracker = app.stepTrackerManager

    private val _selectedHistoryDate = MutableStateFlow(DateUtils.todayKey())
    val selectedHistoryDate: StateFlow<String> = _selectedHistoryDate.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _generatedRecoveryCode = MutableStateFlow<String?>(null)
    val generatedRecoveryCode: StateFlow<String?> = _generatedRecoveryCode.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _previewItemId = MutableStateFlow<String?>(null)
    val previewItemId: StateFlow<String?> = _previewItemId.asStateFlow()

    val savedAccounts: StateFlow<List<UserAccountEntity>> = repo.observeAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val userIdFlow = repo.activeUserIdFlow

    private val userFlow = userIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(null) else repo.observeUser(uid)
    }

    private val todayLogFlow = userIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(null) else repo.observeDailyLog(uid, DateUtils.todayKey())
    }

    private val allLogsFlow = userIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else repo.observeDailyLogs(uid)
    }

    private val stepStateFlow = userIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(null) else repo.observeStepState(uid)
    }

    private val transactionsFlow = userIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else repo.observeTransactions(uid)
    }

    private val todayFoodsFlow = userIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else repo.observeFoodsForDate(uid, DateUtils.todayKey())
    }

    private val selectedDateFoodsFlow = combine(userIdFlow, _selectedHistoryDate) { uid, date ->
        uid to date
    }.flatMapLatest { (uid, date) ->
        if (uid == null) flowOf(emptyList()) else repo.observeFoodsForDate(uid, date)
    }

    private val selectedDateWorkoutsFlow = combine(userIdFlow, _selectedHistoryDate) { uid, date ->
        uid to date
    }.flatMapLatest { (uid, date) ->
        if (uid == null) flowOf(emptyList()) else repo.observeWorkoutsForDate(uid, date)
    }

    private val allWorkoutsFlow = userIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else repo.observeAllWorkouts(uid)
    }

    private val scansFlow = userIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else repo.observeScans(uid)
    }

    private val purchasesFlow = userIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else repo.observePurchases(uid)
    }

    private val equippedFlow = userIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else repo.observeEquipped(uid)
    }

    private val achievementsFlow = userIdFlow.flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList()) else repo.observeAchievements(uid)
    }

    private val partA = combine(
        userFlow,
        todayLogFlow,
        allLogsFlow,
        stepStateFlow,
        transactionsFlow
    ) { user, todayLog, allLogs, stepState, txs ->
        Tuple5(user, todayLog, allLogs, stepState, txs)
    }

    private val partB = combine(
        todayFoodsFlow,
        selectedDateFoodsFlow,
        selectedDateWorkoutsFlow,
        allWorkoutsFlow,
        scansFlow
    ) { todayFoods, selFoods, selWorkouts, allWorkouts, scans ->
        Tuple5(todayFoods, selFoods, selWorkouts, allWorkouts, scans)
    }

    private val partC = combine(
        purchasesFlow,
        equippedFlow,
        achievementsFlow,
        _selectedHistoryDate,
        combine(repo.notificationsEnabledFlow, repo.stepServiceEnabledFlow) { n, s -> n to s }
    ) { purchases, equipped, achievements, selDate, prefs ->
        Tuple5(purchases, equipped, achievements, selDate, prefs)
    }

    val uiState: StateFlow<DeFittMainState> = combine(partA, partB, partC) { a, b, c ->
        val user = a.first
        val todayLog = a.second
        val scans = b.fifth
        val avatar = user?.let { repo.parseAvatarParams(it.avatarConfigJson) } ?: AvatarParams()
        val exp = try {
            ExperienceLevel.valueOf(user?.experienceLevel ?: ExperienceLevel.PRINCIPIANTE.name)
        } catch (_: Exception) {
            ExperienceLevel.PRINCIPIANTE
        }
        val daysSinceScan = if (user != null && user.lastWeeklyScanAt > 0L) {
            DateUtils.daysBetween(user.lastWeeklyScanAt, System.currentTimeMillis())
        } else 999

        val comparison = AvatarEvolutionEngine.compareScans(scans.getOrNull(1), scans.firstOrNull())

        DeFittMainState(
            isLoadingSession = false,
            activeUser = user,
            avatarParams = avatar,
            todayLog = todayLog,
            allDailyLogs = a.third,
            stepState = a.fourth,
            transactions = a.fifth,
            todayFoods = b.first,
            selectedDateFoods = b.second,
            selectedDateWorkouts = b.third,
            allWorkouts = b.fourth,
            bodyScans = scans,
            purchases = c.first,
            equipped = c.second,
            achievements = c.third,
            selectedHistoryDate = c.fourth,
            notificationsEnabled = c.fifth.first,
            stepServiceEnabled = c.fifth.second,
            stepSensorAvailable = stepTracker.isStepSensorAvailable(),
            weeklyScanDue = user != null && (user.lastWeeklyScanAt == 0L || daysSinceScan >= 7),
            daysSinceLastScan = if (daysSinceScan == 999) 0 else daysSinceScan,
            scanComparison = comparison,
            dailyRoutine = WorkoutGenerator.generateDailyRoutine(
                age = user?.age ?: 25,
                experienceLevel = exp,
                recentSteps = todayLog?.steps ?: 0L
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DeFittMainState(
            isLoadingSession = true,
            stepSensorAvailable = stepTracker.isStepSensorAvailable()
        )
    )

    init {
        viewModelScope.launch {
            val uid = repo.getActiveUserId()
            if (uid != null) {
                val closed = repo.reconcilePendingClosuresIfNeeded(uid)
                if (closed > 0) {
                    _statusBanner.value = "Cierre diario automático completado para $closed día(s) anterior(es)."
                }
                startStepTrackingIfPermitted()
            }
        }
    }

    fun startStepTrackingIfPermitted() {
        stepTracker.startListening()
        if (stepTracker.hasActivityPermission() && stepTracker.isStepSensorAvailable()) {
            try {
                StepCounterForegroundService.start(app)
            } catch (_: Exception) {
                // Foreground service start restriction fallback handled by in-app StepTrackerManager
            }
        }
    }

    fun reconcileDayRollover() {
        viewModelScope.launch {
            val uid = repo.getActiveUserId() ?: return@launch
            val closed = repo.reconcilePendingClosuresIfNeeded(uid)
            if (closed > 0) {
                _statusBanner.value = "Cierre de medianoche procesado: se guardó tu historial y el saldo COD permanece intacto."
            }
        }
    }

    fun clearBanner() { _statusBanner.value = null }
    fun clearAuthError() { _authError.value = null }
    fun dismissRecoveryCode() { _generatedRecoveryCode.value = null }

    fun selectHistoryDate(dateKey: String) {
        _selectedHistoryDate.value = dateKey
    }

    fun setPreviewStoreItem(itemId: String?) {
        _previewItemId.value = itemId
    }

    fun register(name: String, nickname: String, ageText: String, pin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            val age = ageText.toIntOrNull() ?: -1
            when (val res = repo.registerAccount(name, nickname, age, pin)) {
                is AuthResult.Success -> {
                    _generatedRecoveryCode.value = res.recoveryCodePlain
                    startStepTrackingIfPermitted()
                    onSuccess()
                }
                is AuthResult.Error -> _authError.value = res.message
            }
        }
    }

    fun login(nickname: String, pin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            when (val res = repo.loginAccount(nickname, pin)) {
                is AuthResult.Success -> {
                    startStepTrackingIfPermitted()
                    onSuccess()
                }
                is AuthResult.Error -> _authError.value = res.message
            }
        }
    }

    fun recoverPin(nickname: String, code: String, newPin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            when (val res = repo.recoverAccountWithCode(nickname, code, newPin)) {
                is AuthResult.Success -> {
                    _generatedRecoveryCode.value = res.recoveryCodePlain
                    startStepTrackingIfPermitted()
                    onSuccess()
                }
                is AuthResult.Error -> _authError.value = res.message
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            stepTracker.stopListening()
            StepCounterForegroundService.stop(app)
            repo.logout()
            onDone()
        }
    }

    fun deleteActiveAccount(onDone: () -> Unit) {
        val uid = uiState.value.activeUser?.id ?: return
        viewModelScope.launch {
            stepTracker.stopListening()
            StepCounterForegroundService.stop(app)
            repo.deleteAccount(uid)
            onDone()
        }
    }

    fun updateProfile(name: String, age: Int, level: ExperienceLevel, stepGoal: Int) {
        val uid = uiState.value.activeUser?.id ?: return
        viewModelScope.launch {
            repo.updateProfileSettings(uid, name, age, level.name, stepGoal)
            _statusBanner.value = "Perfil actualizado localmente."
        }
    }

    fun updateAvatarStyle(skinToneHex: String, hairColorHex: String, hairStyle: Int, heightCm: Int) {
        val uid = uiState.value.activeUser?.id ?: return
        val current = uiState.value.avatarParams
        viewModelScope.launch {
            repo.updateAvatarAppearance(
                uid,
                current.copy(
                    skinToneHex = skinToneHex,
                    hairColorHex = hairColorHex,
                    hairStyle = hairStyle,
                    heightCm = heightCm.coerceIn(130, 215)
                )
            )
            _statusBanner.value = "Apariencia del avatar guardada."
        }
    }

    fun logFood(
        mealType: MealType,
        food: FoodCatalogItem,
        portionGrams: Int,
        confidenceLabel: String,
        recognizedByAi: Boolean
    ) {
        val uid = uiState.value.activeUser?.id ?: return
        viewModelScope.launch {
            repo.addFoodEntry(uid, mealType, food, portionGrams, confidenceLabel, recognizedByAi)
            _statusBanner.value = "${food.name} (${portionGrams}g) guardado en ${mealType.label}."
        }
    }

    fun deleteFood(entryId: String, dateKey: String) {
        val uid = uiState.value.activeUser?.id ?: return
        viewModelScope.launch {
            repo.deleteFoodEntry(uid, entryId, dateKey)
        }
    }

    fun confirmWorkout(completed: Boolean) {
        val uid = uiState.value.activeUser?.id ?: return
        val routine = uiState.value.dailyRoutine
        viewModelScope.launch {
            repo.recordWorkoutConfirmation(
                userId = uid,
                routineTitle = routine.title,
                difficulty = routine.difficulty,
                durationMinutes = routine.estimatedMinutes,
                exercisesSummary = routine.exercises.joinToString(", ") { it.name },
                completed = completed
            )
            _statusBanner.value = if (completed) {
                "¡Rutina completada y registrada! (+50 XP)"
            } else {
                "Rutina guardada como pendiente para hoy."
            }
        }
    }

    fun saveScanResult(
        scanType: String,
        anglesCaptured: Int,
        shoulderWidthRatio: Float,
        waistRatio: Float,
        hipWidthRatio: Float,
        limbThicknessRatio: Float,
        postureAlignmentScore: Float,
        estimatedBuildLabel: String,
        notes: String,
        heightCm: Int? = null,
        skinToneHex: String? = null,
        hairColorHex: String? = null,
        hairStyle: Int? = null
    ) {
        val uid = uiState.value.activeUser?.id ?: return
        viewModelScope.launch {
            repo.saveBodyScan(
                userId = uid,
                scanType = scanType,
                anglesCaptured = anglesCaptured,
                shoulderWidthRatio = shoulderWidthRatio,
                waistRatio = waistRatio,
                hipWidthRatio = hipWidthRatio,
                limbThicknessRatio = limbThicknessRatio,
                postureAlignmentScore = postureAlignmentScore,
                estimatedBuildLabel = estimatedBuildLabel,
                notes = notes,
                skinToneHex = skinToneHex,
                hairColorHex = hairColorHex,
                hairStyle = hairStyle,
                heightCm = heightCm
            )
            _statusBanner.value = "Escaneo guardado y avatar calibrado localmente."
        }
    }

    fun buyStoreItem(item: StoreItem) {
        val uid = uiState.value.activeUser?.id ?: return
        viewModelScope.launch {
            when (val res = repo.purchaseStoreItem(uid, item)) {
                is PurchaseResult.Success -> {
                    _previewItemId.value = null
                    _statusBanner.value = "¡Compraste ${item.name} por ${item.priceCod} COD!"
                }
                is PurchaseResult.Error -> _statusBanner.value = res.message
            }
        }
    }

    fun toggleEquip(item: StoreItem) {
        val uid = uiState.value.activeUser?.id ?: return
        viewModelScope.launch {
            repo.toggleEquipItem(uid, item)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { repo.setNotificationsEnabled(enabled) }
    }

    fun setStepServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repo.setStepServiceEnabled(enabled)
            if (enabled) startStepTrackingIfPermitted() else {
                stepTracker.stopListening()
                StepCounterForegroundService.stop(app)
            }
        }
    }

    fun exportBackupToUri(context: Context, uri: Uri) {
        val uid = uiState.value.activeUser?.id ?: return
        viewModelScope.launch {
            try {
                val backupText = repo.exportBackupJson(uid)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(backupText.toByteArray(Charsets.UTF_8))
                }
                _statusBanner.value = "Copia de seguridad local exportada correctamente."
            } catch (e: Exception) {
                _statusBanner.value = "Error al exportar copia: ${e.localizedMessage ?: "desconocido"}"
            }
        }
    }

    fun importBackupFromUri(context: Context, uri: Uri, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (raw.isNullOrBlank()) {
                    _authError.value = "El archivo seleccionado está vacío."
                    return@launch
                }
                when (val res = repo.importBackupJson(raw)) {
                    is AuthResult.Success -> {
                        startStepTrackingIfPermitted()
                        _statusBanner.value = "Cuenta '${res.user.nickname}' restaurada sin internet."
                        onSuccess()
                    }
                    is AuthResult.Error -> {
                        _authError.value = res.message
                        _statusBanner.value = res.message
                    }
                }
            } catch (e: Exception) {
                val msg = "No se pudo leer la copia: ${e.localizedMessage ?: "error de archivo"}"
                _authError.value = msg
                _statusBanner.value = msg
            }
        }
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private data class Tuple5<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
}
