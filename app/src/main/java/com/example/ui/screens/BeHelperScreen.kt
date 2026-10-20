package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HelperAlertEntity
import com.example.ui.components.LiveSafetyMap
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.BeaconCyan
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.SafeGreen
import com.example.ui.viewmodel.SafetyViewModel

@Composable
fun BeHelperScreen(
    viewModel: SafetyViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val alerts by viewModel.helperAlerts.collectAsState()
    val activeSos by viewModel.activeSos.collectAsState()
    val isHelperActive = currentUser?.isHelper == true

    val activeDistressAlerts = alerts.filter { it.status != "DECLINED" }
    var selectedAlertId by remember { mutableStateOf<String?>(null) }

    // Helper device base coordinates
    val helperLat = activeSos?.lat ?: 6.4281
    val helperLng = activeSos?.lng ?: 3.4219

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090F1D))
            .testTag("be_helper_screen")
    ) {
        // Header
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = NavySurface,
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Handshake,
                                contentDescription = null,
                                tint = SafeGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Safe Circle Helper",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    if (isHelperActive) SafeGreen.copy(alpha = 0.2f) else Color.DarkGray,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isHelperActive) "ACTIVE HELPER" else "DISABLED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isHelperActive) SafeGreen else Color.LightGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Opt-in community first responder. Receive live distress beacons within 2km and locate victims on Google Maps for rapid rescue.",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }
            }
        }

        // Helper Toggle Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, if (isHelperActive) SafeGreen.copy(alpha = 0.4f) else NavyCardBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "I Am Ready to Help Nearby",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isHelperActive)
                                "Live radar listening for distress beacons within 2km"
                            else
                                "Toggle to join the community first-responder net",
                            fontSize = 12.sp,
                            color = if (isHelperActive) SafeGreen else Color.LightGray
                        )
                    }

                    Switch(
                        checked = isHelperActive,
                        onCheckedChange = { viewModel.setHelperStatus(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SafeGreen,
                            checkedTrackColor = SafeGreen.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        ),
                        modifier = Modifier.testTag("helper_toggle_switch")
                    )
                }
            }
        }

        // Live Helper Radar & Tracking Map (Visible when helper mode is active)
        if (isHelperActive) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (activeDistressAlerts.isNotEmpty()) CrimsonPrimary else SafeGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE DISTRESS TRACKING RADAR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = if (activeDistressAlerts.isNotEmpty()) CrimsonPrimary else SafeGreen
                            )
                        }

                        Text(
                            text = "${activeDistressAlerts.size} in range",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = BeaconCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Embedded Live Safety Map with Live Google Maps Launching
                    LiveSafetyMap(
                        lat = helperLat,
                        lng = helperLng,
                        isTrackingActive = false,
                        isHelperView = true,
                        helperAlerts = activeDistressAlerts,
                        selectedAlertId = selectedAlertId,
                        onAlertSelected = { selectedAlertId = it.id },
                        onLocationClick = { targetLat, targetLng, label ->
                            viewModel.openLocationInGoogleMaps(targetLat, targetLng, label)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Helper Controls Row: Test Simulation & Google Maps direct launch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.simulateNewDistressAlert() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A6E)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("simulate_distress_beacon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAlert,
                                contentDescription = null,
                                tint = BeaconCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Simulate Beacon",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val target = activeDistressAlerts.find { it.id == selectedAlertId }
                                    ?: activeDistressAlerts.firstOrNull()
                                if (target != null) {
                                    viewModel.openLocationInGoogleMaps(
                                        target.lat,
                                        target.lng,
                                        "Distress: ${target.crisisType} (${target.distanceMeters}m)"
                                    )
                                } else {
                                    viewModel.openLocationInGoogleMaps(
                                        helperLat,
                                        helperLng,
                                        "Helper Safe Zone Hub"
                                    )
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("open_google_maps_helper_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Directions,
                                contentDescription = null,
                                tint = SafeGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open on Google Map",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }

        // Privacy & Anonymity Callout
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A2F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Privacy Shield",
                        tint = BeaconCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Consent & Mutual Privacy Protection",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BeaconCyan
                        )
                        Text(
                            text = "Distress signals show anonymized distance and live GPS coordinates on Google Maps. Victim contact info remains private until they accept your help.",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Nearby Alerts Feed Header
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "DISTRESS BEACONS FEED (CLICK TO LOCATE ON GOOGLE MAP)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = AlertAmber
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (!isHelperActive) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Helper mode is currently paused",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Turn on the toggle above to activate the live radar and receive emergency distress alerts.",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }
        } else if (alerts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = SafeGreen,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "All quiet in your 2km radius",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Tap 'Simulate Beacon' above to test live tracking and Google Maps victim location.",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }
        } else {
            items(alerts, key = { it.id }) { alert ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                    HelperAlertCard(
                        alert = alert,
                        isSelected = selectedAlertId == alert.id,
                        onSelect = { selectedAlertId = alert.id },
                        onAccept = { viewModel.respondToHelperAlert(alert.id, accept = true) },
                        onDecline = { viewModel.respondToHelperAlert(alert.id, accept = false) },
                        onOpenGoogleMaps = {
                            viewModel.openLocationInGoogleMaps(
                                alert.lat,
                                alert.lng,
                                "Victim Distress: ${alert.crisisType} (${alert.distanceMeters}m away)"
                            )
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HelperAlertCard(
    alert: HelperAlertEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onOpenGoogleMaps: () -> Unit
) {
    val isAccepted = alert.status == "ACCEPTED"
    val isDeclined = alert.status == "DECLINED"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                when {
                    isSelected -> BeaconCyan
                    isAccepted -> SafeGreen
                    else -> NavyCardBorder
                },
                RoundedCornerShape(12.dp)
            )
            .testTag("helper_alert_${alert.id}"),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isAccepted -> Color(0xFF0F2618)
                isSelected -> Color(0xFF13223E)
                else -> NavySurface
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Distress beacon distance + Crisis Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isAccepted -> SafeGreen
                                    isDeclined -> Color.Gray
                                    else -> CrimsonPrimary
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Someone needs help ${alert.distanceMeters}m away",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                Text(
                    text = alert.crisisType,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAccepted) SafeGreen else AlertAmber
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Live Coordinates display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = BeaconCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "%.4f° N, %.4f° E".format(alert.lat, alert.lng),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = BeaconCyan
                    )
                }

                Text(
                    text = "Live GPS Beacon",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Google Maps Location Button: Clicking opens Google Maps directly
            Button(
                onClick = onOpenGoogleMaps,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAccepted) SafeGreen else Color(0xFF1E3A6E)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_in_google_maps_${alert.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription = "Open in Google Maps",
                    tint = if (isAccepted) Color.Black else BeaconCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAccepted) "Start Google Maps Live Navigation" else "Locate Someone on Google Maps",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isAccepted) Color.Black else Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    tint = if (isAccepted) Color.Black else BeaconCyan,
                    modifier = Modifier.size(13.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isAccepted -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = SafeGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Assistance Accepted — Victim notified that you are navigating to them",
                            color = SafeGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
                isDeclined -> {
                    Text(
                        text = "Alert declined",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAccept,
                            colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("accept_help_${alert.id}")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Accept Help", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onDecline,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("decline_help_${alert.id}")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.LightGray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Decline", color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}
