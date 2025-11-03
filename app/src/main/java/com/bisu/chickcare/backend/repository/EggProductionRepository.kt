package com.bisu.chickcare.backend.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class EggProductionRecord(
    val id: String = "",
    val date: Long = 0,
    val totalEggs: Int = 0,
    val healthyEggs: Int = 0,
    val brokenEggs: Int = 0,
    val coopLocation: String = "",
    val notes: String = "",
    val createdAt: Long = 0
)

class EggProductionRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun saveEggProduction(userId: String, record: EggProductionRecord) {
        try {
            val recordData = hashMapOf(
                "date" to record.date,
                "totalEggs" to record.totalEggs,
                "healthyEggs" to record.healthyEggs,
                "brokenEggs" to record.brokenEggs,
                "coopLocation" to record.coopLocation,
                "notes" to record.notes,
                "createdAt" to System.currentTimeMillis()
            )
            
            if (record.id.isEmpty()) {
                usersCollection.document(userId).collection("eggProduction").add(recordData).await()
            } else {
                usersCollection.document(userId).collection("eggProduction")
                    .document(record.id).set(recordData).await()
            }
            Log.d("EggProductionRepository", "Egg production record saved successfully")
        } catch (e: Exception) {
            Log.e("EggProductionRepository", "Error saving egg production record: ${e.message}", e)
            throw e
        }
    }

    fun getEggProductionRecords(userId: String): Flow<List<EggProductionRecord>> = callbackFlow {
        val listener = usersCollection.document(userId).collection("eggProduction")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("EggProductionRepository", "Error fetching egg production records: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val records = snapshot.documents.map { doc ->
                        EggProductionRecord(
                            id = doc.id,
                            date = doc.getLong("date") ?: 0L,
                            totalEggs = (doc.getLong("totalEggs") ?: 0L).toInt(),
                            healthyEggs = (doc.getLong("healthyEggs") ?: 0L).toInt(),
                            brokenEggs = (doc.getLong("brokenEggs") ?: 0L).toInt(),
                            coopLocation = doc.getString("coopLocation") ?: "",
                            notes = doc.getString("notes") ?: "",
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    }
                    trySend(records)
                }
            }
        
        awaitClose { listener.remove() }
    }

    suspend fun deleteEggProductionRecord(userId: String, recordId: String) {
        try {
            usersCollection.document(userId).collection("eggProduction")
                .document(recordId).delete().await()
            Log.d("EggProductionRepository", "Egg production record deleted successfully")
        } catch (e: Exception) {
            Log.e("EggProductionRepository", "Error deleting egg production record: ${e.message}", e)
            throw e
        }
    }
}

