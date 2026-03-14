package com.bisu.chickcare.frontend.screen

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import com.bisu.chickcare.frontend.utils.persistUriToAppStorage
import kotlinx.coroutines.delay

/**
 * Processing screen shown while fusion model analyzes both inputs
 */
 
@Composable
fun ProcessingScreen(
    navController: NavController,
    imageUri: String? = null,
    audioUri: String? = null,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var detectionTriggered by remember { mutableStateOf(false) }
    var timeoutReached by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val decodedImageUri = remember(imageUri) {
        imageUri?.takeIf { it.isNotBlank() }?.let { Uri.decode(it) }
    }
    val decodedAudioUri = remember(audioUri) {
        audioUri?.takeIf { it.isNotBlank() }?.let { Uri.decode(it) }
    }
    var processedImageUri by remember { mutableStateOf(decodedImageUri) }
    var processedAudioUri by remember { mutableStateOf(decodedAudioUri) }
    var imageReady by remember { mutableStateOf(decodedImageUri.isNullOrEmpty()) }
    var audioReady by remember { mutableStateOf(decodedAudioUri.isNullOrEmpty()) }

    LaunchedEffect(decodedImageUri) {
        imageReady = false
        processedImageUri = decodedImageUri
        if (!decodedImageUri.isNullOrEmpty() && !decodedImageUri.startsWith("file://")) {
            processedImageUri = persistUriToAppStorage(
                context = context,
                sourceUriString = decodedImageUri,
                subdirectory = "detection_images",
                fallbackExtension = "jpg",
                logTag = "ProcessingScreen"
            ) ?: decodedImageUri
        }
        imageReady = true
    }

    LaunchedEffect(decodedAudioUri) {
        audioReady = false
        processedAudioUri = decodedAudioUri
        if (!decodedAudioUri.isNullOrEmpty() && !decodedAudioUri.startsWith("file://")) {
            processedAudioUri = persistUriToAppStorage(
                context = context,
                sourceUriString = decodedAudioUri,
                subdirectory = "detection_audio",
                fallbackExtension = "m4a",
                logTag = "ProcessingScreen"
            ) ?: decodedAudioUri
        }
        audioReady = true
    }

    // Trigger detection when screen loads
    LaunchedEffect(imageReady, audioReady, processedImageUri, processedAudioUri) {
        if (!detectionTriggered && imageReady && audioReady && !processedAudioUri.isNullOrEmpty()) {
            Log.d(
                "ProcessingScreen",
                "Triggering detection - Image: ${processedImageUri ?: "none"}, Audio: $processedAudioUri"
            )
            detectionTriggered = true
            viewModel.onScanNowClicked(processedImageUri, processedAudioUri)
        } else {
            Log.w(
                "ProcessingScreen",
                "Cannot trigger detection - Image: ${processedImageUri ?: "none"}, Audio: ${processedAudioUri ?: "none"} (ready: image=$imageReady, audio=$audioReady)"
            )
            if (audioReady && processedAudioUri.isNullOrEmpty()) {
                Log.e(
                    "ProcessingScreen",
                    "Missing audio input! Image: ${!processedImageUri.isNullOrEmpty()}, Audio: false"
                )
                // Navigate back if audio is missing (required)
                navController.popBackStack()
            }
        }
    }

    // Timeout after 150 seconds (increased to give model more time)
    LaunchedEffect(detectionTriggered) {
        if (detectionTriggered && !timeoutReached) {
            delay(150000) // 150 seconds timeout (increased from 90)
            
            // Check current state after delay
            val currentState = viewModel.uiState.value
            val currentIsDetecting = currentState.isDetecting
            val currentResult = currentState.detectionResult
            
            // Only timeout if:
            // 1. Detection has stopped (isDetecting = false)
            // 2. No result exists
            // 3. Timeout hasn't been reached yet
            if (currentResult == null && !currentIsDetecting && !timeoutReached) {
                Log.e("ProcessingScreen", "Detection timeout after 150 seconds! Model may be stuck or failed silently.")
                Log.e("ProcessingScreen", "UI State - isDetecting: ${false}, result: $currentResult")
                timeoutReached = true
                // Set an error result if detection never completed
                viewModel.clearDetectionResult()
                // Navigate to result screen with timeout error
                val encodedAudio = processedAudioUri?.let { Uri.encode(it) } ?: ""
                val baseRoute = StringBuilder("detection_result?status=${Uri.encode("Error: Processing timeout. Please try again with valid chicken image and audio.")}")
                baseRoute.append("&suggestions=")
                baseRoute.append("&audioUri=$encodedAudio")
                processedImageUri?.let {
                    baseRoute.append("&imageUri=").append(Uri.encode(it))
                }
                navController.navigate(baseRoute.toString()) {
                    popUpTo("processing") { inclusive = true }
                }
            } else if (currentResult != null) {
                Log.d("ProcessingScreen", "Detection completed before timeout - result: $currentResult")
                // Result exists, cancel timeout - let the result navigation handler take over
                timeoutReached = true
            } else if (currentIsDetecting) {
                Log.d("ProcessingScreen", "Detection still in progress after 150s (isDetecting=true), waiting for completion...")
                // Still detecting, don't timeout - the detection is actively running
                // Wait a bit more and check again
                delay(30000) // Wait another 30 seconds
                // Check one more time
                val followUpState = viewModel.uiState.value
                val followUpResult = followUpState.detectionResult
                val followUpDetecting = followUpState.isDetecting
                if (followUpResult == null && !followUpDetecting && !timeoutReached) {
                    Log.e("ProcessingScreen", "Detection stopped without result after 180 seconds total")
                    timeoutReached = true
                    val encodedAudio = processedAudioUri?.let { Uri.encode(it) } ?: ""
                    val route = StringBuilder("detection_result?status=${Uri.encode("Error: Processing timeout. Please try again with valid chicken image and audio.")}")
                    route.append("&suggestions=")
                    route.append("&audioUri=$encodedAudio")
                    processedImageUri?.let {
                        route.append("&imageUri=").append(Uri.encode(it))
                    }
                    navController.navigate(route.toString()) {
                        popUpTo("processing") { inclusive = true }
                    }
                }
            }
        }
    }

    // Navigate to result when detection completes
    // This LaunchedEffect should have priority over timeout
    LaunchedEffect(uiState.detectionResult, uiState.isDetecting, timeoutReached) {
        // Only navigate when detection is complete (not detecting anymore) and we have a result
        // Also prevent navigation if we already timed out (to avoid double navigation)
        if (!uiState.isDetecting && uiState.detectionResult != null && !timeoutReached) {
            val (_, status) = uiState.detectionResult!!
            Log.d("ProcessingScreen", "Detection completed! Status: $status")
            
            // Mark timeout as reached to prevent timeout navigation
            timeoutReached = true
            
            // Check if it's an error status
            if (status.startsWith("Error:", ignoreCase = true)) {
                Log.w("ProcessingScreen", "Detection returned error: $status")
            }
            
            val suggestionsString = uiState.remedySuggestions.joinToString("|")
            val routeParams = mutableListOf(
                "status" to status,
                "suggestions" to suggestionsString,
                "audioUri" to (processedAudioUri ?: "")
            )
            processedImageUri?.let {
                if (it.isNotEmpty()) {
                    routeParams += "imageUri" to it
                }
            }
            val route = buildString {
                append("detection_result?")
                append(
                    routeParams.joinToString("&") { (key, value) ->
                        "$key=${Uri.encode(value)}"
                    }
                )
            }

            navController.navigate(route) {
                popUpTo("processing") { inclusive = true }
            }
            viewModel.clearDetectionResult()
        } else if (uiState.isDetecting) {
            Log.d("ProcessingScreen", "Still detecting... isDetecting: ${true}, result: ${uiState.detectionResult}")
        } else if (timeoutReached && uiState.detectionResult != null) {
            Log.d("ProcessingScreen", "Timeout already reached, but result exists - will let timeout handler navigate")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ThemeColorUtils.beige(Color(0xFFF1E0C9)),
                        ThemeColorUtils.beige(Color(0xFFF1E0C9))
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            val progressIndicatorColor = if (ThemeViewModel.isDarkMode) {
                // Dark mode - use white/light color
                Color.White
            } else {
                ThemeColorUtils.beige(Color(0xFFE3B386))
            }

            val indicatorBackgroundColors = if (ThemeViewModel.isDarkMode) {
                listOf(
                    Color(0xFF2C2C2C).copy(alpha = 0.3f),
                    Color(0xFF1E1E1E).copy(alpha = 0.1f)
                )
            } else {
                listOf(
                    ThemeColorUtils.beige(Color(0xFFE3B386)).copy(alpha = 0.3f),
                    Color(0xFFD4A574).copy(alpha = 0.1f)
                )
            }

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        Brush.radialGradient(colors = indicatorBackgroundColors),
                        RoundedCornerShape(60.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(80.dp),
                    color = progressIndicatorColor,
                    strokeWidth = 4.dp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Processing...",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (ThemeViewModel.isDarkMode) Color.White else Color(0xFF1E293B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Please wait",
                fontSize = 14.sp,
                color = if (ThemeViewModel.isDarkMode) Color.White else ThemeColorUtils.black(),
                textAlign = TextAlign.Center
            )
        }
    }
}

