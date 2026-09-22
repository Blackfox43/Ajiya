package com.example.data.safety

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

/**
 * Shake-to-SOS detector using Android Accelerometer sensor.
 * Calculates g-force delta and detects rapid successive directional changes.
 */
class ShakeDetector(
    private val context: Context,
    private val onShakeTriggered: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var isListening = false
    private var lastShakeTimestamp: Long = 0
    private var shakeCount = 0
    private var lastDirectionChangeTime: Long = 0

    companion object {
        private const val TAG = "ShakeDetector"
        // G-force threshold for a deliberate vigorous shake (avoids casual walking false positives)
        private const val SHAKE_THRESHOLD_G_FORCE = 2.6f
        private const val SHAKE_WINDOW_MS = 1200L // 1.2s window for multiple shakes
        private const val MIN_SHAKES_FOR_TRIGGER = 3
        private const val COOLDOWN_MS = 4000L // 4s cooldown between triggers
    }

    fun startListening(): Boolean {
        if (isListening || accelerometer == null || sensorManager == null) {
            return false
        }
        val registered = sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )
        isListening = registered
        Log.i(TAG, "Shake detection registered: $registered")
        return registered
    }

    fun stopListening() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        isListening = false
        shakeCount = 0
        Log.i(TAG, "Shake detection stopped")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate G-force
        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH
        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

        val now = System.currentTimeMillis()

        if (gForce > SHAKE_THRESHOLD_G_FORCE) {
            if (now - lastDirectionChangeTime > 200) { // minimum spacing between directional peaks
                if (now - lastShakeTimestamp > SHAKE_WINDOW_MS) {
                    shakeCount = 0
                }
                shakeCount++
                lastDirectionChangeTime = now
                lastShakeTimestamp = now

                Log.d(TAG, "Shake peak detected ($shakeCount/$MIN_SHAKES_FOR_TRIGGER) - gForce: $gForce")

                if (shakeCount >= MIN_SHAKES_FOR_TRIGGER) {
                    shakeCount = 0
                    Log.w(TAG, "EMERGENCY SHAKE TRIGGER REACHED! Triggering SOS...")
                    onShakeTriggered()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
