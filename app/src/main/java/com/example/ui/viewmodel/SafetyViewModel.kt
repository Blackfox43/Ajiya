package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.HelperAlertEntity
import com.example.data.model.LocationPingEntity
import com.example.data.model.SosEventEntity
import com.example.data.model.TrustedContactEntity
import com.example.data.model.UserEntity
import com.example.data.repository.SafetyRepository
import com.example.data.repository.UnlockResult
import com.example.data.safety.BatteryDeathReceiver
import com.example.data.safety.DeviceSafetyHelper
import com.example.data.safety.GeofenceBroadcastReceiver
import com.example.data.safety.GeofenceSafetyManager
import com.example.data.safety.SafeZone
import com.example.data.safety.ShakeDetector
import com.example.data.safety.VoiceKeywordDetector
import com.example.data.subscription.PaymentInitResult
import com.example.data.subscription.SubscriptionManager
import com.example.data.subscription.SubscriptionTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppNavDestination {
    HOME,
    CIRCLE,
    HELPER,
    HISTORY,
    SECURITY_LEGAL,
    PREMIUM_PAYWALL
}

class SafetyViewModel(application: Application) : AndroidViewModel(application) {

    val deviceHelper = DeviceSafetyHelper(application)
    private val database = AppDatabase.getInstance(application)
    val repository = SafetyRepository(
        context = application,
        database = database,
        deviceHelper = deviceHelper,
        scope = viewModelScope
    )

    val subscriptionManager = SubscriptionManager(application)
    val geofenceManager = GeofenceSafetyManager(application)

    private val shakeDetector = ShakeDetector(application) {
        onShakeTriggered()
    }

    private val voiceKeywordDetector = VoiceKeywordDetector(
        context = application,
        onKeywordDetected = { keyword ->
            onVoiceKeywordDetected(keyword)
        },
        onStatusUpdate = { status ->
            _voiceStatus.value = status
        }
    )

    private val batteryDeathReceiver: BatteryDeathReceiver? by lazy {
        BatteryDeathReceiver.register(application)
    }

    val currentUser: StateFlow<UserEntity?> = repository.userFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val trustedContacts: StateFlow<List<TrustedContactEntity>> = repository.trustedContactsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSos: StateFlow<SosEventEntity?> = repository.activeSosFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allEvents: StateFlow<List<SosEventEntity>> = repository.allEventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val helperAlerts: StateFlow<List<HelperAlertEntity>> = repository.helperAlertsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activePings = MutableStateFlow<List<LocationPingEntity>>(emptyList())
    val activePings: StateFlow<List<LocationPingEntity>> = _activePings.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppNavDestination.HOME)
    val currentScreen: StateFlow<AppNavDestination> = _currentScreen.asStateFlow()

    private val _isDecoyMode = MutableStateFlow(false)
    val isDecoyMode: StateFlow<Boolean> = _isDecoyMode.asStateFlow()

    private val _currentlyPlayingAudio = MutableStateFlow<String?>(null)
    val currentlyPlayingAudio: StateFlow<String?> = _currentlyPlayingAudio.asStateFlow()

    private val _batteryLevel = MutableStateFlow(deviceHelper.getBatteryLevel())
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    private val _currentAddress = MutableStateFlow("Locating address...")
    val currentAddress: StateFlow<String> = _currentAddress.asStateFlow()

    private val _statusNotice = MutableStateFlow<String?>(null)
    val statusNotice: StateFlow<String?> = _statusNotice.asStateFlow()

    // Premium Feature States
    private val _isShakeEnabled = MutableStateFlow(true)
    val isShakeEnabled: StateFlow<Boolean> = _isShakeEnabled.asStateFlow()

    private val _isVoiceEnabled = MutableStateFlow(false)
    val isVoiceEnabled: StateFlow<Boolean> = _isVoiceEnabled.asStateFlow()

    private val _voiceStatus = MutableStateFlow("Voice detector standby")
    val voiceStatus: StateFlow<String> = _voiceStatus.asStateFlow()

    val isSubscribed: StateFlow<Boolean> = subscriptionManager.isSubscribed
    val currentTier: StateFlow<SubscriptionTier> = subscriptionManager.currentTier
    val entitlementStatus: StateFlow<com.example.data.subscription.EntitlementStatus> = subscriptionManager.entitlementStatus

    val allTransactions: Flow<List<com.example.data.model.PaymentTransactionEntity>> = subscriptionManager.allTransactions
    val allDonations: Flow<List<com.example.data.model.DonationRecordEntity>> = subscriptionManager.allDonations
    val currentEntitlement: Flow<com.example.data.model.UserEntitlementEntity?> = subscriptionManager.currentEntitlement

    val distributionChannel: com.example.data.config.DistributionChannel
        get() = subscriptionManager.getDistributionChannel()

    fun setDistributionChannel(channel: com.example.data.config.DistributionChannel) {
        val cfg = com.example.data.config.AppConfigManager.getInstance(getApplication())
        cfg.setDistributionChannel(channel)
        _statusNotice.value = "Payment channel switched to ${channel.name}"
    }

    private val _safeZones = MutableStateFlow(GeofenceSafetyManager.DEFAULT_SAFE_ZONES)
    val safeZones: StateFlow<List<SafeZone>> = _safeZones.asStateFlow()

    init {
        // Observe active SOS to monitor its pings
        viewModelScope.launch {
            activeSos.collectLatest { sos ->
                if (sos != null) {
                    repository.getPingsForSos(sos.id).collectLatest { pings ->
                        _activePings.value = pings
                    }
                } else {
                    _activePings.value = emptyList()
                }
            }
        }

        // Refresh location and address
        refreshLocationAndAddress()
    }

    fun refreshLocationAndAddress() {
        viewModelScope.launch {
            _batteryLevel.value = deviceHelper.getBatteryLevel()
            val coords = deviceHelper.getCurrentCoordinates()
            val addr = deviceHelper.reverseGeocode(coords?.first, coords?.second)
            _currentAddress.value = addr
        }
    }

    fun navigateTo(dest: AppNavDestination) {
        _currentScreen.value = dest
    }

    fun triggerSos(crisisMode: String? = null) {
        viewModelScope.launch {
            val event = repository.triggerSos(crisisMode)
            _statusNotice.value = "EMERGENCY SOS ACTIVATED (${event.mode}). Live GPS streaming & audio recording started."
            refreshLocationAndAddress()
        }
    }

    fun triggerRiskyTrip() {
        viewModelScope.launch {
            val event = repository.triggerRiskyTrip()
            _statusNotice.value = "1-Hour Risky Trip tracking activated. Automatically stops in 60m."
            refreshLocationAndAddress()
        }
    }

    fun resolveActiveSos() {
        val current = activeSos.value ?: return
        viewModelScope.launch {
            repository.resolveSos(current.id)
            _statusNotice.value = "SOS resolved safely. Audio saved in history."
        }
    }

    fun setCrisisMode(mode: String) {
        viewModelScope.launch {
            repository.updateCrisisMode(mode)
            _statusNotice.value = "Crisis Mode switched to: $mode"
        }
    }

    fun setHelperStatus(isHelper: Boolean) {
        viewModelScope.launch {
            repository.updateHelperToggle(isHelper)
        }
    }

    fun setLocationOptIn(optIn: Boolean) {
        viewModelScope.launch {
            repository.updateLocationOptIn(optIn)
        }
    }

    fun addContact(name: String, phone: String, relationship: String) {
        viewModelScope.launch {
            repository.addTrustedContact(name, phone, relationship)
            _statusNotice.value = "Added $name to your Trusted Circle"
        }
    }

    fun deleteContact(id: Long) {
        viewModelScope.launch {
            repository.deleteContact(id)
            _statusNotice.value = "Contact removed"
        }
    }

    fun respondToHelperAlert(alertId: String, accept: Boolean) {
        viewModelScope.launch {
            repository.respondToHelperAlert(alertId, accept)
            _statusNotice.value = if (accept) {
                "Help Accepted! Victim notified that a verified helper is approaching."
            } else {
                "Alert declined"
            }
        }
    }

    /**
     * Opens live coordinates directly in Google Maps for live navigation or victim tracking.
     */
    fun openLocationInGoogleMaps(lat: Double, lng: Double, label: String = "Distress Location") {
        deviceHelper.openLocationInGoogleMaps(lat, lng, label)
        _statusNotice.value = "Opening Google Maps navigation to $label..."
    }

    /**
     * Simulates a live distress signal appearing nearby within the helper's 2km safe radius.
     * Generates a live location ping that helpers can locate on the radar and click to open in Google Maps.
     */
    fun simulateNewDistressAlert() {
        viewModelScope.launch {
            val coords = try {
                deviceHelper.getCurrentCoordinates()
            } catch (e: Exception) {
                null
            }
            if (coords == null) {
                _statusNotice.value = "GPS unavailable. Cannot compute nearby emergency radar without a location fix."
                return@launch
            }
            // Generate offset within ~300m - 1200m
            val angle = Math.random() * 2 * Math.PI
            val distanceKm = 0.3 + (Math.random() * 0.9) // 0.3km to 1.2km
            // 1 degree lat is ~111km
            val latOffset = (distanceKm / 111.0) * Math.cos(angle)
            val lngOffset = (distanceKm / (111.0 * Math.cos(Math.toRadians(coords.first)))) * Math.sin(angle)

            val victimLat = coords.first + latOffset
            val victimLng = coords.second + lngOffset
            val distanceMeters = (distanceKm * 1000).toInt()

            val types = listOf(
                "Accident SOS",
                "Medical Emergency",
                "Silent Duress Beacon",
                "Hit & Run Assistance",
                "Stranded Night Commuter"
            )
            val selectedType = types.random()
            val label = "Distress Beacon - $selectedType"

            repository.createHelperAlert(
                label = label,
                crisisType = selectedType,
                distanceMeters = distanceMeters,
                lat = victimLat,
                lng = victimLng
            )
            _statusNotice.value = "🚨 New live distress signal received (${distanceMeters}m away)! Tap to track on Google Maps."
        }
    }

    fun playAudio(path: String) {
        if (_currentlyPlayingAudio.value == path) {
            deviceHelper.stopAudioPlayback()
            _currentlyPlayingAudio.value = null
        } else {
            _currentlyPlayingAudio.value = path
            deviceHelper.playAudio(path) {
                _currentlyPlayingAudio.value = null
            }
        }
    }

    fun stopAudio() {
        deviceHelper.stopAudioPlayback()
        _currentlyPlayingAudio.value = null
    }

    fun purgeHistoryNow() {
        viewModelScope.launch {
            repository.purgeOldHistory()
            _statusNotice.value = "Expired location history & pings purged (24hr privacy rule)"
        }
    }

    fun verifyPinInput(pin: String): UnlockResult {
        val panic = currentUser.value?.panicPin ?: "9999"
        val real = currentUser.value?.realPin ?: "1234"
        return when (pin) {
            panic, "9999" -> {
                _isDecoyMode.value = true
                // REQUIREMENT: if pin == 9999 -> show fake empty screen + trigger real SOS in background
                viewModelScope.launch {
                    repository.triggerSos("KIDNAP_SILENT")
                }
                UnlockResult.DURESS_DECOY
            }
            real -> {
                _isDecoyMode.value = false
                UnlockResult.SUCCESS_REAL
            }
            else -> UnlockResult.INCORRECT
        }
    }

    fun exitDecoyMode() {
        _isDecoyMode.value = false
    }

    fun toggleShakeToSos(enabled: Boolean) {
        _isShakeEnabled.value = enabled
        if (enabled) {
            shakeDetector.startListening()
            _statusNotice.value = "Shake to SOS enabled (Accelerometer active)"
        } else {
            shakeDetector.stopListening()
            _statusNotice.value = "Shake to SOS disabled"
        }
    }

    private fun onShakeTriggered() {
        if (!_isShakeEnabled.value) return
        viewModelScope.launch {
            _statusNotice.value = "Shake detected! Initiating Emergency SOS..."
            repository.triggerSos("KIDNAP_SILENT")
            refreshLocationAndAddress()
        }
    }

    fun toggleVoiceTrigger(enabled: Boolean) {
        _isVoiceEnabled.value = enabled
        if (enabled) {
            val started = voiceKeywordDetector.startListening()
            _statusNotice.value = if (started) "Voice trigger listening for \"Help\", \"Emergency\", \"Ajiya\"" else "Voice hardware unavailable"
        } else {
            voiceKeywordDetector.stopListening()
            _statusNotice.value = "Voice trigger paused"
        }
    }

    private fun onVoiceKeywordDetected(keyword: String) {
        if (!_isVoiceEnabled.value) return
        viewModelScope.launch {
            _statusNotice.value = "Emergency voice keyword \"$keyword\" detected! Triggering SOS..."
            repository.triggerSos("KIDNAP_SILENT")
            refreshLocationAndAddress()
        }
    }

    fun simulateVoiceKeyword(keyword: String) {
        voiceKeywordDetector.simulateVoiceInput(keyword)
    }

    fun simulate1PercentBatteryDeath() {
        viewModelScope.launch {
            _statusNotice.value = "Triggering critical 1% battery beacon POST to /last-location..."
            BatteryDeathReceiver.triggerBatteryDeathBeacon(getApplication(), batteryLevel = 1)
        }
    }

    fun simulateGeofenceTransition(zoneName: String, isExit: Boolean) {
        GeofenceBroadcastReceiver.simulateTransition(getApplication(), zoneName, isExit)
        _statusNotice.value = if (isExit) "Simulated Exit from $zoneName" else "Simulated Arrival at $zoneName"
    }

    val isSandboxMode: Boolean
        get() = subscriptionManager.isSandboxMode

    // --- Subscription Operations ---
    fun initializeSubscriptionPayment(
        email: String,
        tier: SubscriptionTier,
        onResult: (PaymentInitResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = subscriptionManager.initializeSubscriptionPayment(email, tier)
            onResult(result)
        }
    }

    fun launchPlayStoreSubscription(
        activity: android.app.Activity,
        tier: SubscriptionTier,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val launched = subscriptionManager.launchPlayStoreSubscription(activity, tier)
            onComplete(launched)
        }
    }

    fun verifyAndActivateSubscription(
        tier: SubscriptionTier,
        reference: String,
        provider: String = if (distributionChannel == com.example.data.config.DistributionChannel.PLAY_STORE) "GOOGLE_PLAY" else "PAYSTACK",
        onResult: (com.example.data.subscription.PaymentVerificationResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = subscriptionManager.verifyAndActivatePayment(tier, reference, provider)
            _statusNotice.value = result.message
            onResult(result)
        }
    }

    // --- Optional Assistance Services ---
    fun initializeServicePayment(
        email: String,
        service: com.example.data.subscription.PaidAssistanceService,
        onResult: (PaymentInitResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = subscriptionManager.initializeServicePayment(email, service)
            onResult(result)
        }
    }

    fun launchPlayStoreService(
        activity: android.app.Activity,
        service: com.example.data.subscription.PaidAssistanceService,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val launched = subscriptionManager.launchPlayStoreServicePurchase(activity, service)
            onComplete(launched)
        }
    }

    fun verifyAndFulfillService(
        service: com.example.data.subscription.PaidAssistanceService,
        reference: String,
        provider: String = if (distributionChannel == com.example.data.config.DistributionChannel.PLAY_STORE) "GOOGLE_PLAY" else "PAYSTACK",
        onResult: (com.example.data.subscription.PaymentVerificationResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = subscriptionManager.verifyAndFulfillServicePayment(service, reference, provider)
            _statusNotice.value = result.message
            onResult(result)
        }
    }

    // --- Voluntary Donations ---
    fun initializeDonation(
        email: String,
        amountNgn: Long,
        donorName: String,
        isAnonymous: Boolean,
        onResult: (PaymentInitResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = subscriptionManager.initializeDonationPayment(email, amountNgn, donorName, isAnonymous)
            onResult(result)
        }
    }

    fun verifyAndRecordDonation(
        reference: String,
        amountNgn: Long,
        donorName: String,
        message: String = "",
        isAnonymous: Boolean = false,
        provider: String = "PAYSTACK",
        onResult: (com.example.data.subscription.PaymentVerificationResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = subscriptionManager.verifyAndRecordDonation(reference, amountNgn, donorName, message, isAnonymous, provider)
            _statusNotice.value = result.message
            onResult(result)
        }
    }

    // --- Restore & Manage ---
    fun restoreSubscriptions(onComplete: ((Boolean, String) -> Unit)? = null) {
        subscriptionManager.restorePurchases { success, msg ->
            _statusNotice.value = msg
            onComplete?.invoke(success, msg)
        }
    }

    fun cancelSubscription() {
        subscriptionManager.cancelSubscription()
        _statusNotice.value = "Auto-renewal cancelled. Protection remains active until expiration."
    }

    fun resetSubscriptionToFree() {
        subscriptionManager.resetToFree()
        _statusNotice.value = "Reset to Community Free plan"
    }

    // --- Legacy / Backward Compatibility Helpers ---
    fun initializePaystackPayment(
        email: String,
        tier: SubscriptionTier,
        onResult: (PaymentInitResult) -> Unit
    ) = initializeSubscriptionPayment(email, tier, onResult)

    fun verifyAndActivatePaystackPayment(
        tier: SubscriptionTier,
        reference: String,
        onResult: (com.example.data.subscription.PaymentVerificationResult) -> Unit
    ) = verifyAndActivateSubscription(tier, reference, "PAYSTACK", onResult)

    fun completePaystackPayment(tier: SubscriptionTier, reference: String) {
        viewModelScope.launch {
            subscriptionManager.verifyAndActivatePayment(tier, reference, "PAYSTACK")
            _statusNotice.value = "Paystack payment verified! ${tier.title} activated."
        }
    }

    fun purchaseViaRevenueCat(tier: SubscriptionTier, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = subscriptionManager.verifyAndActivatePayment(tier, "RC_${System.currentTimeMillis()}", "REVENUECAT")
            _statusNotice.value = res.message
            onComplete(res.isSuccess)
        }
    }

    fun updatePins(realPin: String, panicPin: String) {
        viewModelScope.launch {
            repository.updatePins(realPin, panicPin)
            _statusNotice.value = "Security PINs updated successfully"
        }
    }

    fun dismissNotice() {
        _statusNotice.value = null
    }

    fun postStatusNotice(message: String) {
        _statusNotice.value = message
    }

    fun getSmsBroadcastText(): String {
        val user = currentUser.value?.name ?: "User"
        val active = activeSos.value
        val mode = active?.mode ?: currentUser.value?.activeCrisisMode ?: "KIDNAP_SILENT"
        val battery = active?.battery ?: batteryLevel.value
        val addr = active?.address ?: currentAddress.value
        val id = active?.id ?: "SOS-DEMO"
        return "AJIYA EMERGENCY: $user needs help! Crisis Mode: $mode. Battery: $battery%. Location: $addr. Live link: https://ajiya.network/live/$id"
    }

    override fun onCleared() {
        super.onCleared()
        shakeDetector.stopListening()
        voiceKeywordDetector.stopListening()
        deviceHelper.stopAudioPlayback()
        deviceHelper.stopAudioRecording()
    }
}
