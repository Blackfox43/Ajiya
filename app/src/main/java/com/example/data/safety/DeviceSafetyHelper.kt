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
import android.annotation.SuppressLint
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class DeviceSafetyHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var isRecording: Boolean = false

    private var mediaPlayer: MediaPlayer? = null

    /**
     * Reads system battery percentage.
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
                84 // fallback
            }
        } catch (e: Exception) {
            84
        }
    }

    /**
     * Best-effort GPS fix. Returns null when no real fix is available.
     * Never throws for a missing fix (callers decide how to handle it).
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentCoordinatesOrNull(): Pair<Double, Double>? {
        return withContext(Dispatchers.IO) {
            try {
                val cts = CancellationTokenSource()
                try {
                    val locationTask = fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        cts.token
                    )
                    val location: Location? = com.google.android.gms.tasks.Tasks.await(
                        locationTask,
                        8_000,
                        java.util.concurrent.TimeUnit.MILLISECONDS
                    )
                    if (location != null) {
                        return@withContext Pair(location.latitude, location.longitude)
                    }
                } catch (e: Exception) {
                    Log.w("DeviceSafetyHelper", "getCurrentLocation failed: " + e.message)
                }

                try {
                    val lastLoc: Location? = com.google.android.gms.tasks.Tasks.await(
                        fusedLocationClient.lastLocation,
                        2_000,
                        java.util.concurrent.TimeUnit.MILLISECONDS
                    )
                    if (lastLoc != null) {
                        return@withContext Pair(lastLoc.latitude, lastLoc.longitude)
                    }
                } catch (e: Exception) {
                    Log.w("DeviceSafetyHelper", "lastLocation failed: " + e.message)
                }

                requestSingleHighAccuracyFix(12_000L)
            } catch (e: SecurityException) {
                Log.w("DeviceSafetyHelper", "Location permission not granted: " + e.message)
                null
            } catch (e: Exception) {
                Log.w("DeviceSafetyHelper", "Error getting location: " + e.message)
                null
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleHighAccuracyFix(timeoutMs: Long): Pair<Double, Double>? {
        return try {
            suspendCancellableCoroutine { cont ->
                val client = fusedLocationClient
                val request = com.google.android.gms.location.LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    1_000L
                )
                    .setMinUpdateIntervalMillis(500L)
                    .setMaxUpdates(1)
                    .setDurationMillis(timeoutMs)
                    .build()

                val callback = object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        val loc = result.lastLocation
                        try {
                            client.removeLocationUpdates(this)
                        } catch (_: Exception) {
                        }
                        if (cont.isActive) {
                            if (loc != null) {
                                cont.resume(Pair(loc.latitude, loc.longitude), null)
                            } else {
                                cont.resume(null, null)
                            }
                        }
                    }
                }

                try {
                    client.requestLocationUpdates(
                        request,
                        callback,
                        android.os.Looper.getMainLooper()
                    )
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(null, null)
                    return@suspendCancellableCoroutine
                }

                cont.invokeOnCancellation {
                    try {
                        client.removeLocationUpdates(callback)
                    } catch (_: Exception) {
                    }
                }

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        client.removeLocationUpdates(callback)
                    } catch (_: Exception) {
                    }
                    if (cont.isActive) cont.resume(null, null)
                }, timeoutMs)
            }
        } catch (e: Exception) {
            Log.w("DeviceSafetyHelper", "requestSingleHighAccuracyFix failed: " + e.message)
            null
        }
    }

    /**
     * Backward-compatible helper. Prefer [getCurrentCoordinatesOrNull] for SOS paths.
     */
    suspend fun getCurrentCoordinates(): Pair<Double, Double> {
        return getCurrentCoordinatesOrNull()
            ?: throw IllegalStateException("A real location fix is unavailable")
    }

    /**
     * Reverse geocodes coordinates to a short street-style address.
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
                    if (parts.isNotEmpty()) {
                        parts.joinToString(", ")
                    } else {
                        address.getAddressLine(0) ?: "Lat: %.4f, Lng: %.4f".format(lat, lng)
                    }
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
     * Starts up to 30 seconds of ambient audio recording into app cache.
     */
    fun start30SecAudioRecording(sosId: String, onFinished: (String?) -> Unit): String? {
        if (isRecording) {
            stopAudioRecording()
        }

        try {
            val audioDir = File(context.cacheDir, "emergency_recordings")
            if (!audioDir.exists()) audioDir.mkdirs()
            val outputFile = File(audioDir, "sos_" + sosId + "_" + System.currentTimeMillis() + ".m4a")
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
                setMaxDuration(30_000)
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
     * Stops current audio recording and returns the file path if any.
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
     * Plays back recorded emergency audio.
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
     * Prepares an SMS intent for emergency contacts.
     */
    fun createSmsIntent(phoneNumbers: List<String>, messageBody: String): Intent {
        val uri = Uri.parse("smsto:" + phoneNumbers.joinToString(separator = ";"))
        return Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", messageBody)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Prepares an emergency dial intent (default 112).
     */
    fun createEmergencyDialIntent(number: String = "112"): Intent {
        return Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
