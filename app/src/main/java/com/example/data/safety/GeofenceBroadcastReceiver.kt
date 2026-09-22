package com.example.data.safety

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.db.AppDatabase
import com.example.data.network.TermiiSmsGateway
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by Google Geofencing API transitions
 * (Entering or Exiting safe zones / monitored corridors).
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
        private const val CHANNEL_ID = "ajiya_geofence_channel"
        private const val NOTIFICATION_ID = 9911

        /**
         * Simulates geofence transition for local UI testing and verification.
         */
        fun simulateTransition(context: Context, zoneName: String, isExit: Boolean) {
            val receiver = GeofenceBroadcastReceiver()
            receiver.handleTransition(context, zoneName, if (isExit) Geofence.GEOFENCE_TRANSITION_EXIT else Geofence.GEOFENCE_TRANSITION_ENTER)
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofencing event error: $errorMessage")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        if (triggeringGeofences.isNullOrEmpty()) return

        for (geofence in triggeringGeofences) {
            val zoneId = geofence.requestId
            val zoneName = when (zoneId) {
                "zone_home" -> "Home Safe Zone"
                "zone_work" -> "Workplace / Campus"
                "zone_shelter" -> "Safety Haven"
                else -> zoneId
            }
            handleTransition(context, zoneName, geofenceTransition)
        }
    }

    private fun handleTransition(context: Context, zoneName: String, transitionType: Int) {
        val isExit = transitionType == Geofence.GEOFENCE_TRANSITION_EXIT
        val title = if (isExit) "Exited Safe Zone: $zoneName" else "Entered Safe Zone: $zoneName"
        val message = if (isExit) {
            "You left $zoneName. Consensual location sharing active. Stay alert!"
        } else {
            "You have safely arrived at $zoneName. Location sharing is secure."
        }

        Log.i(TAG, "Geofence Transition: $title - $message")
        showNotification(context, title, message)

        // If user is on an active Risky Trip and exits safe perimeter, dispatch SMS update
        if (isExit) {
            scope.launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val activeSos = db.sosEventDao().getActiveSosSync()
                    if (activeSos != null && activeSos.mode == "RISKY_TRIP") {
                        val contacts = db.trustedContactDao().getAllContactsSync()
                        val user = db.userDao().getUserSync()
                        val sms = "AJIYA GEOFENCE ALERT: ${user?.name ?: "User"} has departed safe boundary ($zoneName) while traveling. Tracking active: https://ajiya.network/live/${activeSos.id}"
                        TermiiSmsGateway(context).broadcastEmergencySms(contacts.map { it.phone }, sms)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send geofence SMS update: ${e.message}")
                }
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Safe Zone Geofencing",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Alerts when entering or leaving designated safe zones"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing geofence notification: ${e.message}")
        }
    }
}
