package com.bisu.chickcare.frontend.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.shadow
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
import coil.request.ImageRequest
import com.bisu.chickcare.backend.repository.DetectionEntry
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.bisu.chickcare.frontend.components.PlayAudioButton
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import com.bisu.chickcare.frontend.utils.sanitizeToUri
import com.bisu.chickcare.frontend.utils.sanitizeUriString

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryResultScreen(
    navController: NavController,
    entry: DetectionEntry
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    val mediaPlayer = remember { MediaPlayer() }

    val onPlayClick: () -> Unit = {
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
                // Determine URI type and set data source accordingly
                val audioUri: Uri? = if (entry.audioUri?.startsWith("content://") == true) {
                     Uri.parse(entry.audioUri)
                } else {
                    entry.audioUri?.toUri()
                }
                
                if (audioUri != null) {
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
                } else {
                    // Try Cloud Audio URL if local URI is null/invalid
                    if (!entry.cloudAudioUrl.isNullOrEmpty()) {
                        android.util.Log.d("HistoryResultScreen", "Audio URI null/local failed, trying Cloud URL: ${entry.cloudAudioUrl}")
                        mediaPlayer.reset()
                        mediaPlayer.setDataSource(entry.cloudAudioUrl)
                        mediaPlayer.prepareAsync()
                        mediaPlayer.setOnPreparedListener {
                            mediaPlayer.start()
                            isPlaying = true
                        }
                        mediaPlayer.setOnCompletionListener {
                            isPlaying = false
                        }
                        mediaPlayer.setOnErrorListener { _, what, extra ->
                             android.util.Log.e("HistoryResultScreen", "MediaPlayer cloud error: what=$what, extra=$extra")
                             isPlaying = false
                             false
                        }
                    } else {
                         android.util.Log.e("HistoryResultScreen", "Audio URI is null and no Cloud URL available")
                         isPlaying = false
                    }
                }
            } catch (e: Exception) {
                // If the first attempt (Local) failed, try Cloud URL as fallback inside the catch block
                if (!entry.cloudAudioUrl.isNullOrEmpty()) {
                     try {
                        android.util.Log.d("HistoryResultScreen", "Local audio failed ($e), trying Cloud URL: ${entry.cloudAudioUrl}")
                        mediaPlayer.reset()
                        mediaPlayer.setDataSource(entry.cloudAudioUrl)
                        mediaPlayer.prepareAsync()
                        mediaPlayer.setOnPreparedListener {
                            mediaPlayer.start()
                             isPlaying = true
                        }
                        mediaPlayer.setOnCompletionListener {
                            isPlaying = false
                        }
                         mediaPlayer.setOnErrorListener { _, what, extra ->
                            android.util.Log.e("HistoryResultScreen", "MediaPlayer cloud fallback error: what=$what, extra=$extra")
                            isPlaying = false
                            false
                        }
                     } catch (cloudError: Exception) {
                         android.util.Log.e("HistoryResultScreen", "Error playing cloud audio: ${cloudError.message}")
                         isPlaying = false
                     }
                } else {
                    android.util.Log.e("HistoryResultScreen", "Error playing audio: ${e.message}")
                    android.widget.Toast.makeText(
                        context,
                        "Error playing audio: ${e.message}",
                         android.widget.Toast.LENGTH_SHORT
                    ).show()
                    isPlaying = false
                }
            }
        }
    }
    
    // Grant URI permissions for content URIs before loading images
    LaunchedEffect(entry.imageUri, entry.audioUri) {
        // Grant permissions for image URI
        val sanitizedImageString = sanitizeUriString(entry.imageUri, "HistoryResultScreen")
        val imageUri = sanitizeToUri(entry.imageUri, "HistoryResultScreen")
        if (sanitizedImageString != null && imageUri?.scheme == "content") {
            try {
                context.contentResolver.takePersistableUriPermission(
                    imageUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                android.util.Log.d("HistoryResultScreen", "Taken persistable URI permission for image: $sanitizedImageString")
            } catch (e: SecurityException) {
                android.util.Log.w("HistoryResultScreen", "Cannot take persistable permission for image (may not support it): $sanitizedImageString - ${e.message}")
            } catch (e: Exception) {
                android.util.Log.w("HistoryResultScreen", "Error taking persistable permission for image: $sanitizedImageString - ${e.message}")
            }
        }
        
        val sanitizedAudioString = sanitizeUriString(entry.audioUri, "HistoryResultScreen")
        val audioUri = sanitizeToUri(entry.audioUri, "HistoryResultScreen")
        if (sanitizedAudioString != null && audioUri?.scheme == "content") {
            try {
                context.contentResolver.takePersistableUriPermission(
                    audioUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                android.util.Log.d("HistoryResultScreen", "Taken persistable URI permission for audio: $sanitizedAudioString")
            } catch (e: SecurityException) {
                android.util.Log.w("HistoryResultScreen", "Cannot take persistable permission for audio (may not support it): $sanitizedAudioString - ${e.message}")
            } catch (e: Exception) {
                android.util.Log.w("HistoryResultScreen", "Error taking persistable permission for audio: $sanitizedAudioString - ${e.message}")
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
                        color = ThemeColorUtils.black()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        // Pop back to detection_history screen
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ThemeColorUtils.black()
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeColorUtils.white(),
                    titleContentColor = ThemeColorUtils.black(),
                    navigationIconContentColor = ThemeColorUtils.black()
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    if (ThemeViewModel.isDarkMode) ThemeColorUtils.beige(Color(0xFFDEC6A3)) else ThemeColorUtils.beige(Color(
                        0xFFEAD1AC
                    )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // First Card - Results with captured image, saved audio, confidence
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (ThemeViewModel.isDarkMode) {
                                    Modifier.shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        spotColor = Color.White,
                                        ambientColor = Color.White.copy(alpha = 0.5f)
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFFE5E2DE)) else ThemeColorUtils.surface(Color.White)
                        ),
                        elevation = if (ThemeViewModel.isDarkMode) {
                            CardDefaults.cardElevation(defaultElevation = 0.dp)
                        } else {
                            CardDefaults.cardElevation(8.dp)
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Captured Image (only show if image URI exists)
                            // Use getAccessibleUri for Cloud Fallback
                            val imageModel = com.bisu.chickcare.frontend.utils.getAccessibleUri(context, entry.imageUri, entry.cloudUrl)
                            
                            if (imageModel != null) {
                                val imageRequest = ImageRequest.Builder(context)
                                    .data(imageModel)
                                    .crossfade(true)
                                    .build()

                                AsyncImage(
                                    model = imageRequest,
                                    contentDescription = "Detection Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop,
                                    onError = {
                                        android.util.Log.e("HistoryResultScreen", "Failed to load image: $imageModel - ${it.result.throwable.message}")
                                    }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Result Row with Text and Audio Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    // Clean result string
                                    val cleanResult = entry.result
                                        .replace("+", "")
                                        .replace("?", "")
                                        .replace("❓", "")
                                        .replace(Regex("[❓?]+"), "")
                                        .replace(Regex("\\([^)]*\\)"), "")
                                        .trim()
                                    
                                    val statusText = when {
                                        cleanResult.contains("Healthy", ignoreCase = true) && !cleanResult.contains("Unhealthy", ignoreCase = true) -> "Healthy"
                                        cleanResult.contains("Infected", ignoreCase = true) || cleanResult.contains("Unhealthy", ignoreCase = true) -> "Infected"
                                        entry.isHealthy -> "Healthy"
                                        else -> "Infected"
                                    }
                                    
                                    Text(
                                        text = "Result: $statusText",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 20.sp,
                                            color = if (entry.isHealthy) Color(0xFF4CAF50) else Color(0xFFF44336)
                                        ),
                                        textAlign = TextAlign.Start
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Confidence: ${String.format("%.1f", entry.confidence * 100)}%",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Normal,
                                            fontSize = 16.sp,
                                            color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.darkGray(Color(0xFF666666)) else ThemeColorUtils.lightGray(Color(0xFF666666))
                                        ),
                                        textAlign = TextAlign.Start
                                    )
                                }

                                // Play Audio Button (if available)
                                if (!entry.audioUri.isNullOrEmpty()) {
                                    PlayAudioButton(
                                        isPlaying = isPlaying,
                                        onPlayClick = onPlayClick
                                    )
                                }
                            }
                        }
                    }
                }

                if (!entry.isHealthy) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (ThemeViewModel.isDarkMode) {
                                        Modifier.shadow(
                                            elevation = 12.dp,
                                            shape = RoundedCornerShape(16.dp),
                                            spotColor = Color.White,
                                            ambientColor = Color.White.copy(alpha = 0.5f)
                                        )
                                    } else {
                                        Modifier
                                    }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFFE5E2DE)) else ThemeColorUtils.surface(Color.White)
                            ),
                            elevation = if (ThemeViewModel.isDarkMode) {
                                CardDefaults.cardElevation(defaultElevation = 0.dp)
                            } else {
                                CardDefaults.cardElevation(12.dp)
                            },
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
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(
                                            color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else ThemeColorUtils.black(),
                                            fontWeight = FontWeight.SemiBold
                                        )) {
                                            append("If your chicken has been detected as ")
                                        }
                                        withStyle(SpanStyle(color = Color(0xFFF44336), fontWeight = FontWeight.ExtraBold)) {
                                            append("INFECTED")
                                        }
                                        withStyle(SpanStyle(
                                            color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else ThemeColorUtils.black(),
                                            fontWeight = FontWeight.SemiBold
                                        )) {
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
                                        tint = ThemeColorUtils.white()
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Find Nearby Veterinarian",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeColorUtils.white()
                                    )
                                }
                            }
                        }
                    }
                }

                // Third Card - Recommended Actions After Detection (for both healthy and unhealthy)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (ThemeViewModel.isDarkMode) {
                                    Modifier.shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        spotColor = Color.White,
                                        ambientColor = Color.White.copy(alpha = 0.5f)
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFFE5E2DE)) else ThemeColorUtils.surface(Color.White)
                        ),
                        elevation = if (ThemeViewModel.isDarkMode) {
                            CardDefaults.cardElevation(defaultElevation = 0.dp)
                        } else {
                            CardDefaults.cardElevation(8.dp)
                        },
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
                                        color = ThemeColorUtils.black()
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Recommended Actions After Detection",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ThemeColorUtils.black()
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
                                    "⚠️ CRITICAL: Consult a licensed veterinarian IMMEDIATELY for proper diagnosis and treatment.",
                                    "⚠️ DO NOT self-medicate or use antibiotics without veterinary prescription - this can worsen the condition.",
                                    "Isolate infected chicken immediately from the flock to prevent disease spread.",
                                    "Disinfect the coop and equipment thoroughly to prevent further spread.",
                                    "Monitor other chickens closely for similar symptoms and report to your veterinarian.",
                                    "Follow ONLY the treatment plan prescribed by your licensed veterinarian.",
                                    "Provide clean water and high-quality feed to support recovery."
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
                                            color = ThemeColorUtils.black()
                                        )
                                    )
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Normal,
                                            color = ThemeColorUtils.black()
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
