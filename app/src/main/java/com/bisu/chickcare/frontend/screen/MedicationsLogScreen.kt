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
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Warning
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

data class MedicationLog(
    val id: String,
    val medicationName: String,
    val chickenId: String?,
    val date: Long,
    val dosage: String,
    val frequency: String,
    val duration: String,
    val administeredBy: String,
    val notes: String,
    val isActive: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsLogScreen(navController: NavController) {
    var medications by remember {
        mutableStateOf(
            listOf(
                MedicationLog(
                    id = "1",
                    medicationName = "Amoxicillin",
                    chickenId = "CH001",
                    date = System.currentTimeMillis() - 172800000,
                    dosage = "500mg",
                    frequency = "Twice daily",
                    duration = "7 days",
                    administeredBy = "Dr. John Smith",
                    notes = "For respiratory infection",
                    isActive = true
                ),
                MedicationLog(
                    id = "2",
                    medicationName = "Doxycycline",
                    chickenId = null,
                    date = System.currentTimeMillis() - 86400000,
                    dosage = "200mg",
                    frequency = "Once daily",
                    duration = "5 days",
                    administeredBy = "Farm Staff",
                    notes = "Preventive treatment for whole flock",
                    isActive = true
                ),
                MedicationLog(
                    id = "3",
                    medicationName = "Ivermectin",
                    chickenId = "CH003",
                    date = System.currentTimeMillis() - 2592000000,
                    dosage = "0.2ml",
                    frequency = "Single dose",
                    duration = "1 day",
                    administeredBy = "Farm Staff",
                    notes = "Parasite treatment - Completed",
                    isActive = false
                )
            )
        )
    }

    val activeCount = medications.count { it.isActive }
    val completedCount = medications.count { !it.isActive }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Medications Log",
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
                onClick = { /* Add new medication */ },
                containerColor = Color(0xFFDA8041),
                contentColor = ThemeColorUtils.white()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Medication")
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
                    title = "Active",
                    count = activeCount,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Completed",
                    count = completedCount,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
            }

            // Filter Tabs
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

            Spacer(modifier = Modifier.height(8.dp))

            // Medications List
            val filteredMedications = when (selectedTab) {
                1 -> medications.filter { it.isActive }
                2 -> medications.filter { !it.isActive }
                else -> medications
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredMedications.sortedByDescending { it.date }, key = { it.id }) { medication ->
                    MedicationCard(medication = medication)
                }
            }
        }
    }
}

@Composable
fun MedicationCard(medication: MedicationLog) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val borderColor = if (medication.isActive) Color(0xFFFF9800) else Color(0xFF4CAF50)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (medication.isActive)
                Color(0xFFFF9800).copy(alpha = 0.1f)
            else ThemeColorUtils.white()
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
                            .background(
                                borderColor.copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = borderColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = medication.medicationName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (medication.chickenId != null) {
                            Text(
                                text = "Chicken ID: ${medication.chickenId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThemeColorUtils.lightGray(Color.Gray)
                            )
                        } else {
                            Text(
                                text = "Flock Treatment",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThemeColorUtils.lightGray(Color.Gray)
                            )
                        }
                    }
                }
                if (medication.isActive) {
                    Surface(
                        color = borderColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = borderColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ACTIVE",
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

            InfoRow(label = "Dosage", value = medication.dosage)
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(label = "Frequency", value = medication.frequency)
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(label = "Duration", value = medication.duration)
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow(label = "Administered By", value = medication.administeredBy)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = dateFormat.format(Date(medication.date)),
                style = MaterialTheme.typography.bodySmall,
                color = ThemeColorUtils.lightGray(Color.Gray)
            )

            if (medication.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = ThemeColorUtils.beige(Color(0xFFF5F5DC)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "📝 ${medication.notes}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                    )
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
