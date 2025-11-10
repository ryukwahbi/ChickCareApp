package com.bisu.chickcare.backend.service

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bisu.chickcare.backend.repository.FeedingScheduleEntry
import java.util.concurrent.TimeUnit

class FeedingScheduleNotificationManager(context: Context) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)

    fun schedule(entry: FeedingScheduleEntry) {
        if (entry.id.isBlank() || entry.isCompleted) {
            cancel(entry.id)
            return
        }

        val delayMillis = entry.scheduledAt - System.currentTimeMillis()
        if (delayMillis <= 0L) {
            cancel(entry.id)
            return
        }

        val data = workDataOf(
            FeedingScheduleReminderWorker.KEY_SCHEDULE_ID to entry.id,
            FeedingScheduleReminderWorker.KEY_FEED_TYPE to entry.feedType,
            FeedingScheduleReminderWorker.KEY_QUANTITY to entry.quantity,
            FeedingScheduleReminderWorker.KEY_TARGET_GROUP to entry.targetGroup,
            FeedingScheduleReminderWorker.KEY_SCHEDULED_AT to entry.scheduledAt,
            FeedingScheduleReminderWorker.KEY_TITLE to "Feeding Reminder",
            FeedingScheduleReminderWorker.KEY_MESSAGE to ""
        )

        val request = OneTimeWorkRequestBuilder<FeedingScheduleReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(WORK_TAG)
            .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName(entry.id),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(scheduleId: String) {
        if (scheduleId.isBlank()) return
        workManager.cancelUniqueWork(uniqueWorkName(scheduleId))
    }

    private fun uniqueWorkName(scheduleId: String): String =
        "${WORK_TAG}_$scheduleId"

    companion object {
        private const val WORK_TAG = "feeding_schedule_notification"
    }
}

