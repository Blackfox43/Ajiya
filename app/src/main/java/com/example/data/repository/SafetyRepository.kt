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
import com.example.data.security.PinSecurity
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
        // Production builds must never ship a named demo identity, seeded contacts,
        // sample alerts, or known PINs. Create only a blank local profile.
        val existingUser = database.userDao().getUserSync()
        if (existingUser == null) {
            database.userDao().upsertUser(
                UserEntity(
                    id = "local_installation",
                    name = "",
                    phone = "",
                    isHelper = false,
                    shareLocationOptIn = false,
                    panicPin = "",
                    realPin = "",
                    activeCrisisMode = "KIDNAP_SILENT",
                    hasCompletedConsent = false,
                    onboardingCompleted = false,
                    phoneVerified = false
                )
            )
        }
        // Trusted contacts and helper alerts are intentionally empty until the user
        // explicitly creates/accepts them.
    }

    suspend fun verifyPin(pin: String): UnlockResult {
        val user = database.userDao().getUserSync() ?: return UnlockResult.INCORRECT
        if (PinSecurity.verify(pin, user.panicPin, user.pinSalt)) return UnlockResult.DURESS_DECOY
        if (PinSecurity.verify(pin, user.realPin, user.pinSalt)) return UnlockResult.SUCCESS_REAL
        return UnlockResult.INCORRECT
    }

    suspend fun updatePins(realPin: String, panicPin: String) {
        val current = database.userDao().getUserSync() ?: return
        val salt = PinSecurity.newSalt()
        database.userDao().updateSecurePins(
            realPin = PinSecurity.hash(realPin, salt),
            panicPin = PinSecurity.hash(panicPin, salt),
            pinSalt = salt
        )
    }

    suspend fun completeOnboarding(name: String, phone: String, realPin: String, panicPin: String) {
        database.userDao().getUserSync() ?: return
        val salt = PinSecurity.newSalt()
        database.userDao().completeProfile(name.trim(), phone.trim())
        database.userDao().updateSecurePins(
            realPin = PinSecurity.hash(realPin, salt),
            panicPin = PinSecurity.hash(panicPin, salt),
            pinSalt = salt
        )
        database.userDao().markConsentCompleted()
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
            userId = user?.id ?: "local_installation",
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
            userId = user?.id ?: "local_installation",
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

                // Try real location; never fabricate emergency telemetry
                val realCoords = try {
                    deviceHelper.getCurrentCoordinates()
                } catch (e: Exception) {
                    null
                }

                if (realCoords != null) {
                    currentLat = realCoords.first
                    currentLng = realCoords.second
                    database.locationPingDao().insertPing(
                        LocationPingEntity(
                            sosId = sosId,
                            lat = currentLat,
                            lng = currentLng,
                            accuracy = 0f,
                            speed = 0f,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                } else {
                    Log.w("SafetyRepository", "No real location fix available; skipping telemetry ping")
                }
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
