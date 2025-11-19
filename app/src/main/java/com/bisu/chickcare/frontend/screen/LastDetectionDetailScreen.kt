package com.bisu.chickcare.frontend.screen

import android.content.Intent
import android.net.Uri
import android.util.Log
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bisu.chickcare.backend.repository.DetectionEntry
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.bisu.chickcare.frontend.utils.sanitizeToUri
import com.bisu.chickcare.frontend.utils.sanitizeUriString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastDetectionDetailScreen(
    navController: NavController,
    entry: DetectionEntry
) {
    val context = LocalContext.current
    
    // Grant URI permissions for content URIs before loading images
    LaunchedEffect(entry.imageUri) {
        val sanitizedString = sanitizeUriString(entry.imageUri, "LastDetectionDetail")
        val uri = sanitizeToUri(entry.imageUri, "LastDetectionDetail")
        if (sanitizedString != null && uri?.scheme == "content") {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                Log.d("LastDetectionDetail", "Taken persistable URI permission for: $sanitizedString")
            } catch (e: SecurityException) {
                Log.w("LastDetectionDetail", "Cannot take persistable permission (may not support it): $sanitizedString - ${e.message}")
            } catch (e: Exception) {
                Log.w("LastDetectionDetail", "Error taking persistable permission: $sanitizedString - ${e.message}")
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
                        color = ThemeColorUtils.black()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ThemeColorUtils.black()
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeColorUtils.white(),
                    titleContentColor = ThemeColorUtils.black()
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    if (ThemeViewModel.isDarkMode) ThemeColorUtils.beige(Color(0xFFFFF7E6)) else Color(0xFFE8B88C)
                )
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (ThemeViewModel.isDarkMode) {
                                Modifier.shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(20.dp),
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
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFFD27D2D)
                        )
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Date and Time",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else Color(0xFF333333)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = formatLastDetectionDate(entry.timestamp),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else Color(0xFFD27D2D)
                                ),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }

                // Location Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (ThemeViewModel.isDarkMode) {
                                Modifier.shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(20.dp),
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
                                            color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else Color(0xFF333333)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val formattedLocation = entry.location
                                        ?.replace("+", " ")
                                        ?.replace(",", ", ")
                                        ?.replace("\\s+".toRegex(), " ")
                                        ?.trim()

                                    if (entry.location.isNullOrEmpty()) {
                                        Text(
                                            text = "Location not available",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.darkGray(Color(0xFF999999)) else ThemeColorUtils.lightGray(Color(0xFF999999))
                                            )
                                        )
                                    } else {
                                        Text(
                                            text = formattedLocation ?: "Location not available",
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else Color(0xFF333333)
                                            )
                                        )
                                    }
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
                                            Log.e("LastDetectionDetail", "Error opening maps: ${e.message}")
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (ThemeViewModel.isDarkMode) {
                                    Modifier.shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(20.dp),
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
                                    color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else Color(0xFF333333)
                                ),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            // Decode and parse the image URI - handle URL encoding from navigation
                            // entry.imageUri is already checked for null/empty in the outer if condition
                val imageUriString = entry.imageUri
                val sanitizedUriString = sanitizeUriString(imageUriString, "LastDetectionDetail")
                val imageUri = sanitizeToUri(imageUriString, "LastDetectionDetail")

                if (sanitizedUriString != null && imageUri != null) {
                    val imageRequest = ImageRequest.Builder(context)
                        .data(imageUri)
                        .crossfade(true)
                        .build()

                    AsyncImage(
                        model = imageRequest,
                        contentDescription = "Detection Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                        onError = {
                            Log.e("LastDetectionDetail", "Failed to load image: $sanitizedUriString - ${it.result.throwable.message}")
                        },
                        onSuccess = {
                            Log.d("LastDetectionDetail", "Successfully loaded detection image: $sanitizedUriString")
                        }
                    )
                            }
                        }
                    }
                }

                // Additional Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (ThemeViewModel.isDarkMode) {
                                Modifier.shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(20.dp),
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
                                color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else Color(0xFF333333)
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
                                    color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.darkGray(Color(0xFF666666)) else ThemeColorUtils.lightGray(Color(0xFF666666))
                                )
                            )
                            val statusLabel = com.bisu.chickcare.frontend.utils.DetectionDisplayUtils.statusText(entry.isHealthy, entry.confidence)
                            val confidenceValue = (entry.confidence * 100).coerceAtLeast(0f)
                            Text(
                                text = "$statusLabel - ${confidenceValue.formatOneDecimal()}%",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = com.bisu.chickcare.frontend.utils.DetectionDisplayUtils.statusColor(entry.isHealthy, entry.confidence)
                                )
                            )
                        }
                        if (entry.confidence > 0f) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Confidence:",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.darkGray(Color(0xFF666666)) else ThemeColorUtils.lightGray(Color(0xFF666666))
                                    )
                                )
                                val confidenceValue = (entry.confidence * 100).coerceAtLeast(0f)
                                Text(
                                    text = "${confidenceValue.formatOneDecimal()}%",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else Color(0xFFD27D2D)
                                    )
                                )
                            }
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

private fun Float.formatOneDecimal(): String = String.format(Locale.getDefault(), "%.1f", this)
