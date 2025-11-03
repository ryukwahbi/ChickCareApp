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
    
    suspend fun sendAnnouncementToAll(title: String, message: String) = withContext(Dispatchers.IO) {
        notificationRepository.addAnnouncementToAllUsers(title, message)
    }
    
    suspend fun sendFriendRequest(senderId: String, senderName: String, receiverId: String) = withContext(Dispatchers.IO) {
        notificationRepository.addNotification(
            userId = receiverId,
            type = NotificationType.FRIEND_REQUEST,
            title = "Friend Request",
            message = "$senderName wants to be your friend",
            senderId = senderId,
            senderName = senderName,
            actionRequired = true
        )
    }
    
    suspend fun sendFriendAccept(senderId: String, senderName: String, receiverId: String) = withContext(Dispatchers.IO) {
        notificationRepository.addNotification(
            userId = receiverId,
            type = NotificationType.FRIEND_ACCEPT,
            title = "Friend Request Accepted",
            message = "$senderName accepted your friend request",
            senderId = senderId,
            senderName = senderName
        )
    }
    
    suspend fun sendSystemNotification(userId: String, title: String, message: String, type: NotificationType) = withContext(Dispatchers.IO) {
        notificationRepository.addNotification(
            userId = userId,
            type = type,
            title = title,
            message = message
        )
    }
}