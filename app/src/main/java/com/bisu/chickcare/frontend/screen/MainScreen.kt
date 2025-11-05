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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        bottomBar = {
            CustomTabBar(navController = navController)
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Composable
fun CustomTabBar(navController: NavController) {
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
    val selectedColor = Color.White
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Color(0xFFE1C1A0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 0.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                // More robust route matching - check if route starts with or equals tab route
                val isSelected = currentRoute == tab.route || 
                                 currentRoute?.startsWith("${tab.route}/") == true ||
                                 (currentRoute == null && tab.route == "dashboard")
                // Action and Profile tabs should not show white styling when selected
                val shouldShowWhiteStyling = isSelected && 
                                             tab.route != "action_tools" && 
                                             tab.route != "profile"
                val currentColor = if (shouldShowWhiteStyling) selectedColor else iconAndTextColor
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
                    if (shouldShowWhiteStyling) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 17.dp)
                                .width(42.dp)
                                .height(4.dp)
                                .background(selectedColor, RoundedCornerShape(2.dp))
                        )
                    } else {
                        Spacer(modifier = Modifier.height(17.dp))
                    }
                    Box(
                        modifier = Modifier.size(26.dp),
                        contentAlignment = Alignment.Center
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
                                tint = currentColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = currentColor,
                        fontWeight = if (shouldShowWhiteStyling) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

data class TabItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)
