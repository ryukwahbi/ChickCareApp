package com.bisu.chickcare.backend.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bisu.chickcare.backend.utils.SystemNotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val notificationHelper = SystemNotificationHelper(appContext)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val sharedPrefs = appContext.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "NotificationWorker"
        private const val PREF_LAST_CHECK_TIME = "last_check_time"
        // Initial check lookback (if first run): 24 hours
        private const val INITIAL_LOOKBACK_MS = 24 * 60 * 60 * 1000L 
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d(TAG, "No user logged in, skipping notification check")
            return@withContext Result.success()
        }

        val userId = currentUser.uid
        val currentTime = System.currentTimeMillis()
        
        // Get last check time, default to 24h ago if never run
        val lastCheckTime = sharedPrefs.getLong(PREF_LAST_CHECK_TIME, currentTime - INITIAL_LOOKBACK_MS)
        
        Log.d(TAG, "Checking notifications since: $lastCheckTime (User: $userId)")

        try {
            checkSystemNotifications(userId, lastCheckTime)
            checkChatMessages(userId, lastCheckTime)
            
            // Update last check time
            sharedPrefs.edit().putLong(PREF_LAST_CHECK_TIME, currentTime).apply()
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notifications", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun checkSystemNotifications(userId: String, sinceTime: Long) {
        val snapshot = firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .whereGreaterThan("timestamp", sinceTime)
            .get()
            .await()

        for (doc in snapshot.documents) {
            val type = doc.getString("type")
            val senderId = doc.getString("senderId") ?: continue
            val senderName = doc.getString("senderName") ?: "Someone"
            val photoUrl = doc.getString("senderPhotoUrl")
            val notificationId = doc.id.hashCode()

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
                // Add other types as needed
            }
        }
    }

    private suspend fun checkChatMessages(userId: String, sinceTime: Long) {
        // 1. Get chats updated recently
        val chatsSnapshot = firestore.collection("users")
            .document(userId)
            .collection("chats")
            .whereGreaterThan("updatedAt", sinceTime)
            .get()
            .await()

        for (chatDoc in chatsSnapshot.documents) {
            val lastMessageId = chatDoc.getString("lastMessageId") ?: continue
            val otherUserId = chatDoc.getString("otherUserId") ?: continue
            
            // 2. Fetch the actual message to check sender and read status
            // We optimized by not fetching if we don't need to, but here we need to know who sent it.
            try {
                val messageDoc = firestore.collection("users")
                    .document(userId)
                    .collection("chats")
                    .document(otherUserId)
                    .collection("messages")
                    .document(lastMessageId)
                    .get()
                    .await()

                if (messageDoc.exists()) {
                    val senderId = messageDoc.getString("senderId")
                    val isRead = messageDoc.getBoolean("isRead") ?: false
                    
                    // Only notify if unread and NOT sent by me
                    if (senderId != userId && !isRead) {
                        val senderName = try {
                            // Try to get name from chat metadata or simple fallback
                            // Ideally fetch user profile, but to save reads we can trust local data or fetch if needed
                            // For now, let's fetch the sender profile or use a cached name map if we had one.
                            // To be safe and accurate:
                             val userDoc = firestore.collection("users").document(senderId!!).get().await()
                             userDoc.getString("fullName") ?: "Friend"
                        } catch (e: Exception) {
                            "Friend"
                        }
                        
                        val message = messageDoc.getString("message") ?: "New message"
                        val messageType = messageDoc.getString("messageType") ?: "text"
                        // Get sender photo from profile fetch above effectively
                        // For optimization, we'll skip photo for now or do a separate fetch if we really want it.
                        // Let's keep it simple.
                        
                        notificationHelper.showChatMessageNotification(
                            senderId!!, senderName, message, messageType, null
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to verify message details for chat ${chatDoc.id}", e)
            }
        }
    }
}
