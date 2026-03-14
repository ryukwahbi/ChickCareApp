package com.bisu.chickcare.backend.repository

import android.util.Log
import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bisu.chickcare.backend.worker.MediaSyncWorker
import com.bisu.chickcare.frontend.utils.persistUriToAppStorage
import kotlinx.coroutines.tasks.await

data class DetectionEntry(
    val id: String = "",
    val result: String = "",
    @get:com.google.firebase.firestore.PropertyName("isHealthy")
    val isHealthy: Boolean = false,
    val confidence: Float = 0f,
    val imageUri: String? = null,
    val audioUri: String? = null,
    val timestamp: Long = 0,
    val location: String? = null,
    val recommendations: List<String> = emptyList(),
    @get:com.google.firebase.firestore.PropertyName("isRead")
    val isRead: Boolean = false,
    @get:com.google.firebase.firestore.PropertyName("isDeleted")
    val isDeleted: Boolean = false,
    val deletedTimestamp: Long = 0,
    @get:com.google.firebase.firestore.PropertyName("isFavorite")
    val isFavorite: Boolean = false,
    @get:com.google.firebase.firestore.PropertyName("isArchived")
    val isArchived: Boolean = false,
    val treatment: String? = null,
    val treatmentDate: Long? = null,
    val nextDoseDate: Long? = null,
    val treatmentNotes: String? = null,
    val cloudUrl: String? = null,
    val cloudAudioUrl: String? = null,
    val localImageUri: String? = null,
    val localAudioUri: String? = null
)

class DetectionRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")


    /**
     * Helper to parse DetectionEntry from DocumentSnapshot to avoid duplication
     */
    private fun parseDetectionEntry(doc: com.google.firebase.firestore.DocumentSnapshot): DetectionEntry? {
        return try {
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
                    try {
                        java.net.URLDecoder.decode(str, "UTF-8")
                    } catch (_: Exception) {
                        str 
                    }
                } ?: emptyList(),
                isRead = doc.getBoolean("isRead") ?: false,
                isDeleted = doc.getBoolean("isDeleted") ?: false,
                deletedTimestamp = doc.getLong("deletedTimestamp") ?: 0L,
                isFavorite = doc.getBoolean("isFavorite") ?: false,
                isArchived = doc.getBoolean("isArchived") ?: false,
                treatment = doc.getString("treatment"),
                treatmentDate = doc.getLong("treatmentDate"),
                nextDoseDate = doc.getLong("nextDoseDate"),
                treatmentNotes = doc.getString("treatmentNotes"),
                cloudUrl = doc.getString("cloudUrl"),
                cloudAudioUrl = doc.getString("cloudAudioUrl")
            )
        } catch (e: Exception) {
            Log.w("DetectionRepository", "Error parsing document ${doc.id}: ${e.message}")
            null
        }
    }

    /**
     * Save detection result to Firestore.
     * Returns SaveResult indicating whether it was saved immediately or queued for offline sync.
     */
    suspend fun saveDetection(
        userId: String,
        result: String,
        isHealthy: Boolean,
        confidence: Float,
        imageUri: String?,
        audioUri: String?,
        location: String? = null,
        recommendations: List<String> = emptyList()
    ): SaveResult {
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
            "isArchived" to false,
            "cloudUrl" to null,
            "cloudAudioUrl" to null,
            "localImageUri" to null,
            "localAudioUri" to null
        )
        
        val context = com.bisu.chickcare.ChickCareApplication.getInstance() // I'll need to add this to ChickCareApplication
        
        // 1. Persist media to internal storage IMMEDIATELY (Offline-First)
        val persistentImageUri = if (imageUri != null) {
            persistUriToAppStorage(context, imageUri, "detections/images", "jpg", "DetectionRepo")
        } else null
        
        val persistentAudioUri = if (audioUri != null) {
            persistUriToAppStorage(context, audioUri, "detections/audio", "mp3", "DetectionRepo")
        } else null
        
        detectionData["imageUri"] = persistentImageUri
        detectionData["audioUri"] = persistentAudioUri
        detectionData["localImageUri"] = persistentImageUri
        detectionData["localAudioUri"] = persistentAudioUri
        try {
            // 1. Save initial document to Firestore (Offline First)
            // Use await() to ensure local persistence logic works
            val docRef = usersCollection.document(userId).collection("detections").add(detectionData).await()
            Log.d("DetectionRepository", "Detection saved locally/Firestore with ID: ${docRef.id}")

            // 2. Schedule Background Sync Worker
            scheduleSyncWorker(context, userId, docRef.id)

            return SaveResult.Success(docRef.id)

        } catch (_: java.net.UnknownHostException) {
             // Network unavailable - Firestore persistence will queue this write
            // Don't throw exception, just return queued status
            Log.d("DetectionRepository", "Network unavailable - detection queued for sync (offline persistence enabled)")
            try {
                usersCollection.document(userId).collection("detections").add(detectionData)
                return SaveResult.Queued
            } catch (e: Exception) {
                Log.w("DetectionRepository", "Error queuing detection: ${e.message}")
                return SaveResult.Queued
            }
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE) {
                // Firestore unavailable (offline) - queue the write
                Log.d("DetectionRepository", "Firestore unavailable (offline) - detection queued for sync")
                 try {
                    usersCollection.document(userId).collection("detections").add(detectionData)
                    return SaveResult.Queued
                } catch (queueError: Exception) {
                    Log.w("DetectionRepository", "Error queuing detection: ${queueError.message}")
                     return SaveResult.Queued
                }
            }
            Log.e("DetectionRepository", "Error saving detection: ${e.message}", e)
            return SaveResult.Error(e.message ?: "Unknown error")
        } catch (e: Exception) {
            Log.e("DetectionRepository", "Error saving detection: ${e.message}", e)
            return SaveResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Result of save operation
     */
    sealed class SaveResult {
        data class Success(val documentId: String) : SaveResult()
        object Queued : SaveResult() // Queued for sync when online
        data class Error(val message: String) : SaveResult()
    }

    suspend fun getDetection(userId: String, detectionId: String): DetectionEntry? {
        return try {
            val document = usersCollection.document(userId)
                .collection("detections")
                .document(detectionId)
                .get()
                .await()

            if (document.exists()) {
                parseDetectionEntry(document)
            } else {
                Log.w("DetectionRepository", "Detection document not found: $detectionId")
                null
            }
        } catch (e: Exception) {
            Log.e("DetectionRepository", "Error fetching detection: ${e.message}", e)
            null
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
                            // Use manual parsing to prevent crashes from null values in non-nullable fields
                            // toObjects() uses reflection and might bypass Kotlin null safety if Firestore fields are null/missing
                            val history = snapshot.documents.mapNotNull { doc ->
                                try {
                                    parseDetectionEntry(doc)
                                } catch (e: Exception) {
                                    Log.w("DetectionRepository", "Error parsing document ${doc.id}: ${e.message}")
                                    null
                                }
                            }
                            Log.d("DetectionRepository", "Main query returned ${history.size} detection entries")
                            trySend(history)
                        } catch (e: Exception) {
                            Log.e("DetectionRepository", "Error processing main query results: ${e.message}", e)
                            trySend(emptyList())
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
                        // Use manual parsing to prevent crashes from null values in non-nullable fields
                        val history = snapshot.documents.mapNotNull { doc ->
                            try {
                                parseDetectionEntry(doc)
                            } catch (e: Exception) {
                                Log.w("DetectionRepository", "Error parsing document ${doc.id}: ${e.message}")
                                null
                            }
                        }
                            .filter { !it.isDeleted }
                            .sortedByDescending { it.timestamp }
                        Log.d("DetectionRepository", "Fallback query returned ${history.size} detection entries")
                        onResult(history)
                    } catch (e: Exception) {
                        Log.e("DetectionRepository", "Error processing fallback query results: ${e.message}", e)
                        // Manual parsing already attempted above, so just return empty
                        onResult(emptyList())
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
                        val deletedItems = snapshot.documents.mapNotNull { doc ->
                            try {
                                parseDetectionEntry(doc)
                            } catch (e: Exception) {
                                Log.w("DetectionRepository", "Error parsing document ${doc.id}: ${e.message}")
                                null
                            }
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
                    val deletedItems = snapshot.documents.mapNotNull { doc ->
                        try {
                            parseDetectionEntry(doc)
                        } catch (e: Exception) {
                            Log.w("DetectionRepository", "Error parsing fallback deleted item ${doc.id}: ${e.message}")
                            null
                        }
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
                            val favorites = snapshot.documents.mapNotNull { doc ->
                                try {
                                    parseDetectionEntry(doc)
                                } catch (e: Exception) {
                                    Log.w("DetectionRepository", "Error parsing favorite item ${doc.id}: ${e.message}")
                                    null
                                }
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
                            val archived = snapshot.documents.mapNotNull { doc ->
                                try {
                                    parseDetectionEntry(doc)
                                } catch (e: Exception) {
                                    Log.w("DetectionRepository", "Error parsing archived item ${doc.id}: ${e.message}")
                                    null
                                }
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
                        if (deletedTimestamp in 1..<fourteenDaysAgo) {
                            doc.reference.delete().await()
                        }
                    }
                } catch (fallbackError: Exception) {
                    Log.e("DetectionRepository", "Error in cleanup fallback: ${fallbackError.message}", fallbackError)
                }
            } else {
                throw e
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

    // Update treatment for a detection entry
    suspend fun updateTreatment(
        userId: String,
        detectionId: String,
        treatment: String?,
        treatmentDate: Long? = null,
        nextDoseDate: Long? = null,
        treatmentNotes: String? = null
    ) {
        try {
            // Build update map with only non-null values for Firestore
            val updateData = hashMapOf<String, Any>()

            treatment?.let { updateData["treatment"] = it }
            updateData["treatmentDate"] = treatmentDate ?: System.currentTimeMillis()
            nextDoseDate?.let { updateData["nextDoseDate"] = it }
            treatmentNotes?.let { updateData["treatmentNotes"] = it }

            usersCollection.document(userId)
                .collection("detections")
                .document(detectionId)
                .update(updateData)
                .await()

            Log.d("DetectionRepository", "Treatment updated for detection: $detectionId")
        } catch (e: Exception) {
            Log.e("DetectionRepository", "Error updating treatment: ${e.message}", e)
            throw e
        }
    }

    private fun scheduleSyncWorker(context: Context, userId: String, detectionId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<MediaSyncWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(
                "userId" to userId,
                "detectionId" to detectionId
            ))
            .build()

        WorkManager.getInstance(context).enqueue(syncRequest)
        Log.d("DetectionRepository", "Scheduled MediaSyncWorker for detection: $detectionId")
    }
}
