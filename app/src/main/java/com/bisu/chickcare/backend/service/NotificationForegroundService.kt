package com.bisu.chickcare.backend.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bisu.chickcare.MainActivity
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.utils.SystemNotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground Service that keeps Firestore snapshot listeners alive in the background.
 * This enables real-time notifications even when the app is closed/swiped away.
 *
 * Since the project uses Firebase Spark (free) plan, Cloud Functions are not available.
 * This service is the alternative approach for background notifications.
 */
class NotificationForegroundService : Service() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var notificationHelper: SystemNotificationHelper? = null
    private var notificationListener: ListenerRegistration? = null
    private var chatListener: ListenerRegistration? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null

    companion object {
        private const val TAG = "NotifForegroundSvc"
        private const val FOREGROUND_NOTIFICATION_ID = 9999
        private const val CHANNEL_ID_FOREGROUND = "chickcare_foreground_service"

        fun start(context: Context) {
            val intent = Intent(context, NotificationForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NotificationForegroundService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        notificationHelper = SystemNotificationHelper(this)
        createForegroundChannel()
        startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification())

        // Listen for auth state changes (logout)
        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) {
                Log.d(TAG, "User logged out, stopping service")
                stopSelf()
            }
        }
        auth.addAuthStateListener(authListener!!)

        // Start listening
        startListeners()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        // If listeners got disconnected, reconnect
        if (notificationListener == null || chatListener == null) {
            startListeners()
        }
        // Restart if killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        stopListeners()
        authListener?.let { auth.removeAuthStateListener(it) }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createForegroundChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_FOREGROUND,
                "Background Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps ChickCare monitoring for new notifications"
                setShowBadge(false)
                setSound(null, null)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID_FOREGROUND)
            .setSmallIcon(R.drawable.chicken_icon)
            .setContentTitle("ChickCare")
            .setContentText("Monitoring notifications")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pi)
            .build()
    }

    // =========================================================================
    // Firestore Listeners
    // =========================================================================

    private fun startListeners() {
        val user = auth.currentUser
        if (user == null) {
            Log.d(TAG, "No user logged in, stopping service")
            stopSelf()
            return
        }
        val userId = user.uid
        stopListeners()
        Log.d(TAG, "Starting Firestore listeners for user: $userId")
        listenForNotifications(userId)
        listenForChatMessages(userId)
    }

    private fun stopListeners() {
        notificationListener?.remove()
        chatListener?.remove()
        notificationListener = null
        chatListener = null
    }

    /**
     * Listen for new documents in users/{userId}/notifications that are
     * created after the service starts.
     */
    private fun listenForNotifications(userId: String) {
        val startTime = System.currentTimeMillis()

        notificationListener = firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .whereGreaterThan("timestamp", startTime)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Notification listen error: ${e.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null || snapshot.isEmpty) return@addSnapshotListener

                for (docChange in snapshot.documentChanges) {
                    if (docChange.type == DocumentChange.Type.ADDED) {
                        processNotification(docChange.document)
                    }
                }
            }
    }

    /**
     * Listen for new/updated chat documents in users/{userId}/chats.
     */
    private fun listenForChatMessages(userId: String) {
        val startTime = System.currentTimeMillis()

        chatListener = firestore.collection("users")
            .document(userId)
            .collection("chats")
            .whereGreaterThan("updatedAt", startTime)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Chat listen error: ${e.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null || snapshot.isEmpty) return@addSnapshotListener

                for (docChange in snapshot.documentChanges) {
                    if (docChange.type == DocumentChange.Type.ADDED ||
                        docChange.type == DocumentChange.Type.MODIFIED
                    ) {
                        val otherUserId = docChange.document.getString("otherUserId") ?: continue
                        val lastMessageId = docChange.document.getString("lastMessageId") ?: continue
                        fetchAndNotifyMessage(userId, otherUserId, lastMessageId)
                    }
                }
            }
    }

    // =========================================================================
    // Process individual notification / chat documents
    // =========================================================================

    private fun processNotification(doc: com.google.firebase.firestore.DocumentSnapshot) {
        val type = doc.getString("type")
        val senderId = doc.getString("senderId") ?: return
        val senderName = doc.getString("senderName") ?: "Someone"
        val photoUrl = doc.getString("senderPhotoUrl")
        val notificationId = doc.id.hashCode()

        serviceScope.launch {
            val helper = notificationHelper ?: return@launch
            when (type) {
                "FRIEND_REQUEST" -> {
                    helper.showFriendRequestNotification(notificationId, senderId, senderName, photoUrl)
                }
                "FRIEND_ACCEPT" -> {
                    helper.showFriendRequestAcceptedNotification(notificationId, senderId, senderName, photoUrl)
                }
                "REACTION" -> {
                    val postId = doc.getString("postId") ?: return@launch
                    val postOwnerId = doc.getString("postOwnerId") ?: return@launch
                    val reactionType = doc.getString("reactionType") ?: "heart"
                    helper.showReactionNotification(notificationId, senderName, photoUrl, postId, postOwnerId, reactionType)
                }
                "COMMENT" -> {
                    val postId = doc.getString("postId") ?: return@launch
                    val postOwnerId = doc.getString("postOwnerId") ?: return@launch
                    helper.showCommentNotification(notificationId, senderName, photoUrl, postId, postOwnerId)
                }
                "FOLLOW" -> {
                    helper.showFollowNotification(notificationId, senderId, senderName, photoUrl)
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
                if (!messageDoc.exists()) return@addOnSuccessListener

                val senderId = messageDoc.getString("senderId") ?: return@addOnSuccessListener
                val isRead = messageDoc.getBoolean("isRead") ?: false

                // Only notify for unread messages NOT sent by me
                if (senderId != currentUserId && !isRead) {
                    firestore.collection("users").document(senderId).get()
                        .addOnSuccessListener { userDoc ->
                            val senderName = userDoc.getString("fullName") ?: "Friend"
                            val message = messageDoc.getString("message") ?: "New message"
                            val messageType = messageDoc.getString("messageType") ?: "text"

                            serviceScope.launch {
                                notificationHelper?.showChatMessageNotification(
                                    senderId, senderName, message, messageType, null
                                )
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to fetch message details: ${e.message}")
            }
    }
}
