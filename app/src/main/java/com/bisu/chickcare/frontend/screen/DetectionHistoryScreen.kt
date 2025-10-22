package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionHistoryScreen(navController: NavController, paddingValues: PaddingValues) {
    val dashboardViewModel: DashboardViewModel = viewModel()
    val history by dashboardViewModel.detectionHistory.collectAsState()

    // FIX: Corrected indentation for the LazyColumn
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Date & Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Result",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        if (history.isEmpty()) {
            item {
                Text(
                    text = "No detection history found.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = Color.Gray
                )
            }
        }

        items(history, key = { it.id }) { entry ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable {
                        // When the card itself is clicked, navigate to the result
                        val imageUri = entry.imageUri?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) } ?: ""
                        val audioUri = entry.audioUri?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) } ?: ""
                        val status = URLEncoder.encode(entry.result, StandardCharsets.UTF_8.toString())
                        // Note: Suggestions are not stored in history, so we pass an empty list
                        val suggestions = ""
                        navController.navigate("detection_result?imageUri=$imageUri&audioUri=$audioUri&status=$status&suggestions=$suggestions")
                    },
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (entry.isHealthy) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = "Status Icon",
                        tint = if (entry.isHealthy) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = dashboardViewModel.formatDate(entry.timestamp),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        entry.result,
                        color = if (entry.isHealthy) Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    // The MoreVert icon can be removed if the whole card is clickable,
                    // or kept if it's for other options like 'Delete'.
                    // For now, let's assume the whole card click is enough.
                }
            }
        }
    }
}
