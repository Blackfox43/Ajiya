package com.example.data.config

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppConfigState(
    val isProductionMode: Boolean = false,
    val termiiApiKey: String = "TLR_TERMI_KEY_LIVE_MOCK",
    val termiiSenderId: String = "AJIYA_ALERT",
    val paystackPublicKey: String = "pk_test_ajiya_live_mock_paystack_key_999",
    val supabaseUrl: String = "https://mock-ajiya.supabase.co",
    val supabaseAnonKey: String = "mock_key_prod",
    val emergencyHotline: String = "112",
    val backgroundLocationConsentGranted: Boolean = false,
    val smsConsentGranted: Boolean = false
)

/**
 * Manages production credentials, environment toggles (Sandbox vs. Production),
 * and compliance consent states for Google Play Background Location & SMS.
 */
class AppConfigManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ajiya_app_config_prefs", Context.MODE_PRIVATE)

    private val _configState = MutableStateFlow(loadConfig())
    val configState: StateFlow<AppConfigState> = _configState.asStateFlow()

    private fun loadConfig(): AppConfigState {
        return AppConfigState(
            isProductionMode = prefs.getBoolean("is_production_mode", false),
            termiiApiKey = prefs.getString("termii_api_key", "TLR_TERMI_KEY_LIVE_MOCK") ?: "TLR_TERMI_KEY_LIVE_MOCK",
            termiiSenderId = prefs.getString("termii_sender_id", "AJIYA_ALERT") ?: "AJIYA_ALERT",
            paystackPublicKey = prefs.getString("paystack_public_key", "pk_test_ajiya_live_mock_paystack_key_999") ?: "pk_test_ajiya_live_mock_paystack_key_999",
            supabaseUrl = prefs.getString("supabase_url", "https://mock-ajiya.supabase.co") ?: "https://mock-ajiya.supabase.co",
            supabaseAnonKey = prefs.getString("supabase_anon_key", "mock_key_prod") ?: "mock_key_prod",
            emergencyHotline = prefs.getString("emergency_hotline", "112") ?: "112",
            backgroundLocationConsentGranted = prefs.getBoolean("bg_location_consent", false),
            smsConsentGranted = prefs.getBoolean("sms_consent", false)
        )
    }

    fun setProductionMode(isProd: Boolean) {
        val defaultPaystack = if (isProd) "pk_live_ajiya_production_key_001" else "pk_test_ajiya_live_mock_paystack_key_999"
        prefs.edit()
            .putBoolean("is_production_mode", isProd)
            .putString("paystack_public_key", defaultPaystack)
            .apply()
        _configState.value = loadConfig()
    }

    fun updateApiKeys(
        termiiApiKey: String,
        termiiSenderId: String,
        paystackPublicKey: String,
        supabaseUrl: String,
        supabaseAnonKey: String,
        emergencyHotline: String
    ) {
        prefs.edit()
            .putString("termii_api_key", termiiApiKey.trim())
            .putString("termii_sender_id", termiiSenderId.trim())
            .putString("paystack_public_key", paystackPublicKey.trim())
            .putString("supabase_url", supabaseUrl.trim())
            .putString("supabase_anon_key", supabaseAnonKey.trim())
            .putString("emergency_hotline", emergencyHotline.trim())
            .apply()
        _configState.value = loadConfig()
    }

    fun setBackgroundLocationConsent(granted: Boolean) {
        prefs.edit().putBoolean("bg_location_consent", granted).apply()
        _configState.value = loadConfig()
    }

    fun setSmsConsent(granted: Boolean) {
        prefs.edit().putBoolean("sms_consent", granted).apply()
        _configState.value = loadConfig()
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        _configState.value = loadConfig()
    }

    companion object {
        @Volatile
        private var INSTANCE: AppConfigManager? = null

        fun getInstance(context: Context): AppConfigManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AppConfigManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
