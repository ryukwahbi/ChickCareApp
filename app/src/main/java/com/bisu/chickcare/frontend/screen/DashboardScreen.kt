package com.bisu.chickcare.frontend.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.data.DashboardUiState
import com.bisu.chickcare.backend.repository.DetectionEntry
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel
import com.bisu.chickcare.backend.viewmodels.WeatherViewModel
import com.bisu.chickcare.frontend.components.HealthTrendLineGraph
import com.bisu.chickcare.frontend.components.HealthyRatePieChart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Update active status when screen is displayed
    LaunchedEffect(Unit) {
        dashboardViewModel.updateActiveStatus()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(navController, drawerState, scope)
        }
    ) {
        Scaffold(
            topBar = {
                DashboardTopAppBar(
                    notificationCount = notificationCount,
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = it },
                    onDrawerClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    },
                    onNotificationsClicked = { navController.navigate("notifications") }
                )
            },
            bottomBar = {
                CustomTabBar(
                    navController = navController,
                    bottomBarHeight = Dimens.BottomBarHeight
                )
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
                    DashboardScreenContent(
                        uiState = uiState,
                        detectionHistory = detectionHistory,
                        navController = navController,
                        dashboardViewModel = dashboardViewModel,
                        onViewHistoryClicked = { navController.navigate("detection_history") },
                        onHistoryItemClicked = { entry: DetectionEntry ->
                            val imageUri: String = entry.imageUri?.let { uri: String ->
                                URLEncoder.encode(
                                    uri,
                                    StandardCharsets.UTF_8.toString()
                                )
                            } ?: ""
                            val audioUri: String = entry.audioUri?.let { uri: String ->
                                URLEncoder.encode(
                                    uri,
                                    StandardCharsets.UTF_8.toString()
                                )
                            } ?: ""
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
                            Text(
                                "Analyzing chicken health...",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DashboardScreenContent(
    uiState: DashboardUiState,
    detectionHistory: List<DetectionEntry>,
    navController: NavController,
    dashboardViewModel: DashboardViewModel,
    onViewHistoryClicked: () -> Unit,
    onHistoryItemClicked: (DetectionEntry) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge),
        contentPadding = PaddingValues(
            top = Dimens.PaddingLarge,
            bottom = Dimens.BottomBarHeight
        )
    ) {
        item { ChickenGalleryCard() }
        item {
            // Auto-rollover trigger at local midnight
            var midnightTick by rememberSaveable { mutableLongStateOf(0L) }
            LaunchedEffect(midnightTick) {

                val now = System.currentTimeMillis()
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = now
                cal.set(java.util.Calendar.HOUR_OF_DAY, 24)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val delayMs = cal.timeInMillis - now
                delay(delayMs)
                midnightTick = System.currentTimeMillis()
            }

            // Compute today's detections from history timestamps (recomputed on midnightTick change)
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            val todaysDetections = detectionHistory.count { it.timestamp >= startOfDay }

            StatsSummaryCard(
                totalChickens = todaysDetections, 
                totalDetections = uiState.totalDetections,
                healthyRate = uiState.healthyRate,
                imageDetections = uiState.imageDetections,
                audioDetections = uiState.audioDetections
            )
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = Dimens.PaddingLarge),
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
            ) {
                HealthTrendLineGraph(
                    title = "Image Detection Trends",
                    dataPoints = uiState.imageTrendData
                )
                HealthTrendLineGraph(
                    title = "Audio Detection Trends",
                    dataPoints = uiState.audioTrendData
                )
            }
        }
        item {
            DetectionHistoryCard(
                detectionHistory = detectionHistory,
                onViewAllClicked = onViewHistoryClicked,
                onItemClicked = onHistoryItemClicked,
                dashboardViewModel = dashboardViewModel
            )
        }
        item { FarmTipsCard(navController = navController) }
        item { WeatherUpdateCard() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopAppBar(
    notificationCount: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDrawerClick: () -> Unit,
    onNotificationsClicked: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "For you",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF000000)
                )
                IconButton(
                    onClick = { onExpandedChange(true) },
                    modifier = Modifier.offset(x = (-8).dp)
                ) {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Filter",
                        tint = Color(0xFF000000)
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) }) {
                    DropdownMenuItem(
                        text = { Text("Announcements") },
                        onClick = { onExpandedChange(false) })
                    DropdownMenuItem(
                        text = { Text("Tips & Tricks") },
                        onClick = { onExpandedChange(false) })
                    DropdownMenuItem(
                        text = { Text("Quick Actions") },
                        onClick = { onExpandedChange(false) })
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onDrawerClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color(0xFFDA8041))
            }
        },
        actions = {
            BadgedBox(
                badge = {
                    if (notificationCount > 0) {
                        Badge(
                            containerColor = Color.Red
                        ) { 
                            Text(
                                if (notificationCount > 10) "10+" else "$notificationCount",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                modifier = Modifier
                    .offset(x = (-11).dp, y = (4).dp)
            ) {
                IconButton(
                    onClick = onNotificationsClicked,
                    modifier = Modifier
                        .background(Color(0xFFE0E0E0), CircleShape)
                        .size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Notifications, 
                        contentDescription = "Notifications", 
                        tint = Color(0xFF000000),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            titleContentColor = Color(0xFF8B4513)
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChickenGalleryCard() {
    val images = remember {
        listOf(
            R.drawable.chicken_background_1,
            R.drawable.chicken_background_2,
            R.drawable.chicken_background_3,
            R.drawable.chicken_background_4
        )
    }
    
    var currentIndex by remember { mutableIntStateOf(0) }
    var slideDirection by remember { mutableIntStateOf(1) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingLarge),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            // Animated image transition
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth * slideDirection },
                        animationSpec = tween(durationMillis = 300)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth * slideDirection },
                        animationSpec = tween(durationMillis = 300)
                    )
                },
                label = "image_carousel"
            ) { index ->
                Image(
                    painter = painterResource(id = images[index]),
                    contentDescription = "Chicken Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Left navigation arrow (Previous)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            ) {
                IconButton(
                    onClick = {
                        val newIndex = if (currentIndex > 0) currentIndex - 1 else images.size - 1
                        // Determine direction based on actual movement (handle wrap-around)
                        slideDirection = if (newIndex < currentIndex) -1 else 1
                        currentIndex = newIndex
                    },
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            // Right navigation arrow (Next)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            ) {
                IconButton(
                    onClick = {
                        val newIndex = if (currentIndex < images.size - 1) currentIndex + 1 else 0
                        // Determine direction based on actual movement (handle wrap-around)
                        slideDirection = if (newIndex > currentIndex) 1 else -1
                        currentIndex = newIndex
                    },
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatsSummaryCard(
    totalChickens: Int,
    totalDetections: Int,
    healthyRate: Double,
    imageDetections: Int,
    audioDetections: Int
) {
    Column(
        modifier = Modifier.padding(horizontal = Dimens.PaddingLarge),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Today's Detections",
                value = totalChickens.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Total Detections",
                value = totalDetections.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Image Scans",
                value = imageDetections.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Audio Scans",
                value = audioDetections.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        // One card with two charts: Healthy (green) and Unhealthy (red)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // First chart: Healthy only (GREEN)
                HealthyRatePieChart(
                    healthyRate = healthyRate,
                    chartType = com.bisu.chickcare.frontend.components.ChartType.HEALTHY_ONLY,
                    modifier = Modifier.weight(1f)
                )
                
                // Second chart: Unhealthy only (RED)
                HealthyRatePieChart(
                    healthyRate = healthyRate,
                    chartType = com.bisu.chickcare.frontend.components.ChartType.UNHEALTHY_ONLY,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun DetectionHistoryCard(
    detectionHistory: List<DetectionEntry>,
    onViewAllClicked: () -> Unit,
    onItemClicked: (DetectionEntry) -> Unit,
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingLarge),
        elevation = CardDefaults.cardElevation(Dimens.CardElevation),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
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
                        Text(
                            dashboardViewModel.formatDate(entry.timestamp),
                            modifier = Modifier.weight(1f)
                        )
                        Text(entry.result, fontWeight = FontWeight.Bold)
                    }
                }
                if (detectionHistory.size > 3) {
                    Text(
                        text = "View all...",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium,
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
fun FarmTipsCard(navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingLarge),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevation),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(Dimens.PaddingLarge)) {
            Text(
                "Farm Tips",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
            
            // Tips list (non-scrollable, shows first few tips)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "1. Ensure proper ventilation to prevent respiratory issues.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "2. Provide clean water daily to maintain chicken health.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "3. Monitor chicken behavior daily using image and audio detection.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "4. Keep the coop clean and dry to prevent diseases.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
            
            Text(
                text = "See more...",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clickable { navController.navigate("farm_tips") }
                    .align(Alignment.End)
            )
        }
    }
}

@Composable
fun WeatherUpdateCard() {
    val weatherViewModel: WeatherViewModel = viewModel()
    val weatherState by weatherViewModel.weatherState.collectAsState()
    val useCelsius by weatherViewModel.useCelsius.collectAsState()
    val hourly by weatherViewModel.hourly.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val fineLocationPermission = Manifest.permission.ACCESS_FINE_LOCATION
    val coarseLocationPermission = Manifest.permission.ACCESS_COARSE_LOCATION
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[fineLocationPermission] == true || permissions[coarseLocationPermission] == true
        if (granted) {
            weatherViewModel.fetchWeather(useCelsius = useCelsius)
        } else {
            // Permission denied; fallback to city will be used by ViewModel
            weatherViewModel.fetchWeather(useCelsius = useCelsius)
        }
        permissionDenied = !granted
    }
    
    // Fetch weather data or request permission when component is first displayed
    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, fineLocationPermission) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, coarseLocationPermission) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            weatherViewModel.fetchWeather(useCelsius = useCelsius)
            permissionDenied = false
        } else {
            permissionLauncher.launch(arrayOf(fineLocationPermission, coarseLocationPermission))
        }
    }
    
    // Hide the card completely if there's an error - don't show error messages
    if (weatherState.error != null && !weatherState.isLoading) {
        return
    }
    
    // Use real weather data or fallback to placeholder
    val hasValidData = weatherState.error == null && !weatherState.isLoading && weatherState.currentTemp > 0
    
    val currentTemp = if (hasValidData) {
        weatherState.currentTemp.toInt()
    } else {
        25 // Placeholder temperature
    }
    val highTemp = if (hasValidData) {
        weatherState.highTemp.toInt()
    } else {
        28 // Placeholder high
    }
    val lowTemp = if (hasValidData) {
        weatherState.lowTemp.toInt()
    } else {
        22 // Placeholder low
    }
    val humidity = if (hasValidData) weatherState.humidity else 60
    val windSpeed = if (hasValidData) weatherState.windSpeed.toInt() else 10
    val weatherCondition = if (hasValidData) weatherState.weatherCondition else "Clear sky"
    val feelsLike = if (hasValidData) {
        weatherState.feelsLike.toInt()
    } else {
        currentTemp
    }
    val location = if (hasValidData && weatherState.location.isNotEmpty()) {
        weatherState.location
    } else {
        ""
    }
    val uvIndex = 7
    
    // Thresholds adapt to selected unit
    val hotThreshold = if (useCelsius) 30 else 86
    val warmThreshold = if (useCelsius) 20 else 68

    val tempColor = when {
        currentTemp > hotThreshold -> Color(0xFFFF6B35)
        currentTemp > warmThreshold -> Color(0xFF4CAF50)
        else -> Color(0xFF2196F3)
    }
    
    // Removed unused variable that was triggering linter warnings
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingLarge),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevation),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(Dimens.PaddingLarge)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Weather Update",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (location.isNotEmpty() && location != "Loading...") {
                        Text(
                            location,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Today",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = { weatherViewModel.fetchWeather(useCelsius = useCelsius) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh weather",
                            tint = Color(0xFF000000)
                        )
                    }
                    // Units toggle: °C / °F
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF1F1F1))
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val selectedBg = Color.White
                        val unselectedBg = Color(0xFFF1F1F1)
                        val selectedColor = Color(0xFF000000)
                        val unselectedColor = Color(0xFF666666)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (useCelsius) selectedBg else unselectedBg)
                                .clickable {
                                    if (!useCelsius) {
                                        weatherViewModel.setUseCelsius(true)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("°C", color = if (useCelsius) selectedColor else unselectedColor, fontSize = 12.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (!useCelsius) selectedBg else unselectedBg)
                                .clickable {
                                    if (useCelsius) {
                                        weatherViewModel.setUseCelsius(false)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("°F", color = if (!useCelsius) selectedColor else unselectedColor, fontSize = 12.sp)
                        }
                    }
                }
            }
            
            // Show loading or error state
            if (weatherState.isLoading) {
                Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFD27D2D),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Loading weather...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            } else {
                if (permissionDenied) {
                    Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Location is off. Using default city.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "Use location",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { permissionLauncher.launch(arrayOf(fineLocationPermission, coarseLocationPermission)) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                // Show weather data
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            "$currentTemp",
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 56.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = tempColor
                        )
                        Text(
                            if (useCelsius) "°C" else "°F",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Text(
                        weatherCondition,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                // Temperature range
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowDropUp,
                            contentDescription = "High",
                            tint = Color(0xFFFF6B35),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "$highTemp°",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Low",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "$lowTemp°",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
            
            // Weather details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WeatherInfoItem(
                    icon = "💧",
                    label = "Humidity",
                    value = "$humidity%",
                    modifier = Modifier
                )
                WeatherInfoItem(
                    icon = "💨",
                    label = "Wind",
                    value = if (useCelsius) "$windSpeed km/h" else "$windSpeed mph",
                    modifier = Modifier
                )
                WeatherInfoItem(
                    icon = "☀️",
                    label = "UV Index",
                    value = "$uvIndex",
                    modifier = Modifier
                )
                WeatherInfoItem(
                    icon = "🌡️",
                    label = "Feels like",
                    value = if (useCelsius) "$feelsLike°C" else "$feelsLike°F",
                    modifier = Modifier
                )
            }
            
            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))

            // Hourly forecast (next ~24h)
            if (hourly.isNotEmpty()) {
                Text(
                    "Next hours",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(hourly.size) { index ->
                        val item = hourly[index]
                        val hour = SimpleDateFormat("h a", Locale.getDefault()).format(Date(item.timestamp * 1000))
                        val t = item.main.temp.roundToInt()
                        val pop = (item.precipitationProb * 100).roundToInt()
                        val condition = item.weather.firstOrNull()?.main ?: ""
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(hour, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("$t°", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(condition, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("$pop%", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2196F3))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
            }
 
            // Recommendation section removed per user request
            }
        }
    }
}

@Composable
fun RowScope.WeatherInfoItem(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.weight(1f)
    ) {
        Text(
            icon,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@Composable
fun CustomTabBar(navController: NavController, bottomBarHeight: Dp) {
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
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                ) {
                    BadgedBox(
                        badge = {
                            if (tab.route == "detection_history" && newHistoryCount > 0) {
                                Badge(containerColor = Color.Red) { 
                                    Text("$newHistoryCount", color = Color.White) 
                                }
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
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevation)
    ) {
        Column(
            modifier = Modifier
                .padding(Dimens.PaddingLarge)
                .fillMaxWidth(),
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

@Composable
fun NavigationDrawerContent(navController: NavController, drawerState: DrawerState, scope: kotlinx.coroutines.CoroutineScope) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Fixed Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            drawerState.close()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Close drawer",
                        tint = Color(0xFFDA8041)
                    )
                }
                Text(
                    text = "ChickCare",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFDA8041),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            HorizontalDivider()

            // Scrollable Menu Items
            val scrollState = rememberLazyListState()
            LazyColumn(
                state = scrollState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Existing items that are useful
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                        label = { Text("Favorites") },
                        selected = false,
                        onClick = {
                            navController.navigate("favorites") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                        )
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Archive, contentDescription = null) },
                        label = { Text("Archives") },
                        selected = false,
                        onClick = {
                            navController.navigate("archives") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                        )
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        label = { Text("Trash") },
                        selected = false,
                        onClick = {
                            navController.navigate("trash") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                        )
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                        label = { Text("Notifications") },
                        selected = false,
                        onClick = {
                            navController.navigate("notifications") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                        )
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Book, contentDescription = null) },
                        label = { Text("Farm Tips") },
                        selected = false,
                        onClick = {
                            navController.navigate("farm_tips") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                        )
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Settings") },
                        selected = false,
                        onClick = {
                            navController.navigate("settings") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                        )
                    )
                }

                // New Menu Items
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.LocalHospital, contentDescription = null) },
                        label = { Text("Health Records") },
                        selected = false,
                        onClick = {
                            navController.navigate("health_records") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                        )
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Vaccines, contentDescription = null) },
                        label = { Text("Vaccination Schedule") },
                        selected = false,
                        onClick = {
                            navController.navigate("vaccination_schedule") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                        )
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                        label = { Text("Feeding Schedule") },
                        selected = false,
                        onClick = {
                            navController.navigate("feeding_schedule") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                        )
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Egg, contentDescription = null) },
                        label = { Text("Egg Production Tracker") },
                        selected = false,
                        onClick = {
                            navController.navigate("egg_production") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                        )
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null) },
                        label = { Text("Growth Monitoring") },
                        selected = false,
                        onClick = {
                            navController.navigate("growth_monitoring") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                        )
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                        label = { Text("Expense Tracker") },
                        selected = false,
                        onClick = {
                            navController.navigate("expense_tracker") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                        )
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Link, contentDescription = null) },
                        label = { Text("Disease Database") },
                        selected = false,
                        onClick = {
                            navController.navigate("disease_database") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                        )
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.MedicalServices, contentDescription = null) },
                        label = { Text("Medications Log") },
                        selected = false,
                        onClick = {
                            navController.navigate("medications_log") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                        )
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                        label = { Text("Coop Management") },
                        selected = false,
                        onClick = {
                            navController.navigate("coop_management") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                        )
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Grass, contentDescription = null) },
                        label = { Text("Breeding Records") },
                        selected = false,
                        onClick = {
                            navController.navigate("breeding_records") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                        )
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                        label = { Text("Reports & Analytics") },
                        selected = false,
                        onClick = {
                            navController.navigate("reports_analytics") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                        )
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Fixed Footer
            HorizontalDivider()
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                label = { Text("About") },
                selected = false,
                onClick = {
                    scope.launch {
                        drawerState.close()
                    }
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                )
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                label = { Text("Logout") },
                selected = false,
                onClick = {
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                    scope.launch {
                        drawerState.close()
                    }
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                )
            )
        }
    }
}

