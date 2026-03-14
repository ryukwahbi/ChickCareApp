package com.bisu.chickcare.backend.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

enum class ReportCategory { HEALTH, PRODUCTION, FINANCIAL, COMPREHENSIVE }

data class ReportEntry(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: ReportCategory = ReportCategory.HEALTH,
    val lastGenerated: Long = 0L,
    val createdAt: Long = 0L
)

class ReportsRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    private val defaultReports = emptyList<ReportEntry>()

    suspend fun seedDefaultReports(userId: String) {
        // Placeholder data seeding removed
    }

    fun getReports(userId: String): Flow<List<ReportEntry>> = callbackFlow {
        val listener = usersCollection.document(userId).collection("reports")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ReportsRepository", "Error fetching reports: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val reports = snapshot.documents.map { doc ->
                        ReportEntry(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            type = doc.getString("type")?.let { ReportCategory.valueOf(it) } ?: ReportCategory.HEALTH,
                            lastGenerated = doc.getLong("lastGenerated") ?: 0L,
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    }
                    trySend(reports)
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun saveReport(userId: String, entry: ReportEntry) {
        try {
            val data = mapOf(
                "title" to entry.title,
                "description" to entry.description,
                "type" to entry.type.name,
                "lastGenerated" to entry.lastGenerated,
                "createdAt" to (entry.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis())
            )
            val collection = usersCollection.document(userId).collection("reports")
            if (entry.id.isEmpty()) {
                collection.add(data).await()
            } else {
                collection.document(entry.id).set(data).await()
            }
        } catch (e: Exception) {
            Log.e("ReportsRepository", "Error saving report: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateLastGenerated(userId: String, reportId: String) {
        try {
            usersCollection.document(userId).collection("reports")
                .document(reportId)
                .update("lastGenerated", System.currentTimeMillis())
                .await()
        } catch (e: Exception) {
            Log.e("ReportsRepository", "Error updating lastGenerated: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteReport(userId: String, reportId: String) {
        try {
            usersCollection.document(userId).collection("reports").document(reportId).delete().await()
        } catch (e: Exception) {
            Log.e("ReportsRepository", "Error deleting report: ${e.message}", e)
            throw e
        }
    }
}


