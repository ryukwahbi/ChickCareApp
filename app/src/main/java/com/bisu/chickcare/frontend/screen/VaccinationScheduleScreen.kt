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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.backend.repository.Vaccination
import com.bisu.chickcare.backend.viewmodels.VaccinationViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationScheduleScreen(navController: NavController) {
    val viewModel: VaccinationViewModel = viewModel()
    val vaccinations by viewModel.vaccinations.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingVaccination by remember { mutableStateOf<Vaccination?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Calculate status counts
    val currentTime = System.currentTimeMillis()
    val upcomingCount = vaccinations.count { it.date > currentTime }
    val completedCount = vaccinations.count { it.date <= currentTime }
    val overdueCount = vaccinations.count { 
        it.nextDueDate > 0 && it.nextDueDate < currentTime && it.date <= currentTime
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Vaccination Schedule",
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
                    editingVaccination = null
                    showAddDialog = true
                },
                containerColor = Color(0xFF8F8C8A),
                contentColor = ThemeColorUtils.white()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Vaccination")
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
                        title = "Upcoming",
                        count = upcomingCount,
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
                        title = "Overdue",
                        count = overdueCount,
                        color = Color(0xFFF44336),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Filter Tabs
                val tabs = listOf("All", "Upcoming", "Completed", "Overdue")
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

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = ThemeColorUtils.lightGray(Color(0xFFBDBDBD)),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Vaccinations List
                val filteredVaccinations = when (selectedTab) {
                    1 -> vaccinations.filter { it.date > currentTime }
                    2 -> vaccinations.filter { it.date <= currentTime }
                    3 -> vaccinations.filter { 
                        it.nextDueDate > 0 && it.nextDueDate < currentTime && it.date <= currentTime
                    }
                    else -> vaccinations
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredVaccinations.isEmpty()) {
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
                                        Icons.Default.Vaccines,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = ThemeColorUtils.lightGray(Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No vaccinations found",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = ThemeColorUtils.lightGray(Color.Gray)
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredVaccinations, key = { it.id }) { vaccination ->
                            VaccinationCard(
                                vaccination = vaccination,
                                onEdit = {
                                    editingVaccination = vaccination
                                    showAddDialog = true
                                },
                                onDelete = {
                                    viewModel.deleteVaccination(vaccination.id)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Add/Edit Dialog
        if (showAddDialog) {
            VaccinationInputDialog(
                vaccination = editingVaccination,
                onDismiss = {
                    showAddDialog = false
                    editingVaccination = null
                },
                onSave = { vaccination ->
                    viewModel.saveVaccination(vaccination)
                    showAddDialog = false
                    editingVaccination = null
                }
            )
        }
    }
}

@Composable
fun VaccinationInputDialog(
    vaccination: Vaccination?,
    onDismiss: () -> Unit,
    onSave: (Vaccination) -> Unit
) {
    var chickenId by remember { mutableStateOf(vaccination?.chickenId ?: "") }
    var vaccineName by remember { mutableStateOf(vaccination?.vaccineName ?: "") }
    var batchNumber by remember { mutableStateOf(vaccination?.batchNumber ?: "") }
    var administeredBy by remember { mutableStateOf(vaccination?.administeredBy ?: "") }
    var notes by remember { mutableStateOf(vaccination?.notes ?: "") }
    var hasNextDueDate by remember { mutableStateOf((vaccination?.nextDueDate ?: -1L) > 0) }
    var daysUntilNext by remember {
        mutableLongStateOf(
            if ((vaccination?.nextDueDate ?: -1L) > 0) {
                ((vaccination?.nextDueDate ?: 0L) - (vaccination?.date
                    ?: System.currentTimeMillis())) / (1000 * 60 * 60 * 24)
            } else 0
        )
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
                    .width(485.dp)
                    .heightIn(max = 580.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White))
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
                            text = if (vaccination == null) "Add Vaccination" else "Edit Vaccination",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = vaccineName,
                        onValueChange = { vaccineName = it },
                        label = { Text("Vaccine Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = chickenId,
                        onValueChange = { chickenId = it },
                        label = { Text("Chicken ID (Optional - leave empty for flock)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = batchNumber,
                        onValueChange = { batchNumber = it },
                        label = { Text("Batch Number *") },
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = hasNextDueDate,
                            onCheckedChange = { hasNextDueDate = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Has next due date")
                    }

                    if (hasNextDueDate) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = daysUntilNext.toString(),
                            onValueChange = {
                                val days = it.toIntOrNull() ?: 0
                                daysUntilNext = days.toLong()
                            },
                            label = { Text("Days until next vaccination") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

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
                            if (vaccineName.isNotBlank() && batchNumber.isNotBlank() && administeredBy.isNotBlank()) {
                                val currentDate = vaccination?.date ?: System.currentTimeMillis()
                                val nextDue = if (hasNextDueDate && daysUntilNext > 0) {
                                    currentDate + (daysUntilNext * 24 * 60 * 60 * 1000L)
                                } else -1L

                                val newVaccination = Vaccination(
                                    id = vaccination?.id ?: "",
                                    chickenId = chickenId.takeIf { it.isNotBlank() },
                                    vaccineName = vaccineName.trim(),
                                    date = currentDate,
                                    nextDueDate = nextDue,
                                    batchNumber = batchNumber.trim(),
                                    administeredBy = administeredBy.trim(),
                                    notes = notes.trim()
                                )
                                onSave(newVaccination)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = vaccineName.isNotBlank() && batchNumber.isNotBlank() && administeredBy.isNotBlank(),
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
    fun VaccinationCard(
        vaccination: Vaccination,
        onEdit: () -> Unit,
        onDelete: () -> Unit
    ) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val currentTime = System.currentTimeMillis()
        val isUpcoming = vaccination.date > currentTime
        val isOverdue =
            vaccination.nextDueDate > 0 && vaccination.nextDueDate < currentTime && vaccination.date <= currentTime

        val borderColor = when {
            isUpcoming -> Color(0xFF2196F3)
            isOverdue -> Color(0xFFF44336)
            else -> Color(0xFF4CAF50)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isUpcoming || isOverdue)
                    borderColor.copy(alpha = 0.1f)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(borderColor.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Vaccines,
                                contentDescription = null,
                                tint = borderColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = vaccination.vaccineName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (vaccination.chickenId != null) {
                                Text(
                                    text = "Chicken ID: ${vaccination.chickenId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThemeColorUtils.lightGray(Color.Gray)
                                )
                            } else {
                                Text(
                                    text = "Flock Vaccination",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThemeColorUtils.lightGray(Color.Gray)
                                )
                            }
                        }
                    }
                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color(0xFF7EC6E3)
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFEA7B76)
                            )
                        }
                    }
                    if (isUpcoming || isOverdue) {
                        Surface(
                            color = borderColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = borderColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isOverdue) "OVERDUE" else "UPCOMING",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = borderColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                InfoRow(
                    label = "Administered Date",
                    value = dateFormat.format(Date(vaccination.date))
                )

                if (vaccination.nextDueDate > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(
                        label = "Next Due Date",
                        value = dateFormat.format(Date(vaccination.nextDueDate)),
                        valueColor = if (isOverdue) Color(0xFFF44336) else Color(0xFF2196F3)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(label = "Batch Number", value = vaccination.batchNumber)

                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(label = "Administered By", value = vaccination.administeredBy)

                if (vaccination.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notes: ${vaccination.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeColorUtils.lightGray(Color.Gray),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }

@Composable
fun InfoRow(label: String, value: String, valueColor: Color = ThemeColorUtils.black()) {
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
            color = valueColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
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