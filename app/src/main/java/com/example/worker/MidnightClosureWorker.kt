package com.example.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.DeFittApplication
import com.example.domain.engine.DateUtils
import com.example.receiver.BootAndMidnightReceiver
import java.util.concurrent.TimeUnit

class MidnightClosureWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? DeFittApplication ?: return Result.success()
        val userId = app.repository.getActiveUserId() ?: return Result.success()
        app.repository.reconcilePendingClosuresIfNeeded(userId)
        MidnightScheduler.scheduleNextMidnightAlarm(applicationContext)
        return Result.success()
    }
}

object MidnightScheduler {
    private const val PERIODIC_WORK_NAME = "defitt_midnight_reconcile"

    fun scheduleWork(context: Context) {
        if (!WorkManager.isInitialized()) {
            try {
                WorkManager.initialize(
                    context.applicationContext,
                    Configuration.Builder().build()
                )
            } catch (_: IllegalStateException) {
                // Already initialized concurrently
            }
        }
        val work = PeriodicWorkRequestBuilder<MidnightClosureWorker>(
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            work
        )
        scheduleNextMidnightAlarm(context)
    }

    fun scheduleNextMidnightAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, BootAndMidnightReceiver::class.java).apply {
            action = BootAndMidnightReceiver.ACTION_MIDNIGHT_CLOSURE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2048,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + DateUtils.millisUntilNextMidnight()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }
}
