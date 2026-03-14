package com.bisu.chickcare.backend.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class Alert(
    val id: String = "",
    val disease: String = "",
    val location: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val count: Int = 0
)

class AlertRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val alertsCollection = firestore.collection("alerts")

    /**
     * Check for disease outbreak.
     * Queries recent posts (last 24h) with same disease and location.
     * If count >= threshold, create an alert.
     */
    suspend fun checkOutbreak(disease: String, location: String) {
        if (location.isEmpty() || disease.isEmpty()) return

        val threshold = 3
        val twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)

        try {
            // Query public timeline posts across all users
            val querySnapshot = firestore.collectionGroup("timelinePosts")
                .whereEqualTo("detectionResult", disease)
                .whereEqualTo("location", location)
                .whereGreaterThan("timestamp", twentyFourHoursAgo)
                .get()
                .await()

            if (querySnapshot.size() >= threshold) {
                createAlert(disease, location, querySnapshot.size())
            }
        } catch (e: Exception) {
            // Note: This query might require a Firestore composite index.
            // If it fails, check the logs for the index creation URL.
            e.printStackTrace()
        }
    }

    private suspend fun createAlert(disease: String, location: String, count: Int) {
        // Check if a recent alert already exists for this disease/location (to avoid spam)
        val twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        
        val existingAlerts = alertsCollection
            .whereEqualTo("disease", disease)
            .whereEqualTo("location", location)
            .whereGreaterThan("timestamp", twentyFourHoursAgo)
            .get()
            .await()

        if (existingAlerts.isEmpty) {
            val alert = Alert(
                // Auto-generated ID will be assigned by add user
                disease = disease,
                location = location,
                timestamp = System.currentTimeMillis(),
                count = count
            )
            alertsCollection.add(alert).await()
        }
    }

    /**
     * Get alerts for a specific location.
     */
    fun getAlerts(location: String): Flow<List<Alert>> = callbackFlow {
        if (location.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val query = alertsCollection
            .whereEqualTo("location", location)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("AlertRepository", "Error listening to alerts: ${error.message}")
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    // Safe manual parsing to prevent crashes
                    val alerts = snapshot.documents.mapNotNull { doc ->
                        try {
                            Alert(
                                id = doc.id,
                                disease = doc.getString("disease") ?: "",
                                location = doc.getString("location") ?: "",
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                count = doc.getLong("count")?.toInt() ?: 0
                            )
                        } catch (e: Exception) {
                            android.util.Log.w("AlertRepository", "Error parsing alert ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    trySend(alerts)
                } catch (e: Exception) {
                    android.util.Log.e("AlertRepository", "Error processing alerts: ${e.message}")
                    trySend(emptyList())
                }
            }
        }

        awaitClose { listener.remove() }
    }
}
