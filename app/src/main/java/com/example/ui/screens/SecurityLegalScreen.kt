package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BeaconCyan
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySurface
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SafetyViewModel

@Composable
fun SecurityLegalScreen(viewModel: SafetyViewModel) {
    val user by viewModel.currentUser.collectAsState()
    var pins by remember { mutableStateOf(false) }
    var real by remember { mutableStateOf("") }
    var panic by remember { mutableStateOf("") }
    var test by remember { mutableStateOf(false) }
    var testPin by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .padding(20.dp),
        contentPadding = PaddingValues(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Security", fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(
                "Privacy controls, device triggers and account protection.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
        item {
            SecurityRow(
                Icons.Default.Shield,
                "Consent first",
                "Location sharing is opt-in and can be paused at any time."
            )
        }
        item {
            SecurityRow(
                Icons.Default.DeleteSweep,
                "24-hour privacy window",
                "Resolved location history is automatically eligible for purge."
            )
        }
        item {
            SecurityRow(
                Icons.Default.VisibilityOff,
                "Covert duress mode",
                "Your panic PIN opens a neutral decoy surface while an emergency signal can be raised."
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Emergency access", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Real PIN: ${if (user?.realPin.isNullOrBlank()) "not set" else "configured"}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text("Panic PIN: configured", fontSize = 12.sp, color = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { pins = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Configure") }
                        OutlinedButton(
                            onClick = { test = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Test decoy") }
                    }
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BeaconCyan.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Privacy promise", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "AJIYA is designed for consensual safety: no phone-number spying, no silent permanent tracking dossiers, and user-controlled sharing.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

    if (pins) {
        AlertDialog(
            onDismissRequest = { pins = false },
            title = { Text("Configure PINs") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = real,
                        onValueChange = { real = it },
                        label = { Text("Real PIN") }
                    )
                    OutlinedTextField(
                        value = panic,
                        onValueChange = { panic = it },
                        label = { Text("Panic PIN") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (real.length >= 4 && panic.length >= 4) {
                            viewModel.updatePins(real, panic)
                            pins = false
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { pins = false }) { Text("Cancel") }
            }
        )
    }

    if (test) {
        AlertDialog(
            onDismissRequest = { test = false },
            title = { Text("Test panic unlock") },
            text = {
                OutlinedTextField(
                    value = testPin,
                    onValueChange = { testPin = it },
                    label = { Text("Enter PIN") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.verifyPinInput(testPin)
                        test = false
                    }
                ) { Text("Unlock") }
            },
            dismissButton = {
                TextButton(onClick = { test = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SecurityRow(icon: ImageVector, title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = BeaconCyan, modifier = Modifier.size(25.dp))
            Spacer(Modifier.width(13.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(body, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}
