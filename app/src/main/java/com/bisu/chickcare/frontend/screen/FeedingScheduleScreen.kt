package com.bisu.chickcare.frontend.screen

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.bisu.chickcare.backend.repository.FeedingScheduleEntry
import com.bisu.chickcare.backend.viewmodels.FeedingScheduleViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedingScheduleScreen(navController: NavController) {
    val viewModel: FeedingScheduleViewModel = viewModel()
    val schedules by viewModel.schedules.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<FeedingScheduleEntry?>(null) }

    val zoneId = remember { ZoneId.systemDefault() }
    var calendarVisibleMonth by remember { mutableStateOf(LocalDate.now()) }
    var selectedDateMillis by remember {
        mutableLongStateOf(calendarVisibleMonth.atStartOfDay(zoneId).toInstant().toEpochMilli())
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let {
            selectedDateMillis = it
            calendarVisibleMonth = Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate()
        }
    }

    val selectedDate = Instant.ofEpochMilli(selectedDateMillis).atZone(zoneId).toLocalDate()
    val today = LocalDate.now()

    val todaySchedules = schedules.filter { it.occursOn(today, zoneId) }
    val completedCount = todaySchedules.count { it.isCompleted }
    val pendingCount = todaySchedules.count { !it.isCompleted }
    val filteredSchedules = schedules.filter { it.occursOn(selectedDate, zoneId) }

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
                    editingSchedule = null
                    showDialog = true
                },
                containerColor = Color(0xFF8F8C8A),
                contentColor = ThemeColorUtils.white()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Schedule")
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
                // Summary cards
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

                // Inline calendar
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Schedule Calendar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DatePicker(
                            state = datePickerState,
                            showModeToggle = false
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Schedules on ${selectedDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))}",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredSchedules.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Restaurant,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = ThemeColorUtils.lightGray(Color.Gray)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No schedules for the selected date",
                                style = MaterialTheme.typography.bodyLarge,
                                color = ThemeColorUtils.lightGray(Color.Gray)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredSchedules, key = { it.id }) { schedule ->
                            FeedingScheduleCard(
                                schedule = schedule,
                                onToggleComplete = { completed ->
                                    viewModel.toggleCompletion(schedule.id, completed)
                                },
                                onEdit = {
                                    editingSchedule = schedule
                                    showDialog = true
                                },
                                onDelete = { viewModel.deleteSchedule(schedule.id) }
                            )
                        }
                    }
                }
            }

        }
    }

    if (showDialog) {
        FeedingScheduleInputDialog(
            entry = editingSchedule,
            onDismiss = {
                showDialog = false
                editingSchedule = null
            },
            onSave = { entry ->
                viewModel.saveSchedule(entry)
                showDialog = false
                editingSchedule = null
            }
        )
    }
}

@Composable
fun FeedingScheduleCard(
    schedule: FeedingScheduleEntry,
    onToggleComplete: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val dateTime = remember(schedule.scheduledAt) {
        Instant.ofEpochMilli(schedule.scheduledAt).atZone(zoneId)
    }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (schedule.isCompleted)
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            else ThemeColorUtils.surface(Color.White)
        ),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFDA8041).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFFDA8041),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dateTime.toLocalTime().format(timeFormatter),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDA8041)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = schedule.feedType,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dateTime.toLocalDate().format(dateFormatter),
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeColorUtils.lightGray(Color.Gray)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onEdit() }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF7EC6E3))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEA7B76))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            ScheduleInfoRow(label = "Quantity", value = schedule.quantity, color = Color(0xFF2196F3))
            Spacer(modifier = Modifier.height(6.dp))
            ScheduleInfoRow(label = "Target Group", value = schedule.targetGroup, color = Color(0xFF6D6D6D))
            Spacer(modifier = Modifier.height(6.dp))
            ScheduleInfoRow(label = "Frequency", value = schedule.frequency, color = Color(0xFFFF9800))

            if (schedule.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = ThemeColorUtils.surface(Color.White),
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

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (schedule.isCompleted) "Completed" else "Pending",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (schedule.isCompleted) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { onToggleComplete(!schedule.isCompleted) }) {
                    Icon(
                        imageVector = if (schedule.isCompleted) Icons.Default.CheckCircle else Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = if (schedule.isCompleted) Color(0xFF4CAF50) else ThemeColorUtils.lightGray(Color.Gray)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleInfoRow(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ThemeColorUtils.lightGray(Color.Gray)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedingScheduleInputDialog(
    entry: FeedingScheduleEntry?,
    onDismiss: () -> Unit,
    onSave: (FeedingScheduleEntry) -> Unit
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val context = LocalContext.current

    var feedType by remember { mutableStateOf(entry?.feedType ?: "") }
    var quantity by remember { mutableStateOf(entry?.quantity ?: "") }
    var targetGroup by remember { mutableStateOf(entry?.targetGroup ?: "") }
    var frequency by remember { mutableStateOf(entry?.frequency ?: "") }
    var notes by remember { mutableStateOf(entry?.notes ?: "") }

    val initialDateMillis = entry?.scheduledAt ?: System.currentTimeMillis()
    var selectedDateMillis by remember { mutableLongStateOf(initialDateMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

    var selectedTime by remember {
        mutableStateOf(
            Instant.ofEpochMilli(initialDateMillis).atZone(zoneId).toLocalTime()
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
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
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White)),
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 580.dp)
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
                    text = if (entry == null) "Add Feeding Schedule" else "Edit Feeding Schedule",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = feedType,
                onValueChange = { feedType = it },
                label = { Text("Feed Type *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Quantity *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = targetGroup,
                onValueChange = { targetGroup = it },
                label = { Text("Target Group *") },
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

            val dateFieldInteraction = remember { MutableInteractionSource() }
            OutlinedTextField(
                value = Instant.ofEpochMilli(selectedDateMillis)
                    .atZone(zoneId)
                    .toLocalDate()
                    .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
                onValueChange = {},
                label = { Text("Schedule Date *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = dateFieldInteraction,
                        indication = null
                    ) { showDatePicker = true },
                readOnly = true,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            val timeFieldInteraction = remember { MutableInteractionSource() }
            OutlinedTextField(
                value = selectedTime.format(DateTimeFormatter.ofPattern("hh:mm a")),
                onValueChange = {},
                label = { Text("Schedule Time *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = timeFieldInteraction,
                        indication = null
                    ) {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                selectedTime = LocalTime.of(hour, minute)
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

            Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val selectedDate = Instant.ofEpochMilli(selectedDateMillis).atZone(zoneId).toLocalDate()
                            val scheduledAt = selectedDate.atTime(selectedTime).atZone(zoneId).toInstant().toEpochMilli()
                            onSave(
                                FeedingScheduleEntry(
                                    id = entry?.id ?: "",
                                    scheduledAt = scheduledAt,
                                    feedType = feedType.trim(),
                                    quantity = quantity.trim(),
                                    targetGroup = targetGroup.trim(),
                                    frequency = frequency.trim(),
                                    notes = notes.trim(),
                                    isCompleted = entry?.isCompleted ?: false,
                                    createdAt = entry?.createdAt ?: System.currentTimeMillis()
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = feedType.isNotBlank() && quantity.isNotBlank() && targetGroup.isNotBlank() && frequency.isNotBlank(),
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

private fun FeedingScheduleEntry.occursOn(date: LocalDate, zoneId: ZoneId): Boolean {
    val scheduleDate = Instant.ofEpochMilli(scheduledAt).atZone(zoneId).toLocalDate()
    return scheduleDate == date
}
