package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.subscription.SubscriptionTier
import com.example.ui.viewmodel.AppNavDestination
import com.example.ui.viewmodel.SafetyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPaywallScreen(
    viewModel: SafetyViewModel,
    modifier: Modifier = Modifier
) {
    val isSubscribed by viewModel.isSubscribed.collectAsState()
    val currentTier by viewModel.currentTier.collectAsState()
    val user by viewModel.currentUser.collectAsState()

    var selectedTier by remember { mutableStateOf(SubscriptionTier.SHIELD_ANNUAL) }
    var isProcessingPayment by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Guardian Pro Shield",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Hero Banner
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E2430)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF2C3447), Color(0xFF151922))
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFFFFB74D).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = "Shield",
                                    tint = Color(0xFFFFB74D),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "CRISIS SAFETY NETWORK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB74D),
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Unlock AJIYA Guardian Pro",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Zero-compromise security: automated sensor triggers, offline Termii cellular SMS, and Google Geofencing safe perimeters.",
                                fontSize = 13.sp,
                                color = Color(0xFFB0B8C8),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            if (isSubscribed) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF2E7D32).copy(alpha = 0.25f),
                                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0xFF81C784), Color(0xFF4CAF50))))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Active Plan: ${currentTier.title}",
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
                    "PREMIUM PROTECTION FEATURES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        FeatureRow(
                            icon = Icons.Default.Vibration,
                            title = "Shake to SOS",
                            desc = "Accelerometer triggers emergency broadcast on 3 rapid shakes."
                        )
                        FeatureRow(
                            icon = Icons.Default.Mic,
                            title = "Voice Keyword Detection",
                            desc = "Continuous hands-free trigger on \"Help\", \"Emergency\", or \"Ajiya\"."
                        )
                        FeatureRow(
                            icon = Icons.Default.Pin,
                            title = "Panic Decoy PIN (9999)",
                            desc = "Shows mundane checklist screen while secretly dispatching real silent SOS."
                        )
                        FeatureRow(
                            icon = Icons.Default.Sms,
                            title = "Termii Offline Cellular SMS",
                            desc = "Direct telecom SMS gateway to circle with baseband fallback during network outage."
                        )
                        FeatureRow(
                            icon = Icons.Default.BatteryAlert,
                            title = "1% Battery Death Beacon",
                            desc = "Dispatches last street coordinates to /last-location right before phone powers down."
                        )
                        FeatureRow(
                            icon = Icons.Default.GpsFixed,
                            title = "Google Geofencing Safe Zones",
                            desc = "Automatic alerts when entering or exiting safe perimeters and havens."
                        )
                    }
                }
            }

            // Pricing Plans Selector
            item {
                Text(
                    "CHOOSE PROTECTION PLAN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlanOptionCard(
                        tier = SubscriptionTier.SHIELD_ANNUAL,
                        badge = "BEST VALUE - SAVE 20%",
                        isSelected = selectedTier == SubscriptionTier.SHIELD_ANNUAL,
                        onSelect = { selectedTier = SubscriptionTier.SHIELD_ANNUAL }
                    )
                    PlanOptionCard(
                        tier = SubscriptionTier.GUARDIAN_MONTHLY,
                        badge = "MOST POPULAR",
                        isSelected = selectedTier == SubscriptionTier.GUARDIAN_MONTHLY,
                        onSelect = { selectedTier = SubscriptionTier.GUARDIAN_MONTHLY }
                    )
                    PlanOptionCard(
                        tier = SubscriptionTier.FAMILY_CIRCLE,
                        badge = "UP TO 5 USERS",
                        isSelected = selectedTier == SubscriptionTier.FAMILY_CIRCLE,
                        onSelect = { selectedTier = SubscriptionTier.FAMILY_CIRCLE }
                    )
                }
            }

            // Status message
            if (statusMessage != null) {
                item {
                    Text(
                        statusMessage ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Payment Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Paystack Button (Nigeria / Africa)
                    Button(
                        onClick = {
                            isProcessingPayment = true
                            statusMessage = "Connecting to Paystack gateway..."
                            viewModel.initializePaystackPayment(
                                email = user?.phone?.replace(" ", "")?.plus("@ajiya.network") ?: "user@ajiya.network",
                                tier = selectedTier
                            ) { result ->
                                isProcessingPayment = false
                                if (result.isSuccess) {
                                    statusMessage = "Paystack checkout verified! Activating ${selectedTier.title}..."
                                    viewModel.completePaystackPayment(selectedTier, result.reference)
                                } else {
                                    statusMessage = "Paystack: ${result.message}"
                                }
                            }
                        },
                        enabled = !isProcessingPayment,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0BA4DB) // Paystack Teal/Blue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("paystack_checkout_button")
                    ) {
                        if (isProcessingPayment) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Pay ${selectedTier.priceNgn} with Paystack",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // RevenueCat Button (Global In-App Purchase)
                    Button(
                        onClick = {
                            isProcessingPayment = true
                            statusMessage = "Processing Google Play / RevenueCat transaction..."
                            viewModel.purchaseViaRevenueCat(selectedTier) { success ->
                                isProcessingPayment = false
                                statusMessage = if (success) {
                                    "RevenueCat active: ${selectedTier.title}"
                                } else {
                                    "Transaction could not be completed."
                                }
                            }
                        },
                        enabled = !isProcessingPayment,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("revenuecat_checkout_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Subscribe via RevenueCat (${selectedTier.priceUsd})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    // Secondary Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.restoreSubscriptions()
                                statusMessage = "Checking RevenueCat & Paystack entitlements..."
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Restore Purchases", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.resetSubscriptionToFree()
                                statusMessage = "Plan switched to Community Free"
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset to Free", fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun FeatureRow(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
        }
    }
}

@Composable
fun PlanOptionCard(
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
            .testTag("tier_${tier.name.lowercase()}")
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
