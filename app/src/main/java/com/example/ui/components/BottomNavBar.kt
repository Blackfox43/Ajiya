package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BeaconCyan
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.NavyBackground
import com.example.ui.theme.NavySurface
import com.example.ui.viewmodel.AppNavDestination

@Composable
fun BottomNavBar(
    currentDestination: AppNavDestination,
    onNavigate: (AppNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("bottom_nav_bar"),
        containerColor = NavySurface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentDestination == AppNavDestination.HOME,
            onClick = { onNavigate(AppNavDestination.HOME) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Home SOS Screen"
                )
            },
            label = { Text("SOS", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CrimsonPrimary,
                selectedTextColor = CrimsonPrimary,
                indicatorColor = CrimsonPrimary.copy(alpha = 0.18f),
                unselectedIconColor = Color.LightGray,
                unselectedTextColor = Color.LightGray
            ),
            modifier = Modifier.testTag("nav_item_home")
        )

        NavigationBarItem(
            selected = currentDestination == AppNavDestination.CIRCLE,
            onClick = { onNavigate(AppNavDestination.CIRCLE) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = "My Circle Screen"
                )
            },
            label = { Text("Circle", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BeaconCyan,
                selectedTextColor = BeaconCyan,
                indicatorColor = BeaconCyan.copy(alpha = 0.18f),
                unselectedIconColor = Color.LightGray,
                unselectedTextColor = Color.LightGray
            ),
            modifier = Modifier.testTag("nav_item_circle")
        )

        NavigationBarItem(
            selected = currentDestination == AppNavDestination.HELPER,
            onClick = { onNavigate(AppNavDestination.HELPER) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Handshake,
                    contentDescription = "Be a Helper Screen"
                )
            },
            label = { Text("Helper", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BeaconCyan,
                selectedTextColor = BeaconCyan,
                indicatorColor = BeaconCyan.copy(alpha = 0.18f),
                unselectedIconColor = Color.LightGray,
                unselectedTextColor = Color.LightGray
            ),
            modifier = Modifier.testTag("nav_item_helper")
        )

        NavigationBarItem(
            selected = currentDestination == AppNavDestination.HISTORY,
            onClick = { onNavigate(AppNavDestination.HISTORY) },
            icon = {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History Screen"
                )
            },
            label = { Text("History", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BeaconCyan,
                selectedTextColor = BeaconCyan,
                indicatorColor = BeaconCyan.copy(alpha = 0.18f),
                unselectedIconColor = Color.LightGray,
                unselectedTextColor = Color.LightGray
            ),
            modifier = Modifier.testTag("nav_item_history")
        )

        NavigationBarItem(
            selected = currentDestination == AppNavDestination.SECURITY_LEGAL,
            onClick = { onNavigate(AppNavDestination.SECURITY_LEGAL) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Security Screen"
                )
            },
            label = { Text("Security", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BeaconCyan,
                selectedTextColor = BeaconCyan,
                indicatorColor = BeaconCyan.copy(alpha = 0.18f),
                unselectedIconColor = Color.LightGray,
                unselectedTextColor = Color.LightGray
            ),
            modifier = Modifier.testTag("nav_item_security")
        )
    }
}
