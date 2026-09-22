package com.example.data.safety

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.db.AppDatabase
import com.example.data.network.SosRemoteDispatcher
import com.example.data.network.TermiiSmsGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Battery death listener: Detects when battery reaches <= 1% and automatically
 * sends a POST to /last-location and dispatches urgent SMS alerts to the Trusted Circle.
 */
class BatteryDeathReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BatteryDeathReceiver"
        private const val CHANNEL_ID = "ajiya_battery_death_channel"
        private const val NOTIFICATION_ID = 8844

        @Volatile
        private var hasTriggeredForCurrentCycle = false

        /**
         * Helper to dynamically register receiver in application lifecycle.
         */
        fun register(context: Context): BatteryDeathReceiver {
            val receiver = BatteryDeathReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_BATTERY_LOW)
            }
            context.registerReceiver(receiver, filter)
            Log.i(TAG, "BatteryDeathReceiver registered successfully.")
            return receiver
        }

        /**
         * Manually triggers the battery death routine for testing/verification.
         */
        fun triggerBatteryDeathBeacon(context: Context, batteryLevel: Int = 1) {
            val receiver = BatteryDeathReceiver()
            receiver.dispatchLastLocationBeacon(context, batteryLevel)
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BATTERY_CHANGED || action == Intent.ACTION_BATTERY_LOW) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

            val percentage = if (level >= 0 && scale > 0) {
                ((level.toFloat() / scale.toFloat()) * 100).toInt()
            } else {
                -1
            }

            Log.d(TAG, "Battery update received: $percentage%")

            // When battery reaches 1% or lower, trigger death beacon
            if (percentage in 0..1 && !hasTriggeredForCurrentCycle) {
                hasTriggeredForCurrentCycle = true
                Log.w(TAG, "BATTERY DEATH THRESHOLD (<= 1%) REACHED! Broadcasting last location...")
                dispatchLastLocationBeacon(context, percentage)
            } else if (percentage > 5) {
                // Reset flag when device is plugged in/recharged
                hasTriggeredForCurrentCycle = false
            }
        }
    }

    private fun dispatchLastLocationBeacon(context: Context, batteryLevel: Int) {
        scope.launch {
            try {
                val db = AppDatabase.getInstance(context)
                val deviceHelper = DeviceSafetyHelper(context)
                val remoteDispatcher = SosRemoteDispatcher()
                val termiiGateway = TermiiSmsGateway(context)

                val coords = deviceHelper.getCurrentCoordinates()
                val address = deviceHelper.reverseGeocode(coords.first, coords.second)
                val user = db.userDao().getUserSync()
                val contacts = db.trustedContactDao().getAllContactsSync()

                val userName = user?.name ?: "Ajiya User"

                // 1. Send POST to /last-location on remote backend
                remoteDispatcher.sendLastLocationOnBatteryDeath(
                    userId = user?.id ?: "user_default",
                    lat = coords.first,
                    lng = coords.second,
                    address = address,
                    batteryLevel = batteryLevel
                )

                // 2. Dispatch urgent SMS alert to trusted contacts
                val emergencySms = "AJIYA CRITICAL ALERT: $userName's phone battery is dying (${batteryLevel}%). " +
                        "Last location: $address. Live link: https://ajiya.network/live/beacon-${System.currentTimeMillis() % 100000}"

                val phoneList = contacts.map { it.phone }
                if (phoneList.isNotEmpty()) {
                    termiiGateway.broadcastEmergencySms(phoneList, emergencySms)
                }

                // 3. Display local system alert notification
                showBatteryDeathNotification(context, address, batteryLevel)

                Log.i(TAG, "Successfully dispatched battery death beacon and SMS alerts for 1% battery.")
            } catch (e: Exception) {
                Log.e(TAG, "Error executing battery death routine: ${e.message}")
            }
        }
    }

    private fun showBatteryDeathNotification(context: Context, address: String, batteryLevel: Int) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Battery Death Alert",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Broadcasts last known location right before phone dies"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
                .setContentTitle("Critical Battery ($batteryLevel%) - Last Location Sent")
                .setContentText("Emergency beacon broadcasted to your trusted contacts: $address")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Emergency beacon broadcasted to your trusted contacts: $address"))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display battery notification: ${e.message}")
        }
    }
}
