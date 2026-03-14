package com.bisu.chickcare.backend.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bisu.chickcare.MainActivity
import com.bisu.chickcare.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChickCareMessagingService : FirebaseMessagingService() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var settingsRepository: com.bisu.chickcare.backend.repository.SettingsRepository
    
    override fun onCreate() {
        super.onCreate()
        settingsRepository = com.bisu.chickcare.backend.repository.SettingsRepository(this)
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Ensure initialized (in case onCreate wasn't called in some edge cases for services)
        if (!::settingsRepository.isInitialized) {
            settingsRepository = com.bisu.chickcare.backend.repository.SettingsRepository(this)
        }
        
        // Check global push notification setting
        if (!settingsRepository.getPushNotificationsEnabled()) {
            Log.d(TAG, "Push notifications disabled by user settings. Ignoring.")
            return
        }
        
        Log.d(TAG, "Notification received: ${remoteMessage.notification?.title}")
        
        val notificationType = remoteMessage.data["type"] ?: "GENERAL"
        
        // Handle chat notifications differently
        // Handle chat notifications differently
        when (notificationType) {
            "CHAT_MESSAGE" -> handleChatNotification(remoteMessage)
            "FRIEND_REQUEST", "FRIEND_ACCEPT", "FOLLOW", "REACTION", "COMMENT", "NEW_POST" -> {
                handleSocialNotification(remoteMessage)
            }
            else -> {
                // Check specific category settings for other types if needed
                // For now, assuming "General" falls under Push Notifications
                
                // Handle other notifications (detection, announcements, etc.)
                // Fallback: Check data if notification payload is missing
                val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "ChickCare"
                val body = remoteMessage.notification?.body ?: remoteMessage.data["message"] ?: remoteMessage.data["body"] ?: ""
                
                
                if (body.isNotEmpty()) {
                    sendNotification(
                        title = title,
                        message = body,
                        data = remoteMessage.data
                    )
                }
            }
        }
    }
    
    /**
     * Handle chat message notifications with proper intent to open chat
     */
    private fun handleChatNotification(remoteMessage: RemoteMessage) {
        val senderId = remoteMessage.data["senderId"] ?: return
        val senderName = remoteMessage.data["senderName"] ?: "Someone"
        val message = remoteMessage.data["message"] ?: ""
        val messageType = remoteMessage.data["messageType"] ?: "text"
        
        // Format message preview
        val messagePreview = when (messageType) {
            "image" -> "📷 Sent an image"
            "audio" -> "🎵 Sent an audio"
            else -> message.take(100)
        }
        
        // Create intent to open chat screen
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("type", "CHAT_MESSAGE")
            putExtra("userId", senderId)
            putExtra("userName", senderName)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            senderId.hashCode(), // Use senderId hash for unique notification ID
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val channelId = com.bisu.chickcare.backend.utils.SystemNotificationHelper.CHANNEL_ID_CHAT
        val soundUri = android.net.Uri.parse("android.resource://${packageName}/${R.raw.notify_sound}")
        
        val soundEnabled = settingsRepository.getSoundEnabled()
        val vibrationEnabled = settingsRepository.getVibrationEnabled()
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.chicken_icon)
            .setContentTitle(senderName)
            .setContentText(messagePreview)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messagePreview))
            
        // Configure sound and vibration
        var defaults = NotificationCompat.DEFAULT_LIGHTS
        
        if (soundEnabled) {
            notificationBuilder.setSound(soundUri)
        } else {
            notificationBuilder.setSound(null)
        }
        
        if (vibrationEnabled) {
            defaults = defaults or NotificationCompat.DEFAULT_VIBRATE
            notificationBuilder.setVibrate(longArrayOf(0, 250, 250, 250))
        } else {
            notificationBuilder.setVibrate(longArrayOf(0))
        }
        
        notificationBuilder.setDefaults(defaults)
        
        // Try to load sender's profile picture for notification
        val senderPhotoUrl = remoteMessage.data["senderPhotoUrl"]
        if (!senderPhotoUrl.isNullOrEmpty()) {
            try {
                notificationBuilder.setLargeIcon(
                    android.graphics.BitmapFactory.decodeStream(
                        java.net.URL(senderPhotoUrl).openConnection().getInputStream()
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not load sender photo for notification: ${e.message}")
            }
        }
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for chat messages
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .build()
                
            val channel = NotificationChannel(
                channelId,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages from friends"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Use senderId hash for notification ID so multiple messages from same sender update the same notification
        notificationManager.notify(senderId.hashCode(), notificationBuilder.build())
    }
    
    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM token: $token")
        
        // Save FCM token to Firestore for this user
        saveTokenToFirestore(token)
    }
    
    private fun sendNotification(title: String, message: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Add notification data if needed
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val channelId = com.bisu.chickcare.backend.utils.SystemNotificationHelper.CHANNEL_ID_GENERAL
        val soundUri = android.net.Uri.parse("android.resource://${packageName}/${R.raw.notify_sound}")
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.chicken_icon)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSound(soundUri)
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .build()
                
            val channel = NotificationChannel(
                channelId,
                "ChickCare Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for chicken health detection results and announcements"
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
    
    private fun saveTokenToFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            serviceScope.launch {
                try {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .update("fcmToken", token)
                        .await()
                    Log.d(TAG, "FCM token saved to Firestore for user: $userId")
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving FCM token", e)
                }
            }
        }
    }
    
    private fun handleSocialNotification(remoteMessage: RemoteMessage) {
        val type = remoteMessage.data["type"] ?: return
        
        // Check specific settings
        if (type == "FRIEND_REQUEST" || type == "FRIEND_ACCEPT") {
             if (!settingsRepository.getFriendRequestsEnabled()) {
                 Log.d(TAG, "Friend request notifications disabled by user. Ignoring.")
                 return
             }
        }
        
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "ChickCare"
        val message = remoteMessage.notification?.body ?: remoteMessage.data["message"] ?: remoteMessage.data["body"] ?: ""
        val senderId = remoteMessage.data["senderId"]
        
        // Create intent based on type
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("type", type)
            putExtra("userId", senderId)
            // Pass all data just in case
            remoteMessage.data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            (senderId?.hashCode() ?: 0) + type.hashCode(), 
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val channelId = "social_notifications"
        val soundUri = android.net.Uri.parse("android.resource://${packageName}/${R.raw.notify_sound}")
        
        val soundEnabled = settingsRepository.getSoundEnabled()
        val vibrationEnabled = settingsRepository.getVibrationEnabled()
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.chicken_icon)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            
        // Configure sound and vibration
        var defaults = NotificationCompat.DEFAULT_LIGHTS
        
        if (soundEnabled) {
            notificationBuilder.setSound(soundUri)
        } else {
            notificationBuilder.setSound(null)
        }
        
        if (vibrationEnabled) {
            defaults = defaults or NotificationCompat.DEFAULT_VIBRATE
            notificationBuilder.setVibrate(longArrayOf(0, 250, 250, 250))
        } else {
            notificationBuilder.setVibrate(longArrayOf(0))
        }
        
        notificationBuilder.setDefaults(defaults)
        
        // Try to load sender photo
        val senderPhotoUrl = remoteMessage.data["senderPhotoUrl"] ?: remoteMessage.data["photoUrl"]
        if (!senderPhotoUrl.isNullOrEmpty()) {
            try {
                notificationBuilder.setLargeIcon(
                    android.graphics.BitmapFactory.decodeStream(
                        java.net.URL(senderPhotoUrl).openConnection().getInputStream()
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not load sender photo: ${e.message}")
            }
        }
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        // Create channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .build()
                
            val channel = NotificationChannel(
                channelId,
                "Social Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for friends and follows"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        notificationManager.notify((senderId?.hashCode() ?: 0) + type.hashCode(), notificationBuilder.build())
    }

    companion object {
        private const val TAG = "ChickCareMessaging"
    }
}

