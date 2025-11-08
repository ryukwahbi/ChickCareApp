package com.bisu.chickcare.frontend.screen

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.backend.repository.DetectionEntry
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlyDeletedScreen(navController: NavController) {
    val dashboardViewModel: DashboardViewModel = viewModel()
    val recentlyDeleted by dashboardViewModel.recentlyDeleted.collectAsState()
    
    var selectionMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showRestoreAllDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showRestoreSelectedDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    
    // Calculate days remaining for each item
    val itemsWithDaysRemaining = recentlyDeleted.map { entry ->
        val daysElapsed = (System.currentTimeMillis() - entry.deletedTimestamp) / (1000L * 60 * 60 * 24)
        val daysRemaining = (14 - daysElapsed).coerceAtLeast(0L).toInt()
        Pair(entry, daysRemaining)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Recently Deleted",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B4513)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF8B4513)
                        )
                    }
                },
                actions = {
                    if (selectionMode) {
                        // In selection mode, show Cancel, Recover All, and Delete All
                        TextButton(
                            onClick = {
                                selectedItems = emptySet()
                                selectionMode = false
                            }
                        ) {
                            Text("Cancel", color = Color(0xFF8B4513))
                        }
                    } else {
                        // Normal mode - show Select button
                        TextButton(
                            onClick = {
                                selectionMode = true
                            }
                        ) {
                            Text("Select", color = Color(0xFF8B4513))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeColorUtils.white(),
                    titleContentColor = Color(0xFF8B4513)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFD5AC8F))
        ) {
            // Action buttons when in selection mode
            if (selectionMode && selectedItems.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showRestoreSelectedDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(
                            Icons.Default.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = ThemeColorUtils.white()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Recover All", color = ThemeColorUtils.white())
                    }
                    Button(
                        onClick = { showDeleteSelectedDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = ThemeColorUtils.white()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete All", color = ThemeColorUtils.white())
                    }
                }
            } else if (selectionMode) {
                // Show Recover All and Delete All when selection mode is active but no items selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showRestoreAllDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(
                            Icons.Default.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = ThemeColorUtils.white()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Recover All", color = ThemeColorUtils.white())
                    }
                    Button(
                        onClick = { showDeleteAllDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = ThemeColorUtils.white()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete All", color = ThemeColorUtils.white())
                    }
                }
            }
            
            // List of Deleted Items
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (recentlyDeleted.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No deleted items found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThemeColorUtils.lightGray(Color.Gray)
                            )
                        }
                    }
                } else {
                    items(itemsWithDaysRemaining, key = { it.first.id }) { (entry, daysRemaining) ->
                        DeletedItemCard(
                            entry = entry,
                            daysRemaining = daysRemaining,
                            isSelected = selectedItems.contains(entry.id),
                            selectionMode = selectionMode,
                            onSelectChanged = { isSelected ->
                                selectedItems = if (isSelected) {
                                    selectedItems + entry.id
                                } else {
                                    selectedItems - entry.id
                                }
                            },
                            onRestore = {
                                dashboardViewModel.restoreDetection(entry.id)
                            },
                            onPermanentDelete = {
                                dashboardViewModel.permanentlyDeleteDetection(entry.id)
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Restore All Dialog
    if (showRestoreAllDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreAllDialog = false },
            title = { Text("Restore All Items") },
            text = { Text("Are you sure you want to restore all deleted items?") },
            confirmButton = {
                Button(
                    onClick = {
                        dashboardViewModel.restoreAllDetections()
                        showRestoreAllDialog = false
                        selectionMode = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Restore All", color = ThemeColorUtils.white())
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete All Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Permanently Delete All") },
            text = { Text("Are you sure you want to permanently delete all items? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        dashboardViewModel.permanentlyDeleteAllTrash()
                        showDeleteAllDialog = false
                        selectionMode = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete All", color = ThemeColorUtils.white())
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Restore Selected Dialog
    if (showRestoreSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreSelectedDialog = false },
            title = { Text("Restore Selected Items") },
            text = { Text("Are you sure you want to restore ${selectedItems.size} item(s)?") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedItems.forEach { id ->
                            dashboardViewModel.restoreDetection(id)
                        }
                        selectedItems = emptySet()
                        selectionMode = false
                        showRestoreSelectedDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Restore", color = ThemeColorUtils.white())
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreSelectedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete Selected Dialog
    if (showDeleteSelectedDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedDialog = false },
            title = { Text("Permanently Delete Selected") },
            text = { Text("Are you sure you want to permanently delete ${selectedItems.size} item(s)? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedItems.forEach { id ->
                            dashboardViewModel.permanentlyDeleteDetection(id)
                        }
                        selectedItems = emptySet()
                        selectionMode = false
                        showDeleteSelectedDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete", color = ThemeColorUtils.white())
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DeletedItemCard(
    entry: DetectionEntry,
    daysRemaining: Int,
    isSelected: Boolean,
    selectionMode: Boolean,
    onSelectChanged: (Boolean) -> Unit,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    val statusColor = if (entry.isHealthy) Color(0xFF4CAF50) else Color(0xFFF44336)
    val statusIcon = if (entry.isHealthy) Icons.Default.CheckCircle else Icons.Default.Cancel
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(enabled = selectionMode) {
                if (selectionMode) {
                    onSelectChanged(!isSelected)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected && selectionMode) Color(0xFFE3F2FD) else ThemeColorUtils.white()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection Checkbox (shown in selection mode)
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectChanged,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
            
            // Status Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(statusColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.result,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF26201C)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeColorUtils.lightGray(Color.Gray)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (daysRemaining > 0) {
                        "Will be permanently deleted in $daysRemaining day(s)"
                    } else {
                        "Will be permanently deleted soon"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (daysRemaining <= 3) Color.Red else Color(0xFF9E9E9E),
                    fontSize = 12.sp
                )
            }
            
            // Action buttons (only shown when not in selection mode)
            if (!selectionMode) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onRestore,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Restore,
                            contentDescription = "Restore",
                            tint = Color(0xFF4CAF50)
                        )
                    }
                    IconButton(
                        onClick = onPermanentDelete,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Permanently Delete",
                            tint = Color.Red
                        )
                    }
                }
            }
        }
    }
}
