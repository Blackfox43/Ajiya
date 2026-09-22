package com.example.data.repository

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.HelperAlertEntity
import com.example.data.model.LocationPingEntity
import com.example.data.model.SosEventEntity
import com.example.data.model.TrustedContactEntity
import com.example.data.model.UserEntity
import com.example.data.safety.DeviceSafetyHelper
import com.example.data.safety.SosForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class UnlockResult {
    SUCCESS_REAL,
    DURESS_DECOY,
    INCORRECT
}

class SafetyRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val deviceHelper: DeviceSafetyHelper,
    private val scope: CoroutineScope
) {
    val userFlow: Flow<UserEntity?> = database.userDao().getUser()
    val trustedContactsFlow: Flow<List<TrustedContactEntity>> = database.trustedContactDao().getAllContacts()
    val activeSosFlow: Flow<SosEventEntity?> = database.sosEventDao().getActiveSos()
    val allEventsFlow: Flow<List<SosEventEntity>> = database.sosEventDao().getAllEvents()
    val helperAlertsFlow: Flow<List<HelperAlertEntity>> = database.helperAlertDao().getAllAlerts()

    private var pingTrackingJob: Job? = null
    private var riskyTripTimerJob: Job? = null

    init {
        // Initialize default user and circle if needed
        scope.launch(Dispatchers.IO) {
            initDefaultDataIfEmpty()
            purgeOldHistory()
        }
    }

    private suspend fun initDefaultDataIfEmpty() {
        val existingUser = database.userDao().getUserSync()
        if (existingUser == null) {
            database.userDao().upsertUser(
                UserEntity(
                    id = "user_default",
                    name = "Tunde Adeleke",
                    phone = "+234 803 555 0192",
                    isHelper = true,
                    shareLocationOptIn = true,
                    panicPin = "9999",
                    realPin = "1234",
                    activeCrisisMode = "KIDNAP_SILENT",
                    hasCompletedConsent = true
                )
            )
        }

        val contacts = database.trustedContactDao().getAllContactsSync()
        if (contacts.isEmpty()) {
            // Seed 3 trusted circle members as requested
            database.trustedContactDao().insertContact(
                TrustedContactEntity(
                    name = "Amina Adeleke",
                    phone = "+234 802 334 1100",
                    relationship = "Spouse / Next of Kin",
                    isPrimary = true
                )
            )
            database.trustedContactDao().insertContact(
                TrustedContactEntity(
                    name = "Dr. Emeka Okafor",
                    phone = "+234 813 445 2299",
                    relationship = "Brother / Doctor",
                    isPrimary = false
                )
            )
            database.trustedContactDao().insertContact(
                TrustedContactEntity(
                    name = "Captain Femi Johnson",
                    phone = "+234 705 667 3388",
                    relationship = "Close Friend / Security",
                    isPrimary = false
                )
            )
        }

        // Add sample nearby alert for helper demonstration
        database.helperAlertDao().insertAlert(
            HelperAlertEntity(
                id = "alert_demo_1",
                anonymizedLabel = "Distress Signal (Anonymized)",
                distanceMeters = 500,
                crisisType = "Accident SOS",
                status = "PENDING",
                lat = 6.4310,
                lng = 3.4245
            )
        )
    }

    suspend fun verifyPin(pin: String): UnlockResult {
        val user = database.userDao().getUserSync() ?: return UnlockResult.SUCCESS_REAL
        return when (pin) {
            user.realPin -> UnlockResult.SUCCESS_REAL
            user.panicPin -> UnlockResult.DURESS_DECOY
            else -> UnlockResult.INCORRECT
        }
    }

    suspend fun updatePins(realPin: String, panicPin: String) {
        val current = database.userDao().getUserSync() ?: return
        database.userDao().upsertUser(current.copy(realPin = realPin, panicPin = panicPin))
    }

    suspend fun updateHelperToggle(isHelper: Boolean) {
        database.userDao().updateHelperStatus(isHelper)
    }

    suspend fun updateLocationOptIn(optIn: Boolean) {
        database.userDao().updateLocationOptIn(optIn)
    }

    suspend fun updateCrisisMode(mode: String) {
        database.userDao().updateCrisisMode(mode)
    }

    suspend fun addTrustedContact(name: String, phone: String, relationship: String) {
        database.trustedContactDao().insertContact(
            TrustedContactEntity(
                name = name,
                phone = phone,
                relationship = relationship,
                isPrimary = false
            )
        )
    }

    suspend fun deleteContact(id: Long) {
        database.trustedContactDao().deleteById(id)
    }

    /**
     * Trigger SOS:
     * a) Start background GPS tracking every 10 seconds
     * b) Record 30 sec audio in background
     * c) Formulate SMS/Push to Trusted Circle: "[Name] needs help! Live location: [link]"
     * d) Show SOS on nearby Safe Circle users within 2km
     * e) Share battery level + last known address
     */
    suspend fun triggerSos(customMode: String? = null): SosEventEntity = withContext(Dispatchers.IO) {
        val user = database.userDao().getUserSync()
        val mode = customMode ?: user?.activeCrisisMode ?: "KIDNAP_SILENT"
        val battery = deviceHelper.getBatteryLevel()
        val coords = deviceHelper.getCurrentCoordinates()
        val address = deviceHelper.reverseGeocode(coords.first, coords.second)

        val sosId = "SOS-${(100000 + (Math.random() * 900000).toInt())}"

        // Vibrate for feedback UNLESS Silent Kidnap mode
        if (mode != "KIDNAP_SILENT") {
            triggerHapticFeedback()
        }

        val newSos = SosEventEntity(
            id = sosId,
            userId = user?.id ?: "user_default",
            lat = coords.first,
            lng = coords.second,
            address = address,
            audioUrl = null,
            battery = battery,
            status = "active",
            mode = mode,
            createdAt = System.currentTimeMillis()
        )

        database.sosEventDao().insertEvent(newSos)

        // Insert initial ping
        database.locationPingDao().insertPing(
            LocationPingEntity(
                sosId = sosId,
                lat = coords.first,
                lng = coords.second,
                accuracy = 4.5f,
                speed = 0.0f,
                timestamp = System.currentTimeMillis()
            )
        )

        // Start ongoing SOS Foreground Service (background GPS every 10s, 30s MediaRecorder, Supabase/Firebase event broadcast)
        SosForegroundService.start(context, sosId, mode)

        newSos
    }

    /**
     * Consensual Risky Trip (1 Hour Live Location Share)
     */
    suspend fun triggerRiskyTrip(): SosEventEntity = withContext(Dispatchers.IO) {
        val user = database.userDao().getUserSync()
        val battery = deviceHelper.getBatteryLevel()
        val coords = deviceHelper.getCurrentCoordinates()
        val address = deviceHelper.reverseGeocode(coords.first, coords.second)
        val sosId = "TRIP-${(100000 + (Math.random() * 900000).toInt())}"

        val tripEvent = SosEventEntity(
            id = sosId,
            userId = user?.id ?: "user_default",
            lat = coords.first,
            lng = coords.second,
            address = address,
            audioUrl = null,
            battery = battery,
            status = "active",
            mode = "RISKY_TRIP",
            createdAt = System.currentTimeMillis()
        )

        database.sosEventDao().insertEvent(tripEvent)
        SosForegroundService.start(context, sosId, "RISKY_TRIP")

        // Auto-stop after 1 hour (3600 seconds)
        riskyTripTimerJob?.cancel()
        riskyTripTimerJob = scope.launch(Dispatchers.IO) {
            delay(3600_000L) // 1 hour
            resolveSos(sosId)
        }

        tripEvent
    }

    private fun startLivePingsLoop(sosId: String, initialLat: Double, initialLng: Double) {
        pingTrackingJob?.cancel()
        pingTrackingJob = scope.launch(Dispatchers.IO) {
            var currentLat = initialLat
            var currentLng = initialLng
            var step = 0
            while (isActive) {
                delay(10_000L) // GPS tracking every 10 seconds
                step++

                // Try real location, or simulate subtle path progression
                val realCoords = try {
                    deviceHelper.getCurrentCoordinates()
                } catch (e: Exception) {
                    null
                }

                if (realCoords != null && realCoords != Pair(6.4281, 3.4219)) {
                    currentLat = realCoords.first
                    currentLng = realCoords.second
                } else {
                    // Small simulated GPS movement along route
                    currentLat += (Math.random() - 0.5) * 0.0004
                    currentLng += (Math.random() - 0.5) * 0.0004
                }

                database.locationPingDao().insertPing(
                    LocationPingEntity(
                        sosId = sosId,
                        lat = currentLat,
                        lng = currentLng,
                        accuracy = 3.5f + (Math.random().toFloat() * 2f),
                        speed = 1.2f + (step * 0.5f),
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun resolveSos(sosId: String) = withContext(Dispatchers.IO) {
        SosForegroundService.stop(context)
        pingTrackingJob?.cancel()
        pingTrackingJob = null
        riskyTripTimerJob?.cancel()
        riskyTripTimerJob = null
        deviceHelper.stopAudioRecording()
        database.sosEventDao().resolveEvent(sosId)
    }

    suspend fun purgeOldHistory() = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) // 24 hours
        database.locationPingDao().deleteOlderThan(cutoff)
        database.sosEventDao().deleteResolvedOlderThan(cutoff)
    }

    suspend fun respondToHelperAlert(alertId: String, accept: Boolean) {
        database.helperAlertDao().updateAlertStatus(
            alertId = alertId,
            newStatus = if (accept) "ACCEPTED" else "DECLINED"
        )
    }

    fun getPingsForSos(sosId: String): Flow<List<LocationPingEntity>> {
        return database.locationPingDao().getPingsForSos(sosId)
    }

    private fun triggerHapticFeedback() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(500)
            }
        } catch (e: Exception) {
            // ignore
        }
    }
}
