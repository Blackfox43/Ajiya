package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary = BeaconCyan,
    onPrimary = Color.Black,
    secondary = SafeGreen,
    onSecondary = Color.Black,
    tertiary = AmberAlert,
    onTertiary = Color.Black,
    background = NavyBackground,
    onBackground = TextPrimary,
    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = NavyCardBorder,
    error = CrimsonPrimary
)

@Composable
fun AjiyaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Dark,
        typography = Typography,
        content = content
    )
}
