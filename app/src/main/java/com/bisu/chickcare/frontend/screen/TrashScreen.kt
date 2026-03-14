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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.backend.repository.DetectionEntry
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(navController: NavController) {
    val dashboardViewModel: DashboardViewModel = viewModel()
    val recentlyDeleted by dashboardViewModel.recentlyDeleted.collectAsState()
    
    var selectionMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showRestoreAllDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showRestoreSelectedDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    var showEmptyingTrash by remember { mutableStateOf(false) }
    
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
                        "Trash",
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
                actions = {
                    if (selectionMode) {
                        TextButton(
                            onClick = {
                                selectedItems = emptySet()
                                selectionMode = false
                            }
                        ) {
                            Text("Cancel", color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16))
                        }
                    } else {
                        TextButton(
                            onClick = {
                                selectionMode = true
                            }
                        ) {
                            Text("Select", color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFF141617) else Color.White,
                    titleContentColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
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
                        Text("Recover Selected", color = ThemeColorUtils.white())
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
                        Text("Delete Selected", color = ThemeColorUtils.white())
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
                        TrashItemCard(
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
        CustomConfirmationDialog(
            title = "Restore All Items",
            message = "Are you sure you want to restore all deleted items?",
            confirmText = "Restore All",
            confirmColor = Color(0xFF4CAF50),
            onDismiss = { showRestoreAllDialog = false },
            onConfirm = {
                dashboardViewModel.restoreAllDetections()
                showRestoreAllDialog = false
                selectionMode = false
            }
        )
    }
    
    // Delete All Dialog (renamed to "Empty Trash?")
    if (showDeleteAllDialog) {
        CustomConfirmationDialog(
            title = "Empty Trash?",
            message = "You're about to delete all items from the Trash. This can't be undone.",
            confirmText = "Empty Trash",
            confirmColor = Color(0xFFE53935),
            onDismiss = { showDeleteAllDialog = false },
            onConfirm = {
                showDeleteAllDialog = false
                showEmptyingTrash = true
            }
        )
    }
    
    // Emptying Trash Loading Dialog
    if (showEmptyingTrash) {
        EmptyingTrashDialog(
            onComplete = {
                dashboardViewModel.permanentlyDeleteAllTrash()
                showEmptyingTrash = false
                selectionMode = false
            }
        )
    }
    
    // Restore Selected Dialog
    if (showRestoreSelectedDialog) {
        CustomConfirmationDialog(
            title = "Restore Selected Items",
            message = "Are you sure you want to restore ${selectedItems.size} item(s)?",
            confirmText = "Restore",
            confirmColor = Color(0xFF4CAF50),
            onDismiss = { showRestoreSelectedDialog = false },
            onConfirm = {
                selectedItems.forEach { id ->
                    dashboardViewModel.restoreDetection(id)
                }
                selectedItems = emptySet()
                selectionMode = false
                showRestoreSelectedDialog = false
            }
        )
    }
    
    // Delete Selected Dialog
    if (showDeleteSelectedDialog) {
        CustomConfirmationDialog(
            title = "Permanently Delete Selected",
            message = "Are you sure you want to permanently delete ${selectedItems.size} item(s)? This action cannot be undone.",
            confirmText = "Delete",
            confirmColor = Color(0xFFE53935),
            onDismiss = { showDeleteSelectedDialog = false },
            onConfirm = {
                selectedItems.forEach { id ->
                    dashboardViewModel.permanentlyDeleteDetection(id)
                }
                selectedItems = emptySet()
                selectionMode = false
                showDeleteSelectedDialog = false
            }
        )
    }
}

@Composable
fun TrashItemCard(
    entry: DetectionEntry,
    daysRemaining: Int,
    isSelected: Boolean,
    selectionMode: Boolean,
    onSelectChanged: (Boolean) -> Unit,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
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
                    modifier = Modifier.padding(end = 12.dp),
                    colors = androidx.compose.material3.CheckboxDefaults.colors(
                        checkedColor = Color(0xFF4D5961),
                        uncheckedColor = Color(0xFF4D5961),
                        checkmarkColor = Color.White
                    )
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
                // Format result text: "Status - Confidence%" instead of "Status (Confidence)"
                val formattedResult = remember(entry.result, entry.confidence) {
                    // Extract just the status word (Healthy/Infected) from entry.result
                    // entry.result might be "Infected (96.0%)" or "Healthy (87.4%)"
                    val statusText = when {
                        entry.result.contains("Healthy", ignoreCase = true) && 
                        !entry.result.contains("Unhealthy", ignoreCase = true) -> "Healthy"
                        entry.result.contains("Infected", ignoreCase = true) || 
                        entry.result.contains("Unhealthy", ignoreCase = true) -> "Infected"
                        entry.isHealthy -> "Healthy"
                        else -> "Infected"
                    }
                    
                    // Format as "Status - Confidence%"
                    if (entry.confidence > 0f) {
                        val confidencePercent = (entry.confidence * 100).let { 
                            if (it % 1 == 0f) it.toInt().toString() 
                            else String.format("%.1f", it)
                        }
                        "$statusText - $confidencePercent%"
                    } else {
                        statusText
                    }
                }
                
                Text(
                    text = formattedResult,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColorUtils.black()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ThemeViewModel.isDarkMode) Color(0xFFB0B0B0) else Color.Gray
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
                        onClick = { showDeleteDialog = true },
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
    
    // Permanent delete confirmation dialog
    if (showDeleteDialog) {
        CustomConfirmationDialog(
            title = "Permanently Delete",
            message = "Are you sure you want to permanently delete this item? This action cannot be undone.",
            confirmText = "Delete",
            confirmColor = Color(0xFFFF0000),
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                onPermanentDelete()
                showDeleteDialog = false
            }
        )
    }
}

@Composable
private fun CustomConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Background scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(onClick = onDismiss)
            )
            
            // Bottom sheet content
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = Color.White,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    // Title
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = com.bisu.chickcare.ui.theme.FiraSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ),
                        color = Color(0xFF1A1A1A)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Message
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF545454),
                        lineHeight = 22.sp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Confirm Button (Red, full width)
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = confirmText,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Cancel Button (White with border, full width)
                    androidx.compose.material3.OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color(0xFF1A1A1A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyingTrashDialog(
    onComplete: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }
    
    // Animate progress from 0 to 1 over 2 seconds
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        val duration = 2000L
        while (progress < 1f) {
            val elapsed = System.currentTimeMillis() - startTime
            progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            kotlinx.coroutines.delay(16) // ~60fps
        }
        kotlinx.coroutines.delay(300) // Small delay before completing
        onComplete()
    }
    
    Dialog(
        onDismissRequest = { /* Cannot dismiss while emptying */ },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Background scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )
            
            // Bottom sheet content
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = Color.White,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Trash Image
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.bisu.chickcare.R.drawable.trash_pic),
                        contentDescription = "Emptying Trash",
                        modifier = Modifier.size(100.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Title
                    Text(
                        text = "Emptying the trash",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = com.bisu.chickcare.ui.theme.FiraSans,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color(0xFF1A1A1A)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Subtitle
                    Text(
                        text = "This may take a few moments...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF545454),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Progress Bar
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Color(0xFFE0E0E0), RoundedCornerShape(3.dp)),
                        color = Color(0xFFE53935),
                        trackColor = Color(0xFFE0E0E0)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
