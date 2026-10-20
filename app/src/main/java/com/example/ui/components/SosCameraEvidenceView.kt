package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.BeaconCyan
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceVariant
import com.example.ui.theme.SafeGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "SosCameraEvidenceView"

/**
 * CameraX-powered forensic evidence preview surface for active SOS distress events.
 * Enables victims to silently capture high-resolution timestamped photos or record
 * tamper-resistant emergency video clips with live GPS coordinate watermarks.
 */
@Composable
fun SosCameraEvidenceView(
    modifier: Modifier = Modifier,
    locationCoordinates: String = "Lat: 6.5244, Lng: 3.3792 (Lagos)",
    onEvidenceCaptured: (file: File, isVideo: Boolean) -> Unit = { _, _ -> },
    onClose: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
    }

    // Camera use cases & state
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<Camera?>(null) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val videoCapture = remember {
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.SD))
            .build()
        VideoCapture.withOutput(recorder)
    }

    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var isRecordingVideo by remember { mutableStateOf(false) }
    var recordingDurationSec by remember { mutableLongStateOf(0L) }
    var lastCapturedNotice by remember { mutableStateOf<String?>(null) }
    var photoCount by remember { mutableIntStateOf(0) }
    var videoCount by remember { mutableIntStateOf(0) }

    // Recording duration timer
    LaunchedEffect(isRecordingVideo) {
        if (isRecordingVideo) {
            recordingDurationSec = 0L
            while (isRecordingVideo) {
                delay(1000L)
                recordingDurationSec++
            }
        }
    }

    // Clean up active recording when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            activeRecording?.stop()
            activeRecording = null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, CrimsonPrimary.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .testTag("sos_camera_evidence_card"),
        colors = CardDefaults.cardColors(containerColor = NavySurface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF26050A))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isRecordingVideo) Color.Red else CrimsonPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRecordingVideo) "RECORDING SOS EVIDENCE" else "CRISIS EVIDENCE CAMERA",
                        color = if (isRecordingVideo) Color.Red else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (photoCount > 0 || videoCount > 0) {
                        Surface(
                            color = SafeGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "${photoCount}P / ${videoCount}V",
                                color = SafeGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    onClose?.let { closeAction ->
                        IconButton(
                            onClick = closeAction,
                            modifier = Modifier.size(28.dp).testTag("close_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Camera",
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (!hasCameraPermission) {
                // Permission Request Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Permission Required",
                        tint = AlertAmber,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Camera Permission Required",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "To preserve critical video or photographic evidence of assailants and surroundings during an active SOS, AJIYA needs camera access.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        modifier = Modifier.testTag("grant_camera_permission_button")
                    ) {
                        Text("Grant Camera & Audio Access", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Camera Viewfinder & Overlays
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .background(Color.Black)
                ) {
                    // CameraX PreviewView hosted inside Compose AndroidView
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            }
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val cameraSelector = CameraSelector.Builder()
                                        .requireLensFacing(lensFacing)
                                        .build()

                                    cameraProvider.unbindAll()
                                    val camera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture,
                                        videoCapture
                                    )
                                    cameraControl = camera
                                } catch (exc: Exception) {
                                    Log.e(TAG, "Failed to bind CameraX use cases", exc)
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        update = { previewView ->
                            // When lensFacing changes, rebind camera provider
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val cameraSelector = CameraSelector.Builder()
                                        .requireLensFacing(lensFacing)
                                        .build()

                                    cameraProvider.unbindAll()
                                    val camera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture,
                                        videoCapture
                                    )
                                    cameraControl = camera
                                    camera.cameraControl.enableTorch(isFlashOn)
                                } catch (exc: Exception) {
                                    Log.e(TAG, "Failed to rebind camera on update", exc)
                                }
                            }, ContextCompat.getMainExecutor(previewView.context))
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Tactical Viewfinder Reticle Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    )

                    // Live Evidence Watermark (Timestamp + GPS Coordinates)
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        Text(
                            text = "EVIDENCE WATERMARK",
                            color = BeaconCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = timestamp,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                        Text(
                            text = locationCoordinates,
                            color = AlertAmber,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    }

                    // Recording indicator overlay
                    if (isRecordingVideo) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Red.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = "Recording",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val minutes = recordingDurationSec / 60
                            val seconds = recordingDurationSec % 60
                            Text(
                                text = String.format(Locale.getDefault(), "REC %02d:%02d", minutes, seconds),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Flash / Lens Flip controls in upper right
                    Row(
                        modifier = Modifier
                            .align(if (isRecordingVideo) Alignment.BottomEnd else Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Flash / Torch toggle
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    isFlashOn = !isFlashOn
                                    cameraControl?.cameraControl?.enableTorch(isFlashOn)
                                },
                                modifier = Modifier.testTag("camera_torch_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Torch Toggle",
                                    tint = if (isFlashOn) AlertAmber else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Switch Camera (Front/Back)
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                        CameraSelector.LENS_FACING_FRONT
                                    } else {
                                        CameraSelector.LENS_FACING_BACK
                                    }
                                },
                                modifier = Modifier.testTag("camera_flip_lens")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlipCameraAndroid,
                                    contentDescription = "Switch Camera",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Shutter & Recording Controls
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NavySurfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Action buttons row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Capture Photo Button
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isRecordingVideo) Color.Gray else Color.White,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .clickable(enabled = !isRecordingVideo) {
                                            takePhoto(
                                                context = context,
                                                imageCapture = imageCapture,
                                                onSuccess = { file ->
                                                    photoCount++
                                                    lastCapturedNotice = "Photo captured: ${file.name}"
                                                    onEvidenceCaptured(file, false)
                                                },
                                                onError = { exc ->
                                                    lastCapturedNotice = "Capture failed: ${exc.message}"
                                                }
                                            )
                                        }
                                        .testTag("take_photo_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = "Capture Photo",
                                            tint = Color.Black,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Capture Photo",
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Record / Stop Video Button
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isRecordingVideo) Color.Red else CrimsonPrimary,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            if (isRecordingVideo) {
                                                // Stop active recording
                                                activeRecording?.stop()
                                                activeRecording = null
                                                isRecordingVideo = false
                                            } else {
                                                // Start new video recording
                                                val videoFile = File(
                                                    context.filesDir,
                                                    "sos_evidence_video_${System.currentTimeMillis()}.mp4"
                                                )
                                                val outputOptions = FileOutputOptions.Builder(videoFile).build()

                                                val hasAudioPermission = ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.RECORD_AUDIO
                                                ) == PackageManager.PERMISSION_GRANTED

                                                val pendingRecording = videoCapture.output
                                                    .prepareRecording(context, outputOptions)
                                                    .let { if (hasAudioPermission) it.withAudioEnabled() else it }

                                                activeRecording = pendingRecording.start(
                                                    ContextCompat.getMainExecutor(context)
                                                ) { recordEvent ->
                                                    when (recordEvent) {
                                                        is VideoRecordEvent.Start -> {
                                                            isRecordingVideo = true
                                                            lastCapturedNotice = "SOS Video recording started..."
                                                        }
                                                        is VideoRecordEvent.Finalize -> {
                                                            isRecordingVideo = false
                                                            if (!recordEvent.hasError()) {
                                                                videoCount++
                                                                lastCapturedNotice = "Video evidence saved: ${videoFile.name}"
                                                                onEvidenceCaptured(videoFile, true)
                                                            } else {
                                                                lastCapturedNotice = "Video error: ${recordEvent.error}"
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .testTag("toggle_video_recording_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isRecordingVideo) Icons.Default.Stop else Icons.Default.Videocam,
                                            contentDescription = if (isRecordingVideo) "Stop Video" else "Record Video",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isRecordingVideo) "Stop Recording" else "Record Video",
                                    fontSize = 11.sp,
                                    color = if (isRecordingVideo) Color.Red else Color.LightGray,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Last Captured Notice Bar
                        lastCapturedNotice?.let { notice ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = SafeGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = notice,
                                        fontSize = 10.sp,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "✕",
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    modifier = Modifier
                                        .clickable { lastCapturedNotice = null }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Captures a timestamped forensic still photo using CameraX ImageCapture use case.
 */
private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onSuccess: (File) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val photoFile = File(
        context.filesDir,
        "sos_evidence_photo_${System.currentTimeMillis()}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                Log.d(TAG, "SOS Evidence photo successfully saved to: ${photoFile.absolutePath}")
                onSuccess(photoFile)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e(TAG, "SOS Evidence photo capture failed", exception)
                onError(exception)
            }
        }
    )
}
