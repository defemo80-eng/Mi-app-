package com.example.domain.hardware

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.repository.DeFittRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StepTrackerDiagnostics(
    val sensorAvailable: Boolean = true,
    val hasPermission: Boolean = false,
    val isListening: Boolean = false,
    val lastRawBootSteps: Float = -1f,
    val lastValidDelta: Long = 0L
)

class StepTrackerManager(
    private val context: Context,
    private val repository: DeFittRepository
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var isListening = false

    private val _diagnostics = MutableStateFlow(
        StepTrackerDiagnostics(
            sensorAvailable = stepCounterSensor != null,
            hasPermission = hasActivityPermission(),
            isListening = false
        )
    )
    val diagnostics: StateFlow<StepTrackerDiagnostics> = _diagnostics.asStateFlow()

    fun isStepSensorAvailable(): Boolean = stepCounterSensor != null

    fun hasActivityPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @Synchronized
    fun startListening() {
        val permGranted = hasActivityPermission()
        val sensor = stepCounterSensor
        scope.launch {
            val userId = repository.getActiveUserId() ?: return@launch
            repository.updateSensorAvailability(userId, sensor != null)
        }
        if (sensor != null && permGranted && !isListening) {
            val registered = sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            isListening = registered
        }
        _diagnostics.value = _diagnostics.value.copy(
            sensorAvailable = sensor != null,
            hasPermission = permGranted,
            isListening = isListening
        )
    }

    @Synchronized
    fun stopListening() {
        if (isListening) {
            sensorManager.unregisterListener(this)
            isListening = false
        }
        _diagnostics.value = _diagnostics.value.copy(
            hasPermission = hasActivityPermission(),
            isListening = false
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val totalSinceBoot = event.values.firstOrNull() ?: return
        if (totalSinceBoot.isNaN() || totalSinceBoot.isInfinite() || totalSinceBoot < 0f) return

        scope.launch {
            val userId = repository.getActiveUserId() ?: return@launch
            val delta = repository.processHardwareStepSensorValue(
                userId = userId,
                sensorTotalStepsSinceBoot = totalSinceBoot,
                sensorAvailable = true
            )
            _diagnostics.value = _diagnostics.value.copy(
                sensorAvailable = true,
                hasPermission = hasActivityPermission(),
                isListening = isListening,
                lastRawBootSteps = totalSinceBoot,
                lastValidDelta = delta
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
