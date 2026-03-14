package com.bisu.chickcare.backend.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class Comment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String? = null,
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class CommentRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    private val notificationRepository = NotificationRepository()
    
    /**
     * Add a comment to a post
     */
    suspend fun addComment(
        postOwnerId: String,
        postId: String,
        comment: Comment
    ): String {
        val postRef = usersCollection.document(postOwnerId)
            .collection("timelinePosts")
            .document(postId)
        
        val commentData = hashMapOf(
            "userId" to comment.userId,
            "userName" to comment.userName,
            "userPhotoUrl" to comment.userPhotoUrl,
            "text" to comment.text,
            "timestamp" to System.currentTimeMillis()
        )
        
        // Add comment to subcollection
        val commentRef = postRef.collection("comments")
            .add(commentData)
            .await()
        
        // Increment comment count on the post
        val postDoc = postRef.get().await()
        val currentCount = postDoc.getLong("commentCount") ?: 0
        postRef.update("commentCount", currentCount + 1).await()
        
        // Centralized notification trigger
        if (postOwnerId != comment.userId) {
            try {
                notificationRepository.notifyComment(
                    postOwnerId = postOwnerId,
                    postId = postId,
                    commentingUserId = comment.userId,
                    commentingUserName = comment.userName,
                    commentingUserPhotoUrl = comment.userPhotoUrl
                )
            } catch (e: Exception) {
                android.util.Log.e("CommentRepository", "Error triggering comment notification: ${e.message}")
            }
        }
        
        return commentRef.id
    }
    
    /**
     * Get comments for a post as a Flow
     */
    fun getComments(postOwnerId: String, postId: String): Flow<List<Comment>> = callbackFlow {
        val query = usersCollection.document(postOwnerId)
            .collection("timelinePosts")
            .document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
        
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                val comments = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Comment::class.java)?.copy(id = doc.id)
                }
                trySend(comments)
            }
        }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Delete a comment
     */
    suspend fun deleteComment(
        postOwnerId: String,
        postId: String,
        commentId: String
    ) {
        val postRef = usersCollection.document(postOwnerId)
            .collection("timelinePosts")
            .document(postId)
        
        // Delete the comment
        postRef.collection("comments")
            .document(commentId)
            .delete()
            .await()
        
        // Decrement comment count on the post
        val postDoc = postRef.get().await()
        val currentCount = postDoc.getLong("commentCount") ?: 0
        if (currentCount > 0) {
            postRef.update("commentCount", currentCount - 1).await()
        }
    }
    
    /**
     * Update a comment
     */
    suspend fun updateComment(
        postOwnerId: String,
        postId: String,
        commentId: String,
        newText: String
    ) {
        usersCollection.document(postOwnerId)
            .collection("timelinePosts")
            .document(postId)
            .collection("comments")
            .document(commentId)
            .update("text", newText)
            .await()
    }
    
    /**
     * Get comment count for a post
     */
    suspend fun getCommentCount(postOwnerId: String, postId: String): Int {
        val postDoc = usersCollection.document(postOwnerId)
            .collection("timelinePosts")
            .document(postId)
            .get()
            .await()
        
        return postDoc.getLong("commentCount")?.toInt() ?: 0
    }
}
