package com.bisu.chickcare.frontend.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val viewModel: DashboardViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    var isCapturing by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(true) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var isBackCamera by remember { mutableStateOf(true) }
    
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val imageCapture = remember { 
        ImageCapture.Builder()
            .setFlashMode(flashMode)
            .build() 
    }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> 
            hasCameraPermission = isGranted
            if (!isGranted) {
                showPermissionDialog = true
            }
        }
    )

    LaunchedEffect(flashMode) {
        imageCapture.flashMode = flashMode
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }


    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Permission Denied Dialog
    if (showPermissionDialog && !hasCameraPermission) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { 
                Text(
                    "Camera Permission Required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ) 
            },
            text = { 
                Text(
                    "ChickCare needs camera permission to capture photos of your chickens for health detection. Please grant permission in settings.",
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        )
                        showPermissionDialog = false
                    }
                ) {
                    Text("Go to Settings", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPermissionDialog = false
                    navController.popBackStack()
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Capture Image",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = Color.Black
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { innerPadding ->
        // Show loading overlay when detecting
        if (uiState.isDetecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF8B4513))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Analyzing chicken health...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        
        if (hasCameraPermission) {
            val previewView = remember { PreviewView(context) }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFE3B386)),
                contentAlignment = Alignment.TopCenter
            ) {
                // Camera preview container with 3:4 aspect ratio
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        { previewView },
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(12.dp),
                                spotColor = Color.Black.copy(alpha = 0.2f)
                            )
                            .background(Color.Black, RoundedCornerShape(12.dp))
                    )
                    
                    // White border - using Box with border to ensure visibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                width = 6.dp,
                                color = Color.White,
                                shape = RoundedCornerShape(12.dp)
                            )
                    )
                    
                    // Grid Overlay - positioned relative to preview
                    if (showGrid && !isCapturing) {
                        GridOverlay(modifier = Modifier.fillMaxSize())
                    }
                    
                    // Top Controls - positioned relative to preview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Grid Toggle
                        IconButton(
                            onClick = { showGrid = !showGrid },
                            modifier = Modifier
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    CircleShape
                                )
                                .size(48.dp)
                        ) {
                            Canvas(modifier = Modifier.size(24.dp)) {
                                val strokeWidth = 2.dp.toPx()
                                val thirdWidth = size.width / 3
                                val thirdHeight = size.height / 3
                                
                                // Vertical lines
                                drawLine(
                                    Color.White.copy(alpha = 0.7f),
                                    androidx.compose.ui.geometry.Offset(thirdWidth, 0f),
                                    androidx.compose.ui.geometry.Offset(thirdWidth, size.height),
                                    strokeWidth = strokeWidth
                                )
                                drawLine(
                                    Color.White.copy(alpha = 0.7f),
                                    androidx.compose.ui.geometry.Offset(thirdWidth * 2, 0f),
                                    androidx.compose.ui.geometry.Offset(thirdWidth * 2, size.height),
                                    strokeWidth = strokeWidth
                                )
                                
                                // Horizontal lines
                                drawLine(
                                    Color.White.copy(alpha = 0.7f),
                                    androidx.compose.ui.geometry.Offset(0f, thirdHeight),
                                    androidx.compose.ui.geometry.Offset(size.width, thirdHeight),
                                    strokeWidth = strokeWidth
                                )
                                drawLine(
                                    Color.White.copy(alpha = 0.7f),
                                    androidx.compose.ui.geometry.Offset(0f, thirdHeight * 2),
                                    androidx.compose.ui.geometry.Offset(size.width, thirdHeight * 2),
                                    strokeWidth = strokeWidth
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Flash Toggle
                        IconButton(
                            onClick = {
                                flashMode = when (flashMode) {
                                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                                    ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                                    else -> ImageCapture.FLASH_MODE_OFF
                                }
                            },
                            modifier = Modifier
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    CircleShape
                                )
                                .size(48.dp)
                        ) {
                            Icon(
                                when (flashMode) {
                                    ImageCapture.FLASH_MODE_OFF -> Icons.Default.FlashOff
                                    ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                    else -> Icons.Default.FlashOn
                                },
                                contentDescription = "Flash",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.size(8.dp))

                        // Camera Flip
                        IconButton(
                            onClick = { isBackCamera = !isBackCamera },
                            modifier = Modifier
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    CircleShape
                                )
                                .size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Flip Camera",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // Center Focus Indicator
                    if (!isCapturing) {
                        FocusIndicator(modifier = Modifier.fillMaxSize())
                    }
                }

                // Capture Button - positioned outside preview, floating
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                ) {
                    val buttonScale by animateFloatAsState(
                        targetValue = if (isCapturing) 0.9f else 1f,
                        animationSpec = tween(200),
                        label = "buttonScale"
                    )
                        if (isCapturing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(80.dp),
                                color = Color.White,
                                strokeWidth = 4.dp
                            )
                        } else {
                            Button(
                                onClick = {
                                    val photoFile = File(
                                        context.externalCacheDir,
                                        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
                                    )
                                    isCapturing = true
                                    imageCapture.takePicture(
                                        ImageCapture.OutputFileOptions.Builder(photoFile).build(),
                                        cameraExecutor,
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                                val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                                                scope.launch(Dispatchers.Main) {
                                                    isCapturing = false
                                                    // Navigate back directly with captured image
                                                    navController.previousBackStackEntry?.savedStateHandle?.set("capturedImageUri", savedUri.toString())
                                                    navController.popBackStack()
                                                }
                                            }
                                            override fun onError(exc: ImageCaptureException) {
                                                scope.launch(Dispatchers.Main) {
                                                    isCapturing = false
                                                }
                                                exc.printStackTrace()
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .size(80.dp)
                                    .scale(buttonScale)
                                    .shadow(12.dp, CircleShape),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White
                                ),
                                enabled = !isCapturing
                            ) {
                                Icon(
                                    Icons.Default.Camera,
                                    contentDescription = "Capture",
                                    tint = Color(0xFF8B4513),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                }

                LaunchedEffect(cameraProviderFuture, isBackCamera) {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val cameraSelector = if (isBackCamera) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    } else {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    }
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Camera,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color(0xFF8B4513).copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Camera Permission Required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B4513)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Please grant camera permission to capture photos",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B4513)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Grant Permission",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun GridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.alpha(0.5f)) {
        val strokeWidth = 1.dp.toPx()
        val thirdWidth = size.width / 3
        val thirdHeight = size.height / 3
        
        // Vertical lines
        drawLine(
            Color.White,
            androidx.compose.ui.geometry.Offset(thirdWidth, 0f),
            androidx.compose.ui.geometry.Offset(thirdWidth, size.height),
            strokeWidth = strokeWidth
        )
        drawLine(
            Color.White,
            androidx.compose.ui.geometry.Offset(thirdWidth * 2, 0f),
            androidx.compose.ui.geometry.Offset(thirdWidth * 2, size.height),
            strokeWidth = strokeWidth
        )
        
        // Horizontal lines
        drawLine(
            Color.White,
            androidx.compose.ui.geometry.Offset(0f, thirdHeight),
            androidx.compose.ui.geometry.Offset(size.width, thirdHeight),
            strokeWidth = strokeWidth
        )
        drawLine(
            Color.White,
            androidx.compose.ui.geometry.Offset(0f, thirdHeight * 2),
            androidx.compose.ui.geometry.Offset(size.width, thirdHeight * 2),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
fun FocusIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .alpha(0f)
        )
    }
}
