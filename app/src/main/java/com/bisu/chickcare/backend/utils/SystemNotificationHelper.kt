package com.bisu.chickcare.backend.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bisu.chickcare.MainActivity
import com.bisu.chickcare.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Helper class to manage system notifications.
 * Handles channel creation and displaying different types of notifications.
 */
class SystemNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID_CHAT = "chickcare_chat_notifications_v2"
        const val CHANNEL_ID_SOCIAL = "chickcare_social_notifications_v2"
        const val CHANNEL_ID_GENERAL = "chickcare_notifications_v2"
        
        private const val TAG = "SystemNotifHelper"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val soundUri = android.net.Uri.parse("android.resource://${context.packageName}/${R.raw.notify_sound}")
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .build()

            // Chat Channel
            val chatChannel = NotificationChannel(
                CHANNEL_ID_CHAT,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages"
                enableVibration(true)
                setShowBadge(true)
                setSound(soundUri, audioAttributes)
            }

            // Social Channel (Friend Requests, etc.)
            val socialChannel = NotificationChannel(
                CHANNEL_ID_SOCIAL,
                "Social Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Friend requests and accepted invites"
                enableVibration(true)
                setShowBadge(true)
                setSound(soundUri, audioAttributes)
            }
            
            // General Channel
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
                setSound(soundUri, audioAttributes)
            }

            notificationManager.createNotificationChannel(chatChannel)
            notificationManager.createNotificationChannel(socialChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    /**
     * Show a notification for a Friend Request
     */
    suspend fun showFriendRequestNotification(
        notificationId: Int,
        senderId: String,
        senderName: String,
        photoUrl: String?
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Navigate to notifications screen
            putExtra("navigation_route", "notifications") 
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 
            notificationId, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val largeIcon = loadBitmapFromUrl(photoUrl)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_SOCIAL)
            .setSmallIcon(R.drawable.chicken_icon)
            .setContentTitle("New Friend Request")
            .setContentText("$senderName sent you a friend request")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        notify(notificationId, builder.build())
    }

    /**
     * Show a notification when a Friend Request is accepted
     */
    suspend fun showFriendRequestAcceptedNotification(
        notificationId: Int,
        friendId: String,
        friendName: String,
        photoUrl: String?
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Navigate to the user's profile
            putExtra("navigation_route", "view_profile?userId=$friendId")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val largeIcon = loadBitmapFromUrl(photoUrl)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_SOCIAL)
            .setSmallIcon(R.drawable.chicken_icon)
            .setContentTitle("Friend Request Accepted")
            .setContentText("$friendName accepted your friend request")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        notify(notificationId, builder.build())
    }

    /**
     * Show a notification for a Reaction
     */
    suspend fun showReactionNotification(
        notificationId: Int,
        reactingUserName: String,
        photoUrl: String?,
        postId: String,
        postOwnerId: String,
        reactionType: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigation_route", "comments/$postId/$postOwnerId")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val emoji = when (reactionType) {
            "heart" -> "❤️"
            "chicken" -> "🐔"
            "wow" -> "😮"
            "pray" -> "🙏"
            else -> "👍"
        }

        val largeIcon = loadBitmapFromUrl(photoUrl)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_SOCIAL)
            .setSmallIcon(R.drawable.reaction_notify)
            .setContentTitle("New Reaction")
            .setContentText("$emoji $reactingUserName reacted to your post")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        notify(notificationId, builder.build())
    }

    /**
     * Show a notification for a Comment
     */
    suspend fun showCommentNotification(
        notificationId: Int,
        commentingUserName: String,
        photoUrl: String?,
        postId: String,
        postOwnerId: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigation_route", "comments/$postId/$postOwnerId")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val largeIcon = loadBitmapFromUrl(photoUrl)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_SOCIAL)
            .setSmallIcon(R.drawable.chicken_icon)
            .setContentTitle("New Comment")
            .setContentText("$commentingUserName commented on your post")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        notify(notificationId, builder.build())
    }

    /**
     * Show a notification for a Follow
     */
    suspend fun showFollowNotification(
        notificationId: Int,
        followerUserId: String,
        followerUserName: String,
        photoUrl: String?
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigation_route", "view_profile?userId=$followerUserId")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val largeIcon = loadBitmapFromUrl(photoUrl)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_SOCIAL)
            .setSmallIcon(R.drawable.follower_notify)
            .setContentTitle("New Follower")
            .setContentText("$followerUserName started following you")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        notify(notificationId, builder.build())
    }

    /**
     * Show a notification for a Chat Message
     */
    suspend fun showChatMessageNotification(
        senderId: String,
        senderName: String,
        message: String,
        messageType: String,
        photoUrl: String?
    ) {
        val notificationId = senderId.hashCode() // Group by sender

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("type", "CHAT_MESSAGE")
            putExtra("userId", senderId)
            putExtra("userName", senderName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val messagePreview = when (messageType) {
            "image" -> "📷 Sent an image"
            "audio" -> "🎵 Sent an audio"
            else -> message
        }

        val largeIcon = loadBitmapFromUrl(photoUrl)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_CHAT)
            .setSmallIcon(R.drawable.chicken_icon)
            .setContentTitle(senderName)
            .setContentText(messagePreview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messagePreview))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        notify(notificationId, builder.build())
    }

    private fun notify(id: Int, notification: android.app.Notification) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(id, notification)
        } else {
            Log.w(TAG, "Missing POST_NOTIFICATIONS permission")
        }
    }

    private suspend fun loadBitmapFromUrl(url: String?): Bitmap? {
        if (url.isNullOrEmpty()) return null
        return withContext(Dispatchers.IO) {
            try {
                val input = URL(url).openStream()
                BitmapFactory.decodeStream(input)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load image: ${e.message}")
                null
            }
        }
    }
}
