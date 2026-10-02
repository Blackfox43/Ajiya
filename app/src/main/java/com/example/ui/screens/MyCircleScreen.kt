package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BeaconCyan
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SafetyViewModel

@Composable
fun MyCircleScreen(viewModel: SafetyViewModel) {
    val contacts by viewModel.trustedContacts.collectAsState()
    var add by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var rel by remember { mutableStateOf("") }
    var sms by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
            .padding(20.dp),
        contentPadding = PaddingValues(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Trusted Circle", fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(
                "Your emergency network. Only people you choose.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = BeaconCyan,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${contacts.size}/5 trusted contacts", fontWeight = FontWeight.Bold)
                        Text("Consent-based sharing only", fontSize = 11.sp, color = TextSecondary)
                    }
                    Button(
                        onClick = { add = true },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Add") }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { sms = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Sms, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Preview emergency SMS")
            }
        }
        items(contacts, key = { it.id }) { c ->
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = BeaconCyan.copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                c.name.take(1).uppercase(),
                                color = BeaconCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, fontWeight = FontWeight.Bold)
                        Text(c.relationship, color = TextSecondary, fontSize = 12.sp)
                        Text(c.phone, color = TextMuted, fontSize = 11.sp)
                    }
                    if (c.isPrimary) {
                        Text(
                            "PRIMARY",
                            color = SafeGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        IconButton(onClick = { viewModel.deleteContact(c.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = TextMuted)
                        }
                    }
                }
            }
        }
    }

    if (add) {
        AlertDialog(
            onDismissRequest = { add = false },
            title = { Text("Add trusted contact") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full name") }
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") }
                    )
                    OutlinedTextField(
                        value = rel,
                        onValueChange = { rel = it },
                        label = { Text("Relationship") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.isNotBlank()) {
                            viewModel.addContact(
                                name,
                                phone,
                                rel.ifBlank { "Trusted contact" }
                            )
                            name = ""
                            phone = ""
                            rel = ""
                            add = false
                        }
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { add = false }) { Text("Cancel") }
            }
        )
    }

    if (sms) {
        AlertDialog(
            onDismissRequest = { sms = false },
            title = { Text("Emergency message") },
            text = {
                Text(viewModel.getSmsBroadcastText(), fontSize = 13.sp)
            },
            confirmButton = {
                Button(onClick = { sms = false }) { Text("Close") }
            }
        )
    }
}
