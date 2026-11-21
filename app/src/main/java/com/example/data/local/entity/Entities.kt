package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "user_accounts",
    indices = [Index(value = ["nickname"], unique = true)]
)
data class UserAccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nickname: String,
    val age: Int,
    val pinHash: String,
    val pinSalt: String,
    val recoveryCodeHash: String,
    val createdAt: Long,
    val experienceLevel: String = "PRINCIPIANTE",
    val dailyStepGoal: Int = 6000,
    val codBalance: Long = 0L,
    val xpTotal: Long = 0L,
    val lastWeeklyScanAt: Long = 0L,
    val lastActiveDateKey: String = "",
    val avatarConfigJson: String
)

@Serializable
@Entity(
    tableName = "daily_logs",
    primaryKeys = ["userId", "dateKey"]
)
data class DailyLogEntity(
    val userId: String,
    val dateKey: String, // YYYY-MM-DD
    val steps: Long = 0L,
    val codEarned: Long = 0L,
    val caloriesTotal: Float = 0f,
    val proteinTotal: Float = 0f,
    val carbsTotal: Float = 0f,
    val fatsTotal: Float = 0f,
    val sugarsTotal: Float = 0f,
    val fiberTotal: Float = 0f,
    val workoutsCompleted: Int = 0,
    val workoutsPending: Int = 0,
    val achievementsUnlockedCount: Int = 0,
    val isClosed: Boolean = false,
    val closedAt: Long = 0L,
    val avatarSnapshotJson: String = "",
    val activitySummary: String = ""
)

@Entity(tableName = "step_hardware_state")
data class StepHardwareStateEntity(
    @PrimaryKey val userId: String,
    val lastSensorTotalSteps: Float = -1f,
    val lastElapsedRealtimeMs: Long = 0L,
    val lastRecordedDateKey: String = "",
    val lastStepEventTimestampMs: Long = 0L,
    val sensorAvailable: Boolean = true
)

@Serializable
@Entity(
    tableName = "cod_transactions",
    indices = [Index(value = ["userId", "timestamp"])]
)
data class CodTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val timestamp: Long,
    val dateKey: String,
    val type: String, // EARN_STEPS, EARN_ACHIEVEMENT, SPEND_STORE
    val amount: Long,
    val description: String,
    val balanceAfter: Long
)

@Serializable
@Entity(
    tableName = "food_entries",
    indices = [Index(value = ["userId", "dateKey"])]
)
data class FoodEntryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val dateKey: String,
    val mealType: String, // DESAYUNO, ALMUERZO, CENA, MERIENDA
    val foodName: String,
    val portionGrams: Int,
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fats: Float,
    val sugars: Float,
    val fiber: Float,
    val confidenceLabel: String,
    val recognizedByAi: Boolean,
    val timestamp: Long
)

@Serializable
@Entity(
    tableName = "workout_logs",
    indices = [Index(value = ["userId", "dateKey"])]
)
data class WorkoutLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val dateKey: String,
    val routineTitle: String,
    val difficulty: String,
    val durationMinutes: Int,
    val completed: Boolean,
    val exercisesSummary: String,
    val timestamp: Long
)

@Serializable
@Entity(
    tableName = "body_scans",
    indices = [Index(value = ["userId", "timestamp"])]
)
data class BodyScanEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val dateKey: String,
    val timestamp: Long,
    val scanType: String, // CAMERA_POSE_MULTI_ANGLE, MANUAL_CALIBRATION
    val anglesCaptured: Int,
    val shoulderWidthRatio: Float,
    val waistRatio: Float,
    val hipWidthRatio: Float,
    val limbThicknessRatio: Float,
    val postureAlignmentScore: Float,
    val estimatedBuildLabel: String,
    val notes: String,
    val avatarParamsJson: String
)

@Serializable
@Entity(
    tableName = "store_purchases",
    primaryKeys = ["userId", "itemId"]
)
data class StorePurchaseEntity(
    val userId: String,
    val itemId: String,
    val purchasedAt: Long,
    val priceCod: Int
)

@Serializable
@Entity(
    tableName = "equipped_items",
    primaryKeys = ["userId", "category"]
)
data class EquippedItemEntity(
    val userId: String,
    val category: String,
    val itemId: String
)

@Serializable
@Entity(
    tableName = "achievements",
    primaryKeys = ["userId", "achievementKey"]
)
data class AchievementEntity(
    val userId: String,
    val achievementKey: String,
    val title: String,
    val description: String,
    val xpReward: Int,
    val codReward: Int,
    val unlockedAt: Long
)
