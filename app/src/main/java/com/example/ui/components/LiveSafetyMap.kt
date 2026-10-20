package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HelperAlertEntity
import com.example.data.model.LocationPingEntity
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.BeaconCyan
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.NavyCardBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.SafeGreen

@Composable
fun LiveSafetyMap(
    lat: Double,
    lng: Double,
    pings: List<LocationPingEntity> = emptyList(),
    isTrackingActive: Boolean = false,
    modifier: Modifier = Modifier,
    helpersCountNearby: Int = 2,
    helperAlerts: List<HelperAlertEntity> = emptyList(),
    selectedAlertId: String? = null,
    onAlertSelected: ((HelperAlertEntity) -> Unit)? = null,
    onLocationClick: ((lat: Double, lng: Double, label: String) -> Unit)? = null,
    isHelperView: Boolean = false
) {
    var activeAlertSelection by remember(selectedAlertId, helperAlerts) {
        mutableStateOf(helperAlerts.find { it.id == selectedAlertId } ?: helperAlerts.firstOrNull())
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadiusFactor by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = modifier
            .testTag("live_safety_map_container")
            .border(1.dp, if (isHelperView && helperAlerts.isNotEmpty()) CrimsonPrimary.copy(alpha = 0.6f) else NavyCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF070D1B)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = minOf(size.width, size.height) * 0.44f

                // Radar Grid Lines
                val gridColor = Color(0xFF162544)
                val stepCount = 5
                val stepX = size.width / stepCount
                for (i in 1 until stepCount) {
                    drawLine(
                        color = gridColor,
                        start = Offset(stepX * i, 0f),
                        end = Offset(stepX * i, size.height),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }
                val stepY = size.height / stepCount
                for (i in 1 until stepCount) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, stepY * i),
                        end = Offset(size.width, stepY * i),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                // Range Circles (500m, 1km, 2km Safe Circle)
                val circleColor = Color(0xFF1E3A6E)
                drawCircle(
                    color = circleColor,
                    radius = maxRadius * 0.33f,
                    center = center,
                    style = Stroke(width = 1f)
                )
                drawCircle(
                    color = circleColor,
                    radius = maxRadius * 0.66f,
                    center = center,
                    style = Stroke(width = 1f)
                )
                drawCircle(
                    color = if (isTrackingActive || (isHelperView && helperAlerts.isNotEmpty())) {
                        CrimsonPrimary.copy(alpha = 0.4f)
                    } else {
                        BeaconCyan.copy(alpha = 0.4f)
                    },
                    radius = maxRadius,
                    center = center,
                    style = Stroke(
                        width = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                )

                // Draw Breadcrumb Path if tracking pings exist
                if (pings.size > 1) {
                    val path = Path()
                    pings.forEachIndexed { index, ping ->
                        val relIndex = index - (pings.size / 2)
                        val px = center.x + (relIndex * 22f)
                        val py = center.y + ((index % 3 - 1) * 16f)
                        if (index == 0) {
                            path.moveTo(px, py)
                        } else {
                            path.lineTo(px, py)
                        }
                    }
                    drawPath(
                        path = path,
                        color = if (isTrackingActive) CrimsonPrimary else BeaconCyan,
                        style = Stroke(width = 3f)
                    )
                }

                // If in Helper view or if helperAlerts exist, plot distress locations of people needing help
                if (helperAlerts.isNotEmpty()) {
                    helperAlerts.forEach { alert ->
                        val dLatKm = (alert.lat - lat) * 111.0
                        val latRad = Math.toRadians(lat)
                        val dLngKm = (alert.lng - lng) * 111.0 * kotlin.math.cos(latRad)
                        val distKm = kotlin.math.sqrt(dLatKm * dLatKm + dLngKm * dLngKm)

                        // Scale so 2.0 km maps to maxRadius
                        val scale = maxRadius / 2.0
                        val relX = if (distKm > 0.05) {
                            (dLngKm * scale).coerceIn(-maxRadius * 0.85, maxRadius * 0.85).toFloat()
                        } else {
                            (maxRadius * 0.4f)
                        }
                        val relY = if (distKm > 0.05) {
                            (-dLatKm * scale).coerceIn(-maxRadius * 0.85, maxRadius * 0.85).toFloat()
                        } else {
                            (-maxRadius * 0.35f)
                        }

                        val alertPos = Offset(center.x + relX, center.y + relY)
                        val isSelected = activeAlertSelection?.id == alert.id

                        // Draw pulsing distress beacon ring
                        drawCircle(
                            color = CrimsonPrimary.copy(alpha = pulseAlpha * (if (isSelected) 0.9f else 0.5f)),
                            radius = (if (isSelected) 34f else 22f) * pulseRadiusFactor + 12f,
                            center = alertPos
                        )

                        // Vector dashed line from helper center to victim needing help
                        drawLine(
                            color = if (isSelected) CrimsonPrimary else AlertAmber.copy(alpha = 0.6f),
                            start = center,
                            end = alertPos,
                            strokeWidth = if (isSelected) 2.5f else 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )

                        // Outer beacon ring
                        drawCircle(
                            color = if (isSelected) CrimsonPrimary else AlertAmber,
                            radius = if (isSelected) 14f else 10f,
                            center = alertPos
                        )

                        // Inner white core
                        drawCircle(
                            color = Color.White,
                            radius = if (isSelected) 5f else 3.5f,
                            center = alertPos
                        )
                    }
                } else if (helpersCountNearby > 0 && !isHelperView) {
                    // Simulated Helper Nodes within Safe Circle when victim is tracking
                    val helper1 = Offset(center.x - maxRadius * 0.45f, center.y - maxRadius * 0.35f)
                    val helper2 = Offset(center.x + maxRadius * 0.55f, center.y + maxRadius * 0.25f)

                    drawCircle(color = SafeGreen.copy(alpha = 0.25f), radius = 18f, center = helper1)
                    drawCircle(color = SafeGreen, radius = 6f, center = helper1)

                    drawCircle(color = AlertAmber.copy(alpha = 0.25f), radius = 18f, center = helper2)
                    drawCircle(color = AlertAmber, radius = 6f, center = helper2)
                }

                // Pulsing Center Waves (Active SOS Beacon on user center)
                if (isTrackingActive) {
                    drawCircle(
                        color = CrimsonPrimary.copy(alpha = pulseAlpha),
                        radius = maxRadius * pulseRadiusFactor,
                        center = center
                    )
                }

                // User Pin Center
                val pinColor = when {
                    isTrackingActive -> CrimsonPrimary
                    isHelperView -> SafeGreen
                    else -> BeaconCyan
                }
                drawCircle(
                    color = pinColor.copy(alpha = 0.3f),
                    radius = 24f,
                    center = center
                )
                drawCircle(
                    color = pinColor,
                    radius = 9f,
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = center
                )
            }

            // Top Status Pill
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .background(NavySurface.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
                    .border(1.dp, NavyCardBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            when {
                                isTrackingActive || helperAlerts.isNotEmpty() -> CrimsonPrimary
                                isHelperView -> SafeGreen
                                else -> BeaconCyan
                            },
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        isTrackingActive -> "LIVE SOS GPS ACTIVE"
                        isHelperView && helperAlerts.isNotEmpty() -> "${helperAlerts.size} DISTRESS BEACON(S)"
                        isHelperView -> "HELPER RADAR ACTIVE"
                        else -> "GPS STANDBY (2KM SAFE ZONE)"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isTrackingActive || helperAlerts.isNotEmpty() -> CrimsonPrimary
                        isHelperView -> SafeGreen
                        else -> BeaconCyan
                    },
                    fontFamily = FontFamily.Monospace
                )
            }

            // Top End Action: Direct Google Maps Quick Launcher
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E3A6E).copy(alpha = 0.92f))
                    .border(1.dp, BeaconCyan.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .clickable {
                        val targetAlert = activeAlertSelection
                        if (targetAlert != null && onLocationClick != null) {
                            onLocationClick(
                                targetAlert.lat,
                                targetAlert.lng,
                                "Distress Victim: ${targetAlert.crisisType} (${targetAlert.distanceMeters}m)"
                            )
                        } else if (onLocationClick != null) {
                            onLocationClick(
                                lat,
                                lng,
                                if (isTrackingActive) "Active SOS Incident" else "My Live Location"
                            )
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("map_open_google_maps_badge"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription = "Open in Google Maps",
                    tint = BeaconCyan,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Google Maps",
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    tint = BeaconCyan,
                    modifier = Modifier.size(11.dp)
                )
            }

            // Interactive Distress Alert Notification Pill overlay (if alerts exist)
            if (helperAlerts.isNotEmpty()) {
                val currentTarget = activeAlertSelection ?: helperAlerts.first()
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xE62A080E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary),
                        modifier = Modifier
                            .testTag("live_distress_alert_banner")
                            .clickable {
                                onLocationClick?.invoke(
                                    currentTarget.lat,
                                    currentTarget.lng,
                                    "Victim Needs Help: ${currentTarget.crisisType} (${currentTarget.distanceMeters}m away)"
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🚨 ${currentTarget.crisisType} (${currentTarget.distanceMeters}m away)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TAP TO LOCATE ➔",
                                color = AlertAmber,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Alert switcher chips if multiple alerts exist
                    if (helperAlerts.size > 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            helperAlerts.forEach { alert ->
                                val isSelected = activeAlertSelection?.id == alert.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) CrimsonPrimary else NavySurface.copy(alpha = 0.8f)
                                        )
                                        .clickable {
                                            activeAlertSelection = alert
                                            onAlertSelected?.invoke(alert)
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${alert.distanceMeters}m",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Telemetry Bar with Direct Click to Open Google Maps
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xE6060B17))
                    .clickable {
                        val targetAlert = activeAlertSelection
                        if (targetAlert != null && onLocationClick != null) {
                            onLocationClick(
                                targetAlert.lat,
                                targetAlert.lng,
                                "Distress Target: ${targetAlert.crisisType}"
                            )
                        } else if (onLocationClick != null) {
                            onLocationClick(
                                lat,
                                lng,
                                if (isTrackingActive) "Active SOS Coordinates" else "Current Coordinates"
                            )
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("map_telemetry_bar"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "GPS Coordinates",
                        tint = if (isTrackingActive || helperAlerts.isNotEmpty()) CrimsonPrimary else BeaconCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "%.4f° N, %.4f° E".format(
                            activeAlertSelection?.lat ?: lat,
                            activeAlertSelection?.lng ?: lng
                        ),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Open in Google Maps",
                        tint = SafeGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tap to open map",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeGreen
                    )
                }
            }
        }
    }
}
