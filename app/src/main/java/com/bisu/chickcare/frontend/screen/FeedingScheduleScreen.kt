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
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

data class FeedingSchedule(
    val id: String,
    val time: String,
    val feedType: String,
    val quantity: String,
    val targetGroup: String,
    val frequency: String,
    val isCompleted: Boolean = false,
    val notes: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedingScheduleScreen(navController: NavController) {
    var schedules by remember {
        mutableStateOf(
            listOf(
                FeedingSchedule(
                    id = "1",
                    time = "06:00",
                    feedType = "Starter Feed",
                    quantity = "2.5 kg",
                    targetGroup = "Chicks (0-8 weeks)",
                    frequency = "Daily",
                    isCompleted = true
                ),
                FeedingSchedule(
                    id = "2",
                    time = "12:00",
                    feedType = "Layer Feed",
                    quantity = "5.0 kg",
                    targetGroup = "Laying Hens",
                    frequency = "Daily",
                    isCompleted = false
                ),
                FeedingSchedule(
                    id = "3",
                    time = "18:00",
                    feedType = "Grower Feed",
                    quantity = "3.0 kg",
                    targetGroup = "Pullets (8-20 weeks)",
                    frequency = "Daily",
                    isCompleted = false
                ),
                FeedingSchedule(
                    id = "4",
                    time = "06:30",
                    feedType = "Supplement Mix",
                    quantity = "500 g",
                    targetGroup = "All Birds",
                    frequency = "Every 3 days",
                    notes = "Calcium and vitamins"
                )
            )
        )
    }

    val todaySchedules = schedules.filter { it.frequency == "Daily" }
    val completedCount = todaySchedules.count { it.isCompleted }
    val pendingCount = todaySchedules.count { !it.isCompleted }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Feeding Schedule",
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
                onClick = { /* Add new schedule */ },
                containerColor = Color(0xFFDA8041),
                contentColor = ThemeColorUtils.white()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Schedule")
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
                    title = "Today's Feedings",
                    count = todaySchedules.size,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Completed",
                    count = completedCount,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Pending",
                    count = pendingCount,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
            }

            // Today's Date
            val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDA8041)),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(
                    text = dateFormat.format(Date()),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColorUtils.white()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Schedule List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(schedules, key = { it.id }) { schedule ->
                    FeedingScheduleCard(
                        schedule = schedule,
                        onCompleteToggle = { scheduleId ->
                            schedules = schedules.map {
                                if (it.id == scheduleId) it.copy(isCompleted = !it.isCompleted)
                                else it
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FeedingScheduleCard(
    schedule: FeedingSchedule,
    onCompleteToggle: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (schedule.isCompleted) 
                Color(0xFF4CAF50).copy(alpha = 0.1f) 
            else ThemeColorUtils.white()
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Time Badge
            Column(
                modifier = Modifier.width(80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Color(0xFFDA8041).copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color(0xFFDA8041),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = schedule.time,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDA8041)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = schedule.feedType,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = schedule.targetGroup,
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeColorUtils.lightGray(Color.Gray)
                        )
                    }
                    
                    IconButton(
                        onClick = { onCompleteToggle(schedule.id) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (schedule.isCompleted) 
                                Icons.Default.CheckCircle 
                            else Icons.Default.Restaurant,
                            contentDescription = if (schedule.isCompleted) "Mark incomplete" else "Mark complete",
                            tint = if (schedule.isCompleted) 
                                Color(0xFF4CAF50) 
                            else ThemeColorUtils.lightGray(Color.Gray),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoBadge(
                        label = "Quantity",
                        value = schedule.quantity,
                        color = Color(0xFF2196F3)
                    )
                    InfoBadge(
                        label = "Frequency",
                        value = schedule.frequency,
                        color = Color(0xFFFF9800)
                    )
                }

                if (schedule.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = ThemeColorUtils.beige(Color(0xFFF5F5DC)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "📝 ${schedule.notes}",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                        )
                    }
                }
            }
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

@Composable
fun InfoBadge(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ThemeColorUtils.lightGray(Color.Gray)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
