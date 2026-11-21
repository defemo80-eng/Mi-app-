package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AchievementDao
import com.example.data.local.dao.BodyScanDao
import com.example.data.local.dao.DailyLogDao
import com.example.data.local.dao.FoodDao
import com.example.data.local.dao.StepStateDao
import com.example.data.local.dao.StoreDao
import com.example.data.local.dao.UserDao
import com.example.data.local.dao.WalletDao
import com.example.data.local.dao.WorkoutDao
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

@Database(
    entities = [
        UserAccountEntity::class,
        DailyLogEntity::class,
        StepHardwareStateEntity::class,
        CodTransactionEntity::class,
        FoodEntryEntity::class,
        WorkoutLogEntity::class,
        BodyScanEntity::class,
        StorePurchaseEntity::class,
        EquippedItemEntity::class,
        AchievementEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DeFittDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun stepStateDao(): StepStateDao
    abstract fun walletDao(): WalletDao
    abstract fun foodDao(): FoodDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun bodyScanDao(): BodyScanDao
    abstract fun storeDao(): StoreDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile
        private var INSTANCE: DeFittDatabase? = null

        fun getInstance(context: Context): DeFittDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DeFittDatabase::class.java,
                    "defitt_offline.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
