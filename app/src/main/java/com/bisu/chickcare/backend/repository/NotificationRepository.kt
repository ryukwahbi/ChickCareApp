package com.bisu.chickcare.backend.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

enum class NotificationType {
    ANNOUNCEMENT,
    FRIEND_REQUEST,
    FRIEND_ACCEPT,
    DETECTION_RESULT,
    SYSTEM_UPDATE,
    PROFILE_UPDATE,
    DATA_ADDED,
    DATA_EDITED,
    DATA_DELETED,
    // Social notification types
    REACTION,
    COMMENT,
    FOLLOW,
    NEW_POST,
    DISEASE_ALERT
}

data class NotificationEntry(
    val id: String = "",
    val type: String = NotificationType.DETECTION_RESULT.name,
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    @get:com.google.firebase.firestore.PropertyName("isRead")
    val isRead: Boolean = false,
    val senderId: String? = null,
    val senderName: String? = null,
    val senderPhotoUrl: String? = null,
    val relatedEntityId: String? = null,
    val postId: String? = null,
    val postOwnerId: String? = null,
    val reactionType: String? = null,
    val actionRequired: Boolean = false,
    val imageUri: String? = null,
    val audioUri: String? = null,
    val cloudImageUri: String? = null,
    val cloudAudioUri: String? = null
)

class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    fun getNotifications(userId: String): Flow<List<NotificationEntry>> = callbackFlow {
        val listener = usersCollection.document(userId).collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("NotificationRepository", "Error listening to notifications: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    try {
                        // Safe manual parsing to prevent crashes from null/missing fields
                        val notifications = snapshot.documents.mapNotNull { doc ->
                            try {
                                NotificationEntry(
                                    id = doc.id,
                                    type = doc.getString("type") ?: NotificationType.DETECTION_RESULT.name,
                                    title = doc.getString("title") ?: "",
                                    message = doc.getString("message") ?: "",
                                    timestamp = doc.getLong("timestamp") ?: 0L,
                                    isRead = doc.getBoolean("isRead") ?: false,
                                    senderId = doc.getString("senderId"),
                                    senderName = doc.getString("senderName"),
                                    senderPhotoUrl = doc.getString("senderPhotoUrl"),
                                    relatedEntityId = doc.getString("relatedEntityId"),
                                    postId = doc.getString("postId"),
                                    postOwnerId = doc.getString("postOwnerId"),
                                    reactionType = doc.getString("reactionType"),
                                    actionRequired = doc.getBoolean("actionRequired") ?: false,
                                    imageUri = doc.getString("imageUri"),
                                    audioUri = doc.getString("audioUri"),
                                    cloudImageUri = doc.getString("cloudImageUri"),
                                    cloudAudioUri = doc.getString("cloudAudioUri")
                                )
                            } catch (e: Exception) {
                                android.util.Log.w("NotificationRepository", "Error parsing notification ${doc.id}: ${e.message}")
                                null
                            }
                        }
                        trySend(notifications)
                    } catch (e: Exception) {
                        android.util.Log.e("NotificationRepository", "Error processing notifications: ${e.message}")
                        trySend(emptyList())
                    }
                }
            }
        awaitClose { listener.remove() }
    }
    
    suspend fun markAsRead(userId: String, notificationId: String) {
        try {
            usersCollection.document(userId)
                .collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .await()
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "Error marking notification as read: ${e.message}")
            // Don't crash - just log the error
        }
    }
    
    suspend fun markAllAsRead(userId: String) {
        try {
            val snapshot = usersCollection.document(userId).collection("notifications")
                .whereEqualTo("isRead", false)
                .get()
                .await()
            
            snapshot.documents.forEach { doc ->
                try {
                    doc.reference.update("isRead", true).await()
                } catch (e: Exception) {
                    android.util.Log.e("NotificationRepository", "Error marking notification ${doc.id} as read: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "Error marking all notifications as read: ${e.message}")
        }
    }
    
    suspend fun deleteNotification(userId: String, notificationId: String) {
        try {
            usersCollection.document(userId)
                .collection("notifications")
                .document(notificationId)
                .delete()
                .await()
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "Error deleting notification: ${e.message}")
        }
    }
    
    suspend fun addNotification(
        userId: String,
        type: NotificationType,
        title: String,
        message: String,
        senderId: String? = null,
        senderName: String? = null,
        relatedEntityId: String? = null,
        actionRequired: Boolean = false,
        imageUri: String? = null,
        audioUri: String? = null
    ) {
        try {
            val notificationData = hashMapOf(
                "type" to type.name,
                "title" to title,
                "message" to message,
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false,
                "senderId" to senderId,
                "senderName" to senderName,
                "relatedEntityId" to relatedEntityId,
                "actionRequired" to actionRequired,
                "imageUri" to imageUri,
                "audioUri" to audioUri,
                "cloudImageUri" to if (imageUri != null && imageUri.startsWith("http")) imageUri else null,
                "cloudAudioUri" to if (audioUri != null && audioUri.startsWith("http")) audioUri else null
            )
            usersCollection.document(userId).collection("notifications").add(notificationData).await()
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "Error adding notification: ${e.message}")
            // Don't crash - just log the error
        }
    }
    
    suspend fun addAnnouncementToAllUsers(title: String, message: String) {
        try {
            val usersSnapshot = usersCollection.get().await()
            usersSnapshot.documents.forEach { userDoc ->
                addNotification(
                    userId = userDoc.id,
                    type = NotificationType.ANNOUNCEMENT,
                    title = title,
                    message = message
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "Error adding announcement: ${e.message}")
        }
    }

    
    // ==================== SOCIAL NOTIFICATION HELPERS ====================
    
    /**
     * Notify when someone reacts to a post
     */
    suspend fun notifyReaction(
        postOwnerId: String,
        postId: String,
        reactingUserId: String,
        reactingUserName: String,
        reactingUserPhotoUrl: String?,
        reactionType: String
    ) {
        // Don't notify if user reacted to their own post
        if (postOwnerId == reactingUserId) return
        
        try {
            val emoji = when (reactionType) {
                "heart" -> "❤️"
                "chicken" -> "🐔"
                "wow" -> "😮"
                "pray" -> "🙏"
                else -> "👍"
            }
            
            val notificationData = hashMapOf(
                "type" to NotificationType.REACTION.name,
                "title" to "New Reaction",
                "message" to "$emoji $reactingUserName reacted to your post",
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false,
                "senderId" to reactingUserId,
                "senderName" to reactingUserName,
                "senderPhotoUrl" to reactingUserPhotoUrl,
                "postId" to postId,
                "postOwnerId" to postOwnerId,
                "reactionType" to reactionType
            )
            usersCollection.document(postOwnerId).collection("notifications").add(notificationData).await()
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "Error sending reaction notification: ${e.message}")
        }
    }
    
    /**
     * Notify when someone comments on a post
     */
    suspend fun notifyComment(
        postOwnerId: String,
        postId: String,
        commentingUserId: String,
        commentingUserName: String,
        commentingUserPhotoUrl: String?
    ) {
        // Don't notify if user commented on their own post
        if (postOwnerId == commentingUserId) return
        
        try {
            val notificationData = hashMapOf(
                "type" to NotificationType.COMMENT.name,
                "title" to "New Comment",
                "message" to "$commentingUserName commented on your post",
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false,
                "senderId" to commentingUserId,
                "senderName" to commentingUserName,
                "senderPhotoUrl" to commentingUserPhotoUrl,
                "postId" to postId,
                "postOwnerId" to postOwnerId
            )
            usersCollection.document(postOwnerId).collection("notifications").add(notificationData).await()
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "Error sending comment notification: ${e.message}")
        }
    }
    
    /**
     * Notify when someone follows a user
     */
    suspend fun notifyFollow(
        targetUserId: String,
        followerUserId: String,
        followerUserName: String,
        followerUserPhotoUrl: String?
    ) {
        try {
            val notificationData = hashMapOf(
                "type" to NotificationType.FOLLOW.name,
                "title" to "New Follower",
                "message" to "$followerUserName started following you",
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false,
                "senderId" to followerUserId,
                "senderName" to followerUserName,
                "senderPhotoUrl" to followerUserPhotoUrl
            )
            usersCollection.document(targetUserId).collection("notifications").add(notificationData).await()
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "Error sending follow notification: ${e.message}")
        }
    }
    
    /**
     * Notify followers when a user posts something new
     */

    suspend fun notifyNewPost(
        followerUserId: String,
        posterUserId: String,
        posterUserName: String,
        posterUserPhotoUrl: String?,
        postId: String
    ) {
        try {
            val notificationData = hashMapOf(
                "type" to NotificationType.NEW_POST.name,
                "title" to "New Post",
                "message" to "$posterUserName posted a new detection",
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false,
                "senderId" to posterUserId,
                "senderName" to posterUserName,
                "senderPhotoUrl" to posterUserPhotoUrl,
                "postId" to postId,
                "postOwnerId" to posterUserId
            )
            usersCollection.document(followerUserId).collection("notifications").add(notificationData).await()
        } catch (e: Exception) {
            android.util.Log.e("NotificationRepository", "Error sending new post notification: ${e.message}")
        }
    }
}
