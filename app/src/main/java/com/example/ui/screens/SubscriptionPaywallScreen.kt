package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.DistributionChannel
import com.example.data.model.PaymentTransactionEntity
import com.example.data.subscription.EntitlementStatus
import com.example.data.subscription.PaidAssistanceService
import com.example.data.subscription.SubscriptionTier
import com.example.ui.viewmodel.AppNavDestination
import com.example.ui.viewmodel.SafetyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPaywallScreen(
    viewModel: SafetyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    BackHandler {
        viewModel.navigateTo(AppNavDestination.HOME)
    }

    val isSubscribed by viewModel.isSubscribed.collectAsState()
    val currentTier by viewModel.currentTier.collectAsState()
    val entitlementStatus by viewModel.entitlementStatus.collectAsState()
    val user by viewModel.currentUser.collectAsState()

    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList<PaymentTransactionEntity>())
    val donations by viewModel.allDonations.collectAsState(initial = emptyList<com.example.data.model.DonationRecordEntity>())

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Membership", "On-Demand Services", "Crisis Fund", "History & Receipts", "Settings")

    var selectedReceipt by remember { mutableStateOf<PaymentTransactionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "AJIYA Network Payments",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            when (selectedTabIndex) {
                                0 -> "Guardian Pro Memberships"
                                1 -> "Optional Private Emergency Services"
                                2 -> "Voluntary Crisis Relief Donations"
                                3 -> "Digital Transaction Ledger"
                                else -> "Channel & Subscription Management"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppNavDestination.HOME) },
                        modifier = Modifier.testTag("paywall_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Category Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_$index")
                    )
                }
            }

            // CRITICAL SAFETY MANDATE BANNER: Always prominent across all tabs
            Surface(
                color = Color(0xFF1B2A1E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Emergency,
                        contentDescription = "Free Safety Notice",
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Public Safety Guarantee: Instant 1-Tap SOS, family SMS alert, and 112 hotline access are 100% FREE forever.",
                        color = Color(0xFFC8E6C9),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            // Active Tab Content
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                when (selectedTabIndex) {
                    0 -> MembershipTabContent(
                        viewModel = viewModel,
                        activity = activity,
                        userPhone = user?.phone ?: "",
                        isSubscribed = isSubscribed,
                        currentTier = currentTier,
                        entitlementStatus = entitlementStatus
                    )
                    1 -> AssistanceServicesTabContent(
                        viewModel = viewModel,
                        activity = activity,
                        userPhone = user?.phone ?: ""
                    )
                    2 -> DonationsTabContent(
                        viewModel = viewModel,
                        userPhone = user?.phone ?: "",
                        userName = user?.name ?: ""
                    )
                    3 -> HistoryAndReceiptsTabContent(
                        transactions = transactions,
                        onSelectReceipt = { selectedReceipt = it }
                    )
                    4 -> SubscriptionManagementTabContent(
                        viewModel = viewModel,
                        currentTier = currentTier,
                        entitlementStatus = entitlementStatus
                    )
                }
            }
        }
    }

    // Interactive Digital Receipt Dialog
    if (selectedReceipt != null) {
        ReceiptDetailsDialog(
            transaction = selectedReceipt!!,
            onDismiss = { selectedReceipt = null }
        )
    }
}

// =========================================================================
// TAB 1: MEMBERSHIP PLANS
// =========================================================================

@Composable
fun MembershipTabContent(
    viewModel: SafetyViewModel,
    activity: Activity?,
    userPhone: String,
    isSubscribed: Boolean,
    currentTier: SubscriptionTier,
    entitlementStatus: EntitlementStatus
) {
    var selectedTier by remember { mutableStateOf(SubscriptionTier.SHIELD_ANNUAL) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }

    val channel = viewModel.distributionChannel

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Hero Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF2C3447), Color(0xFF151922)))
                        )
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFFFFB74D).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = "Guardian Pro",
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "AJIYA GUARDIAN PRO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB74D),
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Total Defensive Intelligence",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Automated sensor dispatch, unlimited Termii SMS alerts, satellite geofencing, and live radar incident heatmaps.",
                            fontSize = 12.sp,
                            color = Color(0xFFB0B8C8),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        if (isSubscribed) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF2E7D32).copy(alpha = 0.3f),
                                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0xFF81C784), Color(0xFF4CAF50))))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Plan Active: ${currentTier.title} ($entitlementStatus)",
                                        color = Color(0xFF81C784),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Feature Checklist
        item {
            Text(
                "SELECT MEMBERSHIP TIER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PlanCard(
                    tier = SubscriptionTier.SHIELD_ANNUAL,
                    badge = "BEST VALUE - SAVE 20%",
                    isSelected = selectedTier == SubscriptionTier.SHIELD_ANNUAL,
                    onSelect = { selectedTier = SubscriptionTier.SHIELD_ANNUAL }
                )
                PlanCard(
                    tier = SubscriptionTier.GUARDIAN_MONTHLY,
                    badge = "POPULAR CHOICE",
                    isSelected = selectedTier == SubscriptionTier.GUARDIAN_MONTHLY,
                    onSelect = { selectedTier = SubscriptionTier.GUARDIAN_MONTHLY }
                )
                PlanCard(
                    tier = SubscriptionTier.FAMILY_CIRCLE,
                    badge = "UP TO 5 USERS",
                    isSelected = selectedTier == SubscriptionTier.FAMILY_CIRCLE,
                    onSelect = { selectedTier = SubscriptionTier.FAMILY_CIRCLE }
                )
            }
        }

        // Features included in selected tier
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "${selectedTier.title} Features:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    selectedTier.features.forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(feature, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (statusText != null) {
            item {
                Text(
                    statusText ?: "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Payment Execution
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (channel == DistributionChannel.PLAY_STORE) {
                    // Google Play Store Billing flow
                    Button(
                        onClick = {
                            if (activity != null) {
                                isProcessing = true
                                statusText = "Connecting to Google Play Billing..."
                                viewModel.launchPlayStoreSubscription(activity, selectedTier) { launched ->
                                    isProcessing = false
                                    statusText = if (launched) "Google Play checkout initiated" else "Google Play unavailable in current environment"
                                }
                            }
                        },
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("play_billing_subscribe_button")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shop, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Subscribe with Google Play (${selectedTier.priceUsd} / ${selectedTier.priceNgn})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else {
                    // Direct APK Paystack Flow
                    Button(
                        onClick = {
                            isProcessing = true
                            statusText = "Initializing Paystack checkout..."
                            val email = userPhone.replace(" ", "").plus("@ajiya.network")
                            viewModel.initializeSubscriptionPayment(email, selectedTier) { init ->
                                if (init.isSuccess) {
                                    statusText = "Verifying ${init.reference}..."
                                    viewModel.verifyAndActivateSubscription(selectedTier, init.reference) { verify ->
                                        isProcessing = false
                                        statusText = verify.message
                                    }
                                } else {
                                    isProcessing = false
                                    statusText = "Error: ${init.message}"
                                }
                            }
                        },
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0BA4DB)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("paystack_subscribe_button")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Pay ${selectedTier.priceNgn} via Paystack",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Channel indicator badge
                Text(
                    "Active Distribution Channel: ${channel.name} (${if (channel == DistributionChannel.PLAY_STORE) "Google Play In-App Billing" else "Direct APK Paystack Gateways"})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// =========================================================================
// TAB 2: OPTIONAL ASSISTANCE SERVICES
// =========================================================================

@Composable
fun AssistanceServicesTabContent(
    viewModel: SafetyViewModel,
    activity: Activity?,
    userPhone: String
) {
    var selectedService by remember { mutableStateOf<PaidAssistanceService?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Handyman, contentDescription = null, tint = Color(0xFFFFB74D))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ON-DEMAND EMERGENCY SERVICES",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFFFB74D)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Request verified private security escorts, roadside recovery flatbeds, or emergency paramedic dispatch. Pricing and terms are 100% transparent before payment.",
                        fontSize = 12.sp,
                        color = Color(0xFFB0B8C8),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Text(
                "AVAILABLE EMERGENCY DISPATCH UNITS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }

        items(PaidAssistanceService.values()) { service ->
            ServiceCard(
                service = service,
                isSelected = selectedService == service,
                onSelect = { selectedService = service }
            )
        }

        if (statusText != null) {
            item {
                Text(
                    statusText ?: "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Action Button
        item {
            Button(
                onClick = {
                    val svc = selectedService ?: return@Button
                    isProcessing = true
                    statusText = "Initiating booking for ${svc.title}..."
                    val email = userPhone.replace(" ", "").plus("@ajiya.network")
                    viewModel.initializeServicePayment(email, svc) { init ->
                        if (init.isSuccess) {
                            statusText = "Verifying ${init.reference}..."
                            viewModel.verifyAndFulfillService(svc, init.reference) { verify ->
                                isProcessing = false
                                statusText = verify.message
                            }
                        } else {
                            isProcessing = false
                            statusText = "Booking failed: ${init.message}"
                        }
                    }
                },
                enabled = selectedService != null && !isProcessing,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("book_service_button")
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        if (selectedService != null) "Confirm & Dispatch ${selectedService?.title} (${selectedService?.priceNgn})" else "Select a Service Above",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// =========================================================================
// TAB 3: VOLUNTARY DONATIONS
// =========================================================================

@Composable
fun DonationsTabContent(
    viewModel: SafetyViewModel,
    userPhone: String,
    userName: String
) {
    val suggestedAmounts = listOf(1000L, 5000L, 10000L, 25000L)
    var selectedAmount by remember { mutableLongStateOf(5000L) }
    var customAmountText by remember { mutableStateOf("") }
    var isAnonymous by remember { mutableStateOf(false) }
    var donorMessage by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2A1E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = Color(0xFF81C784))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "AJIYA CRISIS RELIEF FUND",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF81C784)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Your voluntary contribution funds free SMS alerts for indigent victims, community patrol fuel subsidies, and rural emergency mesh relays across high-risk zones.",
                        fontSize = 12.sp,
                        color = Color(0xFFC8E6C9),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Text(
                "SELECT CONTRIBUTION AMOUNT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestedAmounts.forEach { amt ->
                    FilterChip(
                        selected = selectedAmount == amt && customAmountText.isEmpty(),
                        onClick = {
                            selectedAmount = amt
                            customAmountText = ""
                        },
                        label = { Text("₦${amt / 1000}k", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = customAmountText,
                onValueChange = {
                    customAmountText = it.filter { char -> char.isDigit() }
                    val custom = customAmountText.toLongOrNull()
                    if (custom != null && custom > 0) {
                        selectedAmount = custom
                    }
                },
                label = { Text("Or Enter Custom Amount (₦)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_donation_input"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = donorMessage,
                onValueChange = { donorMessage = it },
                label = { Text("Encouragement Note (Optional)") },
                placeholder = { Text("Keep our streets safe!") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isAnonymous = !isAnonymous }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Donate Anonymously", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Hide your name on community public relief boards", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isAnonymous,
                    onCheckedChange = { isAnonymous = it },
                    modifier = Modifier.testTag("anonymous_donation_switch")
                )
            }
        }

        if (statusText != null) {
            item {
                Text(
                    statusText ?: "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Button(
                onClick = {
                    isProcessing = true
                    statusText = "Initializing donation transaction..."
                    val email = userPhone.replace(" ", "").plus("@ajiya.network")
                    val donorDisplay = if (isAnonymous) "Anonymous Supporter" else userName.ifBlank { "Community Contributor" }
                    viewModel.initializeDonation(email, selectedAmount, donorDisplay, isAnonymous) { init ->
                        if (init.isSuccess) {
                            statusText = "Confirming donation ref ${init.reference}..."
                            viewModel.verifyAndRecordDonation(init.reference, selectedAmount, donorDisplay, donorMessage, isAnonymous) { verify ->
                                isProcessing = false
                                statusText = verify.message
                            }
                        } else {
                            isProcessing = false
                            statusText = "Error: ${init.message}"
                        }
                    }
                },
                enabled = selectedAmount > 0 && !isProcessing,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("donate_button")
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Contribute ₦$selectedAmount to Crisis Fund",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// =========================================================================
// TAB 4: TRANSACTION HISTORY & RECEIPTS
// =========================================================================

@Composable
fun HistoryAndReceiptsTabContent(
    transactions: List<PaymentTransactionEntity>,
    onSelectReceipt: (PaymentTransactionEntity) -> Unit
) {
    if (transactions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.AutoMirrored.Filled.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No Payment Records Yet",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Transactions, subscriptions and donations will be recorded here offline.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "OFFLINE PAYMENT LEDGER (${transactions.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }

            items(transactions) { tx ->
                TransactionListItem(
                    transaction = tx,
                    onClick = { onSelectReceipt(tx) }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// =========================================================================
// TAB 5: SETTINGS & SUBSCRIPTION MANAGEMENT
// =========================================================================

@Composable
fun SubscriptionManagementTabContent(
    viewModel: SafetyViewModel,
    currentTier: SubscriptionTier,
    entitlementStatus: EntitlementStatus
) {
    val channel = viewModel.distributionChannel
    var statusText by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("ACTIVE ENTITLEMENT STATUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(currentTier.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Status: $entitlementStatus", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Text(
                "DISTRIBUTION & BILLING CHANNEL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Google Play Store Channel", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Uses Google Play Billing Library for digital subscriptions", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = channel == DistributionChannel.PLAY_STORE,
                            onCheckedChange = { isPlay ->
                                viewModel.setDistributionChannel(
                                    if (isPlay) DistributionChannel.PLAY_STORE else DistributionChannel.DIRECT_APK
                                )
                            },
                            modifier = Modifier.testTag("channel_toggle_switch")
                        )
                    }
                }
            }
        }

        item {
            Text(
                "MANAGEMENT & RESTORATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        viewModel.restoreSubscriptions { success, msg ->
                            statusText = msg
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("restore_purchases_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Purchases & Entitlements")
                }

                OutlinedButton(
                    onClick = {
                        viewModel.cancelSubscription()
                        statusText = "Subscription auto-renewal cancelled."
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("cancel_subscription_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel Subscription Auto-Renewal")
                }

                OutlinedButton(
                    onClick = {
                        viewModel.resetSubscriptionToFree()
                        statusText = "Reset to Community Free tier."
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reset_to_free_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Reset to Free Tier")
                }
            }
        }

        if (statusText != null) {
            item {
                Text(
                    statusText ?: "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// =========================================================================
// REUSABLE SUB-COMPONENTS & DIALOGS
// =========================================================================

@Composable
fun PlanCard(
    tier: SubscriptionTier,
    badge: String?,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) borderColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onSelect() }
            .testTag("plan_${tier.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (badge != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF616161)
                    ) {
                        Text(
                            badge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(tier.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(tier.billingCycle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    tier.priceNgn,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    tier.priceUsd,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ServiceCard(
    service: PaidAssistanceService,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) borderColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onSelect() }
            .testTag("service_${service.serviceId}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(service.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(service.priceNgn, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(service.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GpsFixed, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Est. SLA Arrival: ${service.estimatedArrival}", fontSize = 11.sp, color = Color(0xFF81C784), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun TransactionListItem(
    transaction: PaymentTransactionEntity,
    onClick: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(transaction.timestamp))
    val statusColor = when (transaction.status) {
        "SUCCESS" -> Color(0xFF81C784)
        "PENDING" -> Color(0xFFFFB74D)
        else -> Color(0xFFFF5252)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("tx_${transaction.reference}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(dateStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Ref: ${transaction.reference.take(16)}...", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(transaction.amountFormatted, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        transaction.status,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReceiptDetailsDialog(
    transaction: PaymentTransactionEntity,
    onDismiss: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date(transaction.timestamp))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AJIYA Official Receipt", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider()
                ReceiptRow("Transaction Reference", transaction.reference, isMonospace = true)
                ReceiptRow("Item / Purpose", transaction.title)
                ReceiptRow("Category", transaction.category)
                ReceiptRow("Amount Paid", transaction.amountFormatted)
                ReceiptRow("Payment Provider", transaction.provider)
                ReceiptRow("Status", transaction.status)
                ReceiptRow("Date & Time", dateStr)
                if (transaction.receiptNotes.isNotBlank()) {
                    ReceiptRow("Notes", transaction.receiptNotes)
                }
                HorizontalDivider()
                Text(
                    "This receipt is digitally signed and cached securely in your local AJIYA vault.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun ReceiptRow(label: String, value: String, isMonospace: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
        )
    }
}
