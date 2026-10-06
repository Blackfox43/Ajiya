package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.config.AppConfigManager
import com.example.ui.components.BottomNavBar
import com.example.ui.components.ProminentLocationDisclosureDialog
import com.example.ui.screens.BeHelperScreen
import com.example.ui.screens.DecoyScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyCircleScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SecurityLegalScreen
import com.example.ui.screens.SubscriptionPaywallScreen
import com.example.ui.theme.AjiyaTheme
import com.example.ui.theme.NavySurface
import com.example.ui.viewmodel.AppNavDestination
import com.example.ui.viewmodel.SafetyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AjiyaTheme {
                AjiyaApp()
            }
        }
    }
}

@Composable
fun AjiyaApp(
    viewModel: SafetyViewModel = viewModel()
) {
    val currentDestination by viewModel.currentScreen.collectAsState()
    val isDecoyMode by viewModel.isDecoyMode.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val configManager = remember { AppConfigManager.getInstance(context) }
    val configState by configManager.configState.collectAsState()

    var showBackgroundLocationDisclosure by remember { mutableStateOf(false) }

    val bgLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.refreshLocationAndAddress()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        // RECORD_AUDIO grant is consumed by SosForegroundService at SOS time
        if (fineLocationGranted || coarseGranted) {
            viewModel.refreshLocationAndAddress()
            if (!configState.backgroundLocationConsentGranted) {
                showBackgroundLocationDisclosure = true
            }
        }
    }

    LaunchedEffect(currentUser?.onboardingCompleted) {
        if (currentUser?.onboardingCompleted == true) {
            val permissionsList = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.RECORD_AUDIO
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permissionsList.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            // Location + microphone required for SOS tracking and ambient audio evidence
            permissionLauncher.launch(permissionsList.toTypedArray())
        }
    }

    if (currentUser == null) {
        Box(
            Modifier.fillMaxSize().background(NavySurface),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text("Preparing AJIYA…", color = Color.White, fontWeight = FontWeight.Bold)
        }
    } else if (currentUser?.onboardingCompleted != true) {
        OnboardingScreen(viewModel = viewModel)
    } else if (isDecoyMode) {
        DecoyScreen(viewModel = viewModel)
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("main_scaffold"),
            bottomBar = {
                BottomNavBar(
                    currentDestination = currentDestination,
                    onNavigate = { dest -> viewModel.navigateTo(dest) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFF090F1D))
            ) {
                when (currentDestination) {
                    AppNavDestination.HOME -> HomeScreen(viewModel = viewModel)
                    AppNavDestination.CIRCLE -> MyCircleScreen(viewModel = viewModel)
                    AppNavDestination.HELPER -> BeHelperScreen(viewModel = viewModel)
                    AppNavDestination.HISTORY -> HistoryScreen(viewModel = viewModel)
                    AppNavDestination.SECURITY_LEGAL -> SecurityLegalScreen(viewModel = viewModel)
                    AppNavDestination.PREMIUM_PAYWALL -> SubscriptionPaywallScreen(viewModel = viewModel)
                }
            }
        }
    }

    if (showBackgroundLocationDisclosure) {
        ProminentLocationDisclosureDialog(
            onAccept = {
                showBackgroundLocationDisclosure = false
                configManager.setBackgroundLocationConsent(true)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    bgLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            },
            onDecline = {
                showBackgroundLocationDisclosure = false
                configManager.setBackgroundLocationConsent(false)
            }
        )
    }
}
