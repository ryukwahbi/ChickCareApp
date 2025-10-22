package com.bisu.chickcare.backend.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

data class DetectionEntry(
    val id: String = "",
    val result: String = "",
    val isHealthy: Boolean = false,
    val imageUri: String? = null,
    val audioUri: String? = null,
    val timestamp: Long = 0,
    val isRead: Boolean = false
)

class DetectionRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun saveDetection(
        userId: String,
        result: String,
        isHealthy: Boolean,
        imageUri: String?,
        audioUri: String?
    ) {
        val detectionData = hashMapOf(
            "result" to result,
            "isHealthy" to isHealthy,
            "imageUri" to imageUri,
            "audioUri" to audioUri,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false // I-set sa false by default
        )
        usersCollection.document(userId).collection("detections").add(detectionData).await()
    }

    fun getDetectionHistory(userId: String): Flow<List<DetectionEntry>> = flow {
        val snapshot = usersCollection.document(userId).collection("detections")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .await()
        val history = snapshot.toObjects(DetectionEntry::class.java).mapIndexed { index, entry ->
            entry.copy(id = snapshot.documents[index].id)
        }
        emit(history)
    }

    suspend fun fetchUserStats(userId: String): Pair<Int, Int> {
        val totalChickens = 25 // Placeholder
        val alerts = 3 // Placeholder
        return Pair(totalChickens, alerts)
    }
}
