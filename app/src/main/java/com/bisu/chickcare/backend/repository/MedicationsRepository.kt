package com.bisu.chickcare.backend.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class MedicationEntry(
    val id: String = "",
    val medicationName: String = "",
    val chickenId: String? = null,
    val scheduledDate: Long = 0L,
    val dosage: String = "",
    val frequency: String = "",
    val duration: String = "",
    val administeredBy: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = 0L
)

class MedicationsRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun saveMedication(userId: String, entry: MedicationEntry) {
        try {
            val data = hashMapOf(
                "medicationName" to entry.medicationName,
                "chickenId" to entry.chickenId,
                "scheduledDate" to entry.scheduledDate,
                "dosage" to entry.dosage,
                "frequency" to entry.frequency,
                "duration" to entry.duration,
                "administeredBy" to entry.administeredBy,
                "notes" to entry.notes,
                "isActive" to entry.isActive,
                "createdAt" to System.currentTimeMillis()
            )

            val collection = usersCollection.document(userId).collection("medications")
            if (entry.id.isEmpty()) {
                collection.add(data).await()
            } else {
                collection.document(entry.id).set(data).await()
            }
            Log.d("MedicationsRepository", "Medication saved successfully")
        } catch (e: Exception) {
            Log.e("MedicationsRepository", "Error saving medication: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateStatus(userId: String, medicationId: String, isActive: Boolean) {
        try {
            usersCollection.document(userId).collection("medications")
                .document(medicationId)
                .update("isActive", isActive)
                .await()
        } catch (e: Exception) {
            Log.e("MedicationsRepository", "Error updating medication status: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteMedication(userId: String, medicationId: String) {
        try {
            usersCollection.document(userId).collection("medications")
                .document(medicationId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e("MedicationsRepository", "Error deleting medication: ${e.message}", e)
            throw e
        }
    }

    fun getMedications(userId: String): Flow<List<MedicationEntry>> = callbackFlow {
        val listener = usersCollection.document(userId).collection("medications")
            .orderBy("scheduledDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MedicationsRepository", "Error fetching medications: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val medications = snapshot.documents.map { doc ->
                        MedicationEntry(
                            id = doc.id,
                            medicationName = doc.getString("medicationName") ?: "",
                            chickenId = doc.getString("chickenId"),
                            scheduledDate = doc.getLong("scheduledDate") ?: 0L,
                            dosage = doc.getString("dosage") ?: "",
                            frequency = doc.getString("frequency") ?: "",
                            duration = doc.getString("duration") ?: "",
                            administeredBy = doc.getString("administeredBy") ?: "",
                            notes = doc.getString("notes") ?: "",
                            isActive = doc.getBoolean("isActive") ?: true,
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    }
                    trySend(medications)
                }
            }

        awaitClose { listener.remove() }
    }
}


