package com.bisu.chickcare.backend.service

import android.Manifest
//noinspection SuspiciousImport
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bisu.chickcare.MainActivity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FeedingScheduleReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        val scheduleId = inputData.getString(KEY_SCHEDULE_ID) ?: return Result.success()
        val feedType = inputData.getString(KEY_FEED_TYPE).orEmpty()
        val quantity = inputData.getString(KEY_QUANTITY).orEmpty()
        val targetGroup = inputData.getString(KEY_TARGET_GROUP).orEmpty()
        val scheduledAt = inputData.getLong(KEY_SCHEDULED_AT, 0L)
        val customTitle = inputData.getString(KEY_TITLE).orEmpty()
        val customMessage = inputData.getString(KEY_MESSAGE)
            ?.takeIf { it.isNotBlank() }

        if (scheduledAt <= 0L) {
            return Result.success()
        }

        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a")
            .withZone(ZoneId.systemDefault())
        val timeText = formatter.format(Instant.ofEpochMilli(scheduledAt))

        val title = customTitle.ifBlank { "Feeding Reminder" }
        val message = customMessage ?: buildString {
            append(feedType.ifBlank { "Scheduled feeding" })
            if (quantity.isNotBlank()) {
                append(" • $quantity")
            }
            if (targetGroup.isNotBlank()) {
                append("\nTarget group: $targetGroup")
            }
            append("\n$timeText")
        }

        showNotification(scheduleId.hashCode(), title, message)

        return Result.success()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(notificationId: Int, title: String, message: String) {
        createNotificationChannel()

        val openAppIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = android.net.Uri.parse("android.resource://${applicationContext.packageName}/${com.bisu.chickcare.R.raw.notify_sound}")

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS or NotificationCompat.DEFAULT_VIBRATE) // Don't use DEFAULT_SOUND or DEFAULT_ALL
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val soundUri = android.net.Uri.parse("android.resource://${applicationContext.packageName}/${com.bisu.chickcare.R.raw.notify_sound}")

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

            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val KEY_SCHEDULE_ID = "schedule_id"
        const val KEY_FEED_TYPE = "feed_type"
        const val KEY_QUANTITY = "quantity"
        const val KEY_TARGET_GROUP = "target_group"
        const val KEY_SCHEDULED_AT = "scheduled_at"
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"

        private const val CHANNEL_ID = "feeding_schedule_channel_v2"
        private const val CHANNEL_NAME = "Feeding Schedule Alerts"
        private const val CHANNEL_DESCRIPTION = "Notifications for upcoming feeding schedules"
    }
}

