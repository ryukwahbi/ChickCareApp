package com.bisu.chickcare.backend.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class DetectionEntry(
    val id: String = "",
    val result: String = "",
    @get:com.google.firebase.firestore.PropertyName("isHealthy")
    val isHealthy: Boolean = false,
    val confidence: Float = 0f, // 0.0 to 1.0
    val imageUri: String? = null,
    val audioUri: String? = null,
    val timestamp: Long = 0,
    val location: String? = null, // Location where detection was performed
    val recommendations: List<String> = emptyList(), // Recommended actions
    @get:com.google.firebase.firestore.PropertyName("isRead")
    val isRead: Boolean = false,
    @get:com.google.firebase.firestore.PropertyName("isDeleted")
    val isDeleted: Boolean = false,
    val deletedTimestamp: Long = 0,
    @get:com.google.firebase.firestore.PropertyName("isFavorite")
    val isFavorite: Boolean = false,
    @get:com.google.firebase.firestore.PropertyName("isArchived")
    val isArchived: Boolean = false
)

class DetectionRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun saveDetection(
        userId: String,
        result: String,
        isHealthy: Boolean,
        confidence: Float,
        imageUri: String?,
        audioUri: String?,
        location: String? = null,
        recommendations: List<String> = emptyList()
    ) {
        val detectionData = hashMapOf(
            "result" to result,
            "isHealthy" to isHealthy,
            "confidence" to confidence,
            "imageUri" to imageUri,
            "audioUri" to audioUri,
            "timestamp" to System.currentTimeMillis(),
            "location" to location,
            "recommendations" to recommendations,
            "isRead" to false,
            "isDeleted" to false,
            "isFavorite" to false,
            "isArchived" to false
        )
        try {
            usersCollection.document(userId).collection("detections").add(detectionData).await()
            Log.d("DetectionRepository", "Detection saved successfully")
        } catch (_: java.net.UnknownHostException) {
            // Use verbose logging for network errors to reduce logcat spam
            Log.v("DetectionRepository", "Network unavailable - detection will be saved when online (offline persistence enabled)")
            // With offline persistence enabled, Firestore will queue this write
            // and sync when connection is restored
            throw Exception("No internet connection. Detection will be saved when you're back online.")
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE) {
                // Use verbose logging for network errors to reduce logcat spam
                Log.v("DetectionRepository", "Firestore unavailable (offline) - detection will be saved when online")
                throw Exception("No internet connection. Detection will be saved when you're back online.")
            }
            throw e
        } catch (e: Exception) {
            Log.e("DetectionRepository", "Error saving detection: ${e.message}", e)
            throw e
        }
    }

    fun getDetectionHistory(userId: String): Flow<List<DetectionEntry>> = callbackFlow {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        var fallbackListener: com.google.firebase.firestore.ListenerRegistration? = null
        var hasAttemptedFallback = false
        
        try {
            listener = usersCollection.document(userId).collection("detections")
                .whereEqualTo("isDeleted", false) // Only get non-deleted items
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        val errorMsg = error.message ?: ""
                        val errorCode = error.code
                        
                        // Check for network/offline errors
                        val isNetworkError = errorMsg.contains("Unable to resolve host", ignoreCase = true) ||
                            errorMsg.contains("UnknownHostException", ignoreCase = true) ||
                            errorMsg.contains("No address associated with hostname", ignoreCase = true) ||
                            errorCode == com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE
                        
                        if (isNetworkError) {
                            // Use verbose logging for network errors to reduce logcat spam
                            // These are expected when device is offline
                            Log.v("DetectionRepository", "Network unavailable - using cached data (if available). Reconnect to sync.")
                            // With offline persistence, this will use cached data
                            // Return empty list for now, will update when online
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        
                        // If index error, try simpler query without orderBy
                        val isIndexError = errorMsg.contains("index", ignoreCase = true) ||
                            errorCode == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION

                        if (isIndexError && !hasAttemptedFallback) {
                            // Use verbose logging for expected index errors to reduce log spam
                            Log.v("DetectionRepository", "Index missing (expected), using fallback query")
                            // Remove original listener and use fallback
                            listener?.remove()
                            hasAttemptedFallback = true
                            fallbackListener = tryFallbackHistoryQuery(userId) { history ->
                                trySend(history)
                            }
                            return@addSnapshotListener
                        } else if (!isIndexError) {
                            Log.e("DetectionRepository", "Error listening to detection history: $errorMsg", error)
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        try {
                            val history = snapshot.toObjects(DetectionEntry::class.java).mapIndexed { index, entry ->
                                entry.copy(id = snapshot.documents[index].id)
                            }
                            Log.d("DetectionRepository", "Main query returned ${history.size} detection entries")
                            trySend(history)
                        } catch (e: Exception) {
                            Log.e("DetectionRepository", "Error processing main query results: ${e.message}", e)
                            // Try to manually construct entries if automatic mapping fails
                            try {
                                val manualHistory = snapshot.documents.mapNotNull { doc ->
                                    try {
                                        DetectionEntry(
                                            id = doc.id,
                                            result = doc.getString("result") ?: "",
                                            isHealthy = doc.getBoolean("isHealthy") ?: false,
                                            confidence = (doc.getDouble("confidence")?.toFloat()) ?: 0f,
                                            imageUri = doc.getString("imageUri"),
                                            audioUri = doc.getString("audioUri"),
                                            timestamp = doc.getLong("timestamp") ?: 0L,
                                            location = doc.getString("location"),
                                            recommendations = (doc.get("recommendations") as? List<*>)?.mapNotNull { 
                                                val str = it as? String ?: return@mapNotNull null
                                                // Decode URL-encoded recommendations (spaces might be encoded as +)
                                                try {
                                                    java.net.URLDecoder.decode(str, "UTF-8")
                                                } catch (_: Exception) {
                                                    str // Return original if decoding fails
                                                }
                                            } ?: emptyList(),
                                            isRead = doc.getBoolean("isRead") ?: false,
                                            isDeleted = doc.getBoolean("isDeleted") ?: false,
                                            deletedTimestamp = doc.getLong("deletedTimestamp") ?: 0L,
                                            isFavorite = doc.getBoolean("isFavorite") ?: false,
                                            isArchived = doc.getBoolean("isArchived") ?: false
                                        )
                                    } catch (e: Exception) {
                                        Log.w("DetectionRepository", "Error parsing document ${doc.id}: ${e.message}")
                                        null
                                    }
                                }
                                Log.d("DetectionRepository", "Manual parsing returned ${manualHistory.size} detection entries")
                                trySend(manualHistory)
                            } catch (e2: Exception) {
                                Log.e("DetectionRepository", "Manual parsing also failed: ${e2.message}", e2)
                                trySend(emptyList())
                            }
                        }
                    } else if (snapshot != null && snapshot.isEmpty) {
                        // Use verbose logging for empty snapshots (expected when no data exists)
                        Log.v("DetectionRepository", "Main query returned empty snapshot (no detections yet)")
                        trySend(emptyList())
                    }
                }
        } catch (e: Exception) {
            Log.e("DetectionRepository", "Error setting up detection history listener: ${e.message}", e)
            // Use fallback immediately
            fallbackListener = tryFallbackHistoryQuery(userId) { history ->
                trySend(history)
            }
        }

        awaitClose {
            listener?.remove()
            fallbackListener?.remove()
        }
    }

    private fun tryFallbackHistoryQuery(userId: String, onResult: (List<DetectionEntry>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        // Fallback: Get all and filter/sort in-memory
        return usersCollection.document(userId).collection("detections")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val errorMsg = error.message ?: ""
                    val errorCode = error.code
                    
                    // Check for network/offline errors
                    val isNetworkError = errorMsg.contains("Unable to resolve host", ignoreCase = true) ||
                        errorMsg.contains("UnknownHostException", ignoreCase = true) ||
                        errorMsg.contains("No address associated with hostname", ignoreCase = true) ||
                        errorCode == com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE
                    
                    if (isNetworkError) {
                        // Use verbose logging for network errors to reduce logcat spam
                        Log.v("DetectionRepository", "Fallback query: Network unavailable - will use cached data")
                        // Don't return empty list - let Firestore persistence provide cached data
                        // Return empty for now, but listener will update when cached data is available
                        onResult(emptyList())
                        return@addSnapshotListener
                    }
                    
                    Log.e("DetectionRepository", "Error in fallback query: $errorMsg", error)
                    onResult(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    try {
                        val history = snapshot.toObjects(DetectionEntry::class.java).mapIndexed { index, entry ->
                            entry.copy(id = snapshot.documents[index].id)
                        }
                            .filter { !it.isDeleted }
                            .sortedByDescending { it.timestamp }
                        Log.d("DetectionRepository", "Fallback query returned ${history.size} detection entries")
                        onResult(history)
                    } catch (e: Exception) {
                        Log.e("DetectionRepository", "Error processing fallback query results: ${e.message}", e)
                        // Try to manually construct entries if automatic mapping fails
                        try {
                            val manualHistory = snapshot.documents.mapNotNull { doc ->
                                try {
                                    DetectionEntry(
                                        id = doc.id,
                                        result = doc.getString("result") ?: "",
                                        isHealthy = doc.getBoolean("isHealthy") ?: false,
                                        confidence = (doc.getDouble("confidence")?.toFloat()) ?: 0f,
                                        imageUri = doc.getString("imageUri"),
                                        audioUri = doc.getString("audioUri"),
                                        timestamp = doc.getLong("timestamp") ?: 0L,
                                        location = doc.getString("location"),
                                        recommendations = (doc.get("recommendations") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                        isRead = doc.getBoolean("isRead") ?: false,
                                        isDeleted = doc.getBoolean("isDeleted") ?: false,
                                        deletedTimestamp = doc.getLong("deletedTimestamp") ?: 0L,
                                        isFavorite = doc.getBoolean("isFavorite") ?: false,
                                        isArchived = doc.getBoolean("isArchived") ?: false
                                    )
                                } catch (e: Exception) {
                                    Log.w("DetectionRepository", "Error parsing document ${doc.id}: ${e.message}")
                                    null
                                }
                            }.filter { !it.isDeleted }
                             .sortedByDescending { it.timestamp }
                            Log.d("DetectionRepository", "Manual parsing returned ${manualHistory.size} detection entries")
                            onResult(manualHistory)
                        } catch (e2: Exception) {
                            Log.e("DetectionRepository", "Manual parsing also failed: ${e2.message}", e2)
                            onResult(emptyList())
                        }
                    }
                } else if (snapshot != null && snapshot.isEmpty) {
                    // Use verbose logging for empty snapshots (expected when no data exists)
                    Log.v("DetectionRepository", "Fallback query returned empty snapshot (no detections yet)")
                    onResult(emptyList())
                }
            }
    }

    suspend fun fetchUserStats(userId: String): Pair<Int, Int> {
        return try {
            val snapshot = usersCollection.document(userId).collection("detections")
                .get()
                .await()

            val totalChickens = snapshot.size()
            val alerts = snapshot.documents.count { doc ->
                val isHealthy = doc.getBoolean("isHealthy") ?: false
                !isHealthy
            }

            Pair(totalChickens, alerts)
        } catch (_: java.net.UnknownHostException) {
            // Use verbose logging for network errors to reduce logcat spam
            Log.v("DetectionRepository", "Network unavailable - returning cached stats if available")
            // Return default values when offline - with persistence, cached data might be available
            Pair(0, 0)
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE) {
                // Use verbose logging for network errors to reduce logcat spam
                Log.v("DetectionRepository", "Firestore unavailable (offline) - returning default stats")
                Pair(0, 0)
            } else {
                throw e
            }
        } catch (e: Exception) {
            Log.e("DetectionRepository", "Error fetching user stats: ${e.message}", e)
            Pair(0, 0) // Return safe defaults on error
        }
    }

    // Soft delete - move to trash
    suspend fun deleteDetection(userId: String, detectionId: String) {
        usersCollection.document(userId)
            .collection("detections")
            .document(detectionId)
            .update(
                mapOf(
                    "isDeleted" to true,
                    "deletedTimestamp" to System.currentTimeMillis()
                )
            )
            .await()
    }

    // Hard delete - permanently remove
    suspend fun permanentlyDeleteDetection(userId: String, detectionId: String) {
        usersCollection.document(userId)
            .collection("detections")
            .document(detectionId)
            .delete()
            .await()
    }

    // Get recently deleted items
    fun getRecentlyDeleted(userId: String): Flow<List<DetectionEntry>> = callbackFlow {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        var fallbackListener: com.google.firebase.firestore.ListenerRegistration? = null
        var hasAttemptedFallback = false

        try {
            listener = usersCollection.document(userId).collection("detections")
                .whereEqualTo("isDeleted", true)
                .orderBy("deletedTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // If index error, try simpler query without orderBy
                        // Note: error is already FirebaseFirestoreException in addSnapshotListener
                        val errorMsg = error.message ?: ""
                        val isIndexError = errorMsg.contains("index", ignoreCase = true) ||
                            error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION
                        
                        if (isIndexError && !hasAttemptedFallback) {
                            // Use verbose logging for expected index errors (reduce logcat noise)
                            // These are expected until Firestore indexes are created
                            Log.v("DetectionRepository", "Index missing for deleted items (expected), using fallback query")
                            // Remove original listener and use fallback
                            listener?.remove()
                            hasAttemptedFallback = true
                            fallbackListener = tryFallbackDeletedQuery(userId) { deleted ->
                                trySend(deleted)
                            }
                            return@addSnapshotListener
                        } else if (!isIndexError) {
                            // Only log non-index errors (actual problems)
                            Log.e("DetectionRepository", "Error listening to recently deleted: $errorMsg", error)
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        // For index errors after fallback, silently handle (already using fallback)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val deletedItems = snapshot.toObjects(DetectionEntry::class.java).mapIndexed { index, entry ->
                            entry.copy(id = snapshot.documents[index].id)
                        }
                        trySend(deletedItems)
                    }
                }
        } catch (e: Exception) {
            Log.e("DetectionRepository", "Error setting up recently deleted listener: ${e.message}", e)
            // Use fallback immediately
            fallbackListener = tryFallbackDeletedQuery(userId) { deleted ->
                trySend(deleted)
            }
        }
        
        awaitClose { 
            listener?.remove()
            fallbackListener?.remove()
        }
    }
    
    private fun tryFallbackDeletedQuery(userId: String, onResult: (List<DetectionEntry>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        // Fallback: Get all and filter/sort in-memory
        return usersCollection.document(userId).collection("detections")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DetectionRepository", "Error in fallback deleted query: ${error.message}", error)
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val deletedItems = snapshot.toObjects(DetectionEntry::class.java).mapIndexed { index, entry ->
                        entry.copy(id = snapshot.documents[index].id)
                    }
                        .filter { it.isDeleted }
                        .sortedByDescending { it.deletedTimestamp }
                    onResult(deletedItems)
                }
            }
    }
    
    // Restore deleted item
    suspend fun restoreDetection(userId: String, detectionId: String) {
        usersCollection.document(userId)
            .collection("detections")
            .document(detectionId)
            .update(
                mapOf(
                    "isDeleted" to false,
                    "deletedTimestamp" to 0
                )
            )
            .await()
    }
    
    // Permanently delete all items in trash
    suspend fun permanentlyDeleteAllTrash(userId: String) {
        val snapshot = usersCollection.document(userId)
            .collection("detections")
            .whereEqualTo("isDeleted", true)
            .get()
            .await()
        
        snapshot.documents.forEach { doc ->
            doc.reference.delete().await()
        }
    }
    
    // Restore all deleted items
    suspend fun restoreAllDetections(userId: String) {
        val snapshot = usersCollection.document(userId)
            .collection("detections")
            .whereEqualTo("isDeleted", true)
            .get()
            .await()
        
        snapshot.documents.forEach { doc ->
            doc.reference.update(
                mapOf(
                    "isDeleted" to false,
                    "deletedTimestamp" to 0
                )
            ).await()
        }
    }
    
    // Toggle favorite
    suspend fun toggleFavorite(userId: String, detectionId: String, isFavorite: Boolean) {
        usersCollection.document(userId)
            .collection("detections")
            .document(detectionId)
            .update("isFavorite", isFavorite)
            .await()
    }
    
    // Toggle archive
    suspend fun toggleArchive(userId: String, detectionId: String, isArchived: Boolean) {
        usersCollection.document(userId)
            .collection("detections")
            .document(detectionId)
            .update("isArchived", isArchived)
            .await()
    }
    
    // Get favorite detections
    fun getFavoriteDetections(userId: String): Flow<List<DetectionEntry>> = callbackFlow {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        var fallbackListener: com.google.firebase.firestore.ListenerRegistration? = null
        var hasAttemptedFallback = false
        
        try {
            listener = usersCollection.document(userId).collection("detections")
                .whereEqualTo("isDeleted", false)
                .whereEqualTo("isFavorite", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        val errorMsg = error.message ?: ""
                        val errorCode = error.code
                        
                        val isNetworkError = errorMsg.contains("Unable to resolve host", ignoreCase = true) ||
                            errorMsg.contains("UnknownHostException", ignoreCase = true) ||
                            errorCode == com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE
                        
                        if (isNetworkError) {
                            // Use verbose logging for network errors to reduce logcat spam
                            Log.v("DetectionRepository", "Network unavailable for favorites")
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        
                        val isIndexError = errorMsg.contains("index", ignoreCase = true) ||
                            errorCode == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION

                        if (isIndexError && !hasAttemptedFallback) {
                            // Use verbose logging for expected index errors
                            Log.v("DetectionRepository", "Index missing for favorites, using fallback")
                            listener?.remove()
                            hasAttemptedFallback = true
                            fallbackListener = tryFallbackHistoryQuery(userId) { history ->
                                val favorites = history.filter { it.isFavorite }
                                trySend(favorites)
                            }
                            return@addSnapshotListener
                        } else if (!isIndexError) {
                            Log.e("DetectionRepository", "Error listening to favorites: $errorMsg", error)
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        try {
                            val favorites = snapshot.toObjects(DetectionEntry::class.java).mapIndexed { index, entry ->
                                entry.copy(id = snapshot.documents[index].id)
                            }
                            trySend(favorites)
                        } catch (e: Exception) {
                            Log.e("DetectionRepository", "Error processing favorites: ${e.message}", e)
                            trySend(emptyList())
                        }
                    } else {
                        trySend(emptyList())
                    }
                }
        } catch (e: Exception) {
            Log.e("DetectionRepository", "Error setting up favorites listener: ${e.message}", e)
            trySend(emptyList())
        }
        
        awaitClose {
            listener?.remove()
            fallbackListener?.remove()
        }
    }
    
    // Get archived detections
    fun getArchivedDetections(userId: String): Flow<List<DetectionEntry>> = callbackFlow {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        var fallbackListener: com.google.firebase.firestore.ListenerRegistration? = null
        var hasAttemptedFallback = false
        
        try {
            listener = usersCollection.document(userId).collection("detections")
                .whereEqualTo("isDeleted", false)
                .whereEqualTo("isArchived", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        val errorMsg = error.message ?: ""
                        val errorCode = error.code
                        
                        val isNetworkError = errorMsg.contains("Unable to resolve host", ignoreCase = true) ||
                            errorMsg.contains("UnknownHostException", ignoreCase = true) ||
                            errorCode == com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE
                        
                        if (isNetworkError) {
                            // Use verbose logging for network errors to reduce logcat spam
                            Log.v("DetectionRepository", "Network unavailable for archived")
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        
                        val isIndexError = errorMsg.contains("index", ignoreCase = true) ||
                            errorCode == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION

                        if (isIndexError && !hasAttemptedFallback) {
                            // Use verbose logging for expected index errors
                            Log.v("DetectionRepository", "Index missing for archived, using fallback")
                            listener?.remove()
                            hasAttemptedFallback = true
                            fallbackListener = tryFallbackHistoryQuery(userId) { history ->
                                val archived = history.filter { it.isArchived }
                                trySend(archived)
                            }
                            return@addSnapshotListener
                        } else if (!isIndexError) {
                            Log.e("DetectionRepository", "Error listening to archived: $errorMsg", error)
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        try {
                            val archived = snapshot.toObjects(DetectionEntry::class.java).mapIndexed { index, entry ->
                                entry.copy(id = snapshot.documents[index].id)
                            }
                            trySend(archived)
                        } catch (e: Exception) {
                            Log.e("DetectionRepository", "Error processing archived: ${e.message}", e)
                            trySend(emptyList())
                        }
                    } else {
                        trySend(emptyList())
                    }
                }
        } catch (e: Exception) {
            Log.e("DetectionRepository", "Error setting up archived listener: ${e.message}", e)
            trySend(emptyList())
        }
        
        awaitClose {
            listener?.remove()
            fallbackListener?.remove()
        }
    }
    
    // Cleanup items that are older than 14 days (14 days = 14 * 24 * 60 * 60 * 1000 milliseconds)
    suspend fun cleanupOldDeletedItems(userId: String) {
        try {
            val fourteenDaysAgo = System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000L)
            val snapshot = usersCollection.document(userId)
                .collection("detections")
                .whereEqualTo("isDeleted", true)
                .whereLessThan("deletedTimestamp", fourteenDaysAgo)
                .get()
                .await()
            
            snapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            // If index error, use fallback: get all deleted and filter in-memory
            if (e.message?.contains("index") == true || e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                // Use verbose logging for expected index errors
                Log.v("DetectionRepository", "Index missing for cleanup (expected), using fallback method")
                try {
                    val fourteenDaysAgo = System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000L)
                    val snapshot = usersCollection.document(userId)
                        .collection("detections")
                        .whereEqualTo("isDeleted", true)
                        .get()
                        .await()
                    
                    snapshot.documents.forEach { doc ->
                        val deletedTimestamp = doc.getLong("deletedTimestamp") ?: 0L
                        if (deletedTimestamp < fourteenDaysAgo && deletedTimestamp > 0) {
                            doc.reference.delete().await()
                        }
                    }
                } catch (fallbackError: Exception) {
                    Log.e("DetectionRepository", "Error in cleanup fallback: ${fallbackError.message}", fallbackError)
                }
            } else {
                throw e // Re-throw if it's not an index error
            }
        }
    }
    
    // Mark all unread detections as read
    suspend fun markAllDetectionsAsRead(userId: String) {
        try {
            // Use fallback method directly since index might not exist
            // Get all non-deleted detections and filter in-memory
            val snapshot = usersCollection.document(userId)
                .collection("detections")
                .whereEqualTo("isDeleted", false)
                .get()
                .await()
            
            var markedCount = 0
            snapshot.documents.forEach { doc ->
                // Check if isRead field exists and is false, or if field doesn't exist (treat as unread)
                val isRead = doc.getBoolean("isRead")
                if (isRead == null || !isRead) {
                    doc.reference.update("isRead", true).await()
                    markedCount++
                }
            }
            Log.d("DetectionRepository", "Marked $markedCount detections as read")
        } catch (e: Exception) {
            Log.e("DetectionRepository", "Error marking detections as read: ${e.message}", e)
        }
    }
}
