package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

data class Coop(
    val id: String,
    val name: String,
    val location: String,
    val capacity: Int,
    val currentOccupancy: Int,
    val type: String,
    val status: CoopStatus
)

enum class CoopStatus {
    ACTIVE, MAINTENANCE, FULL, EMPTY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoopManagementScreen(navController: NavController) {
    var coops by remember {
        mutableStateOf(
            listOf(
                Coop(
                    id = "1",
                    name = "Coop A",
                    location = "North Section",
                    capacity = 50,
                    currentOccupancy = 42,
                    type = "Layer Coop",
                    status = CoopStatus.ACTIVE
                ),
                Coop(
                    id = "2",
                    name = "Coop B",
                    location = "South Section",
                    capacity = 50,
                    currentOccupancy = 48,
                    type = "Layer Coop",
                    status = CoopStatus.ACTIVE
                ),
                Coop(
                    id = "3",
                    name = "Coop C",
                    location = "East Section",
                    capacity = 30,
                    currentOccupancy = 0,
                    type = "Brooder",
                    status = CoopStatus.EMPTY
                ),
                Coop(
                    id = "4",
                    name = "Coop D",
                    location = "West Section",
                    capacity = 40,
                    currentOccupancy = 40,
                    type = "Breeding Coop",
                    status = CoopStatus.FULL
                )
            )
        )
    }

    val totalCapacity = coops.sumOf { it.capacity }
    val totalOccupancy = coops.sumOf { it.currentOccupancy }
    val utilizationRate = if (totalCapacity > 0) (totalOccupancy.toFloat() / totalCapacity * 100).toInt() else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Coop Management",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Add new coop */ },
                containerColor = Color(0xFFDA8041),
                contentColor = ThemeColorUtils.white()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Coop")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ThemeColorUtils.beige(Color(0xFFF5F5DC)))
        ) {
            // Summary Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Total Coops",
                    count = coops.size,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White)),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$utilizationRate%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Utilization",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeColorUtils.lightGray(Color.Gray)
                        )
                    }
                }
                SummaryCard(
                    title = "Total Birds",
                    count = totalOccupancy,
                    color = Color(0xFFDA8041),
                    modifier = Modifier.weight(1f)
                )
            }

            // Capacity Overview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Capacity Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Capacity: $totalCapacity",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Occupied: $totalOccupancy",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { totalOccupancy.toFloat() / totalCapacity },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF4CAF50),
                        trackColor = ThemeColorUtils.lightGray(Color.Gray).copy(alpha = 0.2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Coops List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(coops, key = { it.id }) { coop ->
                    CoopCard(coop = coop)
                }
            }
        }
    }
}

@Composable
fun CoopCard(coop: Coop) {
    val statusColor = when (coop.status) {
        CoopStatus.ACTIVE -> Color(0xFF4CAF50)
        CoopStatus.FULL -> Color(0xFF2196F3)
        CoopStatus.EMPTY -> Color(0xFF9E9E9E)
        CoopStatus.MAINTENANCE -> Color(0xFFFF9800)
    }
    
    val occupancyPercentage = (coop.currentOccupancy.toFloat() / coop.capacity * 100).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(statusColor.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocationCity,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = coop.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = ThemeColorUtils.lightGray(Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = coop.location,
                                style = MaterialTheme.typography.bodySmall,
                                color = ThemeColorUtils.lightGray(Color.Gray)
                            )
                        }
                        Text(
                            text = coop.type,
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeColorUtils.lightGray(Color.Gray)
                        )
                    }
                }
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = coop.status.name,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Occupancy",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeColorUtils.lightGray(Color.Gray)
                    )
                    Text(
                        text = "${coop.currentOccupancy} / ${coop.capacity}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "$occupancyPercentage%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { occupancyPercentage / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = statusColor,
                trackColor = ThemeColorUtils.lightGray(Color.Gray).copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun SummaryCard(title: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = ThemeColorUtils.lightGray(Color.Gray)
            )
        }
    }
}
