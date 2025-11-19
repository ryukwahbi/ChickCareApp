package com.bisu.chickcare.frontend.screen

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bisu.chickcare.backend.repository.DetectionEntry
import com.bisu.chickcare.backend.service.NetworkConnectivityHelper
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.bisu.chickcare.frontend.components.OfflineIndicator
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import com.bisu.chickcare.frontend.utils.sanitizeToUri
import com.bisu.chickcare.frontend.utils.sanitizeUriString
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// Filter enums - must be defined before use
enum class HealthStatusFilter {
    HEALTHY, UNHEALTHY
}

enum class DetectionTypeFilter {
    IMAGE_ONLY, AUDIO_ONLY, BOTH
}

@Composable
fun TopBarMenu(navController: NavController) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = ThemeColorUtils.darkGray(Color(0xFF231C16))
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(ThemeColorUtils.white())
        ) {
            DropdownMenuItem(
                text = { Text("Favorites", color = ThemeColorUtils.black()) },
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
            HorizontalDivider(
                color = ThemeColorUtils.black(),
                thickness = 1.dp
            )
            DropdownMenuItem(
                text = { Text("Archives", color = ThemeColorUtils.black()) },
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
            HorizontalDivider(
                color = ThemeColorUtils.black(),
                thickness = 1.dp
            )
            DropdownMenuItem(
                text = { Text("Trash", color = ThemeColorUtils.black()) },
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
fun FilterPanel(
    selectedHealthStatus: HealthStatusFilter?,
    onHealthStatusChanged: (HealthStatusFilter?) -> Unit,
    filterByType: DetectionTypeFilter?,
    onTypeFilterChanged: (DetectionTypeFilter?) -> Unit,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        border = if (ThemeViewModel.isDarkMode) {
            BorderStroke(width = 1.dp, color = Color.White)
        } else {
            null
        },
        colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Health Status Filter
            Text(
                text = "Health Status",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedHealthStatus == HealthStatusFilter.HEALTHY,
                    onClick = {
                        onHealthStatusChanged(
                            if (selectedHealthStatus == HealthStatusFilter.HEALTHY) null
                            else HealthStatusFilter.HEALTHY
                        )
                    },
                    label = { Text("Healthy") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                        selectedLabelColor = Color.Black
                    )
                )
                FilterChip(
                    selected = selectedHealthStatus == HealthStatusFilter.UNHEALTHY,
                    onClick = {
                        onHealthStatusChanged(
                            if (selectedHealthStatus == HealthStatusFilter.UNHEALTHY) null else HealthStatusFilter.UNHEALTHY
                        )
                    },
                    label = { Text("Unhealthy") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFF44336).copy(alpha = 0.2f),
                        selectedLabelColor = Color.Black
                    )
                )
            }

            // Detection Type Filter
            Text(
                text = "Detection Type",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = filterByType == DetectionTypeFilter.IMAGE_ONLY,
                    onClick = {
                        onTypeFilterChanged(
                            if (filterByType == DetectionTypeFilter.IMAGE_ONLY) null
                            else DetectionTypeFilter.IMAGE_ONLY
                        )
                    },
                    label = { Text("Image Only") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2196F3).copy(alpha = 0.2f),
                        selectedLabelColor = Color.Black
                    )
                )
                FilterChip(
                    selected = filterByType == DetectionTypeFilter.AUDIO_ONLY,
                    onClick = {
                        onTypeFilterChanged(
                            if (filterByType == DetectionTypeFilter.AUDIO_ONLY) null
                            else DetectionTypeFilter.AUDIO_ONLY
                        )
                    },
                    label = { Text("Audio Only") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF9C27B0).copy(alpha = 0.2f),
                        selectedLabelColor = Color.Black
                    )
                )
                FilterChip(
                    selected = filterByType == DetectionTypeFilter.BOTH,
                    onClick = {
                        onTypeFilterChanged(
                            if (filterByType == DetectionTypeFilter.BOTH) null
                            else DetectionTypeFilter.BOTH
                        )
                    },
                    label = { Text("Both") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF9800).copy(alpha = 0.2f),
                        selectedLabelColor = Color.Black
                    )
                )
            }

            // Clear filters button
            Button(
                onClick = onClearFilters,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeColorUtils.lightGray(Color.Gray)
                )
            ) {
                Text(
                    "Clear All Filters",
                    color = if (ThemeViewModel.isDarkMode) Color.White else Color.Unspecified
                )
            }
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
        border = if (ThemeViewModel.isDarkMode) {
            BorderStroke(width = 1.dp, color = Color.White)
        } else {
            null
        },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White))
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
                val context = LocalContext.current
                val sanitizedString =
                    sanitizeUriString(imageUriString, "DetectionHistoryScreen")
                val imageUri = sanitizeToUri(imageUriString, "DetectionHistoryScreen")
                if (sanitizedString != null && imageUri != null) {
                    val uriType = when (imageUri.scheme) {
                        "file" -> "captured"
                        "content" -> "uploaded"
                        else -> "unknown"
                    }
                    Log.d("DetectionHistoryScreen", "Loading $uriType image: $sanitizedString")

                    val imageRequest = ImageRequest.Builder(context)
                        .data(imageUri)
                        .crossfade(true)
                        .build()

                    AsyncImage(
                        model = imageRequest,
                        contentDescription = "Detection Image",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ThemeColorUtils.beige(Color(0xFFE3B386)), CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        onError = {
                            Log.w(
                                "DetectionHistoryScreen",
                                "Failed to load image ($uriType): $sanitizedString - ${it.result.throwable.message}"
                            )
                        },
                        onSuccess = {
                            Log.d(
                                "DetectionHistoryScreen",
                                "Successfully loaded $uriType image: $sanitizedString"
                            )
                        }
                    )
                } else {
                    Log.w("DetectionHistoryScreen", "Image URI invalid: $imageUriString")
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
                            tint = ThemeColorUtils.white(),
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
                        tint = ThemeColorUtils.white(),
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
                    color = ThemeColorUtils.black()
                )

                if (!entry.location.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            modifier = Modifier.size(16.dp),
                            tint = ThemeColorUtils.lightGray(Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = entry.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeColorUtils.lightGray(Color.Gray)
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
                        tint = ThemeColorUtils.black()
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(ThemeColorUtils.white())
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (entry.isFavorite) "Remove from Favorites" else "Add to Favorites",
                                color = ThemeColorUtils.black()
                            )
                        },
                        onClick = {
                            dashboardViewModel.toggleFavorite(entry.id, !entry.isFavorite)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorites",
                                tint = if (entry.isFavorite) Color(0xFFFF6B6B) else Color(0xFFFF6B6B).copy(alpha = 0.6f)
                            )
                        }
                    )
                    HorizontalDivider(
                        color = ThemeColorUtils.lightGray(Color(0xFF7E7C7C)),
                        thickness = 1.dp
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (entry.isArchived) "Unarchive" else "Archive",
                                color = ThemeColorUtils.black()
                            )
                        },
                        onClick = {
                            dashboardViewModel.toggleArchive(entry.id, !entry.isArchived)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "Archives",
                                tint = if (entry.isArchived) Color(0xFF9E9E9E) else Color(0xFF9E9E9E).copy(alpha = 0.7f)
                            )
                        }
                    )
                    HorizontalDivider(
                        color = ThemeColorUtils.lightGray(Color(0xFF7E7C7C)),
                        thickness = 1.dp
                    )
                    DropdownMenuItem(
                        text = { Text("Move to trash", color = ThemeColorUtils.black()) },
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
                    Text("Move to Trash", color = ThemeColorUtils.white())
                }
            },
            dismissButton = {
                Button(
                    onClick = { showMoveToTrashDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemeColorUtils.lightGray(
                            Color.Gray
                        )
                    )
                ) {
                    Text("Cancel", color = ThemeColorUtils.white())
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
        border = if (ThemeViewModel.isDarkMode) {
            BorderStroke(width = 1.dp, color = Color.White)
        } else {
            null
        },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageUriString = entry.imageUri
            val context = LocalContext.current
            if (!imageUriString.isNullOrEmpty()) {
                val sanitizedString =
                    sanitizeUriString(imageUriString, "DetectionHistoryScreen")
                val imageUri = sanitizeToUri(imageUriString, "DetectionHistoryScreen")
                if (sanitizedString != null && imageUri != null) {
                    val uriType = when (imageUri.scheme) {
                        "file" -> "captured"
                        "content" -> "uploaded"
                        else -> "unknown"
                    }
                    Log.d("DetectionHistoryScreen", "Loading $uriType image: $sanitizedString")

                    val imageRequest = ImageRequest.Builder(context)
                        .data(imageUri)
                        .crossfade(true)
                        .build()

                    AsyncImage(
                        model = imageRequest,
                        contentDescription = "Detection Image",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ThemeColorUtils.beige(Color(0xFFE3B386)), CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        onError = {
                            Log.w(
                                "DetectionHistoryScreen",
                                "Failed to load image ($uriType): $sanitizedString - ${it.result.throwable.message}"
                            )
                        },
                        onSuccess = {
                            Log.d(
                                "DetectionHistoryScreen",
                                "Successfully loaded $uriType image: $sanitizedString"
                            )
                        }
                    )
                } else {
                    Log.w(
                        "DetectionHistoryScreen",
                        "Image URI invalid for result card: $imageUriString"
                    )
                    Icon(
                        imageVector = if (entry.isHealthy) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = "Status Icon",
                        tint = if (entry.isHealthy) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(ThemeColorUtils.beige(Color(0xFFE3B386)), CircleShape)
                            .padding(2.dp)
                            .clip(CircleShape)
                    )
                }
            } else {
                Icon(
                    imageVector = if (entry.isHealthy) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = "Status Icon",
                    tint = if (entry.isHealthy) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(ThemeColorUtils.beige(Color(0xFFE3B386)), CircleShape)
                        .padding(2.dp)
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Decide displayed status with threshold (Unknown below 60%)
                val statusText = remember(entry.confidence, entry.isHealthy, entry.result) {
                    com.bisu.chickcare.frontend.utils.DetectionDisplayUtils.statusText(entry.isHealthy, entry.confidence)
                }
                val statusColor = remember(entry.confidence, entry.isHealthy) {
                    com.bisu.chickcare.frontend.utils.DetectionDisplayUtils.statusColor(entry.isHealthy, entry.confidence)
                }
                Text(
                    statusText,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )
                // Show confidence below result
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Confidence: ${(entry.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.darkGray(Color(0xFF666666)) else ThemeColorUtils.lightGray(Color.Gray)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionHistoryScreen(navController: NavController, paddingValues: PaddingValues) {
    val dashboardViewModel: DashboardViewModel = viewModel()
    val history by dashboardViewModel.detectionHistory.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val route = navBackStackEntry?.destination?.route
    var selectedTab by remember { mutableIntStateOf(0) }

    // Check if we're coming back from HistoryResultScreen and restore Result tab
    LaunchedEffect(route, navBackStackEntry) {
        if (route == "detection_history") {
            val savedState = navBackStackEntry?.savedStateHandle
            val shouldShowResultTab = savedState?.get<Boolean>("showResultTab") ?: false
            if (shouldShowResultTab) {
                selectedTab = 1
                savedState.remove<Boolean>("showResultTab")
            }
        }
    }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchQuery by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var selectedHealthStatus by remember { mutableStateOf<HealthStatusFilter?>(null) }
    var minConfidence by remember { mutableFloatStateOf(0f) }
    var maxConfidence by remember { mutableFloatStateOf(100f) }
    var filterByType by remember { mutableStateOf<DetectionTypeFilter?>(null) }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    val isOnline by NetworkConnectivityHelper.connectivityFlow(context)
        .collectAsState(initial = NetworkConnectivityHelper.isOnline(context))
    val isOffline = !isOnline
    val filteredHistory = remember(
        history,
        searchQuery,
        selectedHealthStatus,
        minConfidence,
        maxConfidence,
        filterByType,
        startDate,
        endDate
    ) {
        history.filter { entry ->
            val matchesSearch = searchQuery.isEmpty() ||
                    entry.result.contains(searchQuery, ignoreCase = true) ||
                    entry.location?.contains(searchQuery, ignoreCase = true) == true

            val matchesHealthStatus =
                selectedHealthStatus == null || when (selectedHealthStatus) {
                    HealthStatusFilter.HEALTHY -> entry.isHealthy
                    HealthStatusFilter.UNHEALTHY -> !entry.isHealthy
                    else -> true
                }

            // Confidence filter
            val confidencePercent = entry.confidence * 100f
            val matchesConfidence =
                confidencePercent >= minConfidence && confidencePercent <= maxConfidence

            // Detection type filter
            val matchesType = filterByType == null || when (filterByType) {
                DetectionTypeFilter.IMAGE_ONLY -> !entry.imageUri.isNullOrEmpty() && entry.audioUri.isNullOrEmpty()
                DetectionTypeFilter.AUDIO_ONLY -> entry.imageUri.isNullOrEmpty() && !entry.audioUri.isNullOrEmpty()
                DetectionTypeFilter.BOTH -> !entry.imageUri.isNullOrEmpty() && !entry.audioUri.isNullOrEmpty()
                else -> true
            }

            // Date range filter
            val matchesDateRange = (startDate == null || entry.timestamp >= startDate!!) &&
                    (endDate == null || entry.timestamp <= endDate!!)

            matchesSearch && matchesHealthStatus && matchesConfidence && matchesType && matchesDateRange
        }
    }

    // Get current route to ensure LaunchedEffect triggers on navigation
    val currentRoute = navBackStackEntry?.destination?.route

    // Grant URI permissions for all content URIs in history before loading images
    LaunchedEffect(history) {
        history.forEach { entry ->
            val sanitizedString = sanitizeUriString(entry.imageUri, "DetectionHistoryScreen")
            val uri = sanitizeToUri(entry.imageUri, "DetectionHistoryScreen")
            if (sanitizedString != null && uri?.scheme == "content") {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    Log.d(
                        "DetectionHistoryScreen",
                        "Taken persistable URI permission for: $sanitizedString"
                    )
                } catch (e: SecurityException) {
                    Log.w(
                        "DetectionHistoryScreen",
                        "Cannot take persistable permission (may not support it): $sanitizedString - ${e.message}"
                    )
                } catch (e: Exception) {
                    Log.w(
                        "DetectionHistoryScreen",
                        "Error taking persistable permission: $sanitizedString - ${e.message}"
                    )
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
                            color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                        )
                    },
                    actions = {
                        TopBarMenu(navController = navController)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ThemeColorUtils.white(),
                        titleContentColor = ThemeColorUtils.darkGray(Color(0xFF231C16))
                    ),
                    modifier = Modifier.clickable {
                        // Dismiss keyboard when clicking on top bar
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
                    .padding(paddingValues)
                    .clickable {
                        // Dismiss keyboard when clicking outside input box
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Offline indicator
                    OfflineIndicator(
                        isOffline = isOffline,
                        modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            placeholder = { 
                                Text(
                                    "Search by result or location...",
                                    color = if (ThemeViewModel.isDarkMode) Color(0xFF8D94A0) else Color.Unspecified
                                ) 
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    }) {
                                        Icon(Icons.Default.Cancel, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                unfocusedBorderColor = Color.Black,
                                focusedBorderColor = Color.Black,
                                focusedLeadingIconColor = Color.Black,
                                unfocusedLeadingIconColor = Color.Black,
                                unfocusedPlaceholderColor = if (ThemeViewModel.isDarkMode) Color(0xFF8D94A0) else Color.Unspecified,
                                focusedPlaceholderColor = if (ThemeViewModel.isDarkMode) Color(0xFF8D94A0) else Color.Unspecified
                            )
                        )

                        // Filter chips row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clickable { showFilters = !showFilters }
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (showFilters) Color(0xFFF0BD7F) else Color.White,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        Color.Black,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.FilterList,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = Color.Black
                                    )
                                    Text(
                                        "Filters",
                                        color = Color.Black,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                            if (selectedHealthStatus != null || filterByType != null ||
                                minConfidence > 0f || maxConfidence < 100f ||
                                startDate != null || endDate != null
                            ) {
                                Text(
                                    text = "${filteredHistory.size} results",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThemeColorUtils.lightGray(Color.Gray),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }

                        // Filter panel
                        if (showFilters) {
                            FilterPanel(
                                selectedHealthStatus = selectedHealthStatus,
                                onHealthStatusChanged = { selectedHealthStatus = it },
                                filterByType = filterByType,
                                onTypeFilterChanged = { filterByType = it },
                                onClearFilters = {
                                    selectedHealthStatus = null
                                    minConfidence = 0f
                                    maxConfidence = 100f
                                    filterByType = null
                                    startDate = null
                                    endDate = null
                                }
                            )
                        }

                        // Segmented Button Row for Tabs
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
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
                            if (filteredHistory.isEmpty() && history.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "No detections match your filters.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.white() else ThemeColorUtils.lightGray(Color.Gray)
                                    )
                                }
                            } else if (history.isEmpty()) {
                                item {
                                    Text(
                                        text = "No detection history found.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.white() else ThemeColorUtils.lightGray(Color.Gray)
                                    )
                                }
                            } else {
                                if (selectedTab == 0) {
                                    // Last Detection Tab - Show Date, Time, and Location only
                                    items(filteredHistory, key = { it.id }) { entry ->
                                        DetectionHistoryItemLastDetection(
                                            entry = entry,
                                            dashboardViewModel = dashboardViewModel,
                                            onClick = {
                                                // Navigate to LastDetectionDetailScreen
                                                val entryId = entry.id
                                                val timestamp = entry.timestamp
                                                val location = entry.location?.let {
                                                    URLEncoder.encode(
                                                        it,
                                                        StandardCharsets.UTF_8.toString()
                                                    )
                                                } ?: ""
                                                val imageUri = entry.imageUri?.let {
                                                    URLEncoder.encode(
                                                        it,
                                                        StandardCharsets.UTF_8.toString()
                                                    )
                                                } ?: ""
                                                val result = URLEncoder.encode(
                                                    entry.result,
                                                    StandardCharsets.UTF_8.toString()
                                                )
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
                                    items(filteredHistory, key = { it.id }) { entry ->
                                        DetectionHistoryItemResult(
                                            entry = entry,
                                            onClick = {
                                                // Save that we're navigating from Result tab
                                                // This will be used to restore the Result tab when coming back
                                                navBackStackEntry?.savedStateHandle?.set(
                                                    "showResultTab",
                                                    true
                                                )

                                                // Navigate to HistoryResultScreen
                                                val entryId = entry.id
                                                val timestamp = entry.timestamp
                                                val location = entry.location?.let {
                                                    URLEncoder.encode(
                                                        it,
                                                        StandardCharsets.UTF_8.toString()
                                                    )
                                                } ?: ""
                                                val imageUri = entry.imageUri?.let {
                                                    URLEncoder.encode(
                                                        it,
                                                        StandardCharsets.UTF_8.toString()
                                                    )
                                                } ?: ""
                                                val audioUri = entry.audioUri?.let {
                                                    URLEncoder.encode(
                                                        it,
                                                        StandardCharsets.UTF_8.toString()
                                                    )
                                                } ?: ""
                                                val result = URLEncoder.encode(
                                                    entry.result,
                                                    StandardCharsets.UTF_8.toString()
                                                )
                                                val isHealthy = entry.isHealthy
                                                val confidence = entry.confidence
                                                val recommendations =
                                                    entry.recommendations.joinToString("|") {
                                                        URLEncoder.encode(
                                                            it,
                                                            StandardCharsets.UTF_8.toString()
                                                        )
                                                    }

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
        }
    }
