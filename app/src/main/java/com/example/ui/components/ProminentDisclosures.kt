package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.BeaconCyan
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.SafeGreen

/**
 * Google Play Background Location Policy Prominent Disclosure Dialog.
 * Must be shown prior to requesting ACCESS_BACKGROUND_LOCATION or engaging continuous background tracking.
 */
@Composable
fun ProminentLocationDisclosureDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDecline,
        containerColor = NavySurface,
        modifier = Modifier.testTag("prominent_location_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = BeaconCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Background Location Disclosure",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BeaconCyan.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "AJIYA collects location data in the background (even when the app is closed or not in use) to enable continuous GPS emergency tracking for your trusted safety contacts and nearby helpers during an active SOS distress alert or a consensual Risky Trip journey.",
                        fontSize = 12.sp,
                        color = Color.White,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = SafeGreen,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Privacy Guarantee: Location data is strictly ephemeral, never used for advertising, never sold, and automatically purged after 24 hours.",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        lineHeight = 15.sp
                    )
                }

                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AlertAmber,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "User Control: Background tracking is solely active when an emergency or Risky Trip is triggered and can be halted at any moment with one tap.",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        lineHeight = 15.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = BeaconCyan),
                modifier = Modifier.testTag("accept_bg_location_button")
            ) {
                Text("Accept & Continue", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDecline,
                modifier = Modifier.testTag("decline_bg_location_button")
            ) {
                Text("Foreground Only", color = Color.LightGray, fontSize = 12.sp)
            }
        }
    )
}

/**
 * Google Play SMS Policy Prominent Disclosure Dialog.
 */
@Composable
fun ProminentSmsDisclosureDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDecline,
        containerColor = NavySurface,
        modifier = Modifier.testTag("prominent_sms_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Message,
                    contentDescription = null,
                    tint = AlertAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Emergency SMS Fallback",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "AJIYA uses SMS permissions strictly to send emergency distress messages and live GPS tracking links to your designated trusted contacts when mobile internet (data/WiFi) is disconnected.",
                    fontSize = 12.sp,
                    color = Color.White,
                    lineHeight = 17.sp
                )

                Text(
                    text = "• Never reads your private incoming or outgoing messages\n• Never contacts anyone outside your saved emergency circle\n• If permission is declined, AJIYA will fall back to opening your phone's default SMS app with pre-filled distress coordinates.",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = AlertAmber),
                modifier = Modifier.testTag("accept_sms_button")
            ) {
                Text("Grant Emergency SMS", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDecline,
                modifier = Modifier.testTag("decline_sms_button")
            ) {
                Text("Use Default App Fallback", color = Color.LightGray, fontSize = 12.sp)
            }
        }
    )
}

/**
 * Play Console Reviewer & Compliance Guide.
 * Provides exact video scripts and submission declaration text for Google Play approval.
 */
@Composable
fun PlayConsoleComplianceKit(
    onTriggerLocationDisclosure: () -> Unit,
    onTriggerSmsDisclosure: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BeaconCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = BeaconCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PLAY STORE COMPLIANCE & REVIEW KIT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = BeaconCyan
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "1. Background Location Video Walkthrough Script",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "For Play Console Background Location Review submission:\n" +
                        "• Step 1: Open AJIYA home screen, tap 'Start 1h Risky Trip' or trigger SOS.\n" +
                        "• Step 2: Show the prominent disclosure explaining emergency live tracking.\n" +
                        "• Step 3: Switch app to background or lock screen.\n" +
                        "• Step 4: Show the ongoing Android Notification: 'AJIYA Emergency Tracking: Active' with live coordinates and battery level updating.\n" +
                        "• Step 5: Return to app and tap 'Resolve / Cancel' to demonstrate tracking halts immediately.",
                fontSize = 11.sp,
                color = Color.LightGray,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "2. Google Play SMS Policy Declaration Text",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Play Console SMS Declaration:\n" +
                        "• Core Feature Category: 'Personal Safety / Emergency alerts'\n" +
                        "• Description: 'AJIYA uses SEND_SMS solely as an offline emergency beacon to broadcast live GPS location links to pre-selected trusted contacts when mobile data or WiFi is unavailable in distress situations.'\n" +
                        "• Zero-Permission Fallback: When declined, app automatically launches Intent.ACTION_SENDTO with default SMS client.",
                fontSize = 11.sp,
                color = Color.LightGray,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTriggerLocationDisclosure,
                    modifier = Modifier.weight(1f).testTag("test_location_disclosure_btn")
                ) {
                    Text("Test Location Disclosure", fontSize = 11.sp, color = BeaconCyan)
                }

                OutlinedButton(
                    onClick = onTriggerSmsDisclosure,
                    modifier = Modifier.weight(1f).testTag("test_sms_disclosure_btn")
                ) {
                    Text("Test SMS Disclosure", fontSize = 11.sp, color = AlertAmber)
                }
            }
        }
    }
}
