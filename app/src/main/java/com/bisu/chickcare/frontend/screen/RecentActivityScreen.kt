package com.bisu.chickcare.frontend.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
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
fun RecentActivityScreen(navController: NavController) {
    val dashboardViewModel: DashboardViewModel = viewModel()
    val detectionHistory by dashboardViewModel.detectionHistory.collectAsState()
    
    // Get recent detections (last 50)
    val recentActivity = detectionHistory.take(50)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Recent Activity",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
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
                            tint = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFF141617) else Color.White,
                    titleContentColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (recentActivity.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.BarChart,
                                    contentDescription = null,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    tint = ThemeColorUtils.lightGray(Color.Gray)
                                )
                                Text(
                                    text = "No recent activity found.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ThemeColorUtils.lightGray(Color.Gray)
                                )
                            }
                        }
                    }
                } else {
                    items(recentActivity.size) { index ->
                        val entry = recentActivity[index]
                        RecentActivityItem(
                            entry = entry,
                            index = index + 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentActivityItem(
    entry: com.bisu.chickcare.backend.repository.DetectionEntry,
    index: Int
) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),

        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = ThemeColorUtils.white()
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Text(
                    text = "#$index",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFB0B0B0) else Color.Gray
                )
                Text(
                    text = if (entry.isHealthy) "Healthy" else "Infected",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (entry.isHealthy) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Confidence: ${(entry.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFB0B0B0) else Color.Gray
            )
            Text(
                text = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(entry.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFF888888) else Color.Gray
            )
        }
    }
}
