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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.backend.repository.HealthRecord
import com.bisu.chickcare.backend.viewmodels.HealthRecordsViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthRecordsScreen(navController: NavController) {
    val viewModel: HealthRecordsViewModel = viewModel()
    val records by viewModel.healthRecords.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<HealthRecord?>(null) }
    var selectedFilter by remember { mutableStateOf("All") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Health Records",
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
                Icon(Icons.Default.Add, contentDescription = "Add Health Record")
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
                        title = "Healthy",
                        count = records.count { it.status == "HEALTHY" },
                        color = Color(0xFF84DA85),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Recovering",
                        count = records.count { it.status == "RECOVERING" },
                        color = Color(0xFFFCCA68),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Sick",
                        count = records.count { it.status == "SICK" },
                        color = Color(0xFFE55A4F),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Filter Chips
                val filters = listOf("All", "Healthy", "Recovering", "Sick", "Critical")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF171A1A).copy(alpha = 0.3f),
                                selectedLabelColor = Color(0xFF171A1A),
                                containerColor = Color.Transparent
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = ThemeColorUtils.lightGray(Color(0xFFBDBDBD)),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Records List
                val filteredRecords = if (selectedFilter == "All") {
                    records
                } else {
                    records.filter { it.status == selectedFilter.uppercase(Locale.getDefault()) }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredRecords.isEmpty()) {
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
                                        Icons.Default.LocalHospital,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = ThemeColorUtils.lightGray(Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No health records found",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = ThemeColorUtils.lightGray(Color.Gray)
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredRecords, key = { it.id }) { record ->
                    var showDeleteDialog by remember { mutableStateOf(false) }

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = {
                                Text("Delete health record?", fontWeight = FontWeight.Bold)
                            },
                            text = {
                                Text("This will permanently remove \"${record.condition}\" from your records.")
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.deleteHealthRecord(record.id)
                                        showDeleteDialog = false
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = Color(0xFFF44336)
                                    )
                                ) {
                                    Text("Delete", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showDeleteDialog = false },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = ThemeColorUtils.lightGray(Color(0xFF757575))
                                    )
                                ) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                            HealthRecordCard(
                                record = record,
                                onEdit = {
                                    editingRecord = record
                                    showAddDialog = true
                                },
                                onDelete = {
                            showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // Add/Edit Dialog
        if (showAddDialog) {
            HealthRecordInputDialog(
                record = editingRecord,
                onDismiss = {
                    showAddDialog = false
                    editingRecord = null
                },
                onSave = { record ->
                    viewModel.saveHealthRecord(record)
                    showAddDialog = false
                    editingRecord = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthRecordInputDialog(
    record: HealthRecord?,
    onDismiss: () -> Unit,
    onSave: (HealthRecord) -> Unit
) {
    var observedCondition by remember { mutableStateOf(record?.condition ?: "") }
    var symptoms by remember { mutableStateOf(record?.symptoms ?: "") }
    var intervention by remember { mutableStateOf(record?.treatment ?: "") }
    var caretaker by remember { mutableStateOf(record?.veterinarian ?: "") }
    var selectedStatus by remember { mutableStateOf(record?.status ?: "HEALTHY") }
    val statusOptions = listOf(
        "HEALTHY" to "Healthy",
        "RECOVERING" to "Recovering",
        "SICK" to "Sick",
        "CRITICAL" to "Critical"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(550.dp)
                .heightIn(max = 580.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (record == null) "Add Health Record" else "Edit Health Record",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = observedCondition,
                    onValueChange = { observedCondition = it },
                    label = { Text("Observed Condition *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = symptoms,
                    onValueChange = { symptoms = it },
                    label = { Text("Symptoms / Notes *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = intervention,
                    onValueChange = { intervention = it },
                    label = { Text("Treatment / Medication *") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = caretaker,
                    onValueChange = { caretaker = it },
                    label = { Text("Handled By (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Status Dropdown
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = statusOptions.firstOrNull { it.first == selectedStatus }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        statusOptions.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedStatus = value
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (observedCondition.isNotBlank() && symptoms.isNotBlank() && intervention.isNotBlank()) {
                            val autoId = record?.chickenId?.takeIf { it.isNotBlank() }
                                ?: "HR-${System.currentTimeMillis()}"
                            val autoLabel = record?.chickenName?.takeIf { it.isNotBlank() }
                                ?: "Entry ${SimpleDateFormat("MMM dd • HH:mm", Locale.getDefault()).format(Date())}"
                            val newRecord = HealthRecord(
                                id = record?.id ?: "",
                                chickenId = autoId,
                                chickenName = autoLabel,
                                date = record?.date ?: System.currentTimeMillis(),
                                condition = observedCondition.trim(),
                                symptoms = symptoms.trim(),
                                treatment = intervention.trim(),
                                veterinarian = caretaker.takeIf { it.isNotBlank() },
                                status = selectedStatus
                            )
                            onSave(newRecord)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = observedCondition.isNotBlank() && symptoms.isNotBlank() && intervention.isNotBlank(),
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
fun HealthRecordCard(
    record: HealthRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val statusColor = when (record.status) {
        "HEALTHY" -> Color(0xFF86D387)
        "RECOVERING" -> Color(0xFFF1C894)
        "SICK" -> Color(0xFFB0664F)
        "CRITICAL" -> Color(0xFFD00B0B)
        else -> ThemeColorUtils.lightGray(Color.Gray)
    }

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
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(statusColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocalHospital,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = record.condition.ifBlank { "Health Record" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (record.chickenId.isNotBlank()) {
                        Text(
                            text = "Reference: ${record.chickenId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeColorUtils.lightGray(Color.Gray)
                        )
                    }
                    if (!record.veterinarian.isNullOrBlank()) {
                        Text(
                            text = "Handled by: ${record.veterinarian}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1A1412),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(
                            0xFF94DE55
                        )
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(
                            0xFFF85C55
                        )
                        )
                    }
                }
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = record.status,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = ThemeColorUtils.lightGray(Color(0xFFBDBDBD)).copy(alpha = 0.6f),
                thickness = 1.dp
            )

            Text(
                text = "Condition: ${record.condition}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Symptoms: ${record.symptoms}",
                style = MaterialTheme.typography.bodySmall,
                color = ThemeColorUtils.lightGray(Color.Gray)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Treatment: ${record.treatment}",
                style = MaterialTheme.typography.bodySmall,
                color = ThemeColorUtils.lightGray(Color.Gray)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dateFormat.format(Date(record.date)),
                style = MaterialTheme.typography.bodySmall,
                color = ThemeColorUtils.lightGray(Color.Gray)
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