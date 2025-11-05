package com.bisu.chickcare.backend.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bisu.chickcare.MainActivity
import com.bisu.chickcare.backend.service.ReminderData
import com.bisu.chickcare.backend.service.ReminderService
import com.bisu.chickcare.backend.service.ReminderType

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Handle boot completed - reschedule all reminders
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            rescheduleAllReminders(context)
            return
        }

        val reminderTitle = intent.getStringExtra("reminder_title") ?: "Daily Reminder"
        val reminderDescription = intent.getStringExtra("reminder_description") ?: ""

        // Create notification channel for Android O and above
        createNotificationChannel(context)

        // Create intent to open app when notification is clicked
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(reminderTitle)
            .setContentText(reminderDescription)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminderDescription))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Show notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = intent.getStringExtra("reminder_type")?.hashCode() ?: System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun rescheduleAllReminders(context: Context) {
        val reminderService = ReminderService(context)
        val prefs: SharedPreferences = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
        
        ReminderType.values().forEach { type ->
            val hour = prefs.getInt("${type.name}_hour", -1)
            val minute = prefs.getInt("${type.name}_minute", -1)
            val enabled = prefs.getBoolean("${type.name}_enabled", false)
            
            if (enabled && hour >= 0 && minute >= 0) {
                val reminder = ReminderData(type, hour, minute, enabled)
                reminderService.scheduleDailyReminder(reminder)
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "daily_reminders_channel"
        private const val CHANNEL_NAME = "Daily Reminders"
        private const val CHANNEL_DESCRIPTION = "Notifications for daily chicken care reminders"
    }
}

