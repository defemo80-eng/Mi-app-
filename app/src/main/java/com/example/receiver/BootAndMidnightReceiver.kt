package com.example.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.DeFittApplication
import com.example.service.StepCounterForegroundService
import com.example.worker.MidnightScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootAndMidnightReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val app = context.applicationContext as? DeFittApplication
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (app != null) {
                    val userId = app.repository.getActiveUserId()
                    if (userId != null) {
                        app.repository.reconcilePendingClosuresIfNeeded(userId)
                        val serviceEnabled = app.repository.stepServiceEnabledFlow.first()
                        val hasPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACTIVITY_RECOGNITION
                            ) == PackageManager.PERMISSION_GRANTED
                        } else true

                        if (serviceEnabled && hasPerm) {
                            StepCounterForegroundService.start(context)
                        }
                    }
                    MidnightScheduler.scheduleWork(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MIDNIGHT_CLOSURE = "com.example.DEFITT_MIDNIGHT_CLOSURE"
    }
}
