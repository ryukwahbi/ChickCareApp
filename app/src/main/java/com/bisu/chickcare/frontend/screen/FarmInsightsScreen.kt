package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmInsightsScreen(navController: NavController) {
    val dashboardViewModel: DashboardViewModel = viewModel()
    val uiState by dashboardViewModel.uiState.collectAsState()
    val detectionHistory by dashboardViewModel.detectionHistory.collectAsState()
    val totalDetections = detectionHistory.size
    val healthyCount = detectionHistory.count { it.isHealthy }
    val infectedCount = detectionHistory.count { !it.isHealthy }
    val avgConfidence = if (detectionHistory.isNotEmpty()) {
        detectionHistory.map { it.confidence }.average()
    } else 0.0
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Farm Insights",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ThemeColorUtils.darkGray(Color(0xFF231C16))
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFFFFF),
                    titleContentColor = ThemeColorUtils.darkGray(Color(0xFF231C16))
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFFF0DB))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    InsightCard(
                        title = "Total Detections",
                        value = totalDetections.toString(),
                        icon = Icons.Default.BarChart
                    )
                }
                item {
                    InsightCard(
                        title = "Healthy Chickens",
                        value = "$healthyCount (${if (totalDetections > 0) (healthyCount * 100 / totalDetections) else 0}%)",
                        icon = Icons.Default.BarChart,
                        valueColor = Color(0xFF4CAF50)
                    )
                }
                item {
                    InsightCard(
                        title = "Infected Chickens",
                        value = "$infectedCount (${if (totalDetections > 0) (infectedCount * 100 / totalDetections) else 0}%)",
                        icon = Icons.Default.BarChart,
                        valueColor = Color(0xFFF44336)
                    )
                }
                item {
                    InsightCard(
                        title = "Average Confidence",
                        value = "${(avgConfidence * 100).toInt()}%",
                        icon = Icons.Default.BarChart
                    )
                }
                item {
                    InsightCard(
                        title = "Image Detections",
                        value = uiState.imageDetections.toString(),
                        icon = Icons.Default.BarChart
                    )
                }
                item {
                    InsightCard(
                        title = "Audio Detections",
                        value = uiState.audioDetections.toString(),
                        icon = Icons.Default.BarChart
                    )
                }
            }
        }
    }
}

@Composable
fun InsightCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valueColor: Color = ThemeColorUtils.darkGray(Color(0xFF231C16))
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.white()
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeColorUtils.lightGray(Color.Gray)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = valueColor
                    )
                }
                Icon(
                    icon,
                    contentDescription = null,
                    tint = ThemeColorUtils.darkGray(Color(0xFF231C16)),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
