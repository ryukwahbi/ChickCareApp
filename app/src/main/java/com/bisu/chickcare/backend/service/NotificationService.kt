package com.bisu.chickcare.backend.service

import com.bisu.chickcare.backend.repository.NotificationRepository
import com.bisu.chickcare.backend.repository.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationService(private val notificationRepository: NotificationRepository) {
    /**
     * Send detection result notification to user
     */
    suspend fun sendDetectionNotification(
        userId: String, 
        result: String, 
        imageUri: String? = null, 
        audioUri: String? = null
    ) = withContext(Dispatchers.IO) {
        notificationRepository.addNotification(
            userId = userId,
            type = NotificationType.DETECTION_RESULT,
            title = "Detection Complete",
            message = "Your chicken health detection result: $result",
            imageUri = imageUri,
            audioUri = audioUri
        )
    }
    
    /**
     * Send notification when data is permanently deleted
     */
    suspend fun sendDataDeletedNotification(userId: String, dataType: String = "Detection") = withContext(Dispatchers.IO) {
        notificationRepository.addNotification(
            userId = userId,
            type = NotificationType.DATA_DELETED,
            title = "Data Deleted",
            message = "Your $dataType has been permanently deleted."
        )
    }
    
    /**
     * Send notification when profile is updated
     */
    suspend fun sendProfileUpdateNotification(userId: String, fieldName: String) = withContext(Dispatchers.IO) {
        notificationRepository.addNotification(
            userId = userId,
            type = NotificationType.PROFILE_UPDATE,
            title = "Profile Updated",
            message = "Your $fieldName has been updated successfully."
        )
    }
    
    /**
     * Send system update notification to user
     * Can be used for app updates, maintenance notices, etc.
     */
    @Suppress("UNUSED")
    suspend fun sendSystemUpdateNotification(userId: String, title: String, message: String) = withContext(Dispatchers.IO) {
        notificationRepository.addNotification(
            userId = userId,
            type = NotificationType.SYSTEM_UPDATE,
            title = title,
            message = message
        )
    }
    
    /**
     * Send announcement to all users
     * This can be called from Firebase Cloud Functions or admin panel
     * Common announcements:
     * - App updates and new features
     * - Maintenance schedules
     * - Important health tips and reminders
     * - Seasonal vaccination schedules
     * - System-wide alerts
     */
    @Suppress("UNUSED")
    suspend fun sendAnnouncementToAllUsers(title: String, message: String) = withContext(Dispatchers.IO) {
        notificationRepository.addAnnouncementToAllUsers(title, message)
    }
    
    /**
     * Send announcement to a specific user
     * Useful for targeted announcements
     */
    @Suppress("UNUSED")
    suspend fun sendAnnouncementToUser(userId: String, title: String, message: String) = withContext(Dispatchers.IO) {
        notificationRepository.addNotification(
            userId = userId,
            type = NotificationType.ANNOUNCEMENT,
            title = title,
            message = message
        )
    }
    
    /**
     * Send welcome announcement to new users
     * Automatically called when a new user signs up
     */
    suspend fun sendWelcomeNotification(userId: String, userName: String) = withContext(Dispatchers.IO) {
        notificationRepository.addNotification(
            userId = userId,
            type = NotificationType.ANNOUNCEMENT,
            title = "Welcome to ChickCare, $userName! 🐔",
            message = "Welcome aboard! 🐔 We're glad you're here. Start monitoring your chickens health with our smart detection system."
        )
    }
}