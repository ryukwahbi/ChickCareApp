package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.bisu.chickcare.backend.repository.DetectionEntry
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel
import android.content.Intent
import android.util.Log
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionHistoryScreen(navController: NavController, paddingValues: PaddingValues) {
    val dashboardViewModel: DashboardViewModel = viewModel()
    val history by dashboardViewModel.detectionHistory.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Last Detection, 1 = Result
    val context = LocalContext.current
    
    // Get current route to ensure LaunchedEffect triggers on navigation
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Grant URI permissions for all content URIs in history before loading images
    LaunchedEffect(history) {
        history.forEach { entry ->
            entry.imageUri?.let { uriString ->
                try {
                    val decodedUriString = try {
                        URLDecoder.decode(uriString, StandardCharsets.UTF_8.toString())
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
                            Log.d("DetectionHistoryScreen", "Taken persistable URI permission for: $decodedUriString")
                        } catch (e: SecurityException) {
                            Log.w("DetectionHistoryScreen", "Cannot take persistable permission (may not support it): $decodedUriString - ${e.message}")
                        } catch (e: Exception) {
                            Log.w("DetectionHistoryScreen", "Error taking persistable permission: $decodedUriString - ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w("DetectionHistoryScreen", "Error parsing URI for permission: ${e.message}")
                }
            }
        }
    }
    
    // Mark all detections as read when screen is displayed
    LaunchedEffect(currentRoute) {
        if (currentRoute == "detection_history") {
            dashboardViewModel.markAllDetectionsAsRead()
        }
    }
    
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            val insetsController = WindowCompat.getInsetsController(it, view)
            insetsController.isAppearanceLightStatusBars = true
            @Suppress("DEPRECATION")
            it.statusBarColor = android.graphics.Color.TRANSPARENT
        }
        onDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detection History",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF231C16)
                    )
                },
                actions = {
                    TopBarMenu(navController = navController)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFFFFF),
                    titleContentColor = Color(0xFF231C16)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Segmented Button Row for Tabs
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Color(0xFFF0BD7F)
                    )
                ) {
                    Text("Last Detection")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = Color(0xFFF0BD7F)
                    )
                ) {
                    Text("Result")
                }
            }

            // Content based on selected tab
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (history.isEmpty()) {
                    item {
                        Text(
                            text = "No detection history found.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            color = Color.Gray
                        )
                    }
                } else {
                    if (selectedTab == 0) {
                        // Last Detection Tab - Show Date, Time, and Location only
                        items(history, key = { it.id }) { entry ->
                            DetectionHistoryItemLastDetection(
                                entry = entry,
                                dashboardViewModel = dashboardViewModel,
                                onClick = {
                                    // Navigate to LastDetectionDetailScreen
                                    val entryId = entry.id
                                    val timestamp = entry.timestamp
                                    val location = entry.location?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) } ?: ""
                                    val imageUri = entry.imageUri?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) } ?: ""
                                    val result = URLEncoder.encode(entry.result, StandardCharsets.UTF_8.toString())
                                    val isHealthy = entry.isHealthy
                                    val confidence = entry.confidence

                                    navController.navigate(
                                        "last_detection_detail?entryId=$entryId" +
                                                "&timestamp=$timestamp" +
                                                "&location=$location" +
                                                "&imageUri=$imageUri" +
                                                "&result=$result" +
                                                "&isHealthy=$isHealthy" +
                                                "&confidence=$confidence"
                                    )
                                }
                            )
                        }
                    } else {
                    // Result Tab - White card style with Result and Confidence
                    items(history, key = { it.id }) { entry ->
                        DetectionHistoryItemResult(
                            entry = entry,
                            onClick = {
                                    // Navigate to HistoryResultScreen
                                    val entryId = entry.id
                                    val timestamp = entry.timestamp
                                    val location = entry.location?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) } ?: ""
                                    val imageUri = entry.imageUri?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) } ?: ""
                                    val audioUri = entry.audioUri?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) } ?: ""
                                    val result = URLEncoder.encode(entry.result, StandardCharsets.UTF_8.toString())
                                    val isHealthy = entry.isHealthy
                                    val confidence = entry.confidence
                                    val recommendations = entry.recommendations.joinToString("|") { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) }

                                    navController.navigate(
                                        "history_result_detail?entryId=$entryId" +
                                                "&timestamp=$timestamp" +
                                                "&location=$location" +
                                                "&imageUri=$imageUri" +
                                                "&audioUri=$audioUri" +
                                                "&result=$result" +
                                                "&isHealthy=$isHealthy" +
                                                "&confidence=$confidence" +
                                                "&recommendations=$recommendations"
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopBarMenu(navController: NavController) {
    var showMenu by remember { mutableStateOf(false) }
    
    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = Color(0xFF231C16)
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Favorites") },
                onClick = {
                    showMenu = false
                    navController.navigate("favorites")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorites",
                        tint = Color(0xFFFF6B6B)
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Archives") },
                onClick = {
                    showMenu = false
                    navController.navigate("archives")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = "Archives",
                        tint = Color(0xFF9E9E9E)
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Trash") },
                onClick = {
                    showMenu = false
                    navController.navigate("trash")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Trash",
                        tint = Color.Red
                    )
                }
            )
        }
    }
}

@Composable
fun DetectionHistoryItemLastDetection(
    entry: DetectionEntry,
    dashboardViewModel: DashboardViewModel,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showMoveToTrashDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(enabled = !showMenu) { onClick() },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular image with actual captured or uploaded image, or fallback icon
            val imageUriString = entry.imageUri
            if (!imageUriString.isNullOrEmpty()) {
                // Handle both captured (file://) and uploaded (content://) images
                val decodedUriString = try {
                    URLDecoder.decode(imageUriString, StandardCharsets.UTF_8.toString())
                } catch (_: Exception) {
                    imageUriString
                }
                
                val imageUri = try {
                    decodedUriString.toUri()
                } catch (e: Exception) {
                    android.util.Log.w("DetectionHistoryScreen", "Failed to parse image URI: $decodedUriString - ${e.message}")
                    null
                }
                
                if (imageUri != null) {
                    // Check if URI is file:// (captured) or content:// (uploaded)
                    val uriType = if (imageUri.scheme == "file") "captured" else if (imageUri.scheme == "content") "uploaded" else "unknown"
                    android.util.Log.d("DetectionHistoryScreen", "Loading $uriType image: $decodedUriString")
                    
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Detection Image",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE3B386), CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        onError = { 
                            android.util.Log.w("DetectionHistoryScreen", "Failed to load image ($uriType): $decodedUriString - ${it.result.throwable.message}")
                            // Show camera icon on error
                        },
                        onSuccess = {
                            android.util.Log.d("DetectionHistoryScreen", "Successfully loaded $uriType image: $decodedUriString")
                        }
                    )
                } else {
                    // Fallback: Camera icon if URI parsing fails
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF9D7A5A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "No Image",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            } else {
                // Fallback: Camera icon if no image available
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF9D7A5A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "No Image",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dashboardViewModel.formatDate(entry.timestamp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                
                if (!entry.location.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = entry.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            // MoreVert icon with dropdown menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = Color.Black
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { 
                            Text(if (entry.isFavorite) "Remove from Favorites" else "Add to Favorites")
                        },
                        onClick = {
                            dashboardViewModel.toggleFavorite(entry.id, !entry.isFavorite)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorites",
                                tint = if (entry.isFavorite) Color(0xFFFF6B6B) else Color.Gray
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { 
                            Text(if (entry.isArchived) "Unarchive" else "Archive")
                        },
                        onClick = {
                            dashboardViewModel.toggleArchive(entry.id, !entry.isArchived)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "Archives",
                                tint = if (entry.isArchived) Color(0xFF9E9E9E) else Color.Gray
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Move to trash") },
                        onClick = {
                            showMenu = false
                            showMoveToTrashDialog = true
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Move to trash",
                                tint = Color.Red
                            )
                        }
                    )
                }
            }
        }
    }
    
    // Move to trash confirmation dialog
    if (showMoveToTrashDialog) {
        AlertDialog(
            onDismissRequest = { showMoveToTrashDialog = false },
            title = { Text("Move to Trash") },
            text = { Text("Are you sure you want to move this item to trash?") },
            confirmButton = {
                Button(
                    onClick = {
                        dashboardViewModel.deleteDetection(entry.id)
                        showMoveToTrashDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Move to Trash", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showMoveToTrashDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun DetectionHistoryItemResult(
    entry: DetectionEntry,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular image with border - handles both captured (file://) and uploaded (content://) images
            val imageUriString = entry.imageUri
            if (!imageUriString.isNullOrEmpty()) {
                // Handle both captured (file://) and uploaded (content://) images
                val decodedUriString = try {
                    URLDecoder.decode(imageUriString, StandardCharsets.UTF_8.toString())
                } catch (_: Exception) {
                    imageUriString
                }
                
                val imageUri = try {
                    decodedUriString.toUri()
                } catch (e: Exception) {
                    android.util.Log.w("DetectionHistoryScreen", "Failed to parse image URI: $decodedUriString - ${e.message}")
                    null
                }
                
                if (imageUri != null) {
                    // Check if URI is file:// (captured) or content:// (uploaded)
                    val uriType = if (imageUri.scheme == "file") "captured" else if (imageUri.scheme == "content") "uploaded" else "unknown"
                    android.util.Log.d("DetectionHistoryScreen", "Loading $uriType image: $decodedUriString")
                    
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Detection Image",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE3B386), CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        onError = { 
                            android.util.Log.w("DetectionHistoryScreen", "Failed to load image ($uriType): $decodedUriString - ${it.result.throwable.message}")
                            // Show status icon on error
                        },
                        onSuccess = {
                            android.util.Log.d("DetectionHistoryScreen", "Successfully loaded $uriType image: $decodedUriString")
                        }
                    )
                } else {
                    // Fallback: Status icon if URI parsing fails
                    Icon(
                        imageVector = if (entry.isHealthy) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = "Status Icon",
                        tint = if (entry.isHealthy) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE3B386), CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape)
                    )
                }
            } else {
                // Fallback: Status icon if no image available
                Icon(
                    imageVector = if (entry.isHealthy) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = "Status Icon",
                    tint = if (entry.isHealthy) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE3B386), CircleShape)
                        .padding(2.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Fix corrupted result strings (e.g., missing % or ) characters)
                val sanitizedResult = remember(entry.result, entry.confidence, entry.isHealthy) {
                    sanitizeResultString(entry.result, entry.confidence, entry.isHealthy)
                }
                Text(
                    sanitizedResult,
                    color = if (entry.isHealthy) Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )
                // Show confidence below result
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Confidence: ${(entry.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * Sanitize result string to fix encoding issues or corrupted characters.
 * If the result string appears corrupted (e.g., missing % or ) characters),
 * reconstruct it from the confidence value.
 */
private fun sanitizeResultString(result: String, confidence: Float, isHealthy: Boolean): String {
    // First, try to decode if it might be URL encoded
    val decodedResult = try {
        URLDecoder.decode(result, StandardCharsets.UTF_8.toString())
    } catch (_: Exception) {
        result // Use original if decoding fails
    }
    
    // Check if the string is corrupted (contains replacement characters or missing %/))
    val isCorrupted = decodedResult.contains('?') || 
                      (decodedResult.contains("(") && !decodedResult.contains("%")) ||
                      (decodedResult.contains("%") && !decodedResult.contains(")")) ||
                      (decodedResult.contains("(") && decodedResult.contains(")") && !decodedResult.contains("%"))
    
    if (isCorrupted) {
        // Reconstruct the result string from confidence
        val status = if (isHealthy) "Healthy" else "Infected"
        val confidencePercent = (confidence * 100.0).let { 
            if (it == it.roundToInt().toDouble()) {
                it.roundToInt().toString()
            } else {
                String.format(Locale.US, "%.1f", it)
            }
        }
        return "$status ($confidencePercent%)"
    }
    
    // If the string ends correctly but might have encoding issues, try to fix common problems
    return decodedResult
        .replace("??", "%") // Fix double question marks
        .replace(Regex("\\(([\\d.]+)\\?"), "($1%") // Fix pattern like (100.0? -> (100.0%
        .replace(Regex("\\(([\\d.]+)%\\?"), "($1%)") // Fix pattern like (100.0%? -> (100.0%)
        .replace(Regex("\\(([\\d.]+)[^%)]"), "($1%)") // Fix incomplete endings
        .let { fixed ->
            // Ensure it ends with %) if it contains (
            if (fixed.contains("(") && !fixed.endsWith(")") && !fixed.endsWith("%")) {
                "$fixed%)"
            } else if (fixed.contains("(") && fixed.endsWith("%") && !fixed.endsWith("%)")) {
                "$fixed)"
            } else {
                fixed
            }
        }
}
