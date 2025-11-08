package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Download
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

data class Report(
    val id: String,
    val title: String,
    val description: String,
    val type: ReportType,
    val lastGenerated: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

enum class ReportType {
    HEALTH, PRODUCTION, FINANCIAL, COMPREHENSIVE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsAnalyticsScreen(navController: NavController) {
    var reports by remember {
        mutableStateOf(
            listOf(
                Report(
                    id = "1",
                    title = "Health Summary Report",
                    description = "Comprehensive health records, vaccination history, and medication logs for the selected period",
                    type = ReportType.HEALTH,
                    lastGenerated = "2 days ago",
                    icon = Icons.Default.BarChart
                ),
                Report(
                    id = "2",
                    title = "Egg Production Report",
                    description = "Daily, weekly, and monthly egg production statistics with trends and analysis",
                    type = ReportType.PRODUCTION,
                    lastGenerated = "1 week ago",
                    icon = Icons.Default.BarChart
                ),
                Report(
                    id = "3",
                    title = "Financial Report",
                    description = "Expense tracking, cost analysis, and revenue calculations by category",
                    type = ReportType.FINANCIAL,
                    lastGenerated = "3 days ago",
                    icon = Icons.Default.BarChart
                ),
                Report(
                    id = "4",
                    title = "Growth & Development Report",
                    description = "Weight tracking, growth metrics, and developmental milestones analysis",
                    type = ReportType.HEALTH,
                    lastGenerated = "5 days ago",
                    icon = Icons.Default.BarChart
                ),
                Report(
                    id = "5",
                    title = "Comprehensive Farm Report",
                    description = "Complete overview including all metrics, health, production, and financial data",
                    type = ReportType.COMPREHENSIVE,
                    lastGenerated = "Never",
                    icon = Icons.Default.BarChart
                ),
                Report(
                    id = "6",
                    title = "Vaccination Schedule Report",
                    description = "Upcoming vaccinations, completion status, and vaccination history",
                    type = ReportType.HEALTH,
                    lastGenerated = "1 week ago",
                    icon = Icons.Default.BarChart
                )
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Reports & Analytics",
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ThemeColorUtils.beige(Color(0xFFF5F5DC)))
        ) {
            // Quick Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickStatCard(
                    title = "Total Reports",
                    value = reports.size.toString(),
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )
                QuickStatCard(
                    title = "Available",
                    value = reports.count { it.lastGenerated != "Never" }.toString(),
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
            }

            // Report Categories
            Text(
                text = "Available Reports",
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
                items(reports, key = { it.id }) { report ->
                    ReportCard(
                        report = report,
                        onGenerate = { /* Generate report */ },
                        onShare = { /* Share report */ },
                        onDownload = { /* Download report */ }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickStatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
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
                text = value,
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
fun ReportCard(
    report: Report,
    onGenerate: () -> Unit,
    onShare: () -> Unit,
    onDownload: () -> Unit
) {
    val typeColor = when (report.type) {
        ReportType.HEALTH -> Color(0xFF2196F3)
        ReportType.PRODUCTION -> Color(0xFFFF9800)
        ReportType.FINANCIAL -> Color(0xFF4CAF50)
        ReportType.COMPREHENSIVE -> Color(0xFF9C27B0)
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
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(typeColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        report.icon,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = typeColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = report.type.name,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyMedium,
                color = ThemeColorUtils.lightGray(Color.Gray)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Last generated: ${report.lastGenerated}",
                style = MaterialTheme.typography.bodySmall,
                color = if (report.lastGenerated == "Never") Color(0xFFFF9800) else ThemeColorUtils.lightGray(Color.Gray)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onGenerate,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = typeColor
                    )
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Generate")
                }
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF2196F3)
                    )
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share")
                }
                OutlinedButton(
                    onClick = onDownload,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
