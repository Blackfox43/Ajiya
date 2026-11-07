package com.example

import android.content.Context
import com.example.data.billing.PlayStoreBillingManager
import com.example.data.config.AppConfigManager
import com.example.data.config.DistributionChannel
import com.example.data.db.AppDatabase
import com.example.data.subscription.EntitlementStatus
import com.example.data.subscription.PaidAssistanceService
import com.example.data.subscription.SubscriptionManager
import com.example.data.subscription.SubscriptionTier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PaymentSystemLogicTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var configManager: AppConfigManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("ajiya_subscription_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        configManager = AppConfigManager.getInstance(context)
        configManager.resetToDefaults()

        db = AppDatabase.getInstance(context)
        subscriptionManager = SubscriptionManager(context)
    }

    @Test
    fun `verify Google Play Billing product configurations and IDs`() {
        assertEquals("ajiya_pro_monthly", PlayStoreBillingManager.SUB_MONTHLY)
        assertEquals("ajiya_shield_yearly", PlayStoreBillingManager.SUB_YEARLY)
        assertEquals("ajiya_family_monthly", PlayStoreBillingManager.SUB_FAMILY)
        assertEquals("ajiya_escort_service", PlayStoreBillingManager.INAPP_ESCORT)
        assertEquals("ajiya_tow_service", PlayStoreBillingManager.INAPP_TOW)
        assertEquals("ajiya_paramedic_service", PlayStoreBillingManager.INAPP_PARAMEDIC)
    }

    @Test
    fun `verify Dual Channel routing logic - Play Store vs Direct APK`() = runBlocking {
        // Default channel is PLAY_STORE
        assertEquals(DistributionChannel.PLAY_STORE, configManager.configState.value.distributionChannel)
        val playInit = subscriptionManager.initializeSubscriptionPayment("user@ajiya.network", SubscriptionTier.GUARDIAN_MONTHLY)
        assertTrue(playInit.isSuccess)
        assertTrue(playInit.reference.startsWith("PLAY_SUB_"))

        // Switch to DIRECT_APK
        configManager.setDistributionChannel(DistributionChannel.DIRECT_APK)
        assertEquals(DistributionChannel.DIRECT_APK, subscriptionManager.getDistributionChannel())
        val apkInit = subscriptionManager.initializeSubscriptionPayment("user@ajiya.network", SubscriptionTier.GUARDIAN_MONTHLY)
        assertTrue(apkInit.isSuccess)
        assertTrue(apkInit.reference.startsWith("AJIYA_PAY_"))
    }

    @Test
    fun `verify Category A - Premium Membership activation, expiry, and entitlement state`() = runBlocking {
        assertEquals(SubscriptionTier.FREE, subscriptionManager.currentTier.value)
        assertEquals(EntitlementStatus.FREE, subscriptionManager.entitlementStatus.value)
        assertFalse(subscriptionManager.isSubscribed.value)

        val ref = "AJIYA_TEST_SUB_REF_001"
        val result = subscriptionManager.verifyAndActivatePayment(
            tier = SubscriptionTier.SHIELD_ANNUAL,
            reference = ref,
            provider = "PAYSTACK"
        )

        assertTrue(result.isSuccess)
        assertTrue(result.expiryTimestamp > System.currentTimeMillis())
        assertEquals(SubscriptionTier.SHIELD_ANNUAL, subscriptionManager.currentTier.value)
        assertEquals(EntitlementStatus.ACTIVE, subscriptionManager.entitlementStatus.value)
        assertTrue(subscriptionManager.isSubscribed.value)

        // Verify Room entitlement was persisted
        val entitlement = db.userEntitlementDao().getEntitlementSync("user_default")
        assertNotNull(entitlement)
        assertEquals("ACTIVE", entitlement?.status)
        assertEquals("SHIELD_ANNUAL", entitlement?.tier)
        assertEquals("PAYSTACK", entitlement?.provider)

        // Verify transaction was logged
        val tx = db.paymentTransactionDao().getByReference(ref)
        assertNotNull(tx)
        assertEquals("MEMBERSHIP", tx?.category)
        assertEquals("SUCCESS", tx?.status)
    }

    @Test
    fun `verify Category B - Optional Assistance Services booking and fulfillment`() = runBlocking {
        val escort = PaidAssistanceService.PRIVATE_SECURITY_ESCORT
        assertEquals("₦15,000", escort.priceNgn)
        assertEquals("20–35 mins", escort.estimatedArrival)

        val tow = PaidAssistanceService.ROADSIDE_RECOVERY_TOW
        assertEquals("₦8,500", tow.priceNgn)

        val paramedic = PaidAssistanceService.RAPID_PARAMEDIC_DISPATCH
        assertEquals("₦12,000", paramedic.priceNgn)

        val ref = "AJIYA_SVC_TEST_ESCORT_99"
        val fulfillResult = subscriptionManager.verifyAndFulfillServicePayment(escort, ref, "PAYSTACK")
        assertTrue(fulfillResult.isSuccess)
        assertTrue(fulfillResult.message.contains("Armed Safety Escort"))

        // Check transaction in Room DB
        val tx = db.paymentTransactionDao().getByReference(ref)
        assertNotNull(tx)
        assertEquals("SERVICE", tx?.category)
        assertEquals(escort.title, tx?.title)
        assertEquals("SUCCESS", tx?.status)
        assertTrue(tx?.receiptNotes?.contains("20–35 mins") == true)
    }

    @Test
    fun `verify Category C - Voluntary Crisis Relief Donations recording`() = runBlocking {
        val donRef = "AJIYA_DON_TEST_5000"
        val donorName = "Ngozi Obi"
        val note = "Support community security patrols"

        val donResult = subscriptionManager.verifyAndRecordDonation(
            reference = donRef,
            amountNgn = 5000L,
            donorName = donorName,
            message = note,
            isAnonymous = false,
            provider = "PAYSTACK"
        )

        assertTrue(donResult.isSuccess)
        assertTrue(donResult.message.contains("donation"))

        // Verify in donation table
        val donation = db.donationRecordDao().getByReference(donRef)
        assertNotNull(donation)
        assertEquals(donorName, donation?.donorName)
        assertEquals(5000L, donation?.amountNgn)
        assertEquals(note, donation?.message)
        assertFalse(donation?.isAnonymous ?: true)

        // Verify also in transactions table for unified digital receipt
        val tx = db.paymentTransactionDao().getByReference(donRef)
        assertNotNull(tx)
        assertEquals("DONATION", tx?.category)
        assertEquals("₦5000", tx?.amountFormatted)
        assertEquals("SUCCESS", tx?.status)
    }

    @Test
    fun `verify Anonymous Donation hides donor name`() = runBlocking {
        val donRef = "AJIYA_DON_ANON_10000"
        val result = subscriptionManager.verifyAndRecordDonation(
            reference = donRef,
            amountNgn = 10000L,
            donorName = "Secret Benefactor",
            message = "Keep fighting crime",
            isAnonymous = true,
            provider = "PAYSTACK"
        )
        assertTrue(result.isSuccess)

        val donation = db.donationRecordDao().getByReference(donRef)
        assertNotNull(donation)
        assertEquals("Anonymous Supporter", donation?.donorName)
        assertTrue(donation?.isAnonymous == true)
    }

    @Test
    fun `verify Idempotency - Duplicate transaction references are rejected`() = runBlocking {
        val ref = "AJIYA_REPLAY_TEST_REF"
        val first = subscriptionManager.verifyAndActivatePayment(SubscriptionTier.GUARDIAN_MONTHLY, ref)
        assertTrue(first.isSuccess)

        // Second attempt must fail
        val duplicate = subscriptionManager.verifyAndActivatePayment(SubscriptionTier.GUARDIAN_MONTHLY, ref)
        assertFalse(duplicate.isSuccess)
        assertTrue(duplicate.message.contains("Duplicate transaction"))
    }

    @Test
    fun `verify Subscription cancellation and reset to Community Free`() = runBlocking {
        subscriptionManager.verifyAndActivatePayment(SubscriptionTier.GUARDIAN_MONTHLY, "REF_CANCEL_TEST")
        assertEquals(EntitlementStatus.ACTIVE, subscriptionManager.entitlementStatus.value)

        // Cancel
        subscriptionManager.cancelSubscription()
        assertEquals(EntitlementStatus.CANCELLED, subscriptionManager.entitlementStatus.value)

        // Reset to Free
        subscriptionManager.resetToFree()
        assertEquals(SubscriptionTier.FREE, subscriptionManager.currentTier.value)
        assertEquals(EntitlementStatus.FREE, subscriptionManager.entitlementStatus.value)
        assertFalse(subscriptionManager.isSubscribed.value)
    }

    @Test
    fun `verify CRITICAL SAFETY REQUIREMENT - Community Free tier retains full emergency capabilities`() {
        val freeFeatures = SubscriptionTier.FREE.features
        assertTrue(freeFeatures.any { it.contains("SOS", ignoreCase = true) })
        assertTrue(freeFeatures.any { it.contains("SMS", ignoreCase = true) })
        assertTrue(freeFeatures.any { it.contains("GPS", ignoreCase = true) })
        assertTrue(freeFeatures.any { it.contains("112", ignoreCase = true) })
    }
}
