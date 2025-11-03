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
    DATA_DELETED
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
    val relatedEntityId: String? = null,
    val actionRequired: Boolean = false
)

class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    fun getNotifications(userId: String): Flow<List<NotificationEntry>> = callbackFlow {
        val listener = usersCollection.document(userId).collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val notifications = snapshot.toObjects(NotificationEntry::class.java).mapIndexed { index, entry ->
                        entry.copy(id = snapshot.documents[index].id)
                    }
                    trySend(notifications)
                }
            }
        awaitClose { listener.remove() }
    }
    
    suspend fun markAsRead(userId: String, notificationId: String) {
        usersCollection.document(userId)
            .collection("notifications")
            .document(notificationId)
            .update("isRead", true)
            .await()
    }
    
    suspend fun markAllAsRead(userId: String) {
        val snapshot = usersCollection.document(userId).collection("notifications")
            .whereEqualTo("isRead", false)
            .get()
            .await()
        
        snapshot.documents.forEach { doc ->
            doc.reference.update("isRead", true).await()
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
        actionRequired: Boolean = false
    ) {
        val notificationData = hashMapOf(
            "type" to type.name,
            "title" to title,
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false,
            "senderId" to senderId,
            "senderName" to senderName,
            "relatedEntityId" to relatedEntityId,
            "actionRequired" to actionRequired
        )
        usersCollection.document(userId).collection("notifications").add(notificationData).await()
    }
    
    suspend fun addAnnouncementToAllUsers(title: String, message: String) {
        val usersSnapshot = usersCollection.get().await()
        usersSnapshot.documents.forEach { userDoc ->
            addNotification(
                userId = userDoc.id,
                type = NotificationType.ANNOUNCEMENT,
                title = title,
                message = message
            )
        }
    }
}
