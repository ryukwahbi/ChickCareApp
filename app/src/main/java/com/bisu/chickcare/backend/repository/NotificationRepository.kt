package com.bisu.chickcare.backend.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

data class NotificationEntry(
    val id: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val isRead: Boolean = false
)

class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun sendNotification(userId: String, message: String) {
        val notificationData = hashMapOf(
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )
        usersCollection.document(userId).collection("notifications").add(notificationData).await()
    }


    suspend fun addNotification(userId: String, message: String) {
        val notificationData = hashMapOf(
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false // I-set sa false by default
        )
        usersCollection.document(userId).collection("notifications").add(notificationData).await()
    }

    fun getNotifications(userId: String): Flow<List<NotificationEntry>> = flow {
        val snapshot = usersCollection.document(userId).collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()
        val notifications = snapshot.toObjects(NotificationEntry::class.java).mapIndexed { index, entry ->
            entry.copy(id = snapshot.documents[index].id)
        }
        emit(notifications)
    }
}
