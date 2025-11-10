package com.bisu.chickcare.backend.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class FeedingScheduleEntry(
    val id: String = "",
    val scheduledAt: Long = 0L,
    val feedType: String = "",
    val quantity: String = "",
    val targetGroup: String = "",
    val frequency: String = "",
    val notes: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L
)

class FeedingScheduleRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun saveSchedule(userId: String, schedule: FeedingScheduleEntry): String {
        try {
            val createdAt = schedule.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis()
            val data = hashMapOf(
                "scheduledAt" to schedule.scheduledAt,
                "feedType" to schedule.feedType,
                "quantity" to schedule.quantity,
                "targetGroup" to schedule.targetGroup,
                "frequency" to schedule.frequency,
                "notes" to schedule.notes,
                "isCompleted" to schedule.isCompleted,
                "createdAt" to createdAt
            )

            val collection = usersCollection.document(userId).collection("feedingSchedules")
            return if (schedule.id.isEmpty()) {
                val documentRef = collection.add(data).await()
                Log.d("FeedingScheduleRepository", "Feeding schedule created: ${documentRef.id}")
                documentRef.id
            } else {
                collection.document(schedule.id).set(data).await()
                Log.d("FeedingScheduleRepository", "Feeding schedule updated: ${schedule.id}")
                schedule.id
            }
        } catch (e: Exception) {
            Log.e("FeedingScheduleRepository", "Error saving feeding schedule: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateCompletion(userId: String, scheduleId: String, completed: Boolean) {
        try {
            usersCollection.document(userId).collection("feedingSchedules")
                .document(scheduleId)
                .update("isCompleted", completed)
                .await()
        } catch (e: Exception) {
            Log.e("FeedingScheduleRepository", "Error updating completion: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteSchedule(userId: String, scheduleId: String) {
        try {
            usersCollection.document(userId).collection("feedingSchedules")
                .document(scheduleId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e("FeedingScheduleRepository", "Error deleting feeding schedule: ${e.message}", e)
            throw e
        }
    }

    fun getSchedules(userId: String): Flow<List<FeedingScheduleEntry>> = callbackFlow {
        val listener = usersCollection.document(userId).collection("feedingSchedules")
            .orderBy("scheduledAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FeedingScheduleRepository", "Error fetching feeding schedules: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val schedules = snapshot.documents.map { doc ->
                        FeedingScheduleEntry(
                            id = doc.id,
                            scheduledAt = doc.getLong("scheduledAt") ?: 0L,
                            feedType = doc.getString("feedType") ?: "",
                            quantity = doc.getString("quantity") ?: "",
                            targetGroup = doc.getString("targetGroup") ?: "",
                            frequency = doc.getString("frequency") ?: "",
                            notes = doc.getString("notes") ?: "",
                            isCompleted = doc.getBoolean("isCompleted") ?: false,
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    }
                    trySend(schedules)
                }
            }

        awaitClose { listener.remove() }
    }
}


