package com.bisu.chickcare.frontend.screen

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    navController: NavController,
    imageUri: String?,
    audioUri: String?,
    status: String,
    suggestions: List<String>,
) {
    val context = LocalContext.current
    val viewModel: DashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    var isPlaying by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    // Create a MediaPlayer that is managed by the composable's lifecycle
    val mediaPlayer = remember { MediaPlayer() }
    
    // Take persistable URI permissions immediately when screen loads (before displaying images)
    LaunchedEffect(imageUri, audioUri) {
        // Take permissions for image URI if it's a picker URI
        imageUri?.let { uriString ->
            try {
                // Decode URL encoding first (navigation may encode URIs)
                val decodedUriString = try {
                    java.net.URLDecoder.decode(uriString, "UTF-8")
                } catch (e: Exception) {
                    uriString // Use original if decoding fails
                }
                
                val uri = decodedUriString.toUri()
                if (uri.scheme == "content" && decodedUriString.contains("picker", ignoreCase = true)) {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        Log.d("ResultScreen", "Taken persistable URI permission for image: $decodedUriString")
                    } catch (e: SecurityException) {
                        Log.w("ResultScreen", "Cannot take persistable permission for image (may already be taken or expired): ${e.message}")
                    } catch (e: Exception) {
                        Log.w("ResultScreen", "Error taking persistable permission for image: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w("ResultScreen", "Error parsing image URI for permission: ${e.message}")
            }
        }
        
        // Take permissions for audio URI if it's a picker URI
        audioUri?.let { uriString ->
            try {
                // Decode URL encoding first (navigation may encode URIs)
                val decodedUriString = try {
                    java.net.URLDecoder.decode(uriString, "UTF-8")
                } catch (e: Exception) {
                    uriString // Use original if decoding fails
                }
                
                val uri = decodedUriString.toUri()
                if (uri.scheme == "content" && decodedUriString.contains("picker", ignoreCase = true)) {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        Log.d("ResultScreen", "Taken persistable URI permission for audio: $decodedUriString")
                    } catch (e: SecurityException) {
                        Log.w("ResultScreen", "Cannot take persistable permission for audio (may already be taken or expired): ${e.message}")
                    } catch (e: Exception) {
                        Log.w("ResultScreen", "Error taking persistable permission for audio: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w("ResultScreen", "Error parsing audio URI for permission: ${e.message}")
            }
        }
    }

    // This effect ensures the MediaPlayer is released when the screen is left
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    // Decode URL-encoded status (spaces become + in URLs)
    val decodedStatus = try {
        java.net.URLDecoder.decode(status, "UTF-8")
    } catch (e: Exception) {
        Log.w("ResultScreen", "Failed to decode status: ${e.message}, using original")
        status
    }

    // Check if this is an invalid image error
    val isInvalidImage = decodedStatus.startsWith("INVALID_IMAGE:", ignoreCase = true)
    val displayStatus = if (isInvalidImage) {
        decodedStatus.substringAfter("INVALID_IMAGE: ", missingDelimiterValue = decodedStatus)
    } else {
        decodedStatus
    }

    // Parse result and confidence from status string
    // Format: "Healthy (XX.X%)" or "Infected (XX.X%)" or "Unhealthy (XX.X%)"
    // Log the status for debugging
    Log.d("ResultScreen", "Parsing status (original): '$status'")
    Log.d("ResultScreen", "Parsing status (decoded): '$decodedStatus'")
    // Use decodedStatus for parsing
    val resultText = when {
        isInvalidImage -> "Error"
        // Check for Healthy first (but not Unhealthy)
        decodedStatus.contains("Healthy", ignoreCase = true) && !decodedStatus.contains(
            "Unhealthy",
            ignoreCase = true
        ) -> {
            Log.d("ResultScreen", "Detected as Healthy")
            "Healthy"
        }
        // Check for Infected or Unhealthy
        decodedStatus.contains("Infected", ignoreCase = true) || decodedStatus.contains(
            "Unhealthy",
            ignoreCase = true
        ) -> {
            Log.d("ResultScreen", "Detected as Infected")
            "Infected"
        }

        decodedStatus.contains("Error", ignoreCase = true) -> {
            Log.d("ResultScreen", "Detected as Error")
            "Error"
        }

        else -> {
            // Log unknown status for debugging
            Log.w(
                "ResultScreen",
                "Unknown status format: '$decodedStatus'. Attempting fallback parsing..."
            )
            // Fallback: try to extract any recognizable pattern
            when {
                decodedStatus.lowercase().contains("healthy") && !decodedStatus.lowercase()
                    .contains("unhealthy") -> {
                    Log.d("ResultScreen", "Fallback: Detected as Healthy")
                    "Healthy"
                }

                decodedStatus.lowercase().contains("infected") || decodedStatus.lowercase()
                    .contains("unhealthy") -> {
                    Log.d("ResultScreen", "Fallback: Detected as Infected")
                    "Infected"
                }

                else -> {
                    Log.e("ResultScreen", "Failed to parse status: '$decodedStatus'")
                    "Unknown"
                }
            }
        }
    }

    // Extract confidence percentage from decodedStatus
    // Handle formats like: "Healthy (69.3%)", "Infected (89.5%)", "Infected+(89.5%)", "Healthy (100.0??"
    // Try multiple patterns to handle URL encoding issues and corrupted endings
    val confidenceRegex1 =
        """\s*\+?\s*\((\d+\.?\d*)%\)""".toRegex() // Matches space/+ before parentheses with %)
    val confidenceRegex2 = """\((\d+\.?\d*)%\)""".toRegex() // Direct parentheses with %)
    val confidenceRegex3 =
        """\((\d+\.?\d*)\?+""".toRegex() // Handle corrupted endings like (100.0??
    val confidenceRegex4 = """\((\d+\.?\d*)\s*""".toRegex() // Handle cases where % is missing
    val confidenceMatch = confidenceRegex1.find(decodedStatus)
        ?: confidenceRegex2.find(decodedStatus)
        ?: confidenceRegex3.find(decodedStatus)
        ?: confidenceRegex4.find(decodedStatus)
    val confidenceText = confidenceMatch?.groupValues?.get(1) ?: ""

    Log.d(
        "ResultScreen",
        "Confidence extraction - Decoded Status: '$decodedStatus', Extracted: '$confidenceText'"
    )

    val isHealthy =
        resultText == "Healthy" // isInvalidImage is already false if resultText is "Healthy"
    val resultColor = when {
        isInvalidImage -> Color(0xFFF44336) // Red for invalid images
        isHealthy -> Color(0xFF4CAF50) // Green for healthy
        else -> Color(0xFFF44336) // Red for infected
    }
    val backgroundUrl =
        "https://media.istockphoto.com/id/1342480600/photo/free-range-healthy-brown-organic-chickens-and-a-white-rooster-on-a-green-meadow.jpg?s=612x612&w=0&k=20&c=HWwPGRkHpEnObkcsMzopcmXorwHD0PS7NQ1EiA8K53c="

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Detection Result",
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
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5DC)) // Light beige background
        ) {
            AsyncImage(
                model = backgroundUrl,
                contentDescription = "Background",
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.3f),
                contentScale = ContentScale.Crop
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Result Display Card ---
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // If an image URI is present, show the image. Otherwise, show an icon.
                            if (!imageUri.isNullOrEmpty()) {
                                // Decode URL encoding first if needed, then convert to Uri
                                val decodedImageUriString = try {
                                    java.net.URLDecoder.decode(imageUri, "UTF-8")
                                } catch (e: Exception) {
                                    android.util.Log.w("ResultScreen", "Failed to decode image URI: ${e.message}, using original")
                                    imageUri
                                }
                                val imageUriObj = try {
                                    decodedImageUriString.toUri()
                                } catch (e: Exception) {
                                    android.util.Log.w("ResultScreen", "Failed to parse image URI: ${e.message}")
                                    null
                                }
                                
                                if (imageUriObj != null) {
                                    AsyncImage(
                                        model = imageUriObj,
                                        contentDescription = "Detection Input Image",
                                        modifier = Modifier
                                            .size(200.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop,
                                        onError = {
                                            android.util.Log.e("ResultScreen", "Failed to load image: $decodedImageUriString - ${it.result.throwable.message}")
                                        },
                                        onSuccess = {
                                            android.util.Log.d("ResultScreen", "Successfully loaded detection image: $decodedImageUriString")
                                        }
                                    )
                                } else {
                                    // Fallback icon if URI parsing fails
                                    Icon(
                                        imageVector = Icons.Default.Healing,
                                        contentDescription = "Health Icon",
                                        modifier = Modifier.size(200.dp),
                                        tint = resultColor
                                    )
                                }
                            } else {
                                // Fallback for when there's no image (e.g., audio-only detection)
                                Icon(
                                    imageVector = Icons.Default.Healing,
                                    contentDescription = "Health Icon",
                                    modifier = Modifier.size(200.dp),
                                    tint = resultColor
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            AnimatedVisibility(visible = true, enter = scaleIn() + fadeIn()) {
                                if (isInvalidImage) {
                                    // Display invalid image error in red
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Healing,
                                            contentDescription = "Error",
                                            modifier = Modifier.size(64.dp),
                                            tint = Color(0xFFF44336)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = displayStatus,
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                color = Color(0xFFF44336) // Red color
                                            ),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    // Display result with proper formatting
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Result line: "Result: Healthy" or "Result: Infected"
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Result: ",
                                                style = MaterialTheme.typography.headlineMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 28.sp,
                                                    color = Color.Black
                                                )
                                            )
                                            Text(
                                                text = resultText,
                                                style = MaterialTheme.typography.headlineMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 28.sp,
                                                    color = resultColor // Green for Healthy, Red for Infected
                                                )
                                            )
                                        }

                                        // Confidence line: "Confidence: XX.X"
                                        if (confidenceText.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Confidence: ",
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 20.sp,
                                                        color = Color.Black
                                                    )
                                                )
                                                Text(
                                                    text = "$confidenceText%",
                                                    style = MaterialTheme.typography.titleLarge.copy(
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 20.sp,
                                                        color = Color.Black
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            // --- Play Audio Button (only shows if audioUri exists) ---
                            if (!audioUri.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        if (isPlaying) {
                                            try {
                                                mediaPlayer.stop()
                                                mediaPlayer.reset()
                                                isPlaying = false
                                            } catch (e: Exception) {
                                                Log.e(
                                                    "ResultScreen",
                                                    "Error stopping audio: ${e.message}",
                                                    e
                                                )
                                                isPlaying = false
                                            }
                                        } else {
                                            try {
                                                val decodedAudioUri = Uri.decode(audioUri).toUri()
                                                Log.d(
                                                    "ResultScreen",
                                                    "Attempting to play audio: $decodedAudioUri"
                                                )

                                                // Grant URI permission for content URIs (e.g., from Google Drive)
                                                if (decodedAudioUri.scheme == "content") {
                                                    try {
                                                        // Try to take persistent URI permission if available
                                                        context.contentResolver.takePersistableUriPermission(
                                                            decodedAudioUri,
                                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                        )
                                                    } catch (e: SecurityException) {
                                                        Log.w(
                                                            "ResultScreen",
                                                            "Could not take persistent URI permission (this is OK for temporary URIs): ${e.message}"
                                                        )
                                                        // This is expected for temporary URIs - MediaPlayer.setDataSource(context, uri) will handle it
                                                    } catch (e: Exception) {
                                                        Log.w(
                                                            "ResultScreen",
                                                            "Error taking URI permission: ${e.message}"
                                                        )
                                                        // Continue anyway - MediaPlayer might still work
                                                    }
                                                }

                                                // Skip file access check for Google Drive URIs - MediaPlayer will handle it
                                                val isGoogleDriveUri =
                                                    decodedAudioUri.host?.contains("google.android.apps.docs") == true

                                                // Only verify file accessibility for non-Google Drive URIs
                                                if (!isGoogleDriveUri) {
                                                    try {
                                                        val inputStream =
                                                            if (decodedAudioUri.scheme == "file") {
                                                                java.io.FileInputStream(
                                                                    decodedAudioUri.path
                                                                )
                                                            } else {
                                                                context.contentResolver.openInputStream(
                                                                    decodedAudioUri
                                                                )
                                                            }
                                                        inputStream?.close()
                                                    } catch (e: Exception) {
                                                        Log.e(
                                                            "ResultScreen",
                                                            "Cannot access audio file: ${e.message}",
                                                            e
                                                        )
                                                        throw Exception("Cannot access audio file. Please ensure the file is available.")
                                                    }
                                                }

                                                mediaPlayer.apply {
                                                    reset()
                                                    setOnErrorListener { _, what, extra ->
                                                        Log.e(
                                                            "ResultScreen",
                                                            "MediaPlayer error: what=$what, extra=$extra"
                                                        )
                                                        isPlaying = false
                                                        false
                                                    }


                                                    when (decodedAudioUri.scheme) {
                                                        "content" -> {
                                                            try {
                                                                setDataSource(
                                                                    context,
                                                                    decodedAudioUri
                                                                )
                                                                Log.d(
                                                                    "ResultScreen",
                                                                    "Set MediaPlayer data source using context and URI"
                                                                )
                                                            } catch (e: SecurityException) {
                                                                Log.e(
                                                                    "ResultScreen",
                                                                    "Security exception setting data source: ${e.message}",
                                                                    e
                                                                )
                                                                throw Exception("Cannot access audio file. For Google Drive files, please ensure you selected it using the file picker.")
                                                            } catch (e: Exception) {
                                                                Log.e(
                                                                    "ResultScreen",
                                                                    "Error setting data source: ${e.message}",
                                                                    e
                                                                )
                                                                throw Exception("Cannot access audio file: ${e.message}")
                                                            }
                                                        }

                                                        "file" -> {
                                                            // For file URIs, use path directly
                                                            setDataSource(
                                                                decodedAudioUri.path
                                                                    ?: throw Exception("Invalid file path")
                                                            )
                                                        }

                                                        else -> throw Exception("Unsupported URI scheme: ${decodedAudioUri.scheme}")
                                                    }

                                                    prepareAsync() // Use async prepare
                                                    setOnPreparedListener {
                                                        Log.d(
                                                            "ResultScreen",
                                                            "MediaPlayer prepared, starting playback"
                                                        )
                                                        start()
                                                    }
                                                    setOnCompletionListener {
                                                        Log.d(
                                                            "ResultScreen",
                                                            "Audio playback completed"
                                                        )
                                                        isPlaying = false
                                                    }
                                                }
                                                isPlaying = true
                                                Log.d("ResultScreen", "Audio playback started")
                                            } catch (e: SecurityException) {
                                                Log.e(
                                                    "ResultScreen",
                                                    "Security error accessing audio: ${e.message}",
                                                    e
                                                )
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Permission denied: Cannot access audio file. Please select the file again.",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                                isPlaying = false
                                            } catch (e: Exception) {
                                                Log.e(
                                                    "ResultScreen",
                                                    "Error playing audio: ${e.message}",
                                                    e
                                                )
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Error playing audio: ${e.message}",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                                isPlaying = false
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPlaying) Color.DarkGray else Color(
                                            0xFF7BC0F6
                                        )
                                    )
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Stop Audio" else "Play Audio"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isPlaying) "Stop Audio" else "Play Audio")
                                }
                            }
                        }
                    }
                }

                // --- Action Required Card (always shown) ---
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        elevation = CardDefaults.cardElevation(12.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Healing,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "▲ Action Required",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Formatted text with color variations
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            style = SpanStyle(
                                                color = Color.Black,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        ) {
                                            append("If your chicken has been detected as ")
                                        }
                                        withStyle(
                                            style = SpanStyle(
                                                color = Color(0xFFF44336),
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        ) {
                                            append("INFECTED")
                                        }
                                        withStyle(
                                            style = SpanStyle(
                                                color = Color.Black,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        ) {
                                            append(". Please go to the veterinarian immediately for proper diagnosis and treatment.")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    // Search for nearby veterinarians using Google Maps or web search
                                    try {
                                        // Try to open Google Maps with search
                                        val mapIntent = Intent(Intent.ACTION_VIEW).apply {
                                            data = "geo:0,0?q=veterinarian".toUri()
                                        }
                                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(mapIntent)
                                        } else {
                                            // Fallback to web search
                                            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                                                data =
                                                    "https://www.google.com/search?q=veterinarian+near+me".toUri()
                                            }
                                            context.startActivity(webIntent)
                                        }
                                    } catch (_: Exception) {
                                        // Fallback to web search if anything fails
                                        val webIntent = Intent(Intent.ACTION_VIEW).apply {
                                            data =
                                                "https://www.google.com/search?q=veterinarian+near+me".toUri()
                                        }
                                        context.startActivity(webIntent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(
                                        0xFFE65100
                                    )
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Find Nearby Veterinarian",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // --- Recommended Actions After Detection ---
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            // Title with big bullet point
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⬤",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 20.sp,
                                        color = Color.Black
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Recommended Actions After Detection",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // Show suggestions for infected or healthy
                            val displaySuggestions = if (suggestions.isNotEmpty()) {
                                // Decode URL-encoded suggestions (spaces might be encoded as +)
                                // Also remove any checkmark emojis
                                suggestions.map { suggestion ->
                                    try {
                                        val decoded = java.net.URLDecoder.decode(suggestion, "UTF-8")
                                        decoded.replace("✅", "").replace("✔", "").trim()
                                    } catch (e: Exception) {
                                        suggestion.replace("✅", "").replace("✔", "").trim()
                                    }
                                }
                            } else if (isHealthy) {
                                // Default suggestions for healthy chickens
                                listOf(
                                    "Your chicken appears healthy. Continue regular health monitoring.",
                                    "Maintain clean and dry coop conditions.",
                                    "Provide balanced nutrition and fresh water daily.",
                                    "Schedule regular check-ups with a veterinarian for preventive care.",
                                    "Observe daily behavior and physical appearance for any changes."
                                )
                            } else {
                                // Default suggestions for infected chickens
                                listOf(
                                    "Seek immediate veterinary consultation for proper diagnosis.",
                                    "Isolate the infected chicken from the flock to prevent spread.",
                                    "Disinfect the coop and equipment thoroughly.",
                                    "Monitor other chickens closely for similar symptoms.",
                                    "Follow veterinarian's prescribed treatment plan strictly."
                                )
                            }

                            displaySuggestions.forEach { suggestion ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier
                                        .padding(vertical = 6.dp)
                                        .fillMaxWidth()
                                ) {
                                    Text(
                                        text = "—  ",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Normal,
                                            color = Color.Black
                                        )
                                    )
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Normal,
                                            color = Color.Black
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

            }
        }

        // Auto-save detection result when screen appears (no buttons needed)
        LaunchedEffect(Unit) {
            if (!isSaving) {
                isSaving = true
                try {
                    // Extract confidence from decodedStatus
                    val confidenceValue = if (confidenceText.isNotEmpty()) {
                        confidenceText.toFloatOrNull()?.div(100f) ?: 0f
                    } else {
                        0f
                    }

                    // Get recommendations from DetectionService
                    val detectionService = com.bisu.chickcare.backend.service.DetectionService(
                        com.bisu.chickcare.backend.repository.DetectionRepository(),
                        context
                    )
                    val recommendations = detectionService.getRemedySuggestions(!isHealthy)

                    // Save to Firebase (with context for URI permissions, location, and recommendations)
                    viewModel.saveDetectionResult(
                        resultString = decodedStatus,
                        isHealthy = isHealthy,
                        confidence = confidenceValue,
                        imageUri = imageUri,
                        audioUri = audioUri,
                        context = context,
                        recommendations = recommendations
                    )

                    Log.d("ResultScreen", "Detection result auto-saved successfully")
                } catch (e: Exception) {
                    Log.e("ResultScreen", "Error auto-saving detection: ${e.message}", e)
                    // Don't show error to user, just log it
                } finally {
                    isSaving = false
                }
            }
        }
    }
}