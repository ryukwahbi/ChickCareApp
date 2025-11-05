package com.bisu.chickcare.frontend.screen

import android.content.Intent
import android.media.MediaPlayer
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
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
import androidx.compose.ui.draw.clip
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
import com.bisu.chickcare.backend.repository.DetectionEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryResultScreen(
    navController: NavController,
    entry: DetectionEntry
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    val mediaPlayer = remember { MediaPlayer() }
    
    // Grant URI permissions for content URIs before loading images
    LaunchedEffect(entry.imageUri, entry.audioUri) {
        // Grant permissions for image URI
        entry.imageUri?.let { uriString ->
            try {
                val decodedUriString = try {
                    java.net.URLDecoder.decode(uriString, "UTF-8")
                } catch (_: Exception) {
                    uriString
                }
                val uri = decodedUriString.toUri()
                if (uri.scheme == "content") {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        android.util.Log.d("HistoryResultScreen", "Taken persistable URI permission for image: $decodedUriString")
                    } catch (e: SecurityException) {
                        android.util.Log.w("HistoryResultScreen", "Cannot take persistable permission for image (may not support it): $decodedUriString - ${e.message}")
                    } catch (e: Exception) {
                        android.util.Log.w("HistoryResultScreen", "Error taking persistable permission for image: $decodedUriString - ${e.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("HistoryResultScreen", "Error parsing image URI for permission: ${e.message}")
            }
        }
        
        // Grant permissions for audio URI
        entry.audioUri?.let { uriString ->
            try {
                val decodedUriString = try {
                    java.net.URLDecoder.decode(uriString, "UTF-8")
                } catch (_: Exception) {
                    uriString
                }
                val uri = decodedUriString.toUri()
                if (uri.scheme == "content") {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        android.util.Log.d("HistoryResultScreen", "Taken persistable URI permission for audio: $decodedUriString")
                    } catch (e: SecurityException) {
                        android.util.Log.w("HistoryResultScreen", "Cannot take persistable permission for audio (may not support it): $decodedUriString - ${e.message}")
                    } catch (e: Exception) {
                        android.util.Log.w("HistoryResultScreen", "Error taking persistable permission for audio: $decodedUriString - ${e.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("HistoryResultScreen", "Error parsing audio URI for permission: ${e.message}")
            }
        }
    }

    // Cleanup MediaPlayer when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            } catch (e: Exception) {
                android.util.Log.w("HistoryResultScreen", "Error releasing MediaPlayer: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detection Result",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        navController.navigate("detection_history") {
                            // Pop up to the start destination to clear the stack
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFE8B88C))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // First Card - Results with captured image, saved audio, confidence
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Captured Image (only show if image URI exists)
                            val imageUriString = entry.imageUri
                            val hasValidImageUri = !imageUriString.isNullOrEmpty()
                            val imageUri = if (hasValidImageUri) {
                                try {
                                    imageUriString.toUri()
                                } catch (e: Exception) {
                                    android.util.Log.w("HistoryResultScreen", "Invalid image URI format: $imageUriString", e)
                                    null
                                }
                            } else {
                                null
                            }

                            if (imageUri != null) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Detection Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop,
                                    onError = {
                                        android.util.Log.e("HistoryResultScreen", "Failed to load image: $imageUriString - ${it.result.throwable.message}")
                                    },
                                    onSuccess = {
                                        android.util.Log.d("HistoryResultScreen", "Successfully loaded detection image: $imageUriString")
                                    }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Result Text - Clean up result text to remove plus signs and confidence
                            val cleanResult = entry.result
                                .replace("+", "") // Remove plus signs
                                .replace(Regex("""\s*\([0-9.]+%?\)$"""), "") // Remove confidence like (99.8%) or (100.0) at the end
                                .trim()
                            
                            Text(
                                text = "Result: $cleanResult",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (entry.isHealthy) Color(0xFF4CAF50) else Color(0xFFF44336)
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Confidence
                            Text(
                                text = "Confidence: ${(entry.confidence * 100).toInt()}%",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF666666)
                                ),
                                textAlign = TextAlign.Center
                            )

                            // Play Audio Button (if available)
                            if (!entry.audioUri.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        if (isPlaying) {
                                            try {
                                                mediaPlayer.stop()
                                                mediaPlayer.reset()
                                                isPlaying = false
                                            } catch (e: Exception) {
                                                android.util.Log.e("HistoryResultScreen", "Error stopping audio: ${e.message}")
                                                isPlaying = false
                                            }
                                        } else {
                                            try {
                                                val audioUri = entry.audioUri.toUri()
                                                mediaPlayer.reset()
                                                mediaPlayer.setDataSource(context, audioUri)
                                                mediaPlayer.prepareAsync()
                                                mediaPlayer.setOnPreparedListener {
                                                    mediaPlayer.start()
                                                    isPlaying = true
                                                }
                                                mediaPlayer.setOnCompletionListener {
                                                    isPlaying = false
                                                }
                                                mediaPlayer.setOnErrorListener { _, what, extra ->
                                                    android.util.Log.e("HistoryResultScreen", "MediaPlayer error: what=$what, extra=$extra")
                                                    isPlaying = false
                                                    false
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.e("HistoryResultScreen", "Error playing audio: ${e.message}")
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Error playing audio: ${e.message}",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                                isPlaying = false
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPlaying) Color.DarkGray else Color(0xFF7BC0F6)
                                    ),
                                    shape = RoundedCornerShape(50)
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

                if (!entry.isHealthy) {
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
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Action Required",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFE65100)
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = Color.Black, fontWeight = FontWeight.SemiBold)) {
                                            append("If your chicken has been detected as ")
                                        }
                                        withStyle(SpanStyle(color = Color(0xFFF44336), fontWeight = FontWeight.ExtraBold)) {
                                            append("INFECTED")
                                        }
                                        withStyle(SpanStyle(color = Color.Black, fontWeight = FontWeight.SemiBold)) {
                                            append(". Please go to the veterinarian immediately for proper diagnosis and treatment.")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        textAlign = TextAlign.Center
                                    )
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        try {
                                            val mapIntent = Intent(Intent.ACTION_VIEW).apply {
                                                data = "geo:0,0?q=veterinarian".toUri()
                                            }
                                            if (mapIntent.resolveActivity(context.packageManager) != null) {
                                                context.startActivity(mapIntent)
                                            } else {
                                                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                                                    data = "https://www.google.com/search?q=veterinarian+near+me".toUri()
                                                }
                                                context.startActivity(webIntent)
                                            }
                                        } catch (_: Exception) {
                                            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                                                data = "https://www.google.com/search?q=veterinarian+near+me".toUri()
                                            }
                                            context.startActivity(webIntent)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
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
                }

                // Third Card - Recommended Actions After Detection (for both healthy and unhealthy)
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

                            val displaySuggestions = if (entry.recommendations.isNotEmpty()) {
                                // Decode URL-encoded recommendations and remove checkmarks
                                entry.recommendations.map { suggestion ->
                                    try {
                                        val decoded = java.net.URLDecoder.decode(suggestion, "UTF-8")
                                        decoded.replace("✅", "").replace("✔", "").trim()
                                    } catch (_: Exception) {
                                        suggestion.replace("✅", "").replace("✔", "").trim()
                                    }
                                }
                            } else if (entry.isHealthy) {
                                listOf(
                                    "Your chicken appears healthy. Continue regular health monitoring.",
                                    "Maintain clean and dry coop conditions.",
                                    "Provide balanced nutrition and fresh water daily.",
                                    "Schedule regular check-ups with a veterinarian for preventive care.",
                                    "Observe daily behavior and physical appearance for any changes."
                                )
                            } else {
                                listOf(
                                    "IMPORTANT: Go to the veterinarian immediately for proper diagnosis and treatment.",
                                    "Isolate infected birds immediately to prevent disease spread.",
                                    "Administer antibiotics only as prescribed by a veterinarian.",
                                    "Improve ventilation in the coop to reduce infection risk.",
                                    "Ensure clean water and high-quality feed to support recovery.",
                                    "Monitor affected chickens closely and report symptoms to the vet."
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
    }
}

