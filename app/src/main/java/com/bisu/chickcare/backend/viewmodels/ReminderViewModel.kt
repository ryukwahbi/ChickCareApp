package com.bisu.chickcare.backend.viewmodels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.service.ReminderData
import com.bisu.chickcare.backend.service.ReminderService
import com.bisu.chickcare.backend.service.ReminderType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val reminderService = ReminderService(application)
    private val prefs: SharedPreferences = application.getSharedPreferences("reminders", Context.MODE_PRIVATE)

    private val _reminders = MutableStateFlow<Map<ReminderType, ReminderData>>(emptyMap())
    val reminders: StateFlow<Map<ReminderType, ReminderData>> = _reminders.asStateFlow()
    
    private val _customReminders = MutableStateFlow<List<ReminderData>>(emptyList())
    val customReminders: StateFlow<List<ReminderData>> = _customReminders.asStateFlow()

    init {
        loadReminders()
        loadCustomReminders()
    }

    private fun loadReminders() {
        val remindersMap = mutableMapOf<ReminderType, ReminderData>()
        
        ReminderType.entries.forEach { type ->
            val hour = prefs.getInt("${type.name}_hour", getDefaultHour(type))
            val minute = prefs.getInt("${type.name}_minute", getDefaultMinute(type))
            val enabled = prefs.getBoolean("${type.name}_enabled", false)
            val selectedDaysString = prefs.getString("${type.name}_days", "1,2,3,4,5,6,7") ?: "1,2,3,4,5,6,7"
            val selectedDays = selectedDaysString.split(",").mapNotNull { it.toIntOrNull() }.toSet()
            
            remindersMap[type] = ReminderData(
                type = type, 
                hour = hour, 
                minute = minute, 
                enabled = enabled,
                selectedDays = selectedDays
            )
        }
        
        _reminders.value = remindersMap
    }
    
    private fun loadCustomReminders() {
        val customRemindersList = mutableListOf<ReminderData>()
        
        try {
            val remindersString = prefs.getString("custom_reminders_list", "") ?: ""
            if (remindersString.isNotEmpty()) {
                remindersString.split(";").forEach { reminderStr ->
                    if (reminderStr.isNotEmpty()) {
                        val parts = reminderStr.split("|")
                        if (parts.size >= 7) {
                            val selectedDaysString = parts.getOrNull(7) ?: "1,2,3,4,5,6,7"
                            val selectedDays = selectedDaysString.split(",").mapNotNull { it.toIntOrNull() }.toSet()
                            
                            customRemindersList.add(
                                ReminderData(
                                    type = null,
                                    hour = parts[4].toIntOrNull() ?: 0,
                                    minute = parts[5].toIntOrNull() ?: 0,
                                    enabled = parts[6].toBoolean(),
                                    id = parts[0],
                                    title = parts[1],
                                    description = parts[2],
                                    icon = parts[3],
                                    selectedDays = selectedDays
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Handle parsing errors
        }
        
        _customReminders.value = customRemindersList
    }

    fun updateReminder(type: ReminderType, hour: Int, minute: Int, enabled: Boolean, selectedDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7)) {
        viewModelScope.launch {
            val updatedReminder = ReminderData(
                type = type, 
                hour = hour, 
                minute = minute, 
                enabled = enabled,
                selectedDays = selectedDays
            )
            val updatedMap = _reminders.value.toMutableMap()
            updatedMap[type] = updatedReminder
            _reminders.value = updatedMap

            // Save to SharedPreferences
            prefs.edit().apply {
                putInt("${type.name}_hour", hour)
                putInt("${type.name}_minute", minute)
                putBoolean("${type.name}_enabled", enabled)
                putString("${type.name}_days", selectedDays.joinToString(","))
                apply()
            }

            // Schedule or cancel reminder
            if (enabled) {
                reminderService.scheduleDailyReminder(updatedReminder)
            } else {
                reminderService.cancelReminder(type)
            }
        }
    }
    
    fun addCustomReminder(title: String, description: String, icon: String, hour: Int, minute: Int, enabled: Boolean, selectedDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7)) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val newReminder = ReminderData(
                type = null,
                hour = hour,
                minute = minute,
                enabled = enabled,
                id = id,
                title = title,
                description = description,
                icon = icon,
                selectedDays = selectedDays
            )
            
            val updatedList = _customReminders.value.toMutableList()
            updatedList.add(newReminder)
            _customReminders.value = updatedList
            
            // Save to SharedPreferences
            saveCustomReminders(updatedList)
            
            // Schedule reminder
            if (enabled) {
                reminderService.scheduleDailyReminder(newReminder)
            }
        }
    }
    
    fun updateCustomReminder(reminderId: String, title: String, description: String, icon: String, hour: Int, minute: Int, enabled: Boolean, selectedDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7)) {
        viewModelScope.launch {
            val updatedList = _customReminders.value.map { reminder ->
                if (reminder.id == reminderId) {
                    ReminderData(
                        type = null,
                        hour = hour,
                        minute = minute,
                        enabled = enabled,
                        id = reminderId,
                        title = title,
                        description = description,
                        icon = icon,
                        selectedDays = selectedDays
                    )
                } else {
                    reminder
                }
            }
            _customReminders.value = updatedList
            
            // Save to SharedPreferences
            saveCustomReminders(updatedList)
            
            // Cancel old and schedule new
            val oldReminder = _customReminders.value.find { it.id == reminderId }
            oldReminder?.let { reminderService.cancelReminder(it) }
            
            if (enabled) {
                val updatedReminder = updatedList.find { it.id == reminderId }!!
                reminderService.scheduleDailyReminder(updatedReminder)
            }
        }
    }
    
    fun deleteCustomReminder(reminderId: String) {
        viewModelScope.launch {
            val reminder = _customReminders.value.find { it.id == reminderId }
            reminder?.let { reminderService.cancelReminder(it) }
            
            val updatedList = _customReminders.value.filter { it.id != reminderId }
            _customReminders.value = updatedList
            
            saveCustomReminders(updatedList)
        }
    }
    
    private fun saveCustomReminders(reminders: List<ReminderData>) {
        val remindersString = reminders.joinToString(";") { reminder ->
            "${reminder.id}|${reminder.title}|${reminder.description}|${reminder.icon}|${reminder.hour}|${reminder.minute}|${reminder.enabled}|${reminder.selectedDays.joinToString(",")}"
        }
        prefs.edit { putString("custom_reminders_list", remindersString) }
    }

    fun toggleReminder(type: ReminderType) {
        val currentReminder = _reminders.value[type] ?: return
        updateReminder(
            type,
            currentReminder.hour,
            currentReminder.minute,
            !currentReminder.enabled,
            currentReminder.selectedDays
        )
    }
    
    fun toggleCustomReminder(reminderId: String) {
        val reminder = _customReminders.value.find { it.id == reminderId } ?: return
        updateCustomReminder(
            reminderId,
            reminder.title,
            reminder.description,
            reminder.icon,
            reminder.hour,
            reminder.minute,
            !reminder.enabled,
            reminder.selectedDays
        )
    }

    private fun getDefaultHour(type: ReminderType): Int {
        return when (type) {
            ReminderType.FEEDING_TIME -> 7  // 7:00 AM
            ReminderType.WATER_CHECK -> 8   // 8:00 AM
            ReminderType.COOP_CLEANING -> 9 // 9:00 AM
            ReminderType.EGG_COLLECTION -> 10 // 10:00 AM
            ReminderType.TEMPERATURE_CHECK -> 14 // 2:00 PM
            ReminderType.COOP_VENTILATION -> 16 // 4:00 PM
        }
    }

    private fun getDefaultMinute(@Suppress("UNUSED_PARAMETER") type: ReminderType): Int {
        return 0
    }

    fun formatTime(hour: Int, minute: Int): String {
        val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val amPm = if (hour < 12) "AM" else "PM"
        return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, amPm)
    }
}

