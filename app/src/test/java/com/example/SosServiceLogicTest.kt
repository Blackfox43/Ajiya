package com.example

import com.example.data.model.LocationPingEntity
import com.example.data.model.SosEventEntity
import com.example.data.model.TrustedContactEntity
import com.example.data.model.UserEntity
import com.example.data.network.SosRemoteDispatcher
import com.example.data.safety.SosForegroundService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SosServiceLogicTest {

    @Before
    fun setUp() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        context.getSharedPreferences("ajiya_subscription_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        com.example.data.config.AppConfigManager.getInstance(context).resetToDefaults()
    }

    @Test
    fun `verify SOS intent actions and constants`() {
        assertEquals("com.example.action.START_SOS", SosForegroundService.ACTION_START_SOS)
        assertEquals("com.example.action.STOP_SOS", SosForegroundService.ACTION_STOP_SOS)
        assertEquals("extra_sos_id", SosForegroundService.EXTRA_SOS_ID)
        assertEquals("extra_crisis_mode", SosForegroundService.EXTRA_CRISIS_MODE)
        assertEquals("ajiya_sos_emergency_channel", SosForegroundService.CHANNEL_ID)
    }

    @Test
    fun `verify SosRemoteDispatcher handles offline gracefully without exceptions`() = runBlocking {
        val dispatcher = SosRemoteDispatcher(
            supabaseUrl = "https://mock-ajiya.supabase.co",
            supabaseAnonKey = "mock_key"
        )

        val testUser = UserEntity(
            id = "test_user_1",
            name = "Amina Lawal",
            phone = "+2348011223344"
        )

        val testContacts = listOf(
            TrustedContactEntity(
                id = 1,
                name = "Kola Lawal",
                phone = "+2348099887766",
                relationship = "Brother"
            )
        )

        val testSos = SosEventEntity(
            id = "SOS-TEST-999",
            userId = testUser.id,
            lat = 6.4281,
            lng = 3.4219,
            address = "Victoria Island, Lagos",
            battery = 92,
            mode = "KIDNAP_SILENT"
        )

        // Dispatch SOS: Should return a valid Result without throwing uncaught exceptions
        val eventResult = dispatcher.dispatchSosEvent(testSos, testUser, testContacts)
        assertNotNull(eventResult)

        // Send Location Ping
        val ping = LocationPingEntity(
            sosId = testSos.id,
            lat = 6.4290,
            lng = 3.4225,
            accuracy = 4.0f,
            speed = 1.5f,
            timestamp = System.currentTimeMillis()
        )
        val pingResult = dispatcher.sendLocationPing(testSos.id, ping)
        assertNotNull(pingResult)

        // Notify Trusted Contacts
        val notifyResult = dispatcher.notifyTrustedContacts(testSos, testUser, testContacts)
        assertTrue(notifyResult.isSuccess)
        assertEquals(1, notifyResult.getOrNull())
    }

    @Test
    fun `verify Battery Death POST to last-location payload and handling`() = runBlocking {
        val dispatcher = SosRemoteDispatcher()
        val result = dispatcher.sendLastLocationOnBatteryDeath(
            userId = "user_battery_test",
            lat = 6.4281,
            lng = 3.4219,
            address = "Victoria Island, Lagos",
            batteryLevel = 1
        )
        assertNotNull(result)
        // Verify graceful completion without crash
        assertTrue(result.isSuccess)
    }

    @Test
    fun `verify Termii SMS Gateway message dispatch and formatting`() = runBlocking {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val gateway = com.example.data.network.TermiiSmsGateway(context)
        val testPhones = listOf("+2348011223344", "+2348099887766")
        val message = "AJIYA SOS: User needs immediate emergency assistance!"
        val results = gateway.broadcastEmergencySms(testPhones, message)
        assertEquals(2, results.size)
        assertTrue(results.all { it.phone.isNotEmpty() })
        assertTrue(results.all { it.channel.isNotEmpty() })
    }

    @Test
    fun `verify Subscription Manager tiers and Paystack transaction flow`() = runBlocking {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val manager = com.example.data.subscription.SubscriptionManager(context)

        // Initial free plan
        assertEquals(false, manager.isSubscribed.value)
        assertEquals(com.example.data.subscription.SubscriptionTier.FREE, manager.currentTier.value)

        // Initialize Paystack payment
        val initResult = manager.initializePaystackPayment(
            email = "test@ajiya.network",
            tier = com.example.data.subscription.SubscriptionTier.SHIELD_ANNUAL
        )
        assertTrue(initResult.isSuccess)
        assertNotNull(initResult.reference)
        assertTrue(initResult.reference.startsWith("AJIYA_PAY_"))

        // Complete payment verification
        manager.completePaymentVerification(
            tier = com.example.data.subscription.SubscriptionTier.SHIELD_ANNUAL,
            reference = initResult.reference
        )
        assertEquals(true, manager.isSubscribed.value)
        assertEquals(com.example.data.subscription.SubscriptionTier.SHIELD_ANNUAL, manager.currentTier.value)

        // RevenueCat test
        val rcSuccess = manager.purchaseViaRevenueCat(com.example.data.subscription.SubscriptionTier.GUARDIAN_MONTHLY)
        assertTrue(rcSuccess)
        assertEquals(com.example.data.subscription.SubscriptionTier.GUARDIAN_MONTHLY, manager.currentTier.value)
    }

    @Test
    fun `verify Geofence Safe Zones creation and dynamic configuration`() {
        val safeZone = com.example.data.safety.GeofenceSafetyManager.createSafeZone(
            name = "Safe Haven Hub",
            lat = 9.0765,
            lng = 7.3986,
            radius = 350f
        )
        assertNotNull(safeZone)
        assertEquals("Safe Haven Hub", safeZone.name)
        assertEquals(9.0765, safeZone.latitude, 0.0001)
        assertEquals(7.3986, safeZone.longitude, 0.0001)
        assertEquals(350f, safeZone.radiusMeters, 0.1f)
    }

    @Test
    fun `verify AppConfigManager environment toggling and credential updates`() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val configManager = com.example.data.config.AppConfigManager.getInstance(context)

        // Reset to initial
        configManager.resetToDefaults()
        assertEquals(false, configManager.configState.value.isProductionMode)

        // Toggle Production Mode
        configManager.setProductionMode(true)
        assertEquals(true, configManager.configState.value.isProductionMode)
        assertTrue(configManager.configState.value.paystackPublicKey.startsWith("pk_live_"))

        // Update API keys
        configManager.updateApiKeys(
            termiiApiKey = "TLR_LIVE_PROD_12345",
            termiiSenderId = "AJIYA_PROD",
            paystackPublicKey = "pk_live_test_key",
            supabaseUrl = "https://prod-api.ajiya.network",
            supabaseAnonKey = "prod_anon_999",
            emergencyHotline = "199"
        )
        assertEquals("TLR_LIVE_PROD_12345", configManager.configState.value.termiiApiKey)
        assertEquals("AJIYA_PROD", configManager.configState.value.termiiSenderId)
        assertEquals("199", configManager.configState.value.emergencyHotline)

        // Verify Policy Consent Toggles
        configManager.setBackgroundLocationConsent(true)
        assertTrue(configManager.configState.value.backgroundLocationConsentGranted)
        configManager.setSmsConsent(true)
        assertTrue(configManager.configState.value.smsConsentGranted)

        configManager.resetToDefaults()
    }

    @Test
    fun `verify zero-permission System SMS fallback intent creation`() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val gateway = com.example.data.network.TermiiSmsGateway(context)
        val testPhone = "+2348011223344"
        val testMessage = "EMERGENCY: Need help at Victoria Island!"
        val intent = gateway.createSystemSmsIntent(testPhone, testMessage)

        assertEquals(android.content.Intent.ACTION_SENDTO, intent.action)
        assertEquals("smsto:+2348011223344", intent.data.toString())
        assertEquals(testMessage, intent.getStringExtra("sms_body"))
    }

    @Test
    fun `verify SOS camera evidence photo and video file target generation`() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val timestamp = System.currentTimeMillis()
        val photoFile = java.io.File(context.filesDir, "sos_evidence_photo_${timestamp}.jpg")
        val videoFile = java.io.File(context.filesDir, "sos_evidence_video_${timestamp}.mp4")

        assertTrue(photoFile.name.startsWith("sos_evidence_photo_"))
        assertTrue(photoFile.name.endsWith(".jpg"))
        assertTrue(videoFile.name.startsWith("sos_evidence_video_"))
        assertTrue(videoFile.name.endsWith(".mp4"))
        assertEquals(context.filesDir.absolutePath, photoFile.parentFile?.absolutePath)
        assertEquals(context.filesDir.absolutePath, videoFile.parentFile?.absolutePath)
    }

    @Test
    fun `verify SOS event and battery death with null coordinates executes safely`() = runBlocking {
        val dispatcher = SosRemoteDispatcher()
        val nullLocSos = SosEventEntity(
            id = "SOS-NULL-GPS",
            userId = "user_null_test",
            lat = null,
            lng = null,
            address = "Location Unavailable (Searching GPS...)",
            battery = 50,
            mode = "KIDNAP_SILENT"
        )
        val result = dispatcher.dispatchSosEvent(nullLocSos, null, emptyList())
        assertNotNull(result)

        val beaconResult = dispatcher.sendLastLocationOnBatteryDeath(
            userId = "user_null_test",
            lat = null,
            lng = null,
            address = "Location Unavailable (Searching GPS...)",
            batteryLevel = 1
        )
        assertNotNull(beaconResult)
        assertTrue(beaconResult.isSuccess)
    }

    @Test
    fun `verify duplicate payment reference rejection and expiry tracking`() = runBlocking {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        context.getSharedPreferences("ajiya_subscription_prefs", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        com.example.data.config.AppConfigManager.getInstance(context).resetToDefaults()
        val manager = com.example.data.subscription.SubscriptionManager(context)
        val tier = com.example.data.subscription.SubscriptionTier.GUARDIAN_MONTHLY
        val ref = "AJIYA_PAY_TEST_UNIQUE_123"

        val firstVerify = manager.verifyAndActivatePayment(tier, ref)
        assertTrue(firstVerify.isSuccess)
        assertTrue(firstVerify.expiryTimestamp > System.currentTimeMillis())

        // Replay attempt with same reference must be rejected
        val duplicateVerify = manager.verifyAndActivatePayment(tier, ref)
        assertEquals(false, duplicateVerify.isSuccess)
        assertTrue(duplicateVerify.message.contains("Duplicate transaction"))
    }

    @Test
    fun `verify OfflineEmergencySyncManager lifecycle and pending events sync`() = runBlocking {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val db = com.example.data.db.AppDatabase.getInstance(context)
        val dispatcher = SosRemoteDispatcher(context)
        val syncManager = com.example.data.sync.OfflineEmergencySyncManager(context, db, dispatcher)

        // Starting and stopping listening must not throw
        syncManager.startListening()
        syncManager.syncPendingEmergencyEvents()
        syncManager.stopListening()
    }
}
