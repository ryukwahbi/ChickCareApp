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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    // Today's record (if exists)
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRecord = null
                    showAddDialog = true
                },
                containerColor = Color(0xFF8F8C8A),
                contentColor = ThemeColorUtils.white()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Record")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
        ) {
            Column {
                // Summary Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = "Today",
                        count = todayRecord?.totalEggs ?: 0,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Weekly Avg",
                        count = weeklyAverage.toInt(),
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Monthly",
                        count = monthlyTotal,
                        color = Color(0xFFDA8041),
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = ThemeColorUtils.lightGray(Color(0xFFBDBDBD)),
                    thickness = 1.dp
                )

                // Today's Details Card
                if (todayRecord != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDA8041)),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Today's Production",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemeColorUtils.white()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                ProductionStat("Total Eggs", todayRecord.totalEggs.toString(), ThemeColorUtils.white())
                                ProductionStat("Healthy", todayRecord.healthyEggs.toString(), Color(0xFF4CAF50))
                                ProductionStat("Broken", todayRecord.brokenEggs.toString(), Color(0xFFFF9800))
                            }
                            if (todayRecord.notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "📝 ${todayRecord.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThemeColorUtils.white(alpha = 0.9f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Production Rate Card
                if (monthlyRecords.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White)),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Production Rate",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ThemeColorUtils.lightGray(Color.Gray)
                                )
                                Text(
                                    text = "$healthyPercentage% Healthy Eggs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Historical Records
                Text(
                    text = "Recent Records",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (records.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Egg,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = ThemeColorUtils.lightGray(Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No egg production records found",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = ThemeColorUtils.lightGray(Color.Gray)
                                    )
                                }
                            }
                        }
                    } else {
                        items(records, key = { it.id }) { record ->
                            EggProductionCard(
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
        }

        // Add/Edit Dialog
        if (showAddDialog) {
            EggProductionInputDialog(
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
fun EggProductionInputDialog(
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
                .width(550.dp)
                .heightIn(max = 580.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (record == null) "Add Egg Production" else "Edit Egg Production",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = totalEggs,
                    onValueChange = {
                        totalEggs = it
                        // Auto-calculate broken eggs if not set
                        val total = it.toIntOrNull() ?: 0
                        val healthy = healthyEggs.toIntOrNull() ?: 0
                        if (total > 0 && healthy >= 0 && total >= healthy) {
                            brokenEggs = (total - healthy).toString()
                        }
                    },
                    label = { Text("Total Eggs *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = healthyEggs,
                    onValueChange = {
                        healthyEggs = it
                        // Auto-calculate broken eggs
                        val total = totalEggs.toIntOrNull() ?: 0
                        val healthy = it.toIntOrNull() ?: 0
                        if (total > 0 && healthy >= 0 && total >= healthy) {
                            brokenEggs = (total - healthy).toString()
                        }
                    },
                    label = { Text("Healthy Eggs *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = brokenEggs,
                    onValueChange = { brokenEggs = it },
                    label = { Text("Broken Eggs") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = false // Auto-calculated
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = coopLocation,
                    onValueChange = { coopLocation = it },
                    label = { Text("Coop Location *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val total = totalEggs.toIntOrNull() ?: 0
                        val healthy = healthyEggs.toIntOrNull() ?: 0
                        val broken = brokenEggs.toIntOrNull() ?: 0

                        if (total > 0 && healthy >= 0 && broken >= 0 &&
                            (healthy + broken) <= total && coopLocation.isNotBlank()) {
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
                    modifier = Modifier.fillMaxWidth(),
                    enabled = ((totalEggs.toIntOrNull() ?: 0) > 0) &&
                              ((healthyEggs.toIntOrNull() ?: 0) >= 0) &&
                              coopLocation.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF66BB1F)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Save", modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun ProductionStat(label: String, value: String, textColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = textColor.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun EggProductionCard(
    record: EggProductionRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White)),
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
                    .size(56.dp)
                    .background(Color(0xFFDA8041).copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Egg,
                    contentDescription = null,
                    tint = Color(0xFFDA8041),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormat.format(Date(record.date)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.coopLocation,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeColorUtils.lightGray(Color.Gray)
                )
                if (record.brokenEggs > 0) {
                    Text(
                        text = "${record.brokenEggs} broken",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF9800)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${record.totalEggs}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                    Text(
                        text = "eggs",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeColorUtils.lightGray(Color.Gray)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF7EC6E3),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEA7B76),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (record.notes.isNotEmpty()) {
            Surface(
                color = ThemeColorUtils.surface(Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📝 ${record.notes}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                )
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
            horizontalAlignment = Alignment.Start
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