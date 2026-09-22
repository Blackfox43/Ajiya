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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.UnlockResult
import com.example.ui.components.PlayConsoleComplianceKit
import com.example.ui.components.ProductionConfigCard
import com.example.ui.components.ProminentLocationDisclosureDialog
import com.example.ui.components.ProminentSmsDisclosureDialog
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.BeaconCyan
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.SafeGreen
import com.example.ui.viewmodel.SafetyViewModel

@Composable
fun SecurityLegalScreen(
    viewModel: SafetyViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val scrollState = rememberScrollState()

    var showEditPinDialog by remember { mutableStateOf(false) }
    var showTestPinDialog by remember { mutableStateOf(false) }

    var realPinInput by remember { mutableStateOf(currentUser?.realPin ?: "1234") }
    var panicPinInput by remember { mutableStateOf(currentUser?.panicPin ?: "9999") }
    var testPinInput by remember { mutableStateOf("") }
    var testResultText by remember { mutableStateOf<String?>(null) }

    var showLocationDisclosurePreview by remember { mutableStateOf(false) }
    var showSmsDisclosurePreview by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090F1D))
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
            .testTag("security_legal_screen")
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = NavySurface,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = BeaconCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Security & Consent",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Strict consent-only safeguards, encrypted hardware storage, and duress safety mechanisms.",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mandatory Legal Disclaimer Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.5.dp, AlertAmber.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1808)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Legal Warning",
                        tint = AlertAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LEGAL & CONSENT DISCLAIMER",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = AlertAmber,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "“This app is for consensual safety. You can only track people who installed app and approved you. Misuse is prohibited.”",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "AJIYA does not support unauthorized or covert surveillance. Live location is only transmitted during an active SOS or consensual 1-hour Risky Trip session.",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Consent-Only Architecture Guarantees
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CORE CONSENT PRINCIPLES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = SafeGreen
                )

                Spacer(modifier = Modifier.height(10.dp))

                ConsentItem(
                    title = "No Phone-Number Spying",
                    desc = "Unlike predatory apps, nobody can track you simply by knowing your phone number. Tracking requires explicit account mutual pairing."
                )

                Spacer(modifier = Modifier.height(8.dp))

                ConsentItem(
                    title = "Opt-In & Instant Pause",
                    desc = "You can toggle off location sharing with one tap from the home screen at any time."
                )

                Spacer(modifier = Modifier.height(8.dp))

                ConsentItem(
                    title = "24-Hour Ephemeral Purge",
                    desc = "All location pings and resolved crisis events are automatically deleted after 24 hours to prevent historical tracking logs."
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Duress / Panic PIN Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, CrimsonPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Duress / Panic PIN",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(CrimsonPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ANTI-COERCION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = CrimsonPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "If kidnappers, robbers, or hostile actors force you to unlock the app, enter your Panic PIN. The app opens a harmless decoy Personal Notes screen, showing no distress features or contacts.",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Real PIN:", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = currentUser?.realPin ?: "1234",
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = SafeGreen
                        )
                    }

                    Column {
                        Text("Panic PIN (Decoy):", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = currentUser?.panicPin ?: "9999",
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = AlertAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showEditPinDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NavySurfaceVariant),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_pins_button")
                    ) {
                        Text("Configure PINs", color = Color.White, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            testPinInput = ""
                            testResultText = null
                            showTestPinDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_duress_button")
                    ) {
                        Text("Test Panic Unlock", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Production API & Telecom Configuration
        ProductionConfigCard(modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(modifier = Modifier.height(14.dp))

        // Google Play Compliance & Video Walkthrough Review Kit
        PlayConsoleComplianceKit(
            onTriggerLocationDisclosure = { showLocationDisclosurePreview = true },
            onTriggerSmsDisclosure = { showSmsDisclosurePreview = true },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }

    if (showLocationDisclosurePreview) {
        ProminentLocationDisclosureDialog(
            onAccept = { showLocationDisclosurePreview = false },
            onDecline = { showLocationDisclosurePreview = false }
        )
    }

    if (showSmsDisclosurePreview) {
        ProminentSmsDisclosureDialog(
            onAccept = { showSmsDisclosurePreview = false },
            onDecline = { showSmsDisclosurePreview = false }
        )
    }

    // Edit PINs Dialog
    if (showEditPinDialog) {
        AlertDialog(
            onDismissRequest = { showEditPinDialog = false },
            containerColor = NavySurface,
            title = {
                Text("Configure Security PINs", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose a normal 4-digit PIN for daily access, and a secondary Panic PIN to show the decoy mode under duress.",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )

                    OutlinedTextField(
                        value = realPinInput,
                        onValueChange = { if (it.length <= 6) realPinInput = it },
                        label = { Text("Standard Real PIN") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SafeGreen,
                            unfocusedBorderColor = NavyCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("real_pin_input")
                    )

                    OutlinedTextField(
                        value = panicPinInput,
                        onValueChange = { if (it.length <= 6) panicPinInput = it },
                        label = { Text("Panic / Duress PIN (Decoy)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AlertAmber,
                            unfocusedBorderColor = NavyCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("panic_pin_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (realPinInput.isNotBlank() && panicPinInput.isNotBlank() && realPinInput != panicPinInput) {
                            viewModel.updatePins(realPinInput, panicPinInput)
                            showEditPinDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                    modifier = Modifier.testTag("save_pins_button")
                ) {
                    Text("Save PINs", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPinDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }

    // Test Duress Unlock Dialog
    if (showTestPinDialog) {
        AlertDialog(
            onDismissRequest = { showTestPinDialog = false },
            containerColor = NavySurface,
            title = {
                Text("Test Coerced Unlock", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter '${currentUser?.panicPin ?: "9999"}' to trigger Decoy Mode, or '${currentUser?.realPin ?: "1234"}' for standard safety app.",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )

                    OutlinedTextField(
                        value = testPinInput,
                        onValueChange = { testPinInput = it },
                        label = { Text("Enter PIN to test") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = NavyCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("test_pin_input")
                    )

                    testResultText?.let {
                        Text(it, fontSize = 12.sp, color = AlertAmber, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val result = viewModel.verifyPinInput(testPinInput)
                        if (result == UnlockResult.DURESS_DECOY) {
                            showTestPinDialog = false
                        } else if (result == UnlockResult.SUCCESS_REAL) {
                            testResultText = "Unlocked normal AJIYA app."
                            showTestPinDialog = false
                        } else {
                            testResultText = "Incorrect PIN."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    modifier = Modifier.testTag("confirm_test_pin_button")
                ) {
                    Text("Unlock Test", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTestPinDialog = false }) {
                    Text("Close", color = Color.LightGray)
                }
            }
        )
    }
}

@Composable
private fun ConsentItem(title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = SafeGreen,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = Color.LightGray,
                lineHeight = 15.sp
            )
        }
    }
}
