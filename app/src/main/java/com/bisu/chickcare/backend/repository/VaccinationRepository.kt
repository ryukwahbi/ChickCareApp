package com.bisu.chickcare.backend.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class Vaccination(
    val id: String = "",
    val chickenId: String? = null,
    val vaccineName: String = "",
    val date: Long = 0,
    val nextDueDate: Long = 0, // -1 if no next due date
    val batchNumber: String = "",
    val administeredBy: String = "",
    val notes: String = "",
    val createdAt: Long = 0
)

class VaccinationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun saveVaccination(userId: String, vaccination: Vaccination) {
        try {
            val vaccinationData = hashMapOf(
                "chickenId" to vaccination.chickenId,
                "vaccineName" to vaccination.vaccineName,
                "date" to vaccination.date,
                "nextDueDate" to vaccination.nextDueDate,
                "batchNumber" to vaccination.batchNumber,
                "administeredBy" to vaccination.administeredBy,
                "notes" to vaccination.notes,
                "createdAt" to System.currentTimeMillis()
            )
            
            if (vaccination.id.isEmpty()) {
                usersCollection.document(userId).collection("vaccinations").add(vaccinationData).await()
            } else {
                usersCollection.document(userId).collection("vaccinations")
                    .document(vaccination.id).set(vaccinationData).await()
            }
            Log.d("VaccinationRepository", "Vaccination saved successfully")
        } catch (e: Exception) {
            Log.e("VaccinationRepository", "Error saving vaccination: ${e.message}", e)
            throw e
        }
    }

    fun getVaccinations(userId: String): Flow<List<Vaccination>> = callbackFlow {
        val listener = usersCollection.document(userId).collection("vaccinations")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("VaccinationRepository", "Error fetching vaccinations: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val vaccinations = snapshot.documents.map { doc ->
                        Vaccination(
                            id = doc.id,
                            chickenId = doc.getString("chickenId"),
                            vaccineName = doc.getString("vaccineName") ?: "",
                            date = doc.getLong("date") ?: 0L,
                            nextDueDate = doc.getLong("nextDueDate") ?: 0L,
                            batchNumber = doc.getString("batchNumber") ?: "",
                            administeredBy = doc.getString("administeredBy") ?: "",
                            notes = doc.getString("notes") ?: "",
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    }
                    trySend(vaccinations)
                }
            }
        
        awaitClose { listener.remove() }
    }

    suspend fun deleteVaccination(userId: String, vaccinationId: String) {
        try {
            usersCollection.document(userId).collection("vaccinations")
                .document(vaccinationId).delete().await()
            Log.d("VaccinationRepository", "Vaccination deleted successfully")
        } catch (e: Exception) {
            Log.e("VaccinationRepository", "Error deleting vaccination: ${e.message}", e)
            throw e
        }
    }
}

