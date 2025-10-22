package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.data.DashboardUiState // <-- IMPORT THE NEW DATA CLASS
import com.bisu.chickcare.backend.repository.DetectionEntry
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// --- Using constants for better maintainability ---
private object Dimens {
    val PaddingSmall = 4.dp
    val PaddingMedium = 8.dp
    val PaddingLarge = 16.dp
    val CardElevation = 8.dp
    val BottomBarHeight = 80.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val dashboardViewModel: DashboardViewModel = viewModel()

    // --- Observe a single UI state object from the ViewModel ---
    val uiState by dashboardViewModel.uiState.collectAsState()
    val notificationCount by dashboardViewModel.newNotificationCount.collectAsState()
    val detectionHistory by dashboardViewModel.detectionHistory.collectAsState()

    var expandedDropdown by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DashboardTopAppBar(
                userName = uiState.userName,
                notificationCount = notificationCount,
                expanded = expandedDropdown,
                onExpandedChange = { expandedDropdown = it },
                onNotificationsClicked = { navController.navigate("notifications") }
            )
        },
        bottomBar = {
            CustomTabBar(navController = navController, bottomBarHeight = Dimens.BottomBarHeight)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Image(
                painter = painterResource(id = R.drawable.background_app),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp)
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFD27D2D))
                }
            } else {
                DashboardContent(
                    uiState = uiState,
                    detectionHistory = detectionHistory,
                    onScanNowClicked = { dashboardViewModel.onScanNowClicked(null, null) },
                    onViewHistoryClicked = { navController.navigate("detection_history") },
                    onHistoryItemClicked = { entry ->
                        val imageUri = entry.imageUri?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) } ?: ""
                        val audioUri = entry.audioUri?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) } ?: ""
                        navController.navigate("detection_result?imageUri=$imageUri&audioUri=$audioUri")
                    }
                )
            }
        }

        // Show loading overlay when detecting
        if (uiState.isDetecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFFD27D2D))
                        Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
                        Text("Analyzing chicken health...", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

// --- Extracted Composables ---

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    detectionHistory: List<DetectionEntry>,
    onScanNowClicked: () -> Unit,
    onViewHistoryClicked: () -> Unit,
    onHistoryItemClicked: (DetectionEntry) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge),
        contentPadding = PaddingValues(top = Dimens.PaddingLarge, bottom = Dimens.BottomBarHeight)
    ) {
        item { ChickenGalleryCard() }
        item { StatsSummaryCard(totalChickens = uiState.totalChickens, alerts = uiState.alerts) }
        item {
            HealthDetectionCard(
                isDetecting = uiState.isDetecting,
                detectionResult = uiState.detectionResult,
                remedySuggestions = uiState.remedySuggestions,
                onScanNowClicked = onScanNowClicked
            )
        }
        item {
            DetectionHistoryCard(
                detectionHistory = detectionHistory,
                onViewAllClicked = onViewHistoryClicked,
                onItemClicked = onHistoryItemClicked
            )
        }
        item { FarmTipsCard() }
        item { WeatherUpdateCard() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopAppBar(
    userName: String,
    notificationCount: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onNotificationsClicked: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "For you, $userName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { onExpandedChange(true) }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Filter")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
                    DropdownMenuItem(text = { Text("All") }, onClick = { onExpandedChange(false) })
                    DropdownMenuItem(text = { Text("Health Scans") }, onClick = { onExpandedChange(false) })
                    DropdownMenuItem(text = { Text("Announcements") }, onClick = { onExpandedChange(false) })
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = { /* TODO: Open drawer */ }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            BadgedBox(
                badge = {
                    if (notificationCount > 0) {
                        Badge(containerColor = Color.Red) { Text("$notificationCount") }
                    }
                }
            ) {
                IconButton(onClick = onNotificationsClicked) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFFD2B48C).copy(alpha = 0.9f),
            titleContentColor = Color(0xFF8B4513)
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChickenGalleryCard() {
    val images = remember {
        listOf(
            R.drawable.chicken_background_1,
            R.drawable.chicken_background_2,
            R.drawable.chicken_background_3,
            R.drawable.chicken_background_4
        )
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingLarge),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingSmall)
        ) {
            items(images, key = { it }) { imageRes ->
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = "Chicken Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillParentMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StatsSummaryCard(totalChickens: Int, alerts: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingLarge),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(title = "Total Chickens", value = totalChickens.toString(), modifier = Modifier.weight(1f))
        StatCard(title = "Alerts", value = alerts.toString(), modifier = Modifier.weight(1f), valueColor = if (alerts > 0) Color.Red else Color.Green)
    }
}

@Composable
private fun HealthDetectionCard(
    isDetecting: Boolean,
    detectionResult: Pair<Boolean, String>?,
    remedySuggestions: List<String>,
    onScanNowClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingLarge),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevation),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(Dimens.PaddingLarge)) {
            Text("Health Detection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
            Button(
                onClick = onScanNowClicked,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isDetecting
            ) {
                Text("Scan Now")
            }
            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
            detectionResult?.let { (isInfected, status) ->
                Text(
                    text = "Result: $status",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isInfected) Color.Red else Color.Green
                )
                if (remedySuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
                    Text("Remedy Suggestions:", style = MaterialTheme.typography.titleSmall)
                    remedySuggestions.forEach { suggestion ->
                        Text("• $suggestion", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun DetectionHistoryCard(
    detectionHistory: List<DetectionEntry>,
    onViewAllClicked: () -> Unit,
    onItemClicked: (DetectionEntry) -> Unit
) {
    val dashboardViewModel: DashboardViewModel = viewModel()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingLarge),
        elevation = CardDefaults.cardElevation(Dimens.CardElevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(Dimens.PaddingLarge)) {
            Text("Recent Detections", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            if (detectionHistory.isEmpty()) {
                Text(
                    "No recent detections found.",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center
                )
            } else {
                detectionHistory.take(3).forEach { entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(vertical = Dimens.PaddingSmall)
                            .clickable { onItemClicked(entry) }
                    ) {
                        Icon(
                            imageVector = if (entry.isHealthy) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = "Status",
                            tint = if (entry.isHealthy) Color.Green else Color.Red
                        )
                        Spacer(modifier = Modifier.width(Dimens.PaddingMedium))
                        Text(dashboardViewModel.formatDate(entry.timestamp), modifier = Modifier.weight(1f))
                        Text(entry.result, fontWeight = FontWeight.Bold)
                    }
                }
                if (detectionHistory.size > 3) {
                    Text(
                        text = "View all...",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = Dimens.PaddingMedium)
                            .align(Alignment.End)
                            .clickable { onViewAllClicked() }
                    )
                }
            }
        }
    }
}

@Composable
private fun FarmTipsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingLarge),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevation),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(Dimens.PaddingLarge)) {
            Text("Farm Tips", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
            Text("1. Ensure proper ventilation to prevent respiratory issues.", style = MaterialTheme.typography.bodyMedium)
            Text("2. Provide clean water daily to maintain chicken health.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun WeatherUpdateCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingLarge),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevation),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(Dimens.PaddingLarge)) {
            Text("Weather Update", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
            Text("Current: 25°C, Sunny", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun CustomTabBar(navController: NavController, bottomBarHeight: androidx.compose.ui.unit.Dp) {
    val dashboardViewModel: DashboardViewModel = viewModel()
    val newHistoryCount by dashboardViewModel.newHistoryCount.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val tabs = listOf(
        TabItem("dashboard", Icons.Default.Home, "Home"),
        TabItem("detection_history", Icons.Default.Search, "Detection"),
        TabItem("action_tools", Icons.Default.Build, "Action"),
        TabItem("help_center", Icons.AutoMirrored.Filled.Help, "Help"),
        TabItem("profile", Icons.Default.Person, "Profile")
    )

    val iconAndTextColor = Color(0xFF26201C)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(bottomBarHeight)
            .background(Color(0xFFD2B48C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = Dimens.PaddingMedium),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = currentRoute == tab.route
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                ) {
                    BadgedBox(
                        badge = {
                            if (tab.route == "detection_history" && newHistoryCount > 0) {
                                Badge(containerColor = Color.Red) { Text("$newHistoryCount") }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = iconAndTextColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(Dimens.PaddingSmall))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = iconAndTextColor
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .width(24.dp)
                                .height(2.dp)
                                .background(iconAndTextColor, RoundedCornerShape(1.dp))
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Color.Unspecified) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevation)
    ) {
        Column(
            modifier = Modifier.padding(Dimens.PaddingLarge).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}
