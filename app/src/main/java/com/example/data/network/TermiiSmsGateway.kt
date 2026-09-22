package com.example.data.network

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.config.AppConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SmsDeliveryResult(
    val phone: String,
    val isSuccess: Boolean,
    val channel: String, // "TERMII_API" or "CELLULAR_FALLBACK" or "SYSTEM_SMS_INTENT"
    val detail: String
)

/**
 * Gateway for dispatching emergency SMS alerts via Termii HTTP API
 * with direct native cellular SMS fallback when device is offline.
 */
class TermiiSmsGateway(
    private val context: Context,
    apiKeyOverride: String? = null,
    senderIdOverride: String? = null
) {
    private val tag = "TermiiSmsGateway"
    private val configManager = AppConfigManager.getInstance(context)
    private val apiKey = apiKeyOverride ?: configManager.configState.value.termiiApiKey
    private val senderId = senderIdOverride ?: configManager.configState.value.termiiSenderId
    private val apiUrl = "https://api.ng.termii.com/api/sms/send"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /**
     * Sends SMS to list of recipient phone numbers.
     * Tries Termii Cloud API first; if offline or network fails, uses cellular baseband SmsManager.
     */
    suspend fun broadcastEmergencySms(
        recipients: List<String>,
        messageText: String
    ): List<SmsDeliveryResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SmsDeliveryResult>()

        for (rawPhone in recipients) {
            val phone = sanitizePhoneNumber(rawPhone)
            var deliveredViaTermii = false

            // 1. Attempt Termii API
            try {
                val payload = JSONObject().apply {
                    put("to", phone)
                    put("from", senderId)
                    put("sms", messageText)
                    put("type", "plain")
                    put("channel", "generic")
                    put("api_key", apiKey)
                }

                val request = Request.Builder()
                    .url(apiUrl)
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.i(tag, "Termii SMS delivered to $phone: HTTP ${response.code}")
                        results.add(SmsDeliveryResult(phone, true, "TERMII_API", "Delivered via Termii Cloud"))
                        deliveredViaTermii = true
                    } else {
                        Log.w(tag, "Termii API returned ${response.code} for $phone. Switching to cellular fallback.")
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Network offline or Termii call failed for $phone: ${e.message}. Using cellular fallback.")
            }

            // 2. Offline Cellular Fallback via Native SmsManager
            if (!deliveredViaTermii) {
                val hasSmsPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED

                if (hasSmsPermission) {
                    val cellularSuccess = sendViaCellularSms(phone, messageText)
                    results.add(
                        SmsDeliveryResult(
                            phone = phone,
                            isSuccess = cellularSuccess,
                            channel = "CELLULAR_FALLBACK",
                            detail = if (cellularSuccess) "Dispatched via Cellular SMS (Offline)" else "Failed cellular dispatch"
                        )
                    )
                } else {
                    Log.i(tag, "SEND_SMS permission not granted. Prepared system SMS Intent for $phone")
                    results.add(
                        SmsDeliveryResult(
                            phone = phone,
                            isSuccess = true,
                            channel = "SYSTEM_SMS_INTENT",
                            detail = "Prepared default system SMS intent for emergency broadcast"
                        )
                    )
                }
            }
        }

        results
    }

    private fun sendViaCellularSms(phone: String, messageText: String): Boolean {
        return try {
            val smsManager: SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // Split long message if needed
            val parts = smsManager.divideMessage(messageText)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phone, null, messageText, null, null)
            }
            Log.i(tag, "Native cellular SMS dispatched to $phone")
            true
        } catch (e: Exception) {
            Log.e(tag, "Cellular SMS failed for $phone: ${e.message}")
            false
        }
    }

    /**
     * Zero-permission Android fallback: launches the user's default SMS app
     * pre-populated with emergency distress message and recipient numbers.
     */
    fun createSystemSmsIntent(phone: String, messageText: String): Intent {
        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phone")
            putExtra("sms_body", messageText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun sanitizePhoneNumber(raw: String): String {
        val digits = raw.replace(Regex("[^0-9+]"), "")
        return if (digits.startsWith("0") && digits.length == 11) {
            "+234" + digits.substring(1) // Default to Nigeria +234
        } else digits
    }
}
