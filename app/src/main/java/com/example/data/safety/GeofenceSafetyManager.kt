package com.example.data.safety

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

data class SafeZone(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 300f,
    val isEnabled: Boolean = true
)

/**
 * Manages Geofences using Google Play Services Geofencing API.
 * Configures Safe Zones and monitors enter/exit transitions.
 */
class GeofenceSafetyManager(private val context: Context) {

    private val tag = "GeofenceSafetyManager"
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    companion object {
        val DEFAULT_SAFE_ZONES = listOf(
            SafeZone("zone_home", "Home Safe Zone", 6.4281, 3.4219, 250f),
            SafeZone("zone_work", "Workplace / Campus", 6.4500, 3.4000, 300f),
            SafeZone("zone_shelter", "Victoria Island Safety Haven", 6.4350, 3.4300, 400f)
        )
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            2026,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun registerSafeZone(zone: SafeZone, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        try {
            val geofence = Geofence.Builder()
                .setRequestId(zone.id)
                .setCircularRegion(zone.latitude, zone.longitude, zone.radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.i(tag, "Geofence registered for safe zone: ${zone.name} (${zone.radiusMeters}m)")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e(tag, "Failed to register geofence for ${zone.name}: ${e.message}")
                    onFailure(e)
                }
        } catch (e: Exception) {
            Log.e(tag, "Error setting up geofence: ${e.message}")
            onFailure(e)
        }
    }

    fun removeSafeZone(zoneId: String, onComplete: () -> Unit = {}) {
        geofencingClient.removeGeofences(listOf(zoneId))
            .addOnCompleteListener {
                Log.i(tag, "Removed geofence for $zoneId")
                onComplete()
            }
    }

    fun removeAllGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnCompleteListener {
                Log.i(tag, "Removed all registered geofences")
            }
    }
}
