package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.data.DashboardUiState
import com.bisu.chickcare.backend.repository.DetectionEntry
import com.bisu.chickcare.backend.service.NetworkConnectivityHelper
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel
import com.bisu.chickcare.frontend.components.ChickenGalleryCard
import com.bisu.chickcare.frontend.components.CustomTabBar
import com.bisu.chickcare.frontend.components.DailyRemindersCard
import com.bisu.chickcare.frontend.components.DashboardTopAppBar
import com.bisu.chickcare.frontend.components.DetectionHistoryCard
import com.bisu.chickcare.frontend.components.FarmTipsCard
import com.bisu.chickcare.frontend.components.HealthTrendLineGraph
import com.bisu.chickcare.frontend.components.NavigationDrawerContent
import com.bisu.chickcare.frontend.components.OfflineIndicator
import com.bisu.chickcare.frontend.components.StatsSummaryCard
import com.bisu.chickcare.frontend.components.SyncStatusIndicator
import com.bisu.chickcare.frontend.components.WeatherUpdateCard
import com.bisu.chickcare.frontend.utils.Dimens
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    dashboardViewModel: DashboardViewModel = viewModel()
) {

    val context = LocalContext.current
    val uiState by dashboardViewModel.uiState.collectAsState()
    val notificationCount by dashboardViewModel.newNotificationCount.collectAsState()
    val detectionHistory by dashboardViewModel.detectionHistory.collectAsState()
    val syncStatus by dashboardViewModel.syncStatus.collectAsState()
    var expandedDropdown by rememberSaveable { mutableStateOf(false) }
    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isOnline by NetworkConnectivityHelper.connectivityFlow(context).collectAsState(initial = NetworkConnectivityHelper.isOnline(context))
    val isOffline = !isOnline

    // Update active status immediately and refresh data on screen load
    LaunchedEffect(Unit) {
        dashboardViewModel.updateActiveStatus()
        dashboardViewModel.refreshData()
    }

    // Periodically update active status every 30 seconds while on dashboard
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000) // 30 seconds
            dashboardViewModel.updateActiveStatus()
        }
    }

    // Login Success Popup Logic - only show once after actual login
    // Use previousBackStackEntry to check if we came from login screen
    val previousRoute = navController.previousBackStackEntry?.destination?.route
    val cameFromLogin = previousRoute == "login"
    var showLoginSuccess by rememberSaveable { mutableStateOf(cameFromLogin) }
    LaunchedEffect(showLoginSuccess) {
        if (showLoginSuccess) {
            delay(2000)
            showLoginSuccess = false
        }
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
                    navController = navController,
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
                    .background(Color(0xFFFAE7C8)) // Warm beige to prevent gray showing through
            ) {
                // Main content layer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.background_app),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(50.dp)
                    )

                    // Warm highlight overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFAE7C8).copy(alpha = 0.25f))
                    )

                    // Offline indicator
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        OfflineIndicator(
                            isOffline = isOffline,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Sync status indicator - shows when operations are queued
                        SyncStatusIndicator(
                            isQueued = syncStatus.isQueued,
                            queuedCount = syncStatus.queuedCount,
                            isSyncing = syncStatus.isSyncing,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (uiState.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFFD27D2D))
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                DashboardScreenContent(
                                    uiState = uiState,
                                    detectionHistory = detectionHistory,
                                    navController = navController,
                                    dashboardViewModel = dashboardViewModel,
                                    onViewHistoryClicked = { navController.navigate("detection_history") }
                                )
                            }
                        }
                    }
                } // End inner content Box

                // Login Success Popup
                androidx.compose.animation.AnimatedVisibility(
                    visible = showLoginSuccess,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = Dimens.BottomBarHeight + 16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.dashboard_login_success),
                            color = Color.White,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            } // End outer Box
            if (uiState.isDetecting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ThemeColorUtils.black(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color(0xFFE5E2DE)))) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFFD27D2D))
                            Spacer(modifier = Modifier.height(Dimens.PaddingLarge))
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.dashboard_analyzing),
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                color = ThemeColorUtils.black()
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
    onViewHistoryClicked: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge),
        contentPadding = PaddingValues(
            top = Dimens.PaddingLarge,
            bottom = Dimens.PaddingLarge
        )
    ) {
        item { ChickenGalleryCard() }
        item {
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
                System.currentTimeMillis()
            }

            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            detectionHistory.count { it.timestamp >= startOfDay }

            StatsSummaryCard(
                totalDetections = uiState.totalDetections,
                healthyRate = uiState.healthyRate,
                unhealthyRate = uiState.unhealthyRate,
                imageDetections = uiState.imageDetections,
                audioDetections = uiState.audioDetections,
                detectionHistory = detectionHistory
            )
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = Dimens.PaddingLarge),
                verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
            ) {
                HealthTrendLineGraph(
                    title = "Image and Audio Trends",
                    dataPoints = uiState.combinedTrendData
                )
            }
        }
        item {
            DetectionHistoryCard(
                detectionHistory = detectionHistory,
                onViewAllClicked = onViewHistoryClicked,
                dashboardViewModel = dashboardViewModel
            )
        }
        item { FarmTipsCard(navController = navController) }
        item { WeatherUpdateCard() }
        item { DailyRemindersCard() }
    }
}
