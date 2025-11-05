package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.data.DashboardUiState
import com.bisu.chickcare.backend.repository.DetectionEntry
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel
import com.bisu.chickcare.frontend.components.ChickenGalleryCard
import com.bisu.chickcare.frontend.components.CustomTabBar
import com.bisu.chickcare.frontend.components.DailyRemindersCard
import com.bisu.chickcare.frontend.components.DashboardTopAppBar
import com.bisu.chickcare.frontend.components.DetectionHistoryCard
import com.bisu.chickcare.frontend.components.FarmTipsCard
import com.bisu.chickcare.frontend.components.HealthTrendLineGraph
import com.bisu.chickcare.frontend.components.NavigationDrawerContent
import com.bisu.chickcare.frontend.components.StatsSummaryCard
import com.bisu.chickcare.frontend.components.WeatherUpdateCard
import com.bisu.chickcare.frontend.utils.Dimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val dashboardViewModel: DashboardViewModel = viewModel()

    val uiState by dashboardViewModel.uiState.collectAsState()
    val notificationCount by dashboardViewModel.newNotificationCount.collectAsState()
    val detectionHistory by dashboardViewModel.detectionHistory.collectAsState()

    var expandedDropdown by rememberSaveable { mutableStateOf(false) }
    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
                    Box(modifier = Modifier.padding(innerPadding)) {
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
            }

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
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
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
        item { DailyRemindersCard() }
    }
}
