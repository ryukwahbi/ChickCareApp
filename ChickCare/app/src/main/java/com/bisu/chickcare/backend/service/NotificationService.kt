package com.bisu.chickcare.backend.service

import com.bisu.chickcare.backend.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationService(private val notificationRepository: NotificationRepository) {
    suspend fun sendDetectionNotification(userId: String, result: String) = withContext(Dispatchers.IO) {
        val message = "New detection result: $result at ${System.currentTimeMillis()}"
        notificationRepository.sendNotification(userId, message)
    }
}