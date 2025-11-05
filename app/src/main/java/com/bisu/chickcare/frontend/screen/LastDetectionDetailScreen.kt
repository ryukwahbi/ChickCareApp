package com.bisu.chickcare.frontend.screen

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bisu.chickcare.backend.repository.DetectionEntry
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastDetectionDetailScreen(
    navController: NavController,
    entry: DetectionEntry
) {
    val context = LocalContext.current
    
    // Grant URI permissions for content URIs before loading images
    LaunchedEffect(entry.imageUri) {
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
                        Log.d("LastDetectionDetail", "Taken persistable URI permission for: $decodedUriString")
                    } catch (e: SecurityException) {
                        Log.w("LastDetectionDetail", "Cannot take persistable permission (may not support it): $decodedUriString - ${e.message}")
                    } catch (e: Exception) {
                        Log.w("LastDetectionDetail", "Error taking persistable permission: $decodedUriString - ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w("LastDetectionDetail", "Error parsing URI for permission: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detection Details",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFFFFF)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Date and Time Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFFD27D2D)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Date and Time",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333)
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = formatLastDetectionDate(entry.timestamp),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD27D2D)
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Location Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color(0xFF4CAF50)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Location",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF333333)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = entry.location ?: "Location not available",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = if (entry.location.isNullOrEmpty()) Color.Gray else Color(0xFF666666)
                                        )
                                    )
                                }
                            }
                            if (!entry.location.isNullOrEmpty()) {
                                IconButton(
                                    onClick = {
                                        // Share location
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "Detection Location: ${entry.location}")
                                            putExtra(Intent.EXTRA_SUBJECT, "Chicken Detection Location")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Location"))
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share Location",
                                        tint = Color(0xFFD27D2D),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        // Open location in Google Maps
                                        try {
                                            val locationQuery = Uri.encode(entry.location)
                                            val mapIntent = Intent(
                                                Intent.ACTION_VIEW,
                                                "geo:0,0?q=$locationQuery".toUri()
                                            )
                                            if (mapIntent.resolveActivity(context.packageManager) != null) {
                                                context.startActivity(mapIntent)
                                            } else {
                                                // Fallback to web search
                                                val webIntent = Intent(
                                                    Intent.ACTION_VIEW,
                                                    "https://www.google.com/maps/search/?api=1&query=$locationQuery".toUri()
                                                )
                                                context.startActivity(webIntent)
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("LastDetectionDetail", "Error opening maps: ${e.message}")
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Open in Maps",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Detection Image Card (only show if image URI exists)
                if (!entry.imageUri.isNullOrEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Detection Image",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF333333)
                                ),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            // Decode and parse the image URI - handle URL encoding from navigation
                            // entry.imageUri is already checked for null/empty in the outer if condition
                            val imageUriString = entry.imageUri
                            val decodedImageUriString = try {
                                // Try to decode URL encoding first
                                java.net.URLDecoder.decode(imageUriString, "UTF-8")
                            } catch (e: Exception) {
                                android.util.Log.w("LastDetectionDetail", "Failed to decode image URI: ${e.message}, using original")
                                imageUriString
                            }
                            
                            val imageUri = try {
                                decodedImageUriString.toUri()
                            } catch (e: Exception) {
                                android.util.Log.w("LastDetectionDetail", "Failed to parse image URI: ${e.message}")
                                null
                            }
                            
                            // Show only the actual captured image - no placeholder
                            if (imageUri != null) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Detection Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop,
                                    onError = {
                                        android.util.Log.e("LastDetectionDetail", "Failed to load image: $decodedImageUriString - ${it.result.throwable.message}")
                                    },
                                    onSuccess = {
                                        android.util.Log.d("LastDetectionDetail", "Successfully loaded detection image: $decodedImageUriString")
                                    }
                                )
                            }
                        }
                    }
                }

                // Additional Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Detection Information",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Status:",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF666666)
                                )
                            )
                            Text(
                                text = entry.result,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (entry.isHealthy) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Confidence:",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF666666)
                                )
                            )
                            Text(
                                text = "${(entry.confidence * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD27D2D)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatLastDetectionDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val date = Date(timestamp)
    return "${dateFormat.format(date)}\n${timeFormat.format(date)}"
}

