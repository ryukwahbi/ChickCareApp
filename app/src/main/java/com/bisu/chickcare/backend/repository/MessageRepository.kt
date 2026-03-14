package com.bisu.chickcare.backend.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val imageUrl: String? = null,
    val audioUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    @get:com.google.firebase.firestore.PropertyName("isRead")
    val isRead: Boolean = false,
    val messageType: String = "text", // "text", "image", "audio"
    // Reactions: Map of emoji to list of user IDs who reacted
    val reactions: Map<String, List<String>> = emptyMap()
)

class MessageRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    
    /**
     * Send a text message
     */
    suspend fun sendMessage(
        senderId: String,
        receiverId: String,
        message: String,
        imageUrl: String? = null,
        audioUrl: String? = null
    ): String {
        return try {
            val messageType = when {
                !imageUrl.isNullOrEmpty() -> "image"
                !audioUrl.isNullOrEmpty() -> "audio"
                else -> "text"
            }
            
            val timestamp = System.currentTimeMillis()
            val messageData = hashMapOf(
                "senderId" to senderId,
                "receiverId" to receiverId,
                "message" to message,
                "imageUrl" to (imageUrl ?: ""),
                "audioUrl" to (audioUrl ?: ""),
                "timestamp" to timestamp,
                "isRead" to false,
                "messageType" to messageType
            )
            
            // Generate a unique ID to be used for both copies of the message
            // This ensures we can update the status on both sides later
            val messageId = usersCollection.document().id
            
            // We MUST wait for this to ensure data integrity
            coroutineScope {
                val senderMessageDeferred = async {
                    usersCollection.document(senderId)
                        .collection("chats")
                        .document(receiverId)
                        .collection("messages")
                        .document(messageId)
                        .set(messageData)
                        .await()
                }
                
                val receiverMessageDeferred = async {
                    usersCollection.document(receiverId)
                        .collection("chats")
                        .document(senderId)
                        .collection("messages")
                        .document(messageId)
                        .set(messageData)
                        .await()
                }
                
                awaitAll(senderMessageDeferred, receiverMessageDeferred)
            }

            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val metadataUpdate1 = async {
                        updateChatMetadata(senderId, receiverId, message, messageType, messageId)
                    }
                    val metadataUpdate2 = async {
                        updateChatMetadata(receiverId, senderId, message, messageType, messageId)
                    }
                    
                    // Send notification
                    val notificationJob = async {
                        try {
                            com.bisu.chickcare.backend.service.ChatNotificationService()
                                .sendChatNotification(senderId, receiverId, message, messageType)
                        } catch (e: Exception) {
                            Log.w("MessageRepository", "Failed to send notification: ${e.message}")
                        }
                    }
                    
                    // Allow them to complete in this background scope
                    awaitAll(metadataUpdate1, metadataUpdate2, notificationJob)
                } catch (e: Exception) {
                    Log.e("MessageRepository", "Background task failed: ${e.message}")
                }
            }
                
            Log.d("MessageRepository", "Message sent successfully (background tasks queued): $messageId")
            messageId
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error sending message: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Update chat metadata (last message, timestamp)
     */
    private suspend fun updateChatMetadata(
        userId: String,
        otherUserId: String,
        lastMessage: String,
        messageType: String,
        messageId: String
    ) {
        try {
            val chatData = hashMapOf(
                "lastMessage" to lastMessage,
                "lastMessageType" to messageType,
                "lastMessageTimestamp" to System.currentTimeMillis(),
                "lastMessageId" to messageId,
                "otherUserId" to otherUserId,
                "updatedAt" to System.currentTimeMillis()
            )
            
            usersCollection.document(userId)
                .collection("chats")
                .document(otherUserId)
                .set(chatData, com.google.firebase.firestore.SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w("MessageRepository", "Error updating chat metadata: ${e.message}")
        }
    }
    
    /**
     * Get messages for a chat conversation
     */
    fun getMessages(userId: String, otherUserId: String): Flow<List<ChatMessage>> = callbackFlow {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        
        try {
            listener = usersCollection.document(userId)
                .collection("chats")
                .document(otherUserId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("MessageRepository", "Error listening to messages: ${error.message}", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null && !snapshot.isEmpty) {
                        val messages = snapshot.toObjects(ChatMessage::class.java).mapIndexed { index, message ->
                            message.copy(id = snapshot.documents[index].id)
                        }
                        trySend(messages)
                    } else {
                        trySend(emptyList())
                    }
                }
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error setting up message listener: ${e.message}", e)
            trySend(emptyList())
        }
        
        awaitClose {
            listener?.remove()
        }
    }
    
    /**
     * Mark messages as read
     * Updated to sync "Seen" status to the sender's copy of the message as well.
     */
    suspend fun markMessagesAsRead(userId: String, otherUserId: String) {
        try {
            // 1. Get unread messages in MY inbox (that came from the other person)
            val snapshot = usersCollection.document(userId)
                .collection("chats")
                .document(otherUserId)
                .collection("messages")
                .whereEqualTo("isRead", false)
                .whereEqualTo("senderId", otherUserId)
                .get()
                .await()
            
            // 2. Iterate and update both copies
            snapshot.documents.forEach { doc ->
                val messageId = doc.id
                
                // Update my copy locally (so badge disappears)
                doc.reference.update("isRead", true)
                
                // Update sender's copy remote (so they see "Seen")
                // NOTE: This works reliably because new messages use Shared IDs.
                // For old messages, this might fail silently if IDs don't match, which is acceptable.
                try {
                     usersCollection.document(otherUserId)
                        .collection("chats")
                        .document(userId)
                        .collection("messages")
                        .document(messageId)
                        .update("isRead", true)
                } catch (e: Exception) {
                    Log.w("MessageRepository", "Failed to update sender's message status: ${e.message}")
                }
            }
            
            Log.d("MessageRepository", "Marked ${snapshot.size()} messages as read")
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error marking messages as read: ${e.message}", e)
        }
    }
    
    /**
     * Get unread message count for a chat
     */
    suspend fun getUnreadCount(userId: String, otherUserId: String): Int {
        return try {
            val snapshot = usersCollection.document(userId)
                .collection("chats")
                .document(otherUserId)
                .collection("messages")
                .whereEqualTo("isRead", false)
                .whereEqualTo("senderId", otherUserId)
                .get()
                .await()
            snapshot.size()
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error getting unread count: ${e.message}", e)
            0
        }
    }

    /**
     * Delete a message for both participants.
     * Returns true on success.
     */
    suspend fun deleteMessage(
        userId: String,
        otherUserId: String,
        messageId: String
    ): Boolean {
        return try {
            // Delete from current user's thread
            usersCollection.document(userId)
                .collection("chats")
                .document(otherUserId)
                .collection("messages")
                .document(messageId)
                .delete()
                .await()

            // Delete from other user's thread
            usersCollection.document(otherUserId)
                .collection("chats")
                .document(userId)
                .collection("messages")
                .document(messageId)
                .delete()
                .await()

            true
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error deleting message: ${e.message}", e)
            false
        }
    }

    /**
     * Toggle a reaction on a message.
     * Updates both sender's and receiver's copy of the message.
     */
    suspend fun toggleReaction(
        userId: String,
        otherUserId: String,
        messageId: String,
        reaction: String
    ): Boolean {
        return try {
            // Function to update reactions in a document
            suspend fun updateDocReactions(uId: String, oId: String, mId: String) {
                val docRef = usersCollection.document(uId)
                    .collection("chats")
                    .document(oId)
                    .collection("messages")
                    .document(mId)

                val snapshot = docRef.get().await()
                if (snapshot.exists()) {
                    val currentReactions = snapshot.get("reactions") as? Map<String, List<String>> ?: emptyMap()
                    val usersWhoReacted = currentReactions[reaction]?.toMutableList() ?: mutableListOf()

                    if (usersWhoReacted.contains(userId)) {
                        usersWhoReacted.remove(userId)
                    } else {
                        usersWhoReacted.add(userId)
                    }

                    val updatedReactions = currentReactions.toMutableMap()
                    if (usersWhoReacted.isEmpty()) {
                        updatedReactions.remove(reaction)
                    } else {
                        updatedReactions[reaction] = usersWhoReacted
                    }

                    docRef.update("reactions", updatedReactions).await()
                }
            }

            // Update my copy
            updateDocReactions(userId, otherUserId, messageId)

            // Update other person's copy
            try {
                updateDocReactions(otherUserId, userId, messageId)
            } catch (e: Exception) {
                Log.w("MessageRepository", "Failed to update other user's message reaction: ${e.message}")
            }

            true
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error toggling reaction: ${e.message}", e)
            false
        }
    }
    /**
     * Delete an entire conversation for the current user.
     * Note: This only deletes the conversation reference and local messages copy.
     * It does not delete the other user's copy.
     */
    suspend fun deleteConversation(
        userId: String,
        otherUserId: String
    ): Boolean {
        return try {
            // 1. Delete the chat document (this removes it from the list)
            usersCollection.document(userId)
                .collection("chats")
                .document(otherUserId)
                .delete()
                .await()

            // 2. Ideally, we should also delete all messages in the subcollection.
            // Since Firestore client doesn't support recursive delete, we'll iterate and batch delete.
            val messagesRef = usersCollection.document(userId)
                .collection("chats")
                .document(otherUserId)
                .collection("messages")

            val snapshot = messagesRef.get().await()
            if (!snapshot.isEmpty) {
                val batch = firestore.batch()
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }
            
            true
        } catch (e: Exception) {
            Log.e("MessageRepository", "Error deleting conversation: ${e.message}", e)
            false
        }
    }
}

