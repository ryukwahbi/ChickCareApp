package com.bisu.chickcare.backend.service

import com.bisu.chickcare.backend.repository.NotificationRepository
import com.bisu.chickcare.backend.repository.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationService(private val notificationRepository: NotificationRepository) {
    suspend fun sendDetectionNotification(userId: String, result: String) = withContext(Dispatchers.IO) {
        notificationRepository.addNotification(
            userId = userId,
            type = NotificationType.DETECTION_RESULT,
            title = "Detection Complete",
            message = "Your chicken health detection result: $result"
        )
    }
}