package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.backend.repository.EggProductionRecord
import com.bisu.chickcare.backend.viewmodels.EggProductionViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EggProductionTrackerScreen(navController: NavController) {
    val viewModel: EggProductionViewModel = viewModel()
    val records by viewModel.eggProductionRecords.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<EggProductionRecord?>(null) }

    // Today's record calculations
    val today = System.currentTimeMillis()
    val startOfDay = Calendar.getInstance().apply {
        timeInMillis = today
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val endOfDay = startOfDay + (24 * 60 * 60 * 1000)
    val todayRecord = records.firstOrNull {
        it.date >= startOfDay && it.date < endOfDay
    }

    val weeklyRecords = records.take(7)
    val weeklyAverage = if (weeklyRecords.isNotEmpty()) {
        weeklyRecords.map { it.totalEggs }.average()
    } else 0.0

    val monthlyRecords = records.filter {
        (today - it.date) < (30L * 24 * 60 * 60 * 1000)
    }
    val monthlyTotal = monthlyRecords.sumOf { it.totalEggs }
    val totalHealthy = monthlyRecords.sumOf { it.healthyEggs }
    val healthyPercentage = if (monthlyTotal > 0) {
        (totalHealthy.toFloat() / monthlyTotal * 100).toInt()
    } else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Egg Production",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = false }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFF141617) else Color(0xFFFDFBF7),
                    titleContentColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRecord = null
                    showAddDialog = true
                },
                containerColor = ThemeColorUtils.primary(), // Main brand color
                contentColor = ThemeColorUtils.white(),
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Record", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFF141617) else Color(0xFFFDFBF7))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
            ) {
                // Header Gradient Section
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFDA8041), // Vibrant brand orange
                                        Color(0xFFE89A64) 
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Text(
                                "Total Monthly Yield",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$monthlyTotal",
                                    color = Color.White,
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "eggs",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                    }
                }

                // Top Stats Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatCard(
                            title = "Collected Today",
                            value = todayRecord?.totalEggs?.toString() ?: "0",
                            icon = Icons.Default.Egg,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Weekly Average",
                            value = "${weeklyAverage.toInt()}",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            color = Color(0xFF2196F3),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Health Insights Card
                if (monthlyRecords.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFF1C1E20) else Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Production Quality",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$healthyPercentage% of this month's eggs were healthy.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ThemeColorUtils.lightGray(Color.Gray)
                                    )
                                }
                            }
                        }
                    }
                }

                // Header for List
                item {
                    Text(
                        text = "Recent Logs",
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
                    )
                }

                if (records.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Egg,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp),
                                    tint = ThemeColorUtils.lightGray(Color.Gray).copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No logs found",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ThemeColorUtils.lightGray(Color.Gray),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    items(records, key = { it.id }) { record ->
                        PremiumEggProductionCard(
                            record = record,
                            onEdit = {
                                editingRecord = record
                                showAddDialog = true
                            },
                            onDelete = {
                                viewModel.deleteEggProductionRecord(record.id)
                            }
                        )
                    }
                }
            }
        }

        // Add/Edit Dialog BottomSheet/Modal style
        if (showAddDialog) {
            PremiumEggProductionInputDialog(
                record = editingRecord,
                onDismiss = {
                    showAddDialog = false
                    editingRecord = null
                },
                onSave = { record ->
                    viewModel.saveEggProductionRecord(record)
                    showAddDialog = false
                    editingRecord = null
                }
            )
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFF1C1E20) else Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .background(color.copy(alpha = 0.15f), CircleShape)
                        .padding(8.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = ThemeColorUtils.lightGray(Color.Gray),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PremiumEggProductionCard(
    record: EggProductionRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFF1C1E20) else Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Yield Box
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFDA8041).copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${record.totalEggs}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFDA8041)
                        )
                        Text(
                            text = "eggs",
                            fontSize = 10.sp,
                            color = Color(0xFFDA8041)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.coopLocation,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = ThemeColorUtils.lightGray(Color.Gray),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dateFormat.format(Date(record.date)),
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeColorUtils.lightGray(Color.Gray)
                        )
                    }
                    if (record.brokenEggs > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${record.brokenEggs} broken",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (record.notes.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFF2A2D30) else Color(0xFFF5F5F5))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = record.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeColorUtils.lightGray(Color.Gray)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumEggProductionInputDialog(
    record: EggProductionRecord?,
    onDismiss: () -> Unit,
    onSave: (EggProductionRecord) -> Unit
) {
    var totalEggs by remember { mutableStateOf(record?.totalEggs?.toString() ?: "") }
    var healthyEggs by remember { mutableStateOf(record?.healthyEggs?.toString() ?: "") }
    var brokenEggs by remember { mutableStateOf(record?.brokenEggs?.toString() ?: "0") }
    var coopLocation by remember { mutableStateOf(record?.coopLocation ?: "") }
    var notes by remember { mutableStateOf(record?.notes ?: "") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .width(360.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFF1C1E20) else Color.White
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (record == null) "Log Production" else "Edit Log",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ThemeColorUtils.lightGray(Color.Gray)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = totalEggs,
                    onValueChange = {
                        totalEggs = it
                        val total = it.toIntOrNull() ?: 0
                        val healthy = healthyEggs.toIntOrNull() ?: 0
                        if (total > 0 && healthy >= 0 && total >= healthy) {
                            brokenEggs = (total - healthy).toString()
                        }
                    },
                    label = { Text("Total Eggs") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = healthyEggs,
                    onValueChange = {
                        healthyEggs = it
                        val total = totalEggs.toIntOrNull() ?: 0
                        val healthy = it.toIntOrNull() ?: 0
                        if (total > 0 && healthy >= 0 && total >= healthy) {
                            brokenEggs = (total - healthy).toString()
                        }
                    },
                    label = { Text("Healthy Eggs") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = brokenEggs,
                    onValueChange = { brokenEggs = it },
                    label = { Text("Broken Eggs") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = false,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = Color(0xFFFF9800),
                        disabledBorderColor = Color(0xFFFF9800).copy(alpha = 0.5f),
                        disabledLabelColor = Color(0xFFFF9800)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = coopLocation,
                    onValueChange = { coopLocation = it },
                    label = { Text("Coop/Pen Location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Remarks (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        val total = totalEggs.toIntOrNull() ?: 0
                        val healthy = healthyEggs.toIntOrNull() ?: 0
                        val broken = brokenEggs.toIntOrNull() ?: 0

                        if (total > 0 && healthy >= 0 && broken >= 0 &&
                            (healthy + broken) <= total && coopLocation.isNotBlank()
                        ) {
                            val newRecord = EggProductionRecord(
                                id = record?.id ?: "",
                                date = record?.date ?: System.currentTimeMillis(),
                                totalEggs = total,
                                healthyEggs = healthy,
                                brokenEggs = broken,
                                coopLocation = coopLocation.trim(),
                                notes = notes.trim()
                            )
                            onSave(newRecord)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    enabled = ((totalEggs.toIntOrNull() ?: 0) > 0) &&
                            ((healthyEggs.toIntOrNull() ?: 0) >= 0) &&
                            coopLocation.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeColorUtils.primary()),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Save Record",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}