package com.example.data.repository

import android.os.SystemClock
import androidx.room.withTransaction
import com.example.data.local.DeFittDatabase
import com.example.data.local.SessionPreferences
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
import com.example.domain.catalog.StoreCatalog
import com.example.domain.engine.AvatarEvolutionEngine
import com.example.domain.engine.DateUtils
import com.example.domain.model.AvatarParams
import com.example.domain.model.BackupPayload
import com.example.domain.model.FoodCatalogItem
import com.example.domain.model.MealType
import com.example.domain.model.StoreItem
import com.example.security.LocalCryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

sealed class AuthResult {
    data class Success(val user: UserAccountEntity, val recoveryCodePlain: String? = null) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

sealed class PurchaseResult {
    data class Success(val newBalance: Long) : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
}

class DeFittRepository(
    private val db: DeFittDatabase,
    private val prefs: SessionPreferences
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val stepMutex = Mutex()
    private val closureMutex = Mutex()

    val activeUserIdFlow: Flow<String?> = prefs.activeUserIdFlow
    val notificationsEnabledFlow: Flow<Boolean> = prefs.notificationsEnabledFlow
    val stepServiceEnabledFlow: Flow<Boolean> = prefs.stepServiceEnabledFlow

    fun observeAllUsers(): Flow<List<UserAccountEntity>> = db.userDao().observeAllUsers()
    fun observeUser(userId: String): Flow<UserAccountEntity?> = db.userDao().observeUserById(userId)
    fun observeDailyLogs(userId: String): Flow<List<DailyLogEntity>> = db.dailyLogDao().observeAllLogsForUser(userId)
    fun observeDailyLog(userId: String, dateKey: String): Flow<DailyLogEntity?> = db.dailyLogDao().observeLogForDate(userId, dateKey)
    fun observeStepState(userId: String): Flow<StepHardwareStateEntity?> = db.stepStateDao().observeState(userId)
    fun observeTransactions(userId: String): Flow<List<CodTransactionEntity>> = db.walletDao().observeTransactions(userId)
    fun observeFoodsForDate(userId: String, dateKey: String): Flow<List<FoodEntryEntity>> = db.foodDao().observeFoodForDate(userId, dateKey)
    fun observeWorkoutsForDate(userId: String, dateKey: String): Flow<List<WorkoutLogEntity>> = db.workoutDao().observeWorkoutsForDate(userId, dateKey)
    fun observeAllWorkouts(userId: String): Flow<List<WorkoutLogEntity>> = db.workoutDao().observeAllWorkouts(userId)
    fun observeScans(userId: String): Flow<List<BodyScanEntity>> = db.bodyScanDao().observeScans(userId)
    fun observePurchases(userId: String): Flow<List<StorePurchaseEntity>> = db.storeDao().observePurchases(userId)
    fun observeEquipped(userId: String): Flow<List<EquippedItemEntity>> = db.storeDao().observeEquipped(userId)
    fun observeAchievements(userId: String): Flow<List<AchievementEntity>> = db.achievementDao().observeAchievements(userId)

    suspend fun getActiveUserId(): String? = prefs.getActiveUserId()

    suspend fun setNotificationsEnabled(enabled: Boolean) = prefs.setNotificationsEnabled(enabled)
    suspend fun setStepServiceEnabled(enabled: Boolean) = prefs.setStepServiceEnabled(enabled)

    fun parseAvatarParams(jsonStr: String): AvatarParams {
        return try {
            if (jsonStr.isBlank()) AvatarParams() else json.decodeFromString(jsonStr)
        } catch (_: Exception) {
            AvatarParams()
        }
    }

    fun encodeAvatarParams(params: AvatarParams): String = json.encodeToString(params)

    suspend fun registerAccount(
        name: String,
        nickname: String,
        age: Int,
        pin: String
    ): AuthResult {
        val cleanName = name.trim()
        val cleanNick = nickname.trim()
        if (cleanName.length < 2) return AuthResult.Error("Ingresa tu nombre completo.")
        if (cleanNick.length < 2) return AuthResult.Error("El apodo debe tener al menos 2 caracteres.")
        if (age !in 10..105) return AuthResult.Error("Ingresa una edad válida (10 a 105 años).")
        if (pin.length < 4) return AuthResult.Error("El PIN o contraseña debe tener al menos 4 caracteres.")

        val existing = db.userDao().getUserByNickname(cleanNick)
        if (existing != null) {
            return AuthResult.Error("Ya existe una cuenta local con el apodo '$cleanNick'.")
        }

        val salt = LocalCryptoManager.generateSaltBase64()
        val pinHash = LocalCryptoManager.hashSecret(pin, salt)
        val recoveryCode = LocalCryptoManager.generateRecoveryCode()
        val recoveryHash = LocalCryptoManager.hashSecret(recoveryCode, salt)
        val today = DateUtils.todayKey()
        val initialAvatar = AvatarParams(lastUpdatedDate = today)

        val user = UserAccountEntity(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            nickname = cleanNick,
            age = age,
            pinHash = pinHash,
            pinSalt = salt,
            recoveryCodeHash = recoveryHash,
            createdAt = System.currentTimeMillis(),
            lastActiveDateKey = today,
            avatarConfigJson = encodeAvatarParams(initialAvatar)
        )

        db.withTransaction {
            db.userDao().insertUser(user)
            db.dailyLogDao().upsertLog(
                DailyLogEntity(
                    userId = user.id,
                    dateKey = today,
                    avatarSnapshotJson = user.avatarConfigJson
                )
            )
            // Starter pack item unlocked & equipped by default: DeFitt Pro Shirt & Tech Jogger
            db.storeDao().insertPurchase(StorePurchaseEntity(user.id, "shirt_defitt_black", user.createdAt, 0))
            db.storeDao().insertPurchase(StorePurchaseEntity(user.id, "pants_tech_jogger", user.createdAt, 0))
            db.storeDao().equipItem(EquippedItemEntity(user.id, "CAMISETAS", "shirt_defitt_black"))
            db.storeDao().equipItem(EquippedItemEntity(user.id, "PANTALONES", "pants_tech_jogger"))
        }

        prefs.setActiveUserId(user.id)
        return AuthResult.Success(user = user, recoveryCodePlain = recoveryCode)
    }

    suspend fun loginAccount(nickname: String, pin: String): AuthResult {
        val cleanNick = nickname.trim()
        if (cleanNick.isBlank() || pin.isBlank()) {
            return AuthResult.Error("Completa tu apodo y PIN.")
        }
        val user = db.userDao().getUserByNickname(cleanNick)
            ?: return AuthResult.Error("Cuenta no encontrada en este dispositivo.")
        val valid = LocalCryptoManager.verifySecret(pin, user.pinSalt, user.pinHash)
        if (!valid) {
            return AuthResult.Error("PIN o contraseña incorrecta.")
        }
        prefs.setActiveUserId(user.id)
        reconcilePendingClosuresIfNeeded(user.id)
        return AuthResult.Success(user)
    }

    suspend fun recoverAccountWithCode(
        nickname: String,
        recoveryCode: String,
        newPin: String
    ): AuthResult {
        val cleanNick = nickname.trim()
        val cleanCode = recoveryCode.trim().uppercase()
        if (newPin.length < 4) return AuthResult.Error("El nuevo PIN debe tener al menos 4 caracteres.")
        val user = db.userDao().getUserByNickname(cleanNick)
            ?: return AuthResult.Error("Cuenta '$cleanNick' no encontrada localmente.")
        val ok = LocalCryptoManager.verifySecret(cleanCode, user.pinSalt, user.recoveryCodeHash)
        if (!ok) return AuthResult.Error("Código de recuperación local inválido.")

        val newSalt = LocalCryptoManager.generateSaltBase64()
        val newHash = LocalCryptoManager.hashSecret(newPin, newSalt)
        val newRecoveryCode = LocalCryptoManager.generateRecoveryCode()
        val newRecoveryHash = LocalCryptoManager.hashSecret(newRecoveryCode, newSalt)
        val updated = user.copy(
            pinSalt = newSalt,
            pinHash = newHash,
            recoveryCodeHash = newRecoveryHash
        )
        db.userDao().updateUser(updated)
        prefs.setActiveUserId(updated.id)
        return AuthResult.Success(updated, recoveryCodePlain = newRecoveryCode)
    }

    suspend fun logout() {
        prefs.setActiveUserId(null)
    }

    suspend fun deleteAccount(userId: String) {
        db.withTransaction {
            db.dailyLogDao().deleteLogsForUser(userId)
            db.stepStateDao().deleteForUser(userId)
            db.walletDao().deleteForUser(userId)
            db.foodDao().deleteForUser(userId)
            db.workoutDao().deleteForUser(userId)
            db.bodyScanDao().deleteForUser(userId)
            db.storeDao().deletePurchasesForUser(userId)
            db.storeDao().deleteEquippedForUser(userId)
            db.achievementDao().deleteForUser(userId)
            db.userDao().deleteUserById(userId)
        }
        if (prefs.getActiveUserId() == userId) {
            prefs.setActiveUserId(null)
        }
    }

    suspend fun updateProfileSettings(
        userId: String,
        name: String,
        age: Int,
        experienceLevel: String,
        dailyStepGoal: Int
    ) {
        val user = db.userDao().getUserById(userId) ?: return
        db.userDao().updateUser(
            user.copy(
                name = name.trim().ifBlank { user.name },
                age = age.coerceIn(10, 105),
                experienceLevel = experienceLevel,
                dailyStepGoal = dailyStepGoal.coerceIn(1000, 30000)
            )
        )
    }

    suspend fun updateAvatarAppearance(userId: String, params: AvatarParams) {
        val user = db.userDao().getUserById(userId) ?: return
        val encoded = encodeAvatarParams(params)
        db.userDao().updateUser(user.copy(avatarConfigJson = encoded))
        val today = DateUtils.todayKey()
        val todayLog = ensureDailyLog(userId, today, encoded)
        db.dailyLogDao().upsertLog(todayLog.copy(avatarSnapshotJson = encoded))
    }

    suspend fun ensureDailyLog(userId: String, dateKey: String, fallbackAvatarJson: String = ""): DailyLogEntity {
        val existing = db.dailyLogDao().getLogForDate(userId, dateKey)
        if (existing != null) return existing
        val userAvatar = fallbackAvatarJson.ifBlank {
            db.userDao().getUserById(userId)?.avatarConfigJson ?: encodeAvatarParams(AvatarParams())
        }
        val created = DailyLogEntity(
            userId = userId,
            dateKey = dateKey,
            avatarSnapshotJson = userAvatar
        )
        db.dailyLogDao().upsertLog(created)
        return created
    }

    /**
     * Processes hardware TYPE_STEP_COUNTER reading safely:
     * - Detects device reboots via sensor reset or elapsedRealtime
     * - Protects against duplicate callbacks & impossible burst spikes
     * - Credits 1 valid step = 1 COD atomically
     */
    suspend fun processHardwareStepSensorValue(
        userId: String,
        sensorTotalStepsSinceBoot: Float,
        sensorAvailable: Boolean = true
    ): Long = stepMutex.withLock {
        val today = DateUtils.todayKey()
        reconcilePendingClosuresIfNeeded(userId)

        val nowMs = System.currentTimeMillis()
        val elapsedMs = SystemClock.elapsedRealtime()
        val prevState = db.stepStateDao().getState(userId)

        if (!sensorAvailable || sensorTotalStepsSinceBoot < 0f) {
            db.stepStateDao().upsertState(
                StepHardwareStateEntity(
                    userId = userId,
                    lastSensorTotalSteps = prevState?.lastSensorTotalSteps ?: -1f,
                    lastElapsedRealtimeMs = elapsedMs,
                    lastRecordedDateKey = today,
                    lastStepEventTimestampMs = nowMs,
                    sensorAvailable = sensorAvailable
                )
            )
            return@withLock 0L
        }

        if (prevState == null || prevState.lastSensorTotalSteps < 0f) {
            // Establish baseline on first reading without fabricating past steps
            db.stepStateDao().upsertState(
                StepHardwareStateEntity(
                    userId = userId,
                    lastSensorTotalSteps = sensorTotalStepsSinceBoot,
                    lastElapsedRealtimeMs = elapsedMs,
                    lastRecordedDateKey = today,
                    lastStepEventTimestampMs = nowMs,
                    sensorAvailable = true
                )
            )
            ensureDailyLog(userId, today)
            return@withLock 0L
        }

        val rebootDetected = sensorTotalStepsSinceBoot < prevState.lastSensorTotalSteps ||
            elapsedMs < prevState.lastElapsedRealtimeMs

        val rawDelta = if (rebootDetected) {
            sensorTotalStepsSinceBoot.toLong().coerceAtLeast(0L)
        } else {
            (sensorTotalStepsSinceBoot - prevState.lastSensorTotalSteps).toLong()
        }

        if (rawDelta <= 0L) {
            db.stepStateDao().upsertState(
                prevState.copy(
                    lastSensorTotalSteps = sensorTotalStepsSinceBoot,
                    lastElapsedRealtimeMs = elapsedMs,
                    lastRecordedDateKey = today,
                    sensorAvailable = true
                )
            )
            return@withLock 0L
        }

        // Anti-manipulation rate guard: human sprint cadence max ~5 steps/sec.
        // Allow up to 18000 steps per single background batch (approx 1 hour sustained walk).
        val timeDiffSec = ((nowMs - prevState.lastStepEventTimestampMs) / 1000.0).coerceAtLeast(1.0)
        val maxPlausibleSteps = (timeDiffSec * 5.0).toLong().coerceIn(25L, 18000L)
        val validDelta = rawDelta.coerceAtMost(maxPlausibleSteps)

        if (validDelta > 0L) {
            creditValidSteps(userId, today, validDelta, nowMs)
        }

        db.stepStateDao().upsertState(
            StepHardwareStateEntity(
                userId = userId,
                lastSensorTotalSteps = sensorTotalStepsSinceBoot,
                lastElapsedRealtimeMs = elapsedMs,
                lastRecordedDateKey = today,
                lastStepEventTimestampMs = nowMs,
                sensorAvailable = true
            )
        )
        return@withLock validDelta
    }

    suspend fun updateSensorAvailability(userId: String, available: Boolean) {
        val current = db.stepStateDao().getState(userId)
        db.stepStateDao().upsertState(
            (current ?: StepHardwareStateEntity(userId = userId)).copy(sensorAvailable = available)
        )
    }

    private suspend fun creditValidSteps(
        userId: String,
        dateKey: String,
        validSteps: Long,
        nowMs: Long
    ) {
        var newBalance = 0L
        var todayStepsTotal = 0L
        var stepGoal = 6000
        db.withTransaction {
            val user = db.userDao().getUserById(userId) ?: return@withTransaction
            stepGoal = user.dailyStepGoal
            val log = ensureDailyLog(userId, dateKey, user.avatarConfigJson)
            val updatedLog = log.copy(
                steps = log.steps + validSteps,
                codEarned = log.codEarned + validSteps
            )
            todayStepsTotal = updatedLog.steps
            newBalance = user.codBalance + validSteps
            db.dailyLogDao().upsertLog(updatedLog)
            db.userDao().addCodBalance(userId, validSteps)
            db.walletDao().insertTransaction(
                CodTransactionEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    timestamp = nowMs,
                    dateKey = dateKey,
                    type = "EARN_STEPS",
                    amount = validSteps,
                    description = "+$validSteps pasos válidos",
                    balanceAfter = newBalance
                )
            )
        }
        evaluateStepAchievements(userId, todayStepsTotal, stepGoal)
    }

    suspend fun addFoodEntry(
        userId: String,
        mealType: MealType,
        catalogItem: FoodCatalogItem,
        portionGrams: Int,
        confidenceLabel: String,
        recognizedByAi: Boolean
    ) {
        val today = DateUtils.todayKey()
        reconcilePendingClosuresIfNeeded(userId)
        val factor = portionGrams.coerceIn(1, 3000) / 100f
        val entry = FoodEntryEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            dateKey = today,
            mealType = mealType.name,
            foodName = catalogItem.name,
            portionGrams = portionGrams.coerceIn(1, 3000),
            calories = catalogItem.caloriesPer100g * factor,
            protein = catalogItem.proteinPer100g * factor,
            carbs = catalogItem.carbsPer100g * factor,
            fats = catalogItem.fatsPer100g * factor,
            sugars = catalogItem.sugarsPer100g * factor,
            fiber = catalogItem.fiberPer100g * factor,
            confidenceLabel = confidenceLabel,
            recognizedByAi = recognizedByAi,
            timestamp = System.currentTimeMillis()
        )
        db.withTransaction {
            db.foodDao().insertFood(entry)
            recalculateDailyNutritionTotals(userId, today)
        }
    }

    suspend fun deleteFoodEntry(userId: String, entryId: String, dateKey: String) {
        db.withTransaction {
            db.foodDao().deleteFoodById(entryId)
            recalculateDailyNutritionTotals(userId, dateKey)
        }
    }

    private suspend fun recalculateDailyNutritionTotals(userId: String, dateKey: String) {
        val entries = db.foodDao().getFoodForDate(userId, dateKey)
        val log = ensureDailyLog(userId, dateKey)
        db.dailyLogDao().upsertLog(
            log.copy(
                caloriesTotal = entries.sumOf { it.calories.toDouble() }.toFloat(),
                proteinTotal = entries.sumOf { it.protein.toDouble() }.toFloat(),
                carbsTotal = entries.sumOf { it.carbs.toDouble() }.toFloat(),
                fatsTotal = entries.sumOf { it.fats.toDouble() }.toFloat(),
                sugarsTotal = entries.sumOf { it.sugars.toDouble() }.toFloat(),
                fiberTotal = entries.sumOf { it.fiber.toDouble() }.toFloat()
            )
        )
    }

    /**
     * Records user confirmation ("SÍ" vs "NO") for a workout routine.
     * Strictly DOES NOT change physical body proportions!
     */
    suspend fun recordWorkoutConfirmation(
        userId: String,
        routineTitle: String,
        difficulty: String,
        durationMinutes: Int,
        exercisesSummary: String,
        completed: Boolean
    ) {
        val today = DateUtils.todayKey()
        reconcilePendingClosuresIfNeeded(userId)
        val now = System.currentTimeMillis()
        db.withTransaction {
            db.workoutDao().insertWorkout(
                WorkoutLogEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    dateKey = today,
                    routineTitle = routineTitle,
                    difficulty = difficulty,
                    durationMinutes = durationMinutes,
                    completed = completed,
                    exercisesSummary = exercisesSummary,
                    timestamp = now
                )
            )
            val log = ensureDailyLog(userId, today)
            db.dailyLogDao().upsertLog(
                log.copy(
                    workoutsCompleted = log.workoutsCompleted + if (completed) 1 else 0,
                    workoutsPending = log.workoutsPending + if (!completed) 1 else 0
                )
            )
            if (completed) {
                db.userDao().addXp(userId, 50L)
            }
        }
        if (completed) {
            unlockAchievement(
                userId = userId,
                key = "FIRST_WORKOUT",
                title = "Primera rutina completada",
                description = "Confirmaste tu primera rutina personalizada sin máquinas.",
                xpReward = 60,
                codReward = 40
            )
        }
    }

    suspend fun saveBodyScan(
        userId: String,
        scanType: String,
        anglesCaptured: Int,
        shoulderWidthRatio: Float,
        waistRatio: Float,
        hipWidthRatio: Float,
        limbThicknessRatio: Float,
        postureAlignmentScore: Float,
        estimatedBuildLabel: String,
        notes: String,
        skinToneHex: String? = null,
        hairColorHex: String? = null,
        hairStyle: Int? = null,
        heightCm: Int? = null
    ): BodyScanEntity {
        val today = DateUtils.todayKey()
        reconcilePendingClosuresIfNeeded(userId)
        val now = System.currentTimeMillis()
        val user = db.userDao().getUserById(userId)!!
        val existingAvatar = parseAvatarParams(user.avatarConfigJson)
        val existingScans = db.bodyScanDao().getScans(userId)

        val newAvatar = existingAvatar.copy(
            heightCm = heightCm ?: existingAvatar.heightCm,
            shoulderWidthRatio = shoulderWidthRatio.coerceIn(0.84f, 1.22f),
            waistRatio = waistRatio.coerceIn(0.82f, 1.24f),
            hipWidthRatio = hipWidthRatio.coerceIn(0.86f, 1.18f),
            limbThicknessRatio = limbThicknessRatio.coerceIn(0.85f, 1.22f),
            postureAlignment = postureAlignmentScore.coerceIn(0.70f, 1.00f),
            skinToneHex = skinToneHex ?: existingAvatar.skinToneHex,
            hairColorHex = hairColorHex ?: existingAvatar.hairColorHex,
            hairStyle = hairStyle ?: existingAvatar.hairStyle,
            calibratedViaCamera = scanType == "CAMERA_POSE_MULTI_ANGLE" || existingAvatar.calibratedViaCamera,
            scanCount = existingScans.size + 1,
            lastUpdatedDate = today
        )
        val avatarJson = encodeAvatarParams(newAvatar)

        val scan = BodyScanEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            dateKey = today,
            timestamp = now,
            scanType = scanType,
            anglesCaptured = anglesCaptured,
            shoulderWidthRatio = newAvatar.shoulderWidthRatio,
            waistRatio = newAvatar.waistRatio,
            hipWidthRatio = newAvatar.hipWidthRatio,
            limbThicknessRatio = newAvatar.limbThicknessRatio,
            postureAlignmentScore = newAvatar.postureAlignment,
            estimatedBuildLabel = estimatedBuildLabel,
            notes = notes,
            avatarParamsJson = avatarJson
        )

        db.withTransaction {
            db.bodyScanDao().insertScan(scan)
            db.userDao().updateUser(
                user.copy(
                    lastWeeklyScanAt = now,
                    avatarConfigJson = avatarJson
                )
            )
            val log = ensureDailyLog(userId, today, avatarJson)
            db.dailyLogDao().upsertLog(log.copy(avatarSnapshotJson = avatarJson))
        }

        unlockAchievement(
            userId = userId,
            key = "FIRST_SCAN",
            title = "Primer escaneo",
            description = "Calibraste tu avatar 3D personal sin conexión.",
            xpReward = 80,
            codReward = 50
        )

        return scan
    }

    suspend fun purchaseStoreItem(userId: String, item: StoreItem): PurchaseResult {
        var result: PurchaseResult = PurchaseResult.Error("No se pudo completar la compra.")
        db.withTransaction {
            val user = db.userDao().getUserById(userId)
            if (user == null) {
                result = PurchaseResult.Error("Usuario no activo.")
                return@withTransaction
            }
            val owned = db.storeDao().getPurchases(userId).any { it.itemId == item.id }
            if (owned) {
                result = PurchaseResult.Error("Ya posees este artículo en tu armario.")
                return@withTransaction
            }
            if (user.codBalance < item.priceCod) {
                result = PurchaseResult.Error("Saldo COD insuficiente. Necesitas ${item.priceCod} COD.")
                return@withTransaction
            }
            val now = System.currentTimeMillis()
            val newBal = user.codBalance - item.priceCod
            db.userDao().addCodBalance(userId, -item.priceCod.toLong())
            db.storeDao().insertPurchase(StorePurchaseEntity(userId, item.id, now, item.priceCod))
            db.storeDao().equipItem(EquippedItemEntity(userId, item.categoryName, item.id))
            db.walletDao().insertTransaction(
                CodTransactionEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    timestamp = now,
                    dateKey = DateUtils.todayKey(),
                    type = "SPEND_STORE",
                    amount = -item.priceCod.toLong(),
                    description = "Compra: ${item.name}",
                    balanceAfter = newBal
                )
            )
            result = PurchaseResult.Success(newBal)
        }
        if (result is PurchaseResult.Success) {
            unlockAchievement(
                userId = userId,
                key = "FIRST_PURCHASE",
                title = "Primera compra",
                description = "Adquiriste tu primer artículo en la tienda con COD.",
                xpReward = 50,
                codReward = 25
            )
        }
        return result
    }

    suspend fun toggleEquipItem(userId: String, item: StoreItem) {
        val equipped = db.storeDao().getEquipped(userId)
        val currentlyEquipped = equipped.any { it.category == item.categoryName && it.itemId == item.id }
        if (currentlyEquipped) {
            db.storeDao().unequipCategory(userId, item.categoryName)
        } else {
            db.storeDao().equipItem(EquippedItemEntity(userId, item.categoryName, item.id))
        }
    }

    /**
     * Mandatory Midnight Daily Closure & Recovery Engine:
     * Closes any unclosed previous dates cleanly, preserves history, evaluates daily avatar evolution,
     * and initializes today's clean log without resetting COD wallet balance.
     */
    suspend fun reconcilePendingClosuresIfNeeded(userId: String): Int = closureMutex.withLock {
        val today = DateUtils.todayKey()
        val user = db.userDao().getUserById(userId) ?: return@withLock 0
        val unclosed = db.dailyLogDao().getUnclosedLogsBefore(userId, today)
        var closedCount = 0

        var runningAvatar = parseAvatarParams(user.avatarConfigJson)
        val scans = db.bodyScanDao().getScans(userId)
        val latestScan = scans.firstOrNull()
        val previousScan = scans.getOrNull(1)

        db.withTransaction {
            for (log in unclosed) {
                val (updatedAvatar, evoMsg) = AvatarEvolutionEngine.evaluateMidnightUpdate(
                    currentAvatar = runningAvatar,
                    latestScan = latestScan,
                    previousScan = previousScan,
                    dateKey = log.dateKey
                )
                runningAvatar = updatedAvatar
                val encodedAvatar = encodeAvatarParams(runningAvatar)
                val foods = db.foodDao().getFoodForDate(userId, log.dateKey)
                val workouts = db.workoutDao().getWorkoutsForDate(userId, log.dateKey)
                val achCount = db.achievementDao().getAchievements(userId)
                    .count { DateUtils.dateKeyFromMillis(it.unlockedAt) == log.dateKey }

                val summary = buildString {
                    append("${log.steps} pasos (${log.codEarned} COD)")
                    append(" • ${foods.size} comidas (${log.caloriesTotal.toInt()} kcal)")
                    append(" • ${workouts.count { it.completed }} rutinas")
                    append(" • $evoMsg")
                }

                db.dailyLogDao().upsertLog(
                    log.copy(
                        caloriesTotal = foods.sumOf { it.calories.toDouble() }.toFloat(),
                        proteinTotal = foods.sumOf { it.protein.toDouble() }.toFloat(),
                        carbsTotal = foods.sumOf { it.carbs.toDouble() }.toFloat(),
                        fatsTotal = foods.sumOf { it.fats.toDouble() }.toFloat(),
                        sugarsTotal = foods.sumOf { it.sugars.toDouble() }.toFloat(),
                        fiberTotal = foods.sumOf { it.fiber.toDouble() }.toFloat(),
                        workoutsCompleted = workouts.count { it.completed },
                        workoutsPending = workouts.count { !it.completed },
                        achievementsUnlockedCount = achCount,
                        isClosed = true,
                        closedAt = System.currentTimeMillis(),
                        avatarSnapshotJson = encodedAvatar,
                        activitySummary = summary
                    )
                )
                closedCount++
            }

            val encodedFinal = encodeAvatarParams(runningAvatar)
            if (user.lastActiveDateKey != today || closedCount > 0) {
                db.userDao().updateUser(
                    user.copy(
                        lastActiveDateKey = today,
                        avatarConfigJson = encodedFinal
                    )
                )
            }
            ensureDailyLog(userId, today, encodedFinal)
        }

        evaluateStreakAchievements(userId)
        return@withLock closedCount
    }

    private suspend fun evaluateStepAchievements(userId: String, todaySteps: Long, stepGoal: Int) {
        if (todaySteps >= 100L) {
            unlockAchievement(
                userId = userId,
                key = "FIRST_STEPS",
                title = "Primeros pasos",
                description = "Registraste tus primeros 100 pasos válidos con DeFitt.",
                xpReward = 40,
                codReward = 20
            )
        }
        if (todaySteps >= stepGoal.toLong()) {
            unlockAchievement(
                userId = userId,
                key = "STEP_GOAL_REACHED",
                title = "Meta de actividad alcanzada",
                description = "Alcanzaste tu meta diaria de $stepGoal pasos.",
                xpReward = 100,
                codReward = 75
            )
        }
    }

    private suspend fun evaluateStreakAchievements(userId: String) {
        val logs = db.dailyLogDao().getAllLogsForUser(userId)
        val activeDays = logs.count { it.steps >= 500 || it.workoutsCompleted > 0 }
        if (activeDays >= 3) {
            unlockAchievement(
                userId = userId,
                key = "DAILY_STREAK_3",
                title = "Constancia diaria",
                description = "Mantuviste actividad registrada durante 3 días.",
                xpReward = 90,
                codReward = 60
            )
        }
        if (activeDays >= 7) {
            unlockAchievement(
                userId = userId,
                key = "FIRST_WEEK_ACTIVE",
                title = "Primera semana activa",
                description = "Completaste 7 días con actividad física registrada.",
                xpReward = 200,
                codReward = 150
            )
        }
    }

    private suspend fun unlockAchievement(
        userId: String,
        key: String,
        title: String,
        description: String,
        xpReward: Int,
        codReward: Int
    ) {
        val now = System.currentTimeMillis()
        val inserted = db.achievementDao().insertAchievement(
            AchievementEntity(
                userId = userId,
                achievementKey = key,
                title = title,
                description = description,
                xpReward = xpReward,
                codReward = codReward,
                unlockedAt = now
            )
        )
        if (inserted != -1L) {
            val user = db.userDao().getUserById(userId) ?: return
            val newBal = user.codBalance + codReward
            db.userDao().addCodBalance(userId, codReward.toLong())
            db.userDao().addXp(userId, xpReward.toLong())
            db.walletDao().insertTransaction(
                CodTransactionEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    timestamp = now,
                    dateKey = DateUtils.todayKey(),
                    type = "EARN_ACHIEVEMENT",
                    amount = codReward.toLong(),
                    description = "Logro: $title (+$codReward COD)",
                    balanceAfter = newBal
                )
            )
        }
    }

    suspend fun exportBackupJson(userId: String): String {
        val user = db.userDao().getUserById(userId)
            ?: throw IllegalStateException("No hay cuenta activa para exportar.")
        val logs = db.dailyLogDao().getAllLogsForUser(userId)
        val txs = db.walletDao().getTransactions(userId)
        val foods = db.foodDao().getAllFoodForUser(userId)
        val workouts = db.workoutDao().getAllWorkouts(userId)
        val scans = db.bodyScanDao().getScans(userId)
        val purchases = db.storeDao().getPurchases(userId)
        val equipped = db.storeDao().getEquipped(userId)
        val achievements = db.achievementDao().getAchievements(userId)

        val userJson = json.encodeToString(user)
        val logsJson = json.encodeToString(logs)
        val txsJson = json.encodeToString(txs)
        val foodsJson = json.encodeToString(foods)
        val workoutsJson = json.encodeToString(workouts)
        val scansJson = json.encodeToString(scans)
        val purchasesJson = json.encodeToString(purchases)
        val equippedJson = json.encodeToString(equipped)
        val achievementsJson = json.encodeToString(achievements)

        val digestInput = listOf(
            userJson, logsJson, txsJson, foodsJson, workoutsJson,
            scansJson, purchasesJson, equippedJson, achievementsJson
        ).joinToString("|")

        val payload = BackupPayload(
            formatVersion = 1,
            exportedAt = System.currentTimeMillis(),
            checksumSha256 = LocalCryptoManager.sha256Hex(digestInput),
            userJson = userJson,
            dailyLogsJson = logsJson,
            transactionsJson = txsJson,
            foodsJson = foodsJson,
            workoutsJson = workoutsJson,
            scansJson = scansJson,
            purchasesJson = purchasesJson,
            equippedJson = equippedJson,
            achievementsJson = achievementsJson
        )
        return json.encodeToString(payload)
    }

    suspend fun importBackupJson(rawJson: String): AuthResult {
        return try {
            val payload = json.decodeFromString<BackupPayload>(rawJson)
            val digestInput = listOf(
                payload.userJson, payload.dailyLogsJson, payload.transactionsJson,
                payload.foodsJson, payload.workoutsJson, payload.scansJson,
                payload.purchasesJson, payload.equippedJson, payload.achievementsJson
            ).joinToString("|")

            if (LocalCryptoManager.sha256Hex(digestInput) != payload.checksumSha256) {
                return AuthResult.Error("El archivo de copia de seguridad está dañado o alterado.")
            }

            val user = json.decodeFromString<UserAccountEntity>(payload.userJson)
            val logs = json.decodeFromString<List<DailyLogEntity>>(payload.dailyLogsJson)
            val txs = json.decodeFromString<List<CodTransactionEntity>>(payload.transactionsJson)
            val foods = json.decodeFromString<List<FoodEntryEntity>>(payload.foodsJson)
            val workouts = json.decodeFromString<List<WorkoutLogEntity>>(payload.workoutsJson)
            val scans = json.decodeFromString<List<BodyScanEntity>>(payload.scansJson)
            val purchases = json.decodeFromString<List<StorePurchaseEntity>>(payload.purchasesJson)
            val equipped = json.decodeFromString<List<EquippedItemEntity>>(payload.equippedJson)
            val achievements = json.decodeFromString<List<AchievementEntity>>(payload.achievementsJson)

            db.withTransaction {
                db.userDao().insertUser(user)
                db.dailyLogDao().insertAllLogs(logs)
                db.walletDao().insertAllTransactions(txs)
                db.foodDao().insertAllFood(foods)
                db.workoutDao().insertAllWorkouts(workouts)
                db.bodyScanDao().insertAllScans(scans)
                db.storeDao().insertAllPurchases(purchases)
                db.storeDao().insertAllEquipped(equipped)
                db.achievementDao().insertAllAchievements(achievements)
            }

            prefs.setActiveUserId(user.id)
            reconcilePendingClosuresIfNeeded(user.id)
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error("No se pudo importar la copia local: ${e.message ?: "formato inválido"}")
        }
    }
}
