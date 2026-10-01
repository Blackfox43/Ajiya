package com.example.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val Dark = darkColorScheme(
 primary=CrimsonPrimary,onPrimary=Color.White,primaryContainer=CrimsonDark,onPrimaryContainer=Color.White,
 secondary=BeaconCyan,onSecondary=Color.Black,secondaryContainer=BeaconCyanDark,onSecondaryContainer=Color.White,
 tertiary=AlertAmber,onTertiary=Color.Black,background=NavyBackground,onBackground=TextPrimary,
 surface=NavySurface,onSurface=TextPrimary,surfaceVariant=NavySurfaceVariant,onSurfaceVariant=TextSecondary,
 outline=NavyCardBorder,error=CrimsonPrimary)
@Composable fun AjiyaTheme(content:@Composable()->Unit)=MaterialTheme(colorScheme=Dark, typography=Typography, content=content)
