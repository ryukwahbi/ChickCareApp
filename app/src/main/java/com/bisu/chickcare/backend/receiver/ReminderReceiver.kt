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
        // Handle boot completed - reschedule all reminders and restart notification service
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            rescheduleAllReminders(context)
            
            // Restart foreground notification service if user is logged in
            try {
                if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
                    com.bisu.chickcare.backend.service.NotificationForegroundService.start(context)
                }
            } catch (e: Exception) {
                android.util.Log.e("ReminderReceiver", "Failed to start NotificationForegroundService on boot", e)
            }
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

        val soundUri = android.net.Uri.parse("android.resource://${context.packageName}/${com.bisu.chickcare.R.raw.notify_sound}")
        
        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(reminderTitle)
            .setContentText(reminderDescription)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminderDescription))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS or NotificationCompat.DEFAULT_VIBRATE) // Don't use DEFAULT_SOUND or DEFAULT_ALL
            .setSound(soundUri)
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
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .build()
                
            val soundUri = android.net.Uri.parse("android.resource://${context.packageName}/${com.bisu.chickcare.R.raw.notify_sound}")

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                setSound(soundUri, audioAttributes)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun rescheduleAllReminders(context: Context) {
        val reminderService = ReminderService(context)
        val prefs: SharedPreferences = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
        
        ReminderType.entries.forEach { type ->
            val hour = prefs.getInt("${type.name}_hour", -1)
            val minute = prefs.getInt("${type.name}_minute", -1)
            val enabled = prefs.getBoolean("${type.name}_enabled", false)
            
            if (enabled && hour >= 0 && minute >= 0) {
                val reminder = ReminderData(type, hour, minute, true)
                reminderService.scheduleDailyReminder(reminder)
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "daily_reminders_channel_v2"
        private const val CHANNEL_NAME = "Daily Reminders"
        private const val CHANNEL_DESCRIPTION = "Notifications for daily chicken care reminders"
    }
}

