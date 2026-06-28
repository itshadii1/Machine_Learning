package com.namaaztracker.presentation.screens.tracker

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.namaaztracker.domain.model.RakahState
import com.namaaztracker.presentation.components.PostureIndicator
import com.namaaztracker.presentation.components.ProgressDots
import com.namaaztracker.presentation.components.StepCard
import com.namaaztracker.util.CameraFrameAnalyzer
import com.namaaztracker.util.triggerHaptic
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrackerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Keep screen on for the entire prayer session
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> cameraPermissionGranted = granted }

    LaunchedEffect(Unit) {
        if (!cameraPermissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(Unit) {
        viewModel.hapticEvents.collect { type -> triggerHaptic(context, type) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.session.namaazType.displayName,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xBB000000),
                ),
            )
        },
        containerColor = Color.Black,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!cameraPermissionGranted) {
                PermissionRationale(onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) })
            } else {
                TrackerContent(
                    uiState = uiState,
                    onFrameCaptured = viewModel::onFrameCaptured,
                )
            }

            AnimatedVisibility(
                visible = uiState.isComplete,
                enter = fadeIn(tween(600)),
                exit = fadeOut(),
            ) {
                CompletionOverlay(onDismiss = onNavigateBack)
            }
        }
    }
}

@Composable
private fun TrackerContent(
    uiState: TrackerUiState,
    onFrameCaptured: (ByteArray) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // ── 1. Full-screen live camera ──────────────────────────────────────
        CameraBackground(onFrameCaptured = onFrameCaptured)

        // ── 2. Bottom gradient scrim (info panel backdrop) ──────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xF5000000)),
                    ),
                ),
        )

        // ── 3. Centre: real-time posture indicator ──────────────────────────
        if (uiState.session.currentState != RakahState.COMPLETE) {
            PostureIndicator(
                posture = uiState.currentPosture,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // ── 4. Top-left: LIVE pill ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xCCF44336))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(text = "● LIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // ── 5. Top-right: connection warning ───────────────────────────────
        if (!uiState.apiConnected) {
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(text = "Connecting…", color = Color(0xFFFFB300), fontSize = 11.sp)
            }
        }

        // ── 6. Bottom info panel ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ProgressDots(
                completed = uiState.session.completedRakat,
                total = uiState.totalRakat,
            )

            // Rakah counter — slide-animates on increment
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
            ) {
                AnimatedContent(
                    targetState = uiState.displayRakah,
                    transitionSpec = {
                        slideInVertically { it } togetherWith slideOutVertically { -it }
                    },
                    label = "rakah_num",
                ) { rakah ->
                    Text(
                        text = "$rakah",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                }
                Text(
                    text = " / ${uiState.totalRakat}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    color = Color(0xBBFFFFFF),
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }

            if (uiState.session.currentState != RakahState.COMPLETE) {
                StepCard(
                    emoji = uiState.stateEmoji,
                    currentStep = uiState.stateName,
                    nextInstruction = uiState.stateInstruction,
                )
            }
        }
    }
}

@Composable
private fun CameraBackground(onFrameCaptured: (ByteArray) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { cameraExecutor.shutdown() } }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }.also { previewView ->
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val cameraProvider = future.get()
                    val preview = CameraPreview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { a ->
                            a.setAnalyzer(cameraExecutor, CameraFrameAnalyzer(onFrameCaptured))
                        }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            imageAnalysis,
                        )
                    } catch (_: Exception) {
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis,
                            )
                        } catch (_: Exception) {}
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun PermissionRationale(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "📷", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Camera access is required to track your prayer postures.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color.White,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrant) { Text("Grant Permission") }
    }
}

@Composable
private fun CompletionOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE0D1B12)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "✅", fontSize = 64.sp)
            Text(
                text = "الحمد لله",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50),
            )
            Text(
                text = "Alhamdulillah",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF4CAF50),
            )
            Text(
                text = "Prayer Complete",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDismiss) { Text("Return Home") }
        }
    }
}
