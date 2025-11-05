package com.bisu.chickcare.frontend.components

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel
import com.bisu.chickcare.frontend.utils.Dimens
import kotlinx.coroutines.launch

data class TabItem(val route: String, val icon: ImageVector, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopAppBar(
    navController: NavController,
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
                    onClick = { onExpandedChange(!expanded) },
                    modifier = Modifier.offset(x = (-8).dp)
                ) {
                    Icon(
                        if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = "Filter",
                        tint = Color(0xFF000000)
                    )
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) },
                    modifier = Modifier.background(Color.White)
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Announcements") },
                        onClick = {
                            onExpandedChange(false)
                            navController.navigate("announcements")
                        }
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Color.Gray.copy(alpha = 0.3f)
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Tips & Tricks") },
                        onClick = {
                            onExpandedChange(false)
                            navController.navigate("farm_tips")
                        }
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Color.Gray.copy(alpha = 0.3f)
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Recent Activity") },
                        onClick = {
                            onExpandedChange(false)
                            navController.navigate("recent_activity")
                        }
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Color.Gray.copy(alpha = 0.3f)
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("My Favorites") },
                        onClick = {
                            onExpandedChange(false)
                            navController.navigate("favorites")
                        }
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = Color.Gray.copy(alpha = 0.3f)
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text("Farm Insights") },
                        onClick = {
                            onExpandedChange(false)
                            navController.navigate("farm_insights")
                        }
                    )
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
fun NavigationDrawerContent(navController: NavController, drawerState: DrawerState, scope: kotlinx.coroutines.CoroutineScope) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
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

            val scrollState = rememberLazyListState()
            LazyColumn(
                state = scrollState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                        label = { Text("Favorites") },
                        selected = false,
                        onClick = {
                            navController.navigate("favorites") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch { drawerState.close() }
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
                            scope.launch { drawerState.close() }
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
                            scope.launch { drawerState.close() }
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
                            scope.launch { drawerState.close() }
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
                            scope.launch { drawerState.close() }
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
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                        )
                    )
                }

                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.LocalHospital, contentDescription = null) },
                        label = { Text("Health Records") },
                        selected = false,
                        onClick = {
                            navController.navigate("health_records") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                            scope.launch { drawerState.close() }
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
                            scope.launch { drawerState.close() }
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
                            scope.launch { drawerState.close() }
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
                            scope.launch { drawerState.close() }
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
                            scope.launch { drawerState.close() }
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
                            scope.launch { drawerState.close() }
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
                            scope.launch { drawerState.close() }
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
                            scope.launch { drawerState.close() }
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
                            scope.launch { drawerState.close() }
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
                            scope.launch { drawerState.close() }
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
                            scope.launch { drawerState.close() }
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

            HorizontalDivider()
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                label = { Text("About") },
                selected = false,
                onClick = {
                    scope.launch { drawerState.close() }
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
                    scope.launch { drawerState.close() }
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFFD2B48C).copy(alpha = 0.3f)
                )
            )
        }
    }
}

