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

    private val batteryDeathReceiver: BatteryDeathReceiver by lazy {
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
            val addr = deviceHelper.reverseGeocode(coords.first, coords.second)
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

    fun initializePaystackPayment(
        email: String,
        tier: SubscriptionTier,
        onResult: (PaymentInitResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = subscriptionManager.initializePaystackPayment(email, tier)
            onResult(result)
        }
    }

    fun completePaystackPayment(tier: SubscriptionTier, reference: String) {
        subscriptionManager.completePaymentVerification(tier, reference)
        _statusNotice.value = "Paystack payment verified! ${tier.title} activated."
    }

    fun purchaseViaRevenueCat(tier: SubscriptionTier, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = subscriptionManager.purchaseViaRevenueCat(tier)
            if (success) {
                _statusNotice.value = "RevenueCat subscription active: ${tier.title}"
            }
            onComplete(success)
        }
    }

    fun restoreSubscriptions() {
        subscriptionManager.restorePurchases()
        _statusNotice.value = "Subscriptions restored successfully"
    }

    fun resetSubscriptionToFree() {
        subscriptionManager.resetToFree()
        _statusNotice.value = "Reset to Community Free plan"
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
