package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.network.SosRemoteDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Monitors network connectivity and synchronizes pending emergency SOS events
 * and location pings from the offline Room database to Firebase Firestore
 * as soon as network connection is restored.
 */
class OfflineEmergencySyncManager(
    private val context: Context,
    private val database: AppDatabase,
    private val dispatcher: SosRemoteDispatcher
) {
    private val tag = "OfflineEmergencySyncManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.i(tag, "Network connection restored. Syncing pending emergency events to cloud...")
            syncPendingEmergencyEvents()
        }

        override fun onLost(network: Network) {
            Log.w(tag, "Device went offline. Subsequent SOS events will be stored locally in Room.")
        }
    }

    fun startListening() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
            // Perform an initial sync check if currently online
            syncPendingEmergencyEvents()
        } catch (e: Exception) {
            Log.w(tag, "Failed to register network callback: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.w(tag, "Failed to unregister network callback: ${e.message}")
        }
    }

    fun syncPendingEmergencyEvents() {
        scope.launch {
            try {
                val pendingEvents = database.sosEventDao().getPendingSyncEvents()
                if (pendingEvents.isEmpty()) {
                    Log.d(tag, "No pending emergency SOS events requiring cloud sync.")
                    return@launch
                }

                Log.i(tag, "Found ${pendingEvents.size} pending emergency events to sync.")
                val user = database.userDao().getUserSync()
                val contacts = database.trustedContactDao().getAllContactsSync()

                for (event in pendingEvents) {
                    val result = dispatcher.dispatchSosEvent(event, user, contacts)
                    if (result.isSuccess) {
                        database.sosEventDao().updateDeliveryStatus(event.id, "DELIVERED")
                        Log.i(tag, "Successfully synced emergency event ${event.id} to cloud.")
                    } else {
                        database.sosEventDao().updateDeliveryStatus(event.id, "RETRYING")
                        Log.w(tag, "Cloud sync for event ${event.id} failed; marked RETRYING.")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error during offline emergency sync: ${e.message}", e)
            }
        }
    }
}
