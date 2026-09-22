package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LiveSafetyMap
import com.example.ui.components.SosEmergencyButton
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.BeaconCyan
import com.example.ui.theme.CrimsonDark
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.SafeGreen
import com.example.ui.viewmodel.AppNavDestination
import com.example.ui.viewmodel.SafetyViewModel

@Composable
fun HomeScreen(
    viewModel: SafetyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val activeSos by viewModel.activeSos.collectAsState()
    val pings by viewModel.activePings.collectAsState()
    val battery by viewModel.batteryLevel.collectAsState()
    val address by viewModel.currentAddress.collectAsState()
    val statusNotice by viewModel.statusNotice.collectAsState()

    val isShakeEnabled by viewModel.isShakeEnabled.collectAsState()
    val isVoiceEnabled by viewModel.isVoiceEnabled.collectAsState()
    val voiceStatus by viewModel.voiceStatus.collectAsState()
    val isSubscribed by viewModel.isSubscribed.collectAsState()
    val currentTier by viewModel.currentTier.collectAsState()

    val isSosActive = activeSos != null
    val selectedMode = activeSos?.mode ?: currentUser?.activeCrisisMode ?: "KIDNAP_SILENT"

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090F1D))
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
            .testTag("home_screen")
    ) {
        // Top Safety Status Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = NavySurface,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CrimsonPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "AJIYA Crisis Shield",
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AJIYA",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Text(
                            text = "CRISIS HELP NETWORK",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = BeaconCyan,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // Battery & Pro Plan Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Guardian Pro Badge / Navigation
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSubscribed) Color(0xFFFFB74D).copy(alpha = 0.25f) else NavySurfaceVariant,
                        modifier = Modifier
                            .clickable { viewModel.navigateTo(AppNavDestination.PREMIUM_PAYWALL) }
                            .testTag("premium_paywall_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Pro",
                                tint = if (isSubscribed) Color(0xFFFFB74D) else BeaconCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSubscribed) "PRO" else "UPGRADE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSubscribed) Color(0xFFFFB74D) else Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Battery indicator
                    Row(
                        modifier = Modifier
                            .background(NavySurfaceVariant, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = "Battery level",
                            tint = if (battery < 20) CrimsonPrimary else SafeGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$battery%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Active Emergency Alert Banner (when SOS is running)
        AnimatedVisibility(visible = isSosActive) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.5.dp, CrimsonPrimary, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF26050A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CRISIS EVENT ACTIVE",
                                color = CrimsonPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            text = activeSos?.id ?: "",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = AlertAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Background GPS pings sent every 10s. Ambient audio recorder active (30s). Trusted circle notified with live location link.",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.resolveActiveSos() },
                            colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("resolve_sos_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("I Am Safe (Resolve)", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val contacts = viewModel.trustedContacts.value
                                val phones = contacts.map { it.phone }
                                val text = viewModel.getSmsBroadcastText()
                                val intent = viewModel.deviceHelper.createSmsIntent(phones, text)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.testTag("sms_broadcast_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = BeaconCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SMS", color = BeaconCyan)
                        }

                        if (selectedMode == "ACCIDENT_LOUD") {
                            Button(
                                onClick = {
                                    val dialIntent = viewModel.deviceHelper.createEmergencyDialIntent("112")
                                    context.startActivity(dialIntent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                modifier = Modifier.testTag("emergency_call_button")
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }

        // Status Notice Banner (if message available)
        statusNotice?.let { notice ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(NavySurfaceVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = BeaconCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = notice,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
                Text(
                    text = "DISMISS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlertAmber,
                    modifier = Modifier.clickable { viewModel.dismissNotice() }
                )
            }
        }

        // Consensual Location Sharing Opt-In Switch
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Consensual Live Sharing",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = if (currentUser?.shareLocationOptIn == true)
                            "Opted In: Shares ONLY during active SOS or Risky Trip"
                        else
                            "PAUSED: Location tracking completely disabled",
                        fontSize = 11.sp,
                        color = if (currentUser?.shareLocationOptIn == true) SafeGreen else Color.LightGray
                    )
                }

                Switch(
                    checked = currentUser?.shareLocationOptIn == true,
                    onCheckedChange = { viewModel.setLocationOptIn(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SafeGreen,
                        checkedTrackColor = SafeGreen.copy(alpha = 0.3f),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.DarkGray
                    ),
                    modifier = Modifier.testTag("consent_location_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Crisis Mode Selection Chips
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "CRISIS SCENARIO MODE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = BeaconCyan
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Kidnap / Hostage Mode
                FilterChip(
                    selected = selectedMode == "KIDNAP_SILENT",
                    onClick = { viewModel.setCrisisMode("KIDNAP_SILENT") },
                    label = {
                        Text(
                            text = "Kidnap / Silent",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CrimsonDark,
                        selectedLabelColor = Color.White,
                        containerColor = NavySurface,
                        labelColor = Color.LightGray
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Accident Mode
                FilterChip(
                    selected = selectedMode == "ACCIDENT_LOUD",
                    onClick = { viewModel.setCrisisMode("ACCIDENT_LOUD") },
                    label = {
                        Text(
                            text = "Accident (Loud)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AlertAmber,
                        selectedLabelColor = Color.Black,
                        containerColor = NavySurface,
                        labelColor = Color.LightGray
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Natural Disaster Mode
                FilterChip(
                    selected = selectedMode == "DISASTER_CHECKIN",
                    onClick = { viewModel.setCrisisMode("DISASTER_CHECKIN") },
                    label = {
                        Text(
                            text = "Disaster",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BeaconCyan,
                        selectedLabelColor = Color.Black,
                        containerColor = NavySurface,
                        labelColor = Color.LightGray
                    ),
                    modifier = Modifier.weight(0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center Big SOS Button (3-second hold to trigger)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            SosEmergencyButton(
                crisisMode = selectedMode,
                isActive = isSosActive,
                onTriggerSos = { viewModel.triggerSos() }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Radar & Live Location Map Pane
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE LOCATION & SAFE RADIUS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = BeaconCyan
                )

                IconButton(
                    onClick = { viewModel.refreshLocationAndAddress() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh GPS",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LiveSafetyMap(
                lat = activeSos?.lat ?: 6.4281,
                lng = activeSos?.lng ?: 3.4219,
                pings = pings,
                isTrackingActive = isSosActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Last known geocoded street address
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Address",
                        tint = CrimsonPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Last Known Address",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray
                        )
                        Text(
                            text = address,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Risky Trip 1-Hour Live Location Toggle Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, BeaconCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = BeaconCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "I'm On A Risky Trip",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Boarding an unknown taxi, night bus, or traveling through unsafe terrain? Share live location for 1 hour with your circle. Auto-stops after 1 hour to save battery.",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { viewModel.triggerRiskyTrip() },
                    colors = ButtonDefaults.buttonColors(containerColor = BeaconCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("risky_trip_button")
                ) {
                    Text(
                        text = "Start 1-Hour Live Tracking",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Premium Automation & Sensor Triggers Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, Color(0xFFFFB74D).copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Automated Sensors & Crisis Triggers",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSubscribed) Color(0xFF2E7D32) else Color(0xFFFFB74D).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (isSubscribed) "PRO ACTIVE" else "UPGRADE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSubscribed) Color.White else Color(0xFFFFB74D),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Shake to SOS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = BeaconCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Shake to SOS", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
                            Text("Accelerometer triggers emergency broadcast on 3 rapid shakes", fontSize = 11.sp, color = Color.LightGray)
                        }
                    }
                    Switch(
                        checked = isShakeEnabled,
                        onCheckedChange = { viewModel.toggleShakeToSos(it) },
                        modifier = Modifier.testTag("shake_sos_switch"),
                        colors = SwitchDefaults.colors(checkedThumbColor = BeaconCyan, checkedTrackColor = BeaconCyan.copy(alpha = 0.5f))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Voice Keyword Detection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = AlertAmber, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Voice Keyword Trigger", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
                            Text(voiceStatus, fontSize = 11.sp, color = Color.LightGray)
                        }
                    }
                    Switch(
                        checked = isVoiceEnabled,
                        onCheckedChange = { viewModel.toggleVoiceTrigger(it) },
                        modifier = Modifier.testTag("voice_sos_switch"),
                        colors = SwitchDefaults.colors(checkedThumbColor = AlertAmber, checkedTrackColor = AlertAmber.copy(alpha = 0.5f))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.simulateVoiceKeyword("Help") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("simulate_voice_help_button")
                    ) {
                        Text("Simulate \"Help\"", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.simulateVoiceKeyword("Ajiya") },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("simulate_voice_ajiya_button")
                    ) {
                        Text("Simulate \"Ajiya\"", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Panic PIN (9999) info & trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Pin, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Anti-Coercion Panic PIN (9999)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
                        Text("Entering 9999 reveals fake empty checklist while secretly broadcasting SOS", fontSize = 11.sp, color = Color.LightGray)
                    }
                    Button(
                        onClick = { viewModel.verifyPinInput("9999") },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("simulate_panic_pin_button")
                    ) {
                        Text("Test 9999", fontSize = 11.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. 1% Battery Beacon & Geofence
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.simulate1PercentBatteryDeath() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("simulate_battery_death_button")
                    ) {
                        Icon(Icons.Default.BatteryAlert, contentDescription = null, modifier = Modifier.size(14.dp), tint = CrimsonPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test 1% Beacon", fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.simulateGeofenceTransition("Home Safe Haven", isExit = true) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("simulate_geofence_button")
                    ) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(14.dp), tint = SafeGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Geofence Exit", fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Subscription CTA button
                Button(
                    onClick = { viewModel.navigateTo(AppNavDestination.PREMIUM_PAYWALL) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("manage_paywall_button")
                ) {
                    Text(
                        text = if (isSubscribed) "Manage Pro Shield (${currentTier.title})" else "Unlock Guardian Pro (Paystack / RevenueCat)",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
