package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.BeaconCyan
import com.example.ui.theme.CrimsonDark
import com.example.ui.theme.CrimsonLight
import com.example.ui.theme.CrimsonPrimary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SosEmergencyButton(
    crisisMode: String,
    isActive: Boolean,
    onTriggerSos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    var timerJob by remember { mutableStateOf<Job?>(null) }

    // Pulsing halo animation
    val infiniteTransition = rememberInfiniteTransition(label = "halo")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )

    val primaryColor = when (crisisMode) {
        "KIDNAP_SILENT" -> CrimsonPrimary
        "ACCIDENT_LOUD" -> Color(0xFFE63946)
        "DISASTER_CHECKIN" -> BeaconCyan
        else -> CrimsonPrimary
    }

    val modeTitle = when (crisisMode) {
        "KIDNAP_SILENT" -> "SILENT SOS"
        "ACCIDENT_LOUD" -> "LOUD ACCIDENT SOS"
        "DISASTER_CHECKIN" -> "DISASTER ALERT"
        else -> "EMERGENCY SOS"
    }

    val modeSubtext = when (crisisMode) {
        "KIDNAP_SILENT" -> "Covert background GPS + audio"
        "ACCIDENT_LOUD" -> "Loud siren & auto-call"
        "DISASTER_CHECKIN" -> "Quick broadcast to circle"
        else -> "Hold 3s to alert circle"
    }

    Column(
        modifier = modifier.testTag("sos_button_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            // Animated outer pulsing halo
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .scale(if (isHolding || isActive) haloScale else 1.0f)
                    .clip(CircleShape)
                    .background(
                        primaryColor.copy(alpha = if (isActive) 0.28f else if (isHolding) 0.22f else 0.12f)
                    )
            )

            // Hold 3-second Progress Ring Canvas
            Canvas(modifier = Modifier.size(210.dp)) {
                val strokeWidth = 8.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                val arcSize = Size(radius * 2f, radius * 2f)

                // Background track
                drawArc(
                    color = primaryColor.copy(alpha = 0.25f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Filled progress track during hold
                if (holdProgress > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(AlertAmber, CrimsonLight, CrimsonDark, AlertAmber)
                        ),
                        startAngle = -90f,
                        sweepAngle = holdProgress * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Main Interactive Circle
            Box(
                modifier = Modifier
                    .size(185.dp)
                    .clip(CircleShape)
                    .shadow(elevation = 16.dp, shape = CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (crisisMode == "KIDNAP_SILENT") {
                                listOf(Color(0xFF280B10), Color(0xFF140508))
                            } else {
                                listOf(primaryColor, Color(0xFF8B0000))
                            }
                        )
                    )
                    .pointerInput(crisisMode, isActive) {
                        detectTapGestures(
                            onPress = {
                                if (isActive) {
                                    // Already active, tap does not re-trigger
                                    return@detectTapGestures
                                }
                                isHolding = true
                                val startTime = System.currentTimeMillis()
                                val durationMs = 3000L // 3 seconds required

                                timerJob = coroutineScope.launch {
                                    while (isHolding) {
                                        val elapsed = System.currentTimeMillis() - startTime
                                        holdProgress = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                                        if (elapsed >= durationMs) {
                                            holdProgress = 1f
                                            isHolding = false
                                            onTriggerSos()
                                            break
                                        }
                                        delay(40)
                                    }
                                }

                                tryAwaitRelease()
                                isHolding = false
                                timerJob?.cancel()
                                holdProgress = 0f
                            }
                        )
                    }
                    .testTag("sos_interactive_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (crisisMode == "ACCIDENT_LOUD") Icons.Default.Warning else Icons.Default.Emergency,
                        contentDescription = "SOS Trigger Icon",
                        tint = if (isHolding) AlertAmber else Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isActive) "ACTIVE" else "SOS",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = when {
                            isActive -> "DISPATCHING"
                            isHolding -> "HOLD ${(3 - (holdProgress * 3).toInt()).coerceAtLeast(1)}s"
                            else -> "HOLD 3 SEC"
                        },
                        color = if (isHolding) AlertAmber else Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = modeTitle,
            color = primaryColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Text(
            text = modeSubtext,
            color = Color.LightGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}
