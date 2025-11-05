package com.bisu.chickcare.frontend.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisu.chickcare.backend.service.ReminderData
import com.bisu.chickcare.backend.service.ReminderType
import com.bisu.chickcare.backend.viewmodels.ReminderViewModel
import com.bisu.chickcare.frontend.utils.Dimens

@Composable
fun DailyRemindersCard() {
    val reminderViewModel: ReminderViewModel = viewModel()
    val reminders by reminderViewModel.reminders.collectAsState()
    val customReminders by reminderViewModel.customReminders.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ReminderData?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingLarge),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevation),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                top = Dimens.PaddingLarge,
                start = Dimens.PaddingLarge,
                end = Dimens.PaddingLarge,
                bottom = Dimens.PaddingLarge
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Daily Reminders",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = {
                        editingReminder = null
                        showDialog = true
                    },
                    modifier = Modifier
                        .background(Color(0xFF838181), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add reminder",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
            
            // Reminders list
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Predefined reminders
                reminders[ReminderType.FEEDING_TIME]?.let { reminder ->
                    EnhancedReminderItem(
                        reminder = reminder,
                        reminderViewModel = reminderViewModel,
                        onEditClick = {
                            editingReminder = reminder
                            showDialog = true
                        }
                    )
                }
                
                reminders[ReminderType.WATER_CHECK]?.let { reminder ->
                    EnhancedReminderItem(
                        reminder = reminder,
                        reminderViewModel = reminderViewModel,
                        onEditClick = {
                            editingReminder = reminder
                            showDialog = true
                        }
                    )
                }
                
                reminders[ReminderType.COOP_CLEANING]?.let { reminder ->
                    EnhancedReminderItem(
                        reminder = reminder,
                        reminderViewModel = reminderViewModel,
                        onEditClick = {
                            editingReminder = reminder
                            showDialog = true
                        }
                    )
                }
                
                reminders[ReminderType.EGG_COLLECTION]?.let { reminder ->
                    EnhancedReminderItem(
                        reminder = reminder,
                        reminderViewModel = reminderViewModel,
                        onEditClick = {
                            editingReminder = reminder
                            showDialog = true
                        }
                    )
                }
                
                reminders[ReminderType.TEMPERATURE_CHECK]?.let { reminder ->
                    EnhancedReminderItem(
                        reminder = reminder,
                        reminderViewModel = reminderViewModel,
                        onEditClick = {
                            editingReminder = reminder
                            showDialog = true
                        }
                    )
                }
                
                reminders[ReminderType.COOP_VENTILATION]?.let { reminder ->
                    EnhancedReminderItem(
                        reminder = reminder,
                        reminderViewModel = reminderViewModel,
                        onEditClick = {
                            editingReminder = reminder
                            showDialog = true
                        }
                    )
                }
                
                // Custom reminders
                customReminders.forEach { reminder ->
                    EnhancedReminderItem(
                        reminder = reminder,
                        reminderViewModel = reminderViewModel,
                        onEditClick = {
                            editingReminder = reminder
                            showDialog = true
                        }
                    )
                }
            }
        }
    }
    
    if (showDialog) {
        ReminderDialog(
            reminder = editingReminder,
            reminderViewModel = reminderViewModel,
            onDismiss = { showDialog = false },
            onSave = { title, description, icon, hour, minute, enabled, selectedDays ->
                if (editingReminder == null) {
                    reminderViewModel.addCustomReminder(title, description, icon, hour, minute, enabled, selectedDays)
                } else {
                    if (editingReminder?.type != null) {
                        reminderViewModel.updateReminder(editingReminder!!.type!!, hour, minute, enabled, selectedDays)
                    } else {
                        reminderViewModel.updateCustomReminder(
                            editingReminder!!.id,
                            title,
                            description,
                            icon,
                            hour,
                            minute,
                            enabled,
                            selectedDays
                        )
                    }
                }
                showDialog = false
            },
            onDelete = {
                if (editingReminder?.type == null && editingReminder?.id != null) {
                    reminderViewModel.deleteCustomReminder(editingReminder!!.id)
                }
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedReminderItem(
    reminder: ReminderData,
    reminderViewModel: ReminderViewModel,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = reminder.reminderIcon,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = reminder.reminderTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = reminder.reminderDescription,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = reminderViewModel.formatTime(reminder.hour, reminder.minute),
                style = MaterialTheme.typography.bodySmall,
                color = if (reminder.enabled) Color(0xFF1E2021) else Color.Gray,
                fontWeight = if (reminder.enabled) FontWeight.SemiBold else FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Show selected days
            val dayLabels = mapOf(1 to "Sun", 2 to "Mon", 3 to "Tue", 4 to "Wed", 5 to "Thu", 6 to "Fri", 7 to "Sat")
            val selectedDayLabels = reminder.selectedDays.sorted().map { dayLabels[it] ?: "" }.filter { it.isNotEmpty() }
            if (selectedDayLabels.size < 7) {
                Text(
                    text = "Days: ${selectedDayLabels.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
        Switch(
            checked = reminder.enabled,
            onCheckedChange = {
                if (reminder.type != null) {
                    reminderViewModel.toggleReminder(reminder.type)
                } else {
                    reminderViewModel.toggleCustomReminder(reminder.id)
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color(0xFF53585B)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDialog(
    reminder: ReminderData?,
    reminderViewModel: ReminderViewModel,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, Int, Boolean, Set<Int>) -> Unit,
    onDelete: () -> Unit
) {
    val isEditMode = reminder != null
    val isCustomReminder = reminder?.type == null
    
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val descriptionFocusRequester = remember { FocusRequester() }
    
    val emojiToTitle = mapOf(
        "🍽️" to "Feeding time",
        "💧" to "Water check",
        "🧹" to "Coop cleaning",
        "🥚" to "Egg collection",
        "🌡️" to "Temperature check",
        "💨" to "Coop ventilation"
    )
    
    var icon by remember {
        mutableStateOf(
            if (reminder != null && reminder.type == null) {
                reminder.reminderIcon
            } else {
                "🍽️"
            }
        )
    }
    
    // Initialize title based on reminder title or emoji mapping
    var title by remember { 
        mutableStateOf(
            if (reminder != null && reminder.type == null) {
                reminder.reminderTitle
            } else {
                ""
            }
        ) 
    }
    var description by remember { mutableStateOf(reminder?.reminderDescription ?: "") }
    var enabled by remember { mutableStateOf(reminder?.enabled ?: true) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDays by remember { 
        mutableStateOf(reminder?.selectedDays ?: setOf(1, 2, 3, 4, 5, 6, 7))
    }
    
    val timePickerState = rememberTimePickerState(
        initialHour = reminder?.hour ?: 7,
        initialMinute = reminder?.minute ?: 0,
        is24Hour = false
    )
    
    // Only show emojis that are visible in the card
    val availableIcons = listOf("🍽️", "💧", "🧹", "🥚", "🌡️", "💨")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (isEditMode) "Edit Reminder" else "Add Reminder")
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                if (isCustomReminder) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { }, // Read-only - cannot edit
                        label = { Text("Pick an emoji,") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false, // Make it read-only
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF24262A),
                            focusedLabelColor = Color(0xFF24262A),
                            disabledBorderColor = Color.Gray.copy(alpha = 0.5f),
                            disabledLabelColor = Color.Gray.copy(alpha = 0.7f),
                            disabledTextColor = Color.Black
                        )
                    )
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(descriptionFocusRequester),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF24262A),
                            focusedLabelColor = Color(0xFF24262A)
                        )
                    )
                    
                    Text("Select Icon:", style = MaterialTheme.typography.bodySmall)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availableIcons.size) { index ->
                            val selectedEmoji = availableIcons[index]
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (icon == selectedEmoji) Color(0xDF7E7E7E).copy(alpha = 0.3f) else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { 
                                        icon = selectedEmoji
                                        emojiToTitle[selectedEmoji]?.let {
                                            title = it 
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedEmoji,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = reminder.reminderTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = reminder.reminderDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Time:", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { showTimePicker = true }) {
                        Text(
                            reminderViewModel.formatTime(timePickerState.hour, timePickerState.minute),
                            color = Color.Black
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable notification:", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color(0xFF4A515E)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Day Selection
                Text("Select Days:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf(
                        "Sun" to 1,
                        "Mon" to 2,
                        "Tue" to 3,
                        "Wed" to 4,
                        "Thu" to 5,
                        "Fri" to 6,
                        "Sat" to 7
                    )
                    
                    days.forEach { (label, dayValue) ->
                        val isSelected = selectedDays.contains(dayValue)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .background(
                                    if (isSelected) Color(0xFFAFADAD) else Color.Green
                                )
                                .clickable {
                                    selectedDays = if (isSelected) {
                                        selectedDays - dayValue
                                    } else {
                                        selectedDays + dayValue
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(title, description, icon, timePickerState.hour, timePickerState.minute, enabled, selectedDays)
                }
            ) {
                Text("Save", color = Color.Green)
            }
        },
        dismissButton = {
            if (isEditMode && isCustomReminder) {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = Color.Red)
                }
            }
        }
    )
    
    if (showTimePicker) {
        TimePickerDialog(
            onConfirm = { 
                showTimePicker = false 
            },
            onDismiss = { showTimePicker = false },
            initialTime = timePickerState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    initialTime: TimePickerState
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Set", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Black)
            }
        },
        title = { Text("Select Time") },
        text = {
            TimePicker(
                state = initialTime,
                colors = TimePickerDefaults.colors(
                    clockDialSelectedContentColor = Color.White,
                    clockDialColor = Color(0xFF5C6062).copy(alpha = 0.3f),
                    selectorColor = Color(0xFF5C6062),
                    periodSelectorBorderColor = Color(0xFF5C6062),
                    periodSelectorSelectedContainerColor = Color(0xFF5C6062),
                    periodSelectorSelectedContentColor = Color.White
                )
            )
        }
    )
}

