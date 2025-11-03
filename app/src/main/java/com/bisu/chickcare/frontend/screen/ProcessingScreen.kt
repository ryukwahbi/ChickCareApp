package com.bisu.chickcare.frontend.screen

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel
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

    // Trigger detection when screen loads
    LaunchedEffect(imageUri, audioUri) {
        // At minimum, we need audio. Image can be optional (though fusion model works best with both)
        if (!detectionTriggered && !audioUri.isNullOrEmpty()) {
            Log.d("ProcessingScreen", "Triggering detection - Image: ${imageUri ?: "none"}, Audio: $audioUri")
            detectionTriggered = true
            viewModel.onScanNowClicked(imageUri ?: "", audioUri)
        } else {
            Log.w("ProcessingScreen", "Cannot trigger detection - Image: ${imageUri ?: "none"}, Audio: ${audioUri ?: "none"}")
            if (audioUri.isNullOrEmpty()) {
                Log.e("ProcessingScreen", "Missing audio input! Image: ${!imageUri.isNullOrEmpty()}, Audio: false")
                // Navigate back if audio is missing (required)
                navController.popBackStack()
            }
        }
    }

    // Timeout after 150 seconds (increased to give model more time)
    // Only trigger timeout if detection is NOT in progress AND no result exists
    LaunchedEffect(detectionTriggered) {
        if (detectionTriggered && !timeoutReached) {
            delay(150000) // 150 seconds timeout (increased from 90)
            
            // Check current state after delay
            val currentIsDetecting = uiState.isDetecting
            val currentResult = uiState.detectionResult
            
            // Only timeout if:
            // 1. Detection has stopped (isDetecting = false)
            // 2. No result exists
            // 3. Timeout hasn't been reached yet
            if (currentResult == null && !currentIsDetecting && !timeoutReached) {
                Log.e("ProcessingScreen", "Detection timeout after 150 seconds! Model may be stuck or failed silently.")
                Log.e("ProcessingScreen", "UI State - isDetecting: $currentIsDetecting, result: $currentResult")
                timeoutReached = true
                // Set an error result if detection never completed
                viewModel.clearDetectionResult()
                // Navigate to result screen with timeout error
                val encodedImage = java.net.URLEncoder.encode(imageUri ?: "", java.nio.charset.StandardCharsets.UTF_8.toString())
                val encodedAudio = java.net.URLEncoder.encode(audioUri ?: "", java.nio.charset.StandardCharsets.UTF_8.toString())
                navController.navigate(
                    "detection_result?imageUri=$encodedImage&audioUri=$encodedAudio&status=Error: Processing timeout. Please try again with valid chicken image and audio.&suggestions="
                ) {
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
                if (uiState.detectionResult == null && !uiState.isDetecting) {
                    Log.e("ProcessingScreen", "Detection stopped without result after 180 seconds total")
                    timeoutReached = true
                    val encodedImage = java.net.URLEncoder.encode(imageUri ?: "", java.nio.charset.StandardCharsets.UTF_8.toString())
                    val encodedAudio = java.net.URLEncoder.encode(audioUri ?: "", java.nio.charset.StandardCharsets.UTF_8.toString())
                    navController.navigate(
                        "detection_result?imageUri=$encodedImage&audioUri=$encodedAudio&status=Error: Processing timeout. Please try again with valid chicken image and audio.&suggestions="
                    ) {
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
            val encodedSuggestions = java.net.URLEncoder.encode(
                suggestionsString,
                java.nio.charset.StandardCharsets.UTF_8.toString()
            )
            val encodedImage = java.net.URLEncoder.encode(imageUri ?: "", java.nio.charset.StandardCharsets.UTF_8.toString())
            val encodedAudio = java.net.URLEncoder.encode(audioUri ?: "", java.nio.charset.StandardCharsets.UTF_8.toString())

            navController.navigate(
                "detection_result?imageUri=$encodedImage&audioUri=$encodedAudio&status=$status&suggestions=$encodedSuggestions"
            ) {
                popUpTo("processing") { inclusive = true }
            }
            viewModel.clearDetectionResult()
        } else if (uiState.isDetecting) {
            Log.d("ProcessingScreen", "Still detecting... isDetecting: ${uiState.isDetecting}, result: ${uiState.detectionResult}")
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
                        Color(0xFFF1E0C9),
                        Color(0xFFF1E0C9)
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
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE3B386).copy(alpha = 0.3f),
                                Color(0xFFD4A574).copy(alpha = 0.1f)
                            )
                        ),
                        RoundedCornerShape(60.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(80.dp),
                    color = Color(0xFFE3B386),
                    strokeWidth = 4.dp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Processing...",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Analyzing chicken health using\nfusion model (Image + Audio)",
                fontSize = 16.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Please wait...",
                fontSize = 14.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
        }
    }
}

