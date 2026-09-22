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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    pings: List<LocationPingEntity>,
    isTrackingActive: Boolean,
    modifier: Modifier = Modifier,
    helpersCountNearby: Int = 2
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadiusFactor by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
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
            .border(1.dp, NavyCardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF070D1B)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = minOf(size.width, size.height) * 0.46f

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
                    color = if (isTrackingActive) CrimsonPrimary.copy(alpha = 0.4f) else BeaconCyan.copy(alpha = 0.4f),
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
                        // Map offset relative to center
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

                // Simulated Helper Nodes within Safe Circle
                if (helpersCountNearby > 0) {
                    val helper1 = Offset(center.x - maxRadius * 0.45f, center.y - maxRadius * 0.35f)
                    val helper2 = Offset(center.x + maxRadius * 0.55f, center.y + maxRadius * 0.25f)

                    drawCircle(color = SafeGreen.copy(alpha = 0.25f), radius = 18f, center = helper1)
                    drawCircle(color = SafeGreen, radius = 6f, center = helper1)

                    drawCircle(color = AlertAmber.copy(alpha = 0.25f), radius = 18f, center = helper2)
                    drawCircle(color = AlertAmber, radius = 6f, center = helper2)
                }

                // Pulsing Center Waves (Active SOS Beacon)
                if (isTrackingActive) {
                    drawCircle(
                        color = CrimsonPrimary.copy(alpha = pulseAlpha),
                        radius = maxRadius * pulseRadiusFactor,
                        center = center
                    )
                }

                // User Pin Center
                val pinColor = if (isTrackingActive) CrimsonPrimary else BeaconCyan
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
                    .padding(12.dp)
                    .background(NavySurface.copy(alpha = 0.88f), RoundedCornerShape(20.dp))
                    .border(1.dp, NavyCardBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isTrackingActive) CrimsonPrimary else SafeGreen,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isTrackingActive) "LIVE SOS GPS ACTIVE (10s)" else "GPS STANDBY (OPT-IN)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isTrackingActive) CrimsonPrimary else SafeGreen,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Radius Info Chip (Top End)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(NavySurface.copy(alpha = 0.88f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = BeaconCyan,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "2km Safe Zone",
                    fontSize = 11.sp,
                    color = BeaconCyan,
                    fontWeight = FontWeight.Medium
                )
            }

            // Bottom Telemetry Bar
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xD9060B17))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "GPS Coordinates",
                        tint = Color.LightGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "%.4f° N, %.4f° E".format(lat, lng),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                Text(
                    text = "${pings.size} pings logged",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.LightGray
                )
            }
        }
    }
}
