package com.bisu.chickcare.backend.repository

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
    val content: String = "",
    val detectionId: String = "",
    val detectionResult: String = "",
    @get:com.google.firebase.firestore.PropertyName("isHealthy")
    val isHealthy: Boolean = false,
    val confidence: Float = 0f,
    val imageUri: String? = null,
    val audioUri: String? = null,
    val visibility: String = "public",
    @get:com.google.firebase.firestore.PropertyName("isSaved")
    val isSaved: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class PostRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun createPost(
        userId: String,
        userName: String,
        userPhotoUrl: String?,
        detectionId: String,
        detectionResult: String,
        isHealthy: Boolean,
        confidence: Float,
        imageUri: String?,
        audioUri: String?,
        visibility: String
    ): String {
        val postData = hashMapOf(
            "userId" to userId,
            "userName" to userName,
            "userPhotoUrl" to userPhotoUrl,
            "detectionId" to detectionId,
            "detectionResult" to detectionResult,
            "isHealthy" to isHealthy,
            "confidence" to confidence,
            "imageUri" to imageUri,
            "audioUri" to audioUri,
            "visibility" to visibility,
            "timestamp" to System.currentTimeMillis()
        )
        
        val docRef = usersCollection.document(userId)
            .collection("timelinePosts")
            .add(postData)
            .await()
        
        return docRef.id
    }
    
    suspend fun createTextPost(
        userId: String,
        userName: String,
        userPhotoUrl: String?,
        content: String,
        visibility: String
    ): String {
        val postData = hashMapOf(
            "userId" to userId,
            "userName" to userName,
            "userPhotoUrl" to userPhotoUrl,
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
}

