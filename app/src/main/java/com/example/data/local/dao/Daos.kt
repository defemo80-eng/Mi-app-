package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_accounts ORDER BY createdAt DESC")
    fun observeAllUsers(): Flow<List<UserAccountEntity>>

    @Query("SELECT * FROM user_accounts WHERE id = :userId LIMIT 1")
    fun observeUserById(userId: String): Flow<UserAccountEntity?>

    @Query("SELECT * FROM user_accounts WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE LOWER(nickname) = LOWER(:nickname) LIMIT 1")
    suspend fun getUserByNickname(nickname: String): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccountEntity)

    @Update
    suspend fun updateUser(user: UserAccountEntity)

    @Query("UPDATE user_accounts SET codBalance = codBalance + :delta WHERE id = :userId")
    suspend fun addCodBalance(userId: String, delta: Long)

    @Query("UPDATE user_accounts SET xpTotal = xpTotal + :xpDelta WHERE id = :userId")
    suspend fun addXp(userId: String, xpDelta: Long)

    @Query("DELETE FROM user_accounts WHERE id = :userId")
    suspend fun deleteUserById(userId: String)
}

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_logs WHERE userId = :userId ORDER BY dateKey DESC")
    fun observeAllLogsForUser(userId: String): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs WHERE userId = :userId ORDER BY dateKey DESC")
    suspend fun getAllLogsForUser(userId: String): List<DailyLogEntity>

    @Query("SELECT * FROM daily_logs WHERE userId = :userId AND dateKey = :dateKey LIMIT 1")
    fun observeLogForDate(userId: String, dateKey: String): Flow<DailyLogEntity?>

    @Query("SELECT * FROM daily_logs WHERE userId = :userId AND dateKey = :dateKey LIMIT 1")
    suspend fun getLogForDate(userId: String, dateKey: String): DailyLogEntity?

    @Query("SELECT * FROM daily_logs WHERE userId = :userId AND isClosed = 0 AND dateKey < :todayKey ORDER BY dateKey ASC")
    suspend fun getUnclosedLogsBefore(userId: String, todayKey: String): List<DailyLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: DailyLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLogs(logs: List<DailyLogEntity>)

    @Query("DELETE FROM daily_logs WHERE userId = :userId")
    suspend fun deleteLogsForUser(userId: String)
}

@Dao
interface StepStateDao {
    @Query("SELECT * FROM step_hardware_state WHERE userId = :userId LIMIT 1")
    suspend fun getState(userId: String): StepHardwareStateEntity?

    @Query("SELECT * FROM step_hardware_state WHERE userId = :userId LIMIT 1")
    fun observeState(userId: String): Flow<StepHardwareStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertState(state: StepHardwareStateEntity)

    @Query("DELETE FROM step_hardware_state WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM cod_transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun observeTransactions(userId: String): Flow<List<CodTransactionEntity>>

    @Query("SELECT * FROM cod_transactions WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getTransactions(userId: String): List<CodTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: CodTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTransactions(txs: List<CodTransactionEntity>)

    @Query("DELETE FROM cod_transactions WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_entries WHERE userId = :userId AND dateKey = :dateKey ORDER BY timestamp DESC")
    fun observeFoodForDate(userId: String, dateKey: String): Flow<List<FoodEntryEntity>>

    @Query("SELECT * FROM food_entries WHERE userId = :userId AND dateKey = :dateKey ORDER BY timestamp DESC")
    suspend fun getFoodForDate(userId: String, dateKey: String): List<FoodEntryEntity>

    @Query("SELECT * FROM food_entries WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllFoodForUser(userId: String): List<FoodEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(entry: FoodEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFood(entries: List<FoodEntryEntity>)

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun deleteFoodById(id: String)

    @Query("DELETE FROM food_entries WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_logs WHERE userId = :userId AND dateKey = :dateKey ORDER BY timestamp DESC")
    fun observeWorkoutsForDate(userId: String, dateKey: String): Flow<List<WorkoutLogEntity>>

    @Query("SELECT * FROM workout_logs WHERE userId = :userId AND dateKey = :dateKey ORDER BY timestamp DESC")
    suspend fun getWorkoutsForDate(userId: String, dateKey: String): List<WorkoutLogEntity>

    @Query("SELECT * FROM workout_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun observeAllWorkouts(userId: String): Flow<List<WorkoutLogEntity>>

    @Query("SELECT * FROM workout_logs WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllWorkouts(userId: String): List<WorkoutLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(log: WorkoutLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllWorkouts(logs: List<WorkoutLogEntity>)

    @Query("DELETE FROM workout_logs WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface BodyScanDao {
    @Query("SELECT * FROM body_scans WHERE userId = :userId ORDER BY timestamp DESC")
    fun observeScans(userId: String): Flow<List<BodyScanEntity>>

    @Query("SELECT * FROM body_scans WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getScans(userId: String): List<BodyScanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: BodyScanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllScans(scans: List<BodyScanEntity>)

    @Query("DELETE FROM body_scans WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface StoreDao {
    @Query("SELECT * FROM store_purchases WHERE userId = :userId ORDER BY purchasedAt DESC")
    fun observePurchases(userId: String): Flow<List<StorePurchaseEntity>>

    @Query("SELECT * FROM store_purchases WHERE userId = :userId")
    suspend fun getPurchases(userId: String): List<StorePurchaseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: StorePurchaseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPurchases(purchases: List<StorePurchaseEntity>)

    @Query("SELECT * FROM equipped_items WHERE userId = :userId")
    fun observeEquipped(userId: String): Flow<List<EquippedItemEntity>>

    @Query("SELECT * FROM equipped_items WHERE userId = :userId")
    suspend fun getEquipped(userId: String): List<EquippedItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun equipItem(equipped: EquippedItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEquipped(equipped: List<EquippedItemEntity>)

    @Query("DELETE FROM equipped_items WHERE userId = :userId AND category = :category")
    suspend fun unequipCategory(userId: String, category: String)

    @Query("DELETE FROM store_purchases WHERE userId = :userId")
    suspend fun deletePurchasesForUser(userId: String)

    @Query("DELETE FROM equipped_items WHERE userId = :userId")
    suspend fun deleteEquippedForUser(userId: String)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements WHERE userId = :userId ORDER BY unlockedAt DESC")
    fun observeAchievements(userId: String): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE userId = :userId")
    suspend fun getAchievements(userId: String): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievement(achievement: AchievementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAchievements(achievements: List<AchievementEntity>)

    @Query("DELETE FROM achievements WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}
