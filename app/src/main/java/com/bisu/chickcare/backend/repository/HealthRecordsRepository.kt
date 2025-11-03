package com.bisu.chickcare.backend.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class HealthRecord(
    val id: String = "",
    val chickenId: String = "",
    val chickenName: String = "",
    val date: Long = 0,
    val condition: String = "",
    val symptoms: String = "",
    val treatment: String = "",
    val veterinarian: String? = null,
    val status: String = "HEALTHY", // HEALTHY, RECOVERING, SICK, CRITICAL
    val createdAt: Long = 0
)

class HealthRecordsRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun saveHealthRecord(userId: String, record: HealthRecord) {
        try {
            val recordData = hashMapOf(
                "chickenId" to record.chickenId,
                "chickenName" to record.chickenName,
                "date" to record.date,
                "condition" to record.condition,
                "symptoms" to record.symptoms,
                "treatment" to record.treatment,
                "veterinarian" to record.veterinarian,
                "status" to record.status,
                "createdAt" to System.currentTimeMillis()
            )
            
            if (record.id.isEmpty()) {
                // New record
                usersCollection.document(userId).collection("healthRecords").add(recordData).await()
            } else {
                // Update existing record
                usersCollection.document(userId).collection("healthRecords")
                    .document(record.id).set(recordData).await()
            }
            Log.d("HealthRecordsRepository", "Health record saved successfully")
        } catch (e: Exception) {
            Log.e("HealthRecordsRepository", "Error saving health record: ${e.message}", e)
            throw e
        }
    }

    fun getHealthRecords(userId: String): Flow<List<HealthRecord>> = callbackFlow {
        val listener = usersCollection.document(userId).collection("healthRecords")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HealthRecordsRepository", "Error fetching health records: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val records = snapshot.documents.map { doc ->
                        HealthRecord(
                            id = doc.id,
                            chickenId = doc.getString("chickenId") ?: "",
                            chickenName = doc.getString("chickenName") ?: "",
                            date = doc.getLong("date") ?: 0L,
                            condition = doc.getString("condition") ?: "",
                            symptoms = doc.getString("symptoms") ?: "",
                            treatment = doc.getString("treatment") ?: "",
                            veterinarian = doc.getString("veterinarian"),
                            status = doc.getString("status") ?: "HEALTHY",
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    }
                    trySend(records)
                }
            }
        
        awaitClose { listener.remove() }
    }

    suspend fun deleteHealthRecord(userId: String, recordId: String) {
        try {
            usersCollection.document(userId).collection("healthRecords")
                .document(recordId).delete().await()
            Log.d("HealthRecordsRepository", "Health record deleted successfully")
        } catch (e: Exception) {
            Log.e("HealthRecordsRepository", "Error deleting health record: ${e.message}", e)
            throw e
        }
    }
}

