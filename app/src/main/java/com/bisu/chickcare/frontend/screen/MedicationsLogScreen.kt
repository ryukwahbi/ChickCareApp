package com.bisu.chickcare.frontend.screen

import android.app.TimePickerDialog
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.backend.repository.MedicationEntry
import com.bisu.chickcare.backend.viewmodels.MedicationsViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsLogScreen(navController: NavController) {
    val viewModel: MedicationsViewModel = viewModel()
    val medications by viewModel.medications.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<MedicationEntry?>(null) }
    val activeCount = medications.count { it.isActive }
    val completedCount = medications.size - activeCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Medications Log",
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
                    editingEntry = null
                    showDialog = true
                },
                containerColor = Color(0xFF8F8C8A),
                contentColor = ThemeColorUtils.white()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Medication")
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = "Active",
                        count = activeCount,
                        color = Color(0xFFEFDB46),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Completed",
                        count = completedCount,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                }

                var selectedTab by remember { mutableIntStateOf(0) }
                val tabs = listOf("All", "Active", "Completed")

                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = ThemeColorUtils.white(),
                    contentColor = Color(0xFFDA8041)
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Spacer(modifier = Modifier.height(6.dp))

                val filteredMedications = when (selectedTab) {
                    1 -> medications.filter { it.isActive }
                    2 -> medications.filter { !it.isActive }
                    else -> medications
                }.sortedByDescending { it.scheduledDate }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredMedications.isEmpty()) {
                        item {
                            EmptyStateCard(
                                title = "No medications found",
                                icon = Icons.Default.MedicalServices
                            )
                        }
                    } else {
                        items(filteredMedications, key = { it.id }) { entry ->
                            MedicationCard(
                                entry = entry,
                                onToggleStatus = { isActive ->
                                    viewModel.toggleMedicationStatus(entry.id, isActive)
                                },
                                onEdit = {
                                    editingEntry = entry
                                    showDialog = true
                                },
                                onDelete = { viewModel.deleteMedication(entry.id) }
                            )
                        }
                    }
                }
            }

            if (showDialog) {
                MedicationInputDialog(
                    entry = editingEntry,
                    onDismiss = {
                        showDialog = false
                        editingEntry = null
                    },
                    onSave = { entry ->
                        viewModel.saveMedication(entry)
                        showDialog = false
                        editingEntry = null
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = ThemeColorUtils.lightGray(Color.Gray))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = ThemeColorUtils.lightGray(Color.Gray)
        )
    }
}

@Composable
private fun MedicationCard(
    entry: MedicationEntry,
    onToggleStatus: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val statusColor = if (entry.isActive) Color(0xFFFF9800) else Color(0xFF4CAF50)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.isActive)
                statusColor.copy(alpha = 0.1f)
            else ThemeColorUtils.surface(Color.White)
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .size(48.dp)
                            .background(statusColor.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.medicationName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = entry.chickenId?.let { "Chicken ID: $it" } ?: "Flock Treatment",
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
                        text = if (entry.isActive) "ACTIVE" else "COMPLETED",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(label = "Dosage", value = entry.dosage)
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(label = "Frequency", value = entry.frequency)
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(label = "Duration", value = entry.duration)
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(label = "Administered By", value = entry.administeredBy)
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(label = "Scheduled Date", value = dateFormat.format(Date(entry.scheduledDate)))

            if (entry.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = ThemeColorUtils.surface(Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "📝 ${entry.notes}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onToggleStatus(!entry.isActive) }) {
                    Icon(
                        imageVector = if (entry.isActive) Icons.Default.CheckCircle else Icons.Default.Restore,
                        contentDescription = if (entry.isActive) "Mark Completed" else "Restore",
                        tint = if (entry.isActive) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF7EC6E3))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEA7B76))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicationInputDialog(
    entry: MedicationEntry?,
    onDismiss: () -> Unit,
    onSave: (MedicationEntry) -> Unit
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val context = LocalContext.current

    var medicationName by remember { mutableStateOf(entry?.medicationName ?: "") }
    var chickenId by remember { mutableStateOf(entry?.chickenId ?: "") }
    var dosage by remember { mutableStateOf(entry?.dosage ?: "") }
    var frequency by remember { mutableStateOf(entry?.frequency ?: "") }
    var duration by remember { mutableStateOf(entry?.duration ?: "") }
    var administeredBy by remember { mutableStateOf(entry?.administeredBy ?: "") }
    var notes by remember { mutableStateOf(entry?.notes ?: "") }
    var isActive by remember { mutableStateOf(entry?.isActive ?: true) }

    var selectedDateMillis by remember {
        mutableLongStateOf(
            entry?.scheduledDate ?: System.currentTimeMillis()
        )
    }
    var selectedTime by remember {
        mutableStateOf(Instant.ofEpochMilli(selectedDateMillis).atZone(zoneId).toLocalTime())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 580.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White))
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
                            text = if (entry == null) "Add Medication" else "Edit Medication",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF8F8C8A)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = medicationName,
                        onValueChange = { medicationName = it },
                        label = { Text("Medication Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = chickenId,
                        onValueChange = { chickenId = it },
                        label = { Text("Chicken ID (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { Text("Dosage *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = frequency,
                        onValueChange = { frequency = it },
                        label = { Text("Frequency *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Duration *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = administeredBy,
                        onValueChange = { administeredBy = it },
                        label = { Text("Administered By *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = Instant.ofEpochMilli(selectedDateMillis).atZone(zoneId)
                            .toLocalDate()
                            .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
                        onValueChange = {},
                        label = { Text("Scheduled Date *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        readOnly = true,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = selectedTime.format(DateTimeFormatter.ofPattern("hh:mm a")),
                        onValueChange = {},
                        label = { Text("Scheduled Time *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val localDate =
                                    Instant.ofEpochMilli(selectedDateMillis).atZone(zoneId)
                                        .toLocalDate()
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        selectedTime = LocalTime.of(hour, minute)
                                        selectedDateMillis =
                                            localDate.atTime(selectedTime).atZone(zoneId)
                                                .toInstant().toEpochMilli()
                                    },
                                    selectedTime.hour,
                                    selectedTime.minute,
                                    false
                                ).show()
                            },
                        readOnly = true,
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Active", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = isActive,
                            onCheckedChange = { isActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Gray,
                                checkedTrackColor = Color.LightGray
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val localDate = Instant.ofEpochMilli(selectedDateMillis).atZone(zoneId)
                                .toLocalDate()
                            val combinedMillis =
                                localDate.atTime(selectedTime).atZone(zoneId).toInstant()
                                    .toEpochMilli()
                            val saved = MedicationEntry(
                                id = entry?.id ?: "",
                                medicationName = medicationName.trim(),
                                chickenId = chickenId.trim().ifBlank { null },
                                scheduledDate = combinedMillis,
                                dosage = dosage.trim(),
                                frequency = frequency.trim(),
                                duration = duration.trim(),
                                administeredBy = administeredBy.trim(),
                                notes = notes.trim(),
                                isActive = isActive,
                                createdAt = entry?.createdAt ?: System.currentTimeMillis()
                            )
                            onSave(saved)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = medicationName.isNotBlank() && dosage.isNotBlank() && frequency.isNotBlank() &&
                                duration.isNotBlank() && administeredBy.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF66BB1F)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Save", modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

    @Composable
    private fun SummaryCard(
        title: String,
        count: Int,
        color: Color,
        modifier: Modifier = Modifier
    ) {
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

    @Composable
    private fun InfoRow(label: String, value: String) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = ThemeColorUtils.lightGray(Color.Gray),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }

