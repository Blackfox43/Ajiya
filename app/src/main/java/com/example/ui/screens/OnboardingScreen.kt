package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BeaconCyan
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SafetyViewModel

@Composable
fun OnboardingScreen(viewModel: SafetyViewModel) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var realPin by remember { mutableStateOf("") }
    var confirmRealPin by remember { mutableStateOf("") }
    var duressPin by remember { mutableStateOf("") }
    var confirmDuressPin by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val canContinue = when (step) {
        0 -> true
        1 -> name.trim().length >= 2 && phone.trim().length >= 7
        2 -> realPin.length in 4..8 && duressPin.length in 4..8 &&
                realPin == confirmRealPin && duressPin == confirmDuressPin && realPin != duressPin
        3 -> consent
        else -> false
    }

    Column(
        modifier = Modifier.fillMaxSize().background(NavyBackground).padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = BeaconCyan.copy(alpha = .12f)) {
                Icon(Icons.Default.Security, contentDescription = null, tint = BeaconCyan, modifier = Modifier.padding(12.dp).size(28.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column {
                Text("AJIYA", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text("Set up your personal safety system", color = TextSecondary, fontSize = 12.sp)
            }
        }

        LinearProgressIndicator(
            progress = { (step + 1) / 4f },
            modifier = Modifier.fillMaxWidth(),
            color = BeaconCyan,
            trackColor = NavySurface
        )

        when (step) {
            0 -> WelcomeStep()
            1 -> ProfileStep(name, phone, { name = it }, { phone = it })
            2 -> PinStep(realPin, confirmRealPin, duressPin, confirmDuressPin,
                { realPin = it }, { confirmRealPin = it }, { duressPin = it }, { confirmDuressPin = it })
            3 -> ConsentStep(consent) { consent = it }
        }

        if (error != null) {
            Text(error!!, color = CrimsonPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.weight(1f))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step > 0) {
                TextButton(onClick = { error = null; step-- }, modifier = Modifier.weight(1f)) {
                    Text("Back", color = TextSecondary)
                }
            }
            Button(
                onClick = {
                    error = null
                    if (!canContinue) {
                        error = when (step) {
                            1 -> "Enter your name and a valid phone number."
                            2 -> "Use different 4–8 digit PINs and confirm both."
                            3 -> "Consent is required before AJIYA can be activated."
                            else -> "Please complete this step."
                        }
                    } else if (step < 3) {
                        step++
                    } else {
                        viewModel.completeOnboarding(name, phone, realPin, duressPin)
                    }
                },
                modifier = Modifier.weight(if (step > 0) 2f else 1f).height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BeaconCyan, contentColor = Color.Black)
            ) {
                Text(if (step == 3) "Activate AJIYA" else "Continue", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable private fun WelcomeStep() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Build your safety profile first.", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text("AJIYA needs a real identity and secure credentials before emergency features can be trusted.", color = TextSecondary, fontSize = 15.sp, lineHeight = 22.sp)
        SecurityCard(Icons.Default.Person, "Your identity", "Used to identify you in your private safety profile.")
        SecurityCard(Icons.Default.Lock, "Secure PINs", "Your PINs are hashed locally. AJIYA never needs to store the PIN itself.")
        SecurityCard(Icons.Default.CheckCircle, "Your control", "You choose when to share location and who belongs in your Trusted Circle.")
    }
}

@Composable private fun SecurityCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = NavySurface), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = BeaconCyan, modifier = Modifier.size(24.dp))
            Spacer(Modifier.size(14.dp))
            Column { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold); Text(body, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp) }
        }
    }
}

@Composable private fun ProfileStep(name: String, phone: String, onName: (String) -> Unit, onPhone: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Who should AJIYA protect?", color = TextPrimary, fontSize = 27.sp, fontWeight = FontWeight.Black)
        Text("Use your real details. Emergency contacts will eventually use this identity when receiving alerts.", color = TextSecondary, fontSize = 14.sp)
        OutlinedTextField(value = name, onValueChange = onName, modifier = Modifier.fillMaxWidth(), label = { Text("Full name") }, singleLine = true)
        OutlinedTextField(value = phone, onValueChange = onPhone, modifier = Modifier.fillMaxWidth(), label = { Text("Phone number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
        Text("Phone verification will be added through the production backend before live dispatch is enabled.", color = TextMuted, fontSize = 11.sp)
    }
}

@Composable private fun PinStep(real: String, confirmReal: String, duress: String, confirmDuress: String, onReal: (String) -> Unit, onConfirmReal: (String) -> Unit, onDuress: (String) -> Unit, onConfirmDuress: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Protect your emergency controls.", color = TextPrimary, fontSize = 27.sp, fontWeight = FontWeight.Black)
        Text("Create two different PINs. Your normal PIN opens AJIYA normally; your duress PIN can silently enter the decoy flow.", color = TextSecondary, fontSize = 14.sp)
        PinField("Normal PIN", real, onReal)
        PinField("Confirm normal PIN", confirmReal, onConfirmReal)
        PinField("Duress PIN", duress, onDuress)
        PinField("Confirm duress PIN", confirmDuress, onConfirmDuress)
    }
}

@Composable private fun PinField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) onValue(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
    )
}

@Composable private fun ConsentStep(consent: Boolean, onConsent: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("One final safety agreement.", color = TextPrimary, fontSize = 27.sp, fontWeight = FontWeight.Black)
        Text("AJIYA can use device capabilities such as location, notifications and other permissions when you explicitly enable them. Emergency sharing should only happen according to the choices you configure.", color = TextSecondary, fontSize = 14.sp, lineHeight = 21.sp)
        Card(colors = CardDefaults.cardColors(containerColor = NavySurface), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(checked = consent, onCheckedChange = onConsent)
                Spacer(Modifier.size(8.dp))
                Text("I understand and consent to AJIYA's safety features being configured on this device.", color = TextPrimary, fontSize = 13.sp)
            }
        }
        Text("You can review permissions and safety settings later from the Security section.", color = TextMuted, fontSize = 12.sp)
    }
}
