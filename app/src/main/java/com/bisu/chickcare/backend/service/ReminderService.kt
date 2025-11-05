package com.bisu.chickcare.backend.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.bisu.chickcare.backend.receiver.ReminderReceiver
import java.util.Calendar

enum class ReminderType(val id: Int, val title: String, val description: String) {
    FEEDING_TIME(1, "Feeding time", "Ensure chickens are fed regularly"),
    WATER_CHECK(2, "Water check", "Check and refill water containers"),
    COOP_CLEANING(3, "Coop cleaning", "Maintain clean and dry coop environment"),
    EGG_COLLECTION(4, "Egg collection", "Collect eggs from the coop"),
    TEMPERATURE_CHECK(5, "Temperature check", "Monitor coop temperature levels"),
    COOP_VENTILATION(6, "Coop ventilation", "Check and maintain proper airflow")
}

data class ReminderData(
    val type: ReminderType? = null,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean,
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val icon: String = "⏰",
    val selectedDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7) // Default: All days (1=Sunday, 2=Monday, ..., 7=Saturday)
) {
    // Helper properties for compatibility
    val reminderTitle: String
        get() = type?.title ?: title
    val reminderDescription: String
        get() = type?.description ?: description
    val reminderIcon: String
        get() = when (type) {
            ReminderType.FEEDING_TIME -> "🍽️"
            ReminderType.WATER_CHECK -> "💧"
            ReminderType.COOP_CLEANING -> "🧹"
            ReminderType.EGG_COLLECTION -> "🥚"
            ReminderType.TEMPERATURE_CHECK -> "🌡️"
            ReminderType.COOP_VENTILATION -> "💨"
            null -> icon
        }
    
    // Get unique ID for alarm scheduling
    fun getAlarmId(): Int {
        return type?.id ?: id.hashCode()
    }
}

class ReminderService(private val context: Context) {
    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @Suppress("UNUSED")
    fun scheduleReminder(reminder: ReminderData) {
        if (!reminder.enabled) {
            cancelReminder(reminder)
            return
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("reminder_type", reminder.type?.name ?: reminder.id)
            putExtra("reminder_title", reminder.reminderTitle)
            putExtra("reminder_description", reminder.reminderDescription)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.getAlarmId(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ requires checking canScheduleExactAlarms()
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm if exact alarms are not allowed
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else
                // Android 6.0+ (API 23+)
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
        } catch (e: SecurityException) {
            // Fallback to inexact alarm if exact alarms are not allowed
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(reminder: ReminderData) {
        // Cancel alarms for all days (1-7)
        for (dayOfWeek in 1..7) {
            val requestCode = reminder.getAlarmId() * 10 + dayOfWeek
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
    
    fun cancelReminder(type: ReminderType) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            type.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleDailyReminder(reminder: ReminderData) {
        // Cancel all existing alarms for this reminder first
        cancelReminder(reminder)
        
        if (!reminder.enabled || reminder.selectedDays.isEmpty()) {
            return
        }

        // Schedule separate alarms for each selected day
        reminder.selectedDays.forEach { dayOfWeek ->
            scheduleWeeklyReminderForDay(reminder, dayOfWeek)
        }
    }
    
    private fun scheduleWeeklyReminderForDay(reminder: ReminderData, dayOfWeek: Int) {
        // Calendar.DAY_OF_WEEK: 1=Sunday, 2=Monday, ..., 7=Saturday
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // Calculate next occurrence of this day
            val currentDay = get(Calendar.DAY_OF_WEEK)
            var daysUntil = dayOfWeek - currentDay
            
            // If the day has passed this week or it's today but time has passed, schedule for next week
            if (daysUntil < 0 || (daysUntil == 0 && timeInMillis <= System.currentTimeMillis())) {
                daysUntil += 7
            }
            
            add(Calendar.DAY_OF_YEAR, daysUntil)
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("reminder_type", reminder.type?.name ?: reminder.id)
            putExtra("reminder_title", reminder.reminderTitle)
            putExtra("reminder_description", reminder.reminderDescription)
        }

        // Create unique request code for each day (base ID + day offset)
        val requestCode = reminder.getAlarmId() * 10 + dayOfWeek

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    // Schedule repeating weekly alarm for this specific day
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY * 7, // Weekly
                        pendingIntent
                    )
                } else {
                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY * 7,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY * 7,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntent
            )
        }
    }
}

