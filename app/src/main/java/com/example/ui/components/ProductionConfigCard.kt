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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.AppConfigManager
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.BeaconCyan
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.SafeGreen

@Composable
fun ProductionConfigCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configManager = remember { AppConfigManager.getInstance(context) }
    val configState by configManager.configState.collectAsState()

    var isProdMode by remember(configState.isProductionMode) { mutableStateOf(configState.isProductionMode) }
    var termiiKey by remember(configState.termiiApiKey) { mutableStateOf(configState.termiiApiKey) }
    var termiiSender by remember(configState.termiiSenderId) { mutableStateOf(configState.termiiSenderId) }
    var paystackKey by remember(configState.paystackPublicKey) { mutableStateOf(configState.paystackPublicKey) }
    var supabaseUrl by remember(configState.supabaseUrl) { mutableStateOf(configState.supabaseUrl) }
    var hotline by remember(configState.emergencyHotline) { mutableStateOf(configState.emergencyHotline) }

    var saveConfirmation by remember { mutableStateOf<String?>(null) }
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, if (isProdMode) SafeGreen.copy(alpha = 0.5f) else BeaconCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
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
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = if (isProdMode) SafeGreen else BeaconCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Production & API Gateway Config",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (isProdMode) SafeGreen.copy(alpha = 0.2f) else BeaconCyan.copy(alpha = 0.2f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isProdMode) "LIVE PRODUCTION" else "SANDBOX / TEST",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isProdMode) SafeGreen else BeaconCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Environment Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavySurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Production Environment",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isProdMode) "Live African telecom carriers & Paystack live billing" else "Safe sandbox testing with mocked gateways",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }

                Switch(
                    checked = isProdMode,
                    onCheckedChange = { checked ->
                        isProdMode = checked
                        configManager.setProductionMode(checked)
                        saveConfirmation = if (checked) "Switched to Live Production mode" else "Switched to Sandbox mode"
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SafeGreen,
                        checkedTrackColor = SafeGreen.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("prod_mode_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.fillMaxWidth().testTag("toggle_credentials_btn")
            ) {
                Text(
                    text = if (isExpanded) "Hide API Credentials" else "Manage API Keys & Telecom Endpoints",
                    fontSize = 11.sp,
                    color = Color.White
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = termiiKey,
                        onValueChange = { termiiKey = it },
                        label = { Text("Termii API Key") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BeaconCyan,
                            unfocusedBorderColor = NavyCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("termii_key_input")
                    )

                    OutlinedTextField(
                        value = termiiSender,
                        onValueChange = { termiiSender = it },
                        label = { Text("Termii Sender ID (e.g. AJIYA_ALERT)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BeaconCyan,
                            unfocusedBorderColor = NavyCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("termii_sender_input")
                    )

                    OutlinedTextField(
                        value = paystackKey,
                        onValueChange = { paystackKey = it },
                        label = { Text("Paystack Public Key (pk_live_...)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BeaconCyan,
                            unfocusedBorderColor = NavyCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("paystack_key_input")
                    )

                    OutlinedTextField(
                        value = supabaseUrl,
                        onValueChange = { supabaseUrl = it },
                        label = { Text("Supabase Realtime API URL") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BeaconCyan,
                            unfocusedBorderColor = NavyCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("supabase_url_input")
                    )

                    OutlinedTextField(
                        value = hotline,
                        onValueChange = { hotline = it },
                        label = { Text("National Emergency Hotline") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BeaconCyan,
                            unfocusedBorderColor = NavyCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("hotline_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                configManager.updateApiKeys(
                                    termiiApiKey = termiiKey,
                                    termiiSenderId = termiiSender,
                                    paystackPublicKey = paystackKey,
                                    supabaseUrl = supabaseUrl,
                                    supabaseAnonKey = configState.supabaseAnonKey,
                                    emergencyHotline = hotline
                                )
                                saveConfirmation = "API Credentials updated successfully!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                            modifier = Modifier.weight(1f).testTag("save_config_btn")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Keys", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                configManager.resetToDefaults()
                                termiiKey = configManager.configState.value.termiiApiKey
                                termiiSender = configManager.configState.value.termiiSenderId
                                paystackKey = configManager.configState.value.paystackPublicKey
                                supabaseUrl = configManager.configState.value.supabaseUrl
                                hotline = configManager.configState.value.emergencyHotline
                                isProdMode = false
                                saveConfirmation = "Reset to sandbox defaults"
                            },
                            modifier = Modifier.weight(1f).testTag("reset_config_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            saveConfirmation?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = msg, fontSize = 11.sp, color = SafeGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
