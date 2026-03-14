package com.bisu.chickcare.backend.repository

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bisu.chickcare.ChickCareApplication
import com.bisu.chickcare.backend.data.UserProfile
import com.bisu.chickcare.backend.service.CloudinaryUploadService
import com.bisu.chickcare.backend.worker.MediaSyncWorker
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class TimelinePost(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String? = null,
    val location: String = "",
    val content: String = "",
    val detectionId: String = "",
    val detectionResult: String = "",
    @get:com.google.firebase.firestore.PropertyName("isHealthy")
    val isHealthy: Boolean = false,
    val confidence: Float = 0f,
    val imageUri: String? = null,
    val audioUri: String? = null,
    val cloudImageUri: String? = null,
    val cloudAudioUri: String? = null,
    val visibility: String = "public",
    @get:com.google.firebase.firestore.PropertyName("isSaved")
    val isSaved: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    // Reactions: Map of reaction type to list of user IDs
    val reactions: Map<String, List<String>> = emptyMap(),
    // Convenience field for reaction counts
    val reactionCounts: Map<String, Int> = emptyMap(),
    // Comment count
    val commentCount: Int = 0
)

class PostRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    private val alertRepository = AlertRepository()
    private val notificationRepository = NotificationRepository()


    suspend fun createPost(
        userId: String,
        userName: String,
        userPhotoUrl: String?,
        location: String,
        detectionId: String,
        detectionResult: String,
        isHealthy: Boolean,
        confidence: Float,
        imageUri: String?,
        audioUri: String?,
        visibility: String,
        cloudImageUri: String? = null,
        cloudAudioUri: String? = null
    ): String {
        val postData = hashMapOf(
            "userId" to userId,
            "userName" to userName,
            "userPhotoUrl" to userPhotoUrl,
            "location" to location,
            "detectionId" to detectionId,
            "detectionResult" to detectionResult,
            "isHealthy" to isHealthy,
            "confidence" to confidence,
            "imageUri" to imageUri,
            "audioUri" to audioUri,
            // Prioritize passed cloud URI, fallback to inferring from imageUri if http, else null
            "cloudImageUri" to (cloudImageUri ?: if (imageUri != null && imageUri.startsWith("http")) imageUri else null),
            "cloudAudioUri" to (cloudAudioUri ?: if (audioUri != null && audioUri.startsWith("http")) audioUri else null),
            "visibility" to visibility,
            "timestamp" to System.currentTimeMillis()
        )
        
        val docRef = usersCollection.document(userId)
            .collection("timelinePosts")
            .add(postData)
            .await()
            
        // Check for disease outbreak if unhealthy
        if (!isHealthy && detectionResult.isNotEmpty() && location.isNotEmpty()) {
            try {
                alertRepository.checkOutbreak(detectionResult, location)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // OFFLINE-FIRST: Schedule Background Sync Worker for Post Media
        schedulePostSyncWorker(detectionId, docRef.id, userId)
        
        return docRef.id
    }
    
    suspend fun createTextPost(
        userId: String,
        userName: String,
        userPhotoUrl: String?,
        location: String,
        content: String,
        visibility: String
    ): String {
        val postData = hashMapOf(
            "userId" to userId,
            "userName" to userName,
            "userPhotoUrl" to userPhotoUrl,
            "location" to location,
            "content" to content,
            "visibility" to visibility,
            "timestamp" to System.currentTimeMillis()
        )
        
        val docRef = usersCollection.document(userId)
            .collection("timelinePosts")
            .add(postData)
            .await()
        
        return docRef.id
    }

    fun getUserTimelinePosts(userId: String, includePrivate: Boolean = true): Flow<List<TimelinePost>> {
        val query = if (!includePrivate) {
            // If not including private, filter to public only
            usersCollection.document(userId)
                .collection("timelinePosts")
                .whereEqualTo("visibility", "public")
                .orderBy("timestamp", Query.Direction.DESCENDING)
        } else {
            usersCollection.document(userId)
                .collection("timelinePosts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
        }
        
        return callbackFlow {
            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val posts = snapshot.toObjects(TimelinePost::class.java).mapIndexed { index, post ->
                        post.copy(id = snapshot.documents[index].id)
                    }
                    trySend(posts)
                }
            }
            
            awaitClose { listener.remove() }
        }
    }

    @Suppress("UNUSED")
    suspend fun deletePost(userId: String, postId: String) {
        usersCollection.document(userId)
            .collection("timelinePosts")
            .document(postId)
            .delete()
            .await()
    }
    
    suspend fun updatePostVisibility(userId: String, postId: String, visibility: String) {
        usersCollection.document(userId)
            .collection("timelinePosts")
            .document(postId)
            .update("visibility", visibility)
            .await()
    }
    
    // Save post (works for own posts and other users' posts)
    suspend fun savePost(currentUserId: String, originalUserId: String, postId: String, post: TimelinePost) {
        if (currentUserId == originalUserId) {
            // Own post - update isSaved field
            usersCollection.document(currentUserId)
                .collection("timelinePosts")
                .document(postId)
                .update("isSaved", true)
                .await()
        } else {
            // Other user's post - save reference in savedPosts collection
            val savedPostData = hashMapOf(
                "originalUserId" to originalUserId,
                "originalPostId" to postId,
                "userId" to post.userId,
                "userName" to post.userName,
                "userPhotoUrl" to post.userPhotoUrl,
                "content" to post.content,
                "detectionId" to post.detectionId,
                "detectionResult" to post.detectionResult,
                "isHealthy" to post.isHealthy,
                "confidence" to post.confidence,
                "imageUri" to post.imageUri,
                "audioUri" to post.audioUri,
                "cloudImageUri" to post.cloudImageUri,
                "cloudAudioUri" to post.cloudAudioUri,
                "visibility" to post.visibility,
                "timestamp" to post.timestamp,
                "savedAt" to System.currentTimeMillis()
            )
            
            usersCollection.document(currentUserId)
                .collection("savedPosts")
                .document("${originalUserId}_$postId")
                .set(savedPostData)
                .await()
        }
    }
    
    // Unsave post (works for own posts and other users' posts)
    suspend fun unsavePost(currentUserId: String, originalUserId: String, postId: String) {
        if (currentUserId == originalUserId) {
            // Own post - update isSaved field
            usersCollection.document(currentUserId)
                .collection("timelinePosts")
                .document(postId)
                .update("isSaved", false)
                .await()
        } else {
            usersCollection.document(currentUserId)
                .collection("savedPosts")
                .document("${originalUserId}_$postId")
                .delete()
                .await()
        }
    }
    
    // Check if post is saved by current user
    @Suppress("unused")
    suspend fun isPostSaved(currentUserId: String, originalUserId: String, postId: String): Boolean {
        return if (currentUserId == originalUserId) {
            // Check own post's isSaved field
            val postDoc = usersCollection.document(currentUserId)
                .collection("timelinePosts")
                .document(postId)
                .get()
                .await()
            postDoc.getBoolean("isSaved") ?: false
        } else {
            // Check if exists in savedPosts collection
            val savedPostDoc = usersCollection.document(currentUserId)
                .collection("savedPosts")
                .document("${originalUserId}_$postId")
                .get()
                .await()
            savedPostDoc.exists()
        }
    }
    
    fun getSavedPosts(userId: String): Flow<List<TimelinePost>> = callbackFlow {
        var ownPostsList = emptyList<TimelinePost>()
        var otherPostsList = emptyList<TimelinePost>()
        
        fun updateAndSend() {
            val allSavedPosts = (ownPostsList + otherPostsList).sortedByDescending { it.timestamp }
            trySend(allSavedPosts)
        }
        
        val ownListener = usersCollection.document(userId)
            .collection("timelinePosts")
            .whereEqualTo("isSaved", true)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    ownPostsList = emptyList()
                    updateAndSend()
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    ownPostsList = snapshot.toObjects(TimelinePost::class.java).mapIndexed { index, post ->
                        post.copy(id = snapshot.documents[index].id)
                    }
                    updateAndSend()
                }
            }
        
        val otherListener = usersCollection.document(userId)
            .collection("savedPosts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    otherPostsList = emptyList()
                    updateAndSend()
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    otherPostsList = snapshot.toObjects(TimelinePost::class.java).mapIndexed { index, post ->
                        post.copy(id = snapshot.documents[index].id)
                    }
                    updateAndSend()
                }
            }
        
        awaitClose {
            ownListener.remove()
            otherListener.remove()
        }
    }
    
    // Get a single post by userId and postId
    // This is a utility function used internally by savePostById and can be used for post detail views
    suspend fun getPost(userId: String, postId: String): TimelinePost? {
        val doc = usersCollection.document(userId)
            .collection("timelinePosts")
            .document(postId)
            .get()
            .await()
        
        return if (doc.exists()) {
            doc.toObject(TimelinePost::class.java)?.copy(id = doc.id)
        } else {
            null
        }
    }
    
    // Save post by userId and postId only (fetches post data first using getPost)
    // Useful when you only have the post ID and need to save it
    @Suppress("UNUSED")
    suspend fun savePostById(currentUserId: String, originalUserId: String, postId: String) {
        val post = getPost(originalUserId, postId)
        if (post != null) {
            savePost(currentUserId, originalUserId, postId, post)
        } else {
            throw Exception("Post not found")
        }
    }
    
    // ==================== REACTIONS ====================
    
    /**
     * Toggle a reaction on a post.
     * If the user already has this reaction, remove it.
     * If the user doesn't have this reaction, add it.
     * 
     * @param postOwnerId The user ID who owns the post
     * @param postId The post ID
     * @param reactingUserId The user ID who is reacting
     * @param reactionType The type of reaction (heart, chicken, wow, pray)
     * @return true if reaction was added, false if removed
     */
    suspend fun toggleReaction(
        postOwnerId: String,
        postId: String,
        reactingUserId: String,
        reactionType: String
    ): Boolean {
        val postRef = usersCollection.document(postOwnerId)
            .collection("timelinePosts")
            .document(postId)
        
        val postDoc = postRef.get().await()
        if (!postDoc.exists()) return false
        
        @Suppress("UNCHECKED_CAST")
        val currentReactions = postDoc.get("reactions") as? Map<String, List<String>> ?: emptyMap()
        @Suppress("UNCHECKED_CAST")
        val currentReactionCounts = postDoc.get("reactionCounts") as? Map<String, Long> ?: emptyMap()
        
        val usersForType = currentReactions[reactionType]?.toMutableList() ?: mutableListOf()
        val wasAdded: Boolean
        
        if (usersForType.contains(reactingUserId)) {
            // Remove reaction
            usersForType.remove(reactingUserId)
            wasAdded = false
        } else {
            // Add reaction
            usersForType.add(reactingUserId)
            wasAdded = true
        }
        
        val updatedReactions = currentReactions.toMutableMap()
        val updatedCounts = currentReactionCounts.toMutableMap()
        
        if (usersForType.isEmpty()) {
            updatedReactions.remove(reactionType)
            updatedCounts.remove(reactionType)
        } else {
            updatedReactions[reactionType] = usersForType
            updatedCounts[reactionType] = usersForType.size.toLong()
        }
        
        postRef.update(
            "reactions", updatedReactions,
            "reactionCounts", updatedCounts
        ).await()
        
        // Centralized notification trigger
        if (wasAdded && postOwnerId != reactingUserId) {
            try {
                val userDoc = usersCollection.document(reactingUserId).get().await()
                val reactingUserName = userDoc.getString("fullName") ?: "Someone"
                val reactingUserPhotoUrl = userDoc.getString("photoUrl")
                
                notificationRepository.notifyReaction(
                    postOwnerId = postOwnerId,
                    postId = postId,
                    reactingUserId = reactingUserId,
                    reactingUserName = reactingUserName,
                    reactingUserPhotoUrl = reactingUserPhotoUrl,
                    reactionType = reactionType
                )
            } catch (e: Exception) {
                android.util.Log.e("PostRepository", "Error triggering reaction notification: ${e.message}")
            }
        }
        
        return wasAdded
    }
    
    /**
     * Get a user's reaction on a specific post.
     * Returns the reaction type if the user has reacted, null otherwise.
     */
    @Suppress("unused")
    suspend fun getUserReaction(
        postOwnerId: String,
        postId: String,
        userId: String
    ): String? {
        val postDoc = usersCollection.document(postOwnerId)
            .collection("timelinePosts")
            .document(postId)
            .get()
            .await()
        
        if (!postDoc.exists()) return null
        
        @Suppress("UNCHECKED_CAST")
        val reactions = postDoc.get("reactions") as? Map<String, List<String>> ?: return null
        
        for ((type, users) in reactions) {
            if (users.contains(userId)) {
                return type
            }
        }
        return null
    }
    
    // ==================== FOLLOW USERS ====================
    
    /**
     * Follow a user.
     * Adds targetUserId to current user's "following" list.
     * Adds currentUserId to target user's "followers" list.
     */
    suspend fun followUser(currentUserId: String, targetUserId: String) {
        if (currentUserId == targetUserId) return
        
        // Add to current user's following list
        val currentUserRef = usersCollection.document(currentUserId)
        val currentUserDoc = currentUserRef.get().await()
        @Suppress("UNCHECKED_CAST")
        val currentFollowing = (currentUserDoc.get("following") as? List<String>)?.toMutableList() ?: mutableListOf()
        
        if (!currentFollowing.contains(targetUserId)) {
            currentFollowing.add(targetUserId)
            currentUserRef.update("following", currentFollowing).await()
        }
        
        // Add to target user's followers list
        val targetUserRef = usersCollection.document(targetUserId)
        val targetUserDoc = targetUserRef.get().await()
        @Suppress("UNCHECKED_CAST")
        val targetFollowers = (targetUserDoc.get("followers") as? List<String>)?.toMutableList() ?: mutableListOf()
        
        if (!targetFollowers.contains(currentUserId)) {
            targetFollowers.add(currentUserId)
            targetUserRef.update("followers", targetFollowers).await()
            
            // Centralized notification trigger
            try {
                val currentUserDoc = usersCollection.document(currentUserId).get().await()
                val followerUserName = currentUserDoc.getString("fullName") ?: "Someone"
                val followerUserPhotoUrl = currentUserDoc.getString("photoUrl")
                
                notificationRepository.notifyFollow(
                    targetUserId = targetUserId,
                    followerUserId = currentUserId,
                    followerUserName = followerUserName,
                    followerUserPhotoUrl = followerUserPhotoUrl
                )
            } catch (e: Exception) {
                android.util.Log.e("PostRepository", "Error triggering follow notification: ${e.message}")
            }
        }
    }
    
    /**
     * Unfollow a user.
     */
    suspend fun unfollowUser(currentUserId: String, targetUserId: String) {
        if (currentUserId == targetUserId) return
        
        // Remove from current user's following list
        val currentUserRef = usersCollection.document(currentUserId)
        val currentUserDoc = currentUserRef.get().await()
        @Suppress("UNCHECKED_CAST")
        val currentFollowing = (currentUserDoc.get("following") as? List<String>)?.toMutableList() ?: mutableListOf()
        
        if (currentFollowing.contains(targetUserId)) {
            currentFollowing.remove(targetUserId)
            currentUserRef.update("following", currentFollowing).await()
        }
        
        // Remove from target user's followers list
        val targetUserRef = usersCollection.document(targetUserId)
        val targetUserDoc = targetUserRef.get().await()
        @Suppress("UNCHECKED_CAST")
        val targetFollowers = (targetUserDoc.get("followers") as? List<String>)?.toMutableList() ?: mutableListOf()
        
        if (targetFollowers.contains(currentUserId)) {
            targetFollowers.remove(currentUserId)
            targetUserRef.update("followers", targetFollowers).await()
        }
    }
    
    /**
     * Check if current user is following the target user.
     */
    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
        if (currentUserId == targetUserId) return false
        
        val currentUserDoc = try {
            usersCollection.document(currentUserId).get(com.google.firebase.firestore.Source.SERVER).await()
        } catch (_: Exception) {
            usersCollection.document(currentUserId).get(com.google.firebase.firestore.Source.CACHE).await()
        }
        @Suppress("UNCHECKED_CAST")
        val following = currentUserDoc.get("following") as? List<String> ?: emptyList()
        
        return following.contains(targetUserId)
    }
    
    /**
     * Get list of users that a user is following.
     */
    @Suppress("unused")
    suspend fun getFollowing(userId: String): List<String> {
        val userDoc = usersCollection.document(userId).get().await()
        @Suppress("UNCHECKED_CAST")
        return userDoc.get("following") as? List<String> ?: emptyList()
    }
    
    /**
     * Get list of followers for a user.
     */
    @Suppress("unused")
    suspend fun getFollowers(userId: String): List<String> {
        val userDoc = usersCollection.document(userId).get().await()
        @Suppress("UNCHECKED_CAST")
        return userDoc.get("followers") as? List<String> ?: emptyList()
    }

    suspend fun getUsersByIds(userIds: List<String>): List<UserProfile> {
        return try {
            if (userIds.isEmpty()) return emptyList()
            val chunks = userIds.distinct().chunked(10)
            val users = mutableListOf<UserProfile>()

            for (chunk in chunks) {
                val query = usersCollection.whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk).get().await()
                users.addAll(query.toObjects(UserProfile::class.java))
            }
            users
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun schedulePostSyncWorker(detectionId: String, postId: String, userId: String) {
        val context = ChickCareApplication.getInstance()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<MediaSyncWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(
                "userId" to userId,
                "detectionId" to detectionId,
                "postId" to postId
            ))
            .build()

        WorkManager.getInstance(context).enqueue(syncRequest)
        android.util.Log.d("PostRepository", "Scheduled MediaSyncWorker for post: $postId and detection: $detectionId")
    }
}

