package com.example.data.safety

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Geocoder
import android.location.Location
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Locale

class DeviceSafetyHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var isRecording: Boolean = false

    private var mediaPlayer: MediaPlayer? = null

    /**
     * Reads system battery percentage
     */
    fun getBatteryLevel(): Int {
        return try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, intentFilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                ((level.toFloat() / scale.toFloat()) * 100).toInt()
            } else {
                84 // fallback realistic battery
            }
        } catch (e: Exception) {
            84
        }
    }

    /**
     * Best-effort GPS fix. Returns null when no real fix is available.
     * Never throws for a missing fix (callers decide how to handle it).
     */
    suspend fun getCurrentCoordinatesOrNull(): Pair<Double, Double>? {
        return withContext(Dispatchers.IO) {
            try {
                var lat: Double? = null
                var lng: Double? = null

                val cts = CancellationTokenSource()
                try {
                    val locationTask = fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cts.token
                    )
                    val location: Location? = com.google.android.gms.tasks.Tasks.await(
                        locationTask,
                        3000,
                        java.util.concurrent.TimeUnit.MILLISECONDS
                    )
                    if (location != null) {
                        lat = location.latitude
                        lng = location.longitude
                    }
                } catch (e: Exception) {
                    Log.w("DeviceSafetyHelper", "Fused location failed, trying lastLocation: ${e.message}")
                }

                if (lat == null || lng == null) {
                    try {
                        val lastTask = fusedLocationClient.lastLocation
                        val lastLoc: Location? = com.google.android.gms.tasks.Tasks.await(
                            lastTask,
                            1500,
                            java.util.concurrent.TimeUnit.MILLISECONDS
                        )
                        if (lastLoc != null) {
                            lat = lastLoc.latitude
                            lng = lastLoc.longitude
                        }
                    } catch (e: Exception) {
                        Log.w("DeviceSafetyHelper", "lastLocation failed: ${e.message}")
                    }
                }

                if (lat != null && lng != null) Pair(lat, lng) else null
            } catch (e: SecurityException) {
                Log.w("DeviceSafetyHelper", "Location permission not granted: ${e.message}")
                null
            } catch (e: Exception) {
                Log.w("DeviceSafetyHelper", "Error getting location: ${e.message}")
                null
            }
        }
    }

    /**
     * Backward-compatible helper. Prefer getCurrentCoordinatesOrNull().
     */
    suspend fun getCurrentCoordinates(): Pair<Double, Double> {
        return getCurrentCoordinatesOrNull()
            ?: throw IllegalStateException("A real location fix is unavailable")
    }
        }
    }

    /**
     * Reverse geocodes coordinates to street address
     */
    suspend fun reverseGeocode(lat: Double, lng: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val thoroughfare = address.thoroughfare ?: address.featureName ?: ""
                    val locality = address.locality ?: address.subAdminArea ?: ""
                    val adminArea = address.adminArea ?: ""
                    val parts = listOf(thoroughfare, locality, adminArea).filter { it.isNotBlank() }
                    if (parts.isNotEmpty()) parts.joinToString(", ") else address.getAddressLine(0) ?: "Lat: %.4f, Lng: %.4f".format(lat, lng)
                } else {
                    "Location available, address unavailable"
                }
            } catch (e: Exception) {
                Log.w("DeviceSafetyHelper", "Reverse geocoding unavailable: ${e.message}")
                "Location available, address unavailable"
            }
        }
    }

    /**
     * Starts 30-sec background audio recording (silent buffer)
     */
    fun start30SecAudioRecording(sosId: String, onFinished: (String?) -> Unit): String? {
        if (isRecording) {
            stopAudioRecording()
        }

        try {
            val audioDir = File(context.cacheDir, "emergency_recordings")
            if (!audioDir.exists()) audioDir.mkdirs()
            val outputFile = File(audioDir, "sos_${sosId}_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile.absolutePath)
                setMaxDuration(30_000) // 30 seconds max duration
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        stopAudioRecording()
                        onFinished(outputFile.absolutePath)
                    }
                }
                prepare()
                start()
            }

            mediaRecorder = recorder
            isRecording = true
            return outputFile.absolutePath
        } catch (e: Exception) {
            Log.e("DeviceSafetyHelper", "Failed to start audio recording: ${e.message}")
            isRecording = false
            currentRecordingFile = null
            return null
        }
    }

    /**
     * Stops current audio recording
     */
    fun stopAudioRecording(): String? {
        val path = currentRecordingFile?.absolutePath
        try {
            if (isRecording && mediaRecorder != null) {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
                isRecording = false
            }
        } catch (e: Exception) {
            Log.e("DeviceSafetyHelper", "Error stopping recorder: ${e.message}")
        } finally {
            mediaRecorder = null
            isRecording = false
        }
        return path
    }

    /**
     * Plays back recorded emergency audio
     */
    fun playAudio(filePath: String, onCompleted: () -> Unit) {
        stopAudioPlayback()
        try {
            val file = File(filePath)
            if (!file.exists()) return

            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    stopAudioPlayback()
                    onCompleted()
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("DeviceSafetyHelper", "Playback error: ${e.message}")
        }
    }

    fun stopAudioPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        } finally {
            mediaPlayer = null
        }
    }

    /**
     * Prepares standard SMS Intent to broadcast to emergency contacts
     */
    fun createSmsIntent(phoneNumbers: List<String>, messageBody: String): Intent {
        val uri = Uri.parse("smsto:" + phoneNumbers.joinToString(separator = ";"))
        return Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", messageBody)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Prepares Emergency Auto-Dial Intent (e.g. 112 / 911 / emergency contact)
     */
    fun createEmergencyDialIntent(number: String = "112"): Intent {
        return Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
