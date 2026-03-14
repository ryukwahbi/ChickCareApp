package com.bisu.chickcare.backend.service

import android.content.Context
import android.util.Log
import com.bisu.chickcare.backend.utils.SystemNotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RealtimeNotificationManager(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notificationHelper = SystemNotificationHelper(context)
    
    private var notificationListener: ListenerRegistration? = null
    private var chatListener: ListenerRegistration? = null
    
    companion object {
        private const val TAG = "RealtimeNotifManager"
    }

    /**
     * Start listening for real-time updates.
     * Should be called when the app comes to foreground or user logs in.
     */
    fun startListening() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d(TAG, "No user logged in, cannot start listening")
            return
        }
        
        val userId = currentUser.uid
        stopListening() // Clear any existing listeners
        
        Log.d(TAG, "Starting real-time notification listeners for user: $userId")
        
        listenForNotifications(userId)
        listenForChatMessages(userId)
    }

    /**
     * Stop listening.
     * Should be called when app goes to background or user logs out.
     */
    fun stopListening() {
        notificationListener?.remove()
        chatListener?.remove()
        notificationListener = null
        chatListener = null
        Log.d(TAG, "Stopped real-time listeners")
    }

    private fun listenForNotifications(userId: String) {
        // Listen specifically for new notifications (timestamp > now) would be ideal,
        // but to catch things that happened just moments ago or while connecting,
        // we might just listen to recent ones. 
        // However, a simple way is to listen to the collection updates.
        // LIMITATION: On initial listen, we might get existing documents as "ADDED".
        // To prevent spamming notifications on app open, we can filter by timestamp > appStartTime
        // OR we can rely on `doc.metadata.isFromCache` and ignore initial snapshot?
        // Better approach: Listen for changes where timestamp is greater than "now" (start time)
        
        val startTime = System.currentTimeMillis()
        
        notificationListener = firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .whereGreaterThan("timestamp", startTime)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    for (doc in snapshot.documentChanges) {
                        if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            // New notification!
                            processNotification(doc.document)
                        }
                    }
                }
            }
    }
    
    private fun listenForChatMessages(userId: String) {
        val startTime = System.currentTimeMillis()
        
        // Listen to "chats" collection updates to see which chat updated
        chatListener = firestore.collection("users")
            .document(userId)
            .collection("chats")
            .whereGreaterThan("updatedAt", startTime)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Chat listen failed: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    for (doc in snapshot.documentChanges) {
                        if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED || 
                            doc.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                            
                            // A chat was updated (new message)
                            val otherUserId = doc.document.getString("otherUserId") ?: continue
                            val lastMessageId = doc.document.getString("lastMessageId") ?: continue
                            
                            fetchAndNotifyMessage(userId, otherUserId, lastMessageId)
                        }
                    }
                }
            }
    }

    private fun processNotification(doc: com.google.firebase.firestore.DocumentSnapshot) {
        val type = doc.getString("type")
        val senderId = doc.getString("senderId") ?: return
        val senderName = doc.getString("senderName") ?: "Someone"
        val photoUrl = doc.getString("senderPhotoUrl")
        val notificationId = doc.id.hashCode()
        
        CoroutineScope(Dispatchers.Main).launch {
            when (type) {
                "FRIEND_REQUEST" -> {
                    notificationHelper.showFriendRequestNotification(
                        notificationId, senderId, senderName, photoUrl
                    )
                }
                "FRIEND_ACCEPT" -> {
                    notificationHelper.showFriendRequestAcceptedNotification(
                        notificationId, senderId, senderName, photoUrl
                    )
                }
                "REACTION" -> {
                    val postId = doc.getString("postId") ?: return@launch
                    val postOwnerId = doc.getString("postOwnerId") ?: return@launch
                    val reactionType = doc.getString("reactionType") ?: "heart"
                    notificationHelper.showReactionNotification(
                        notificationId, senderName, photoUrl, postId, postOwnerId, reactionType
                    )
                }
                "COMMENT" -> {
                    val postId = doc.getString("postId") ?: return@launch
                    val postOwnerId = doc.getString("postOwnerId") ?: return@launch
                    notificationHelper.showCommentNotification(
                        notificationId, senderName, photoUrl, postId, postOwnerId
                    )
                }
                "FOLLOW" -> {
                    notificationHelper.showFollowNotification(
                        notificationId, senderId, senderName, photoUrl
                    )
                }
            }
        }
    }
    
    private fun fetchAndNotifyMessage(currentUserId: String, otherUserId: String, messageId: String) {
        firestore.collection("users")
            .document(currentUserId)
            .collection("chats")
            .document(otherUserId)
            .collection("messages")
            .document(messageId)
            .get()
            .addOnSuccessListener { messageDoc ->
                if (messageDoc.exists()) {
                    val senderId = messageDoc.getString("senderId")
                    val isRead = messageDoc.getBoolean("isRead") ?: false
                    
                    if (senderId != currentUserId && !isRead) {
                        // It's a new unread message from the other person
                        // Ensure we have sender name. We might need to fetch it or use "Friend"
                        
                        // Quick optimization: we can try to get the name from the Chat document itself if stored,
                        // otherwise we'll fetch the user profile.
                        // For speed, let's fetch the user profile logic quickly or just use a placeholder
                        
                        firestore.collection("users").document(senderId!!).get()
                            .addOnSuccessListener { userDoc ->
                                val senderName = userDoc.getString("fullName") ?: "Friend"
                                val message = messageDoc.getString("message") ?: "New message"
                                val messageType = messageDoc.getString("messageType") ?: "text"
                                
                                CoroutineScope(Dispatchers.Main).launch {
                                    notificationHelper.showChatMessageNotification(
                                        senderId, senderName, message, messageType, null
                                    )
                                }
                            }
                    }
                }
            }
    }
}
