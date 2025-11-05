package com.bisu.chickcare.backend.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

data class TimelinePost(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String? = null,
    val content: String = "", // Text content for status posts
    val detectionId: String = "",
    val detectionResult: String = "",
    val isHealthy: Boolean = false,
    val confidence: Float = 0f,
    val imageUri: String? = null,
    val audioUri: String? = null,
    val visibility: String = "public", // "public" or "private"
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

    @Suppress("UNUSED")
    fun getUserTimelinePosts(userId: String, includePrivate: Boolean = true): Flow<List<TimelinePost>> = flow {
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
        
        val snapshot = query.get().await()
        val posts = snapshot.toObjects(TimelinePost::class.java).mapIndexed { index, post ->
            post.copy(id = snapshot.documents[index].id)
        }
        emit(posts)
    }

    @Suppress("UNUSED")
    suspend fun deletePost(userId: String, postId: String) {
        usersCollection.document(userId)
            .collection("timelinePosts")
            .document(postId)
            .delete()
            .await()
    }
}

