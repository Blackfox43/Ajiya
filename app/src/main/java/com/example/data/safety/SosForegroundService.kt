package com.example.data.safety

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Geocoder
import android.location.Location
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.model.LocationPingEntity
import com.example.data.model.SosEventEntity
import com.example.data.network.SosRemoteDispatcher
import com.example.data.network.TermiiSmsGateway
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Ongoing Emergency Foreground Service.
 * Manages:
 * 1. Continuous high-accuracy background GPS location breadcrumbs (every 10s).
 * 2. 30-second background ambient audio capture using the MediaRecorder API.
 * 3. Real-time Firebase and Supabase event synchronization & notifications to Trusted Circle.
 */
class SosForegroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var database: AppDatabase
    private val remoteDispatcher = SosRemoteDispatcher()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager

    private var activeSosId: String? = null
    private var crisisMode: String = "KIDNAP_SILENT"

    private var locationCallback: LocationCallback? = null
    private var isLocationTrackingActive = false

    private var mediaRecorder: MediaRecorder? = null
    private var audioRecordingFile: File? = null
    private var isAudioRecording = false
    private var audioWatchdogJob: Job? = null

    companion object {
        private const val TAG = "SosForegroundService"
        const val CHANNEL_ID = "ajiya_sos_emergency_channel"
        const val NOTIFICATION_ID = 9110

        const val ACTION_START_SOS = "com.example.action.START_SOS"
        const val ACTION_STOP_SOS = "com.example.action.STOP_SOS"

        const val EXTRA_SOS_ID = "extra_sos_id"
        const val EXTRA_CRISIS_MODE = "extra_crisis_mode"

        /**
         * Starts the SOS foreground service safely across all Android versions.
         */
        fun start(context: Context, sosId: String, mode: String) {
            val intent = Intent(context, SosForegroundService::class.java).apply {
                action = ACTION_START_SOS
                putExtra(EXTRA_SOS_ID, sosId)
                putExtra(EXTRA_CRISIS_MODE, mode)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stops the SOS foreground service.
         */
        fun stop(context: Context) {
            val intent = Intent(context, SosForegroundService::class.java).apply {
                action = ACTION_STOP_SOS
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(applicationContext)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.i(TAG, "SosForegroundService received action: $action")

        when (action) {
            ACTION_START_SOS -> {
                val sosId = intent.getStringExtra(EXTRA_SOS_ID) ?: "SOS-${System.currentTimeMillis() % 1000000}"
                val mode = intent.getStringExtra(EXTRA_CRISIS_MODE) ?: "KIDNAP_SILENT"
                activeSosId = sosId
                crisisMode = mode

                // 1. Elevate to foreground service with high-priority notification
                val notification = buildNotification(
                    title = "AJIYA Emergency Active",
                    content = "Crisis: $mode. Live GPS streaming & audio capture active."
                )
                startForegroundWithServiceTypes(notification)

                // 2. Start continuous 10s background GPS updates
                startBackgroundLocationTracking(sosId)

                // 3. Start 30-second audio capture using MediaRecorder
                start30SecondAudioCapture(sosId)

                // 4. Trigger Firebase/Supabase broadcast notification to trusted contacts
                broadcastSosToContactsAndBackend(sosId, mode)
            }
            ACTION_STOP_SOS -> {
                stopSosServiceGracefully()
            }
        }

        return START_STICKY
    }

    private fun startForegroundWithServiceTypes(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10-13
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service: ${e.message}")
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Continuous 10-second background GPS updates via FusedLocationProviderClient.
     */
    @SuppressLint("MissingPermission")
    private fun startBackgroundLocationTracking(sosId: String) {
        if (isLocationTrackingActive) return

        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
                .setMinUpdateIntervalMillis(5_000L)
                .setMinUpdateDistanceMeters(0f)
                .setWaitForAccurateLocation(false)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    handleLocationUpdate(sosId, location)
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback as LocationCallback,
                Looper.getMainLooper()
            )
            isLocationTrackingActive = true
            Log.i(TAG, "Background GPS tracking started for SOS: $sosId (every 10s)")
        } catch (se: SecurityException) {
            Log.e(TAG, "Location permission missing for background tracking: ${se.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location updates: ${e.message}")
        }
    }

    private fun handleLocationUpdate(sosId: String, location: Location) {
        serviceScope.launch {
            val lat = location.latitude
            val lng = location.longitude
            val accuracy = location.accuracy
            val speed = location.speed
            val timestamp = System.currentTimeMillis()

            // 1. Insert ping into Room DB
            val ping = LocationPingEntity(
                sosId = sosId,
                lat = lat,
                lng = lng,
                accuracy = accuracy,
                speed = speed,
                timestamp = timestamp
            )
            database.locationPingDao().insertPing(ping)

            // 2. Geocode address asynchronously
            val address = reverseGeocode(lat, lng)

            // 3. Update current SOS event in DB
            database.sosEventDao().updateLocation(sosId, lat, lng, address)

            // 4. Stream ping to Supabase Realtime
            remoteDispatcher.sendLocationPing(sosId, ping)

            // 5. Update notification with current location
            updateNotificationContent(
                title = "AJIYA Emergency Active ($crisisMode)",
                content = "Live Location: $address (Accuracy: ${accuracy.toInt()}m)"
            )
            Log.d(TAG, "Location ping recorded & streamed: $lat, $lng ($address)")
        }
    }

    /**
     * 30-Second Ambient Audio Capture using Android MediaRecorder API.
     */
    private fun start30SecondAudioCapture(sosId: String) {
        if (isAudioRecording) return

        serviceScope.launch(Dispatchers.Main) {
            try {
                val audioDir = File(cacheDir, "emergency_recordings")
                if (!audioDir.exists()) audioDir.mkdirs()

                val outputFile = File(audioDir, "sos_${sosId}_${System.currentTimeMillis()}.m4a")
                audioRecordingFile = outputFile

                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(applicationContext)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }

                recorder.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(64000)
                    setAudioSamplingRate(44100)
                    setOutputFile(outputFile.absolutePath)
                    setMaxDuration(30_000) // 30-second cap
                    setOnInfoListener { _, what, _ ->
                        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                            Log.i(TAG, "MediaRecorder reached 30s max duration.")
                            stopAudioRecordingAndPersist(sosId, outputFile.absolutePath)
                        }
                    }
                    prepare()
                    start()
                }

                mediaRecorder = recorder
                isAudioRecording = true
                Log.i(TAG, "30-second ambient audio recording started: ${outputFile.absolutePath}")

                // Watchdog timer (30.5 seconds) to ensure resource is freed even if hardware callback hangs
                audioWatchdogJob?.cancel()
                audioWatchdogJob = serviceScope.launch {
                    delay(30_500L)
                    if (isAudioRecording) {
                        Log.i(TAG, "Audio watchdog timer triggered completion.")
                        stopAudioRecordingAndPersist(sosId, outputFile.absolutePath)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start MediaRecorder: ${e.message}")
                isAudioRecording = false
                mediaRecorder = null
            }
        }
    }

    private fun stopAudioRecordingAndPersist(sosId: String, audioPath: String) {
        if (!isAudioRecording && mediaRecorder == null) return
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder: ${e.message}")
        } finally {
            mediaRecorder = null
            isAudioRecording = false
            audioWatchdogJob?.cancel()
            audioWatchdogJob = null
        }

        // Persist audio evidence URL in local DB and update remote event
        serviceScope.launch {
            Log.i(TAG, "30s audio recording saved: $audioPath. Updating database and Supabase...")
            database.sosEventDao().updateAudioUrl(sosId, audioPath)
            remoteDispatcher.updateAudioUrl(sosId, audioPath)
        }
    }

    /**
     * Triggers the Firebase / Supabase emergency broadcast notification to all Trusted Circle contacts.
     */
    private fun broadcastSosToContactsAndBackend(sosId: String, mode: String) {
        serviceScope.launch {
            val user = database.userDao().getUserSync()
            val contacts = database.trustedContactDao().getAllContactsSync()
            var event = database.sosEventDao().getById(sosId)

            if (event == null) {
                // In case event was not pre-inserted, create it
                event = SosEventEntity(
                    id = sosId,
                    userId = user?.id ?: "user_default",
                    lat = 6.4281,
                    lng = 3.4219,
                    address = "Victoria Island, Lagos",
                    audioUrl = null,
                    battery = 85,
                    status = "active",
                    mode = mode,
                    createdAt = System.currentTimeMillis()
                )
                database.sosEventDao().insertEvent(event)
            }

            // Dispatch to Supabase and Firebase Cloud Notification
            remoteDispatcher.dispatchSosEvent(event, user, contacts)

            // Dispatch Offline/Online Emergency SMS via Termii API + Cellular Fallback
            val contactPhones = contacts.map { it.phone }
            if (contactPhones.isNotEmpty()) {
                val userName = user?.name ?: "Ajiya User"
                val trackingLink = "https://ajiya.network/live/$sosId"
                val smsMessage = "AJIYA SOS EMERGENCY: $userName needs immediate assistance! Crisis Mode: $mode. " +
                        "Location: ${event.address}. Battery: ${event.battery}%. Live Tracking: $trackingLink"
                TermiiSmsGateway(applicationContext).broadcastEmergencySms(contactPhones, smsMessage)
            }

            // Update lastNotifiedTimestamp for all trusted contacts
            val now = System.currentTimeMillis()
            contacts.forEach { contact ->
                database.trustedContactDao().updateLastNotified(contact.id, now)
            }
            Log.i(TAG, "Emergency broadcast and Termii SMS sent to ${contacts.size} trusted contacts.")
        }
    }

    private fun stopSosServiceGracefully() {
        Log.i(TAG, "Stopping SosForegroundService...")

        // 1. Remove location updates
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        isLocationTrackingActive = false
        locationCallback = null

        // 2. Stop audio recording if still running
        activeSosId?.let { id ->
            audioRecordingFile?.absolutePath?.let { path ->
                stopAudioRecordingAndPersist(id, path)
            }
        }

        // 3. Mark SOS as resolved in local database & notify remote backend
        activeSosId?.let { id ->
            serviceScope.launch {
                database.sosEventDao().resolveEvent(id)
                remoteDispatcher.resolveSosEvent(id)
            }
        }

        // 4. Remove foreground notification and stop service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action to resolve SOS directly from the notification
        val resolveIntent = Intent(this, SosForegroundService::class.java).apply {
            action = ACTION_STOP_SOS
        }
        val resolvePendingIntent = PendingIntent.getService(
            this,
            1,
            resolveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "I'M SAFE / RESOLVE",
                resolvePendingIntent
            )
            .build()
    }

    private fun updateNotificationContent(title: String, content: String) {
        val notification = buildNotification(title, content)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AJIYA Emergency Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Ongoing GPS tracking & safety monitoring during active SOS"
                enableVibration(false) // Silent by default for kidnap/hostage modes
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private suspend fun reverseGeocode(lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(applicationContext, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val thoroughfare = address.thoroughfare ?: address.featureName ?: ""
                val locality = address.locality ?: address.subAdminArea ?: ""
                val parts = listOf(thoroughfare, locality).filter { it.isNotBlank() }
                if (parts.isNotEmpty()) parts.joinToString(", ") else "Lat: %.4f, Lng: %.4f".format(lat, lng)
            } else {
                "Lat: %.4f, Lng: %.4f".format(lat, lng)
            }
        } catch (e: Exception) {
            "Lat: %.4f, Lng: %.4f".format(lat, lng)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopSosServiceGracefully()
        serviceJob.cancel()
    }
}
