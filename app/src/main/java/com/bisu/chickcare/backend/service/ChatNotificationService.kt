package com.bisu.chickcare.backend.service

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Service to send push notifications for chat messages
 * Uses HTTP calls to Firebase Cloud Functions instead of Firebase Functions SDK
 */
class ChatNotificationService {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val httpClient = OkHttpClient()
    
    companion object {
        private const val TAG = "ChatNotificationService"
        // Replace with your deployed Firebase Cloud Functions URL.
        // Format: "https://YOUR_REGION-YOUR_PROJECT_ID.cloudfunctions.net/sendChatNotification"
        // Example: "https://us-central1-myproject.cloudfunctions.net/sendChatNotification"
        // To get your URL: Deploy the Cloud Function, then copy the URL from Firebase Console.
        // NOTE: Leave as-is and the service will log a reminder and skip the call.
        private const val CLOUD_FUNCTIONS_URL: String =
            "https://YOUR_REGION-YOUR_PROJECT_ID.cloudfunctions.net/sendChatNotification"
    }
    
    /**
     * Send push notification when a message is sent
     * Uses HTTP call to Cloud Function instead of Firebase Functions SDK
     */
    suspend fun sendChatNotification(
        senderId: String,
        receiverId: String,
        message: String,
        messageType: String
    ) {
        try {
            // Get sender's profile info
            val senderDoc = firestore.collection("users").document(senderId).get().await()
            val senderName = senderDoc.getString("fullName") ?: "Someone"
            val senderPhotoUrl = senderDoc.getString("photoUrl")
            
            // Get receiver's FCM token
            val receiverDoc = firestore.collection("users").document(receiverId).get().await()
            val receiverFcmToken = receiverDoc.getString("fcmToken")
            
            if (receiverFcmToken.isNullOrEmpty()) {
                Log.w(TAG, "Receiver $receiverId has no FCM token, cannot send notification")
                return
            }
            
            // Format message preview
            val messagePreview = when (messageType) {
                "image" -> "📷 Sent an image"
                "audio" -> "🎵 Sent an audio"
                else -> message.take(100) // Limit to 100 characters
            }
            
            // Get Firebase Auth token for authentication
            val idToken = auth.currentUser?.getIdToken(false)?.await()?.token
            if (idToken == null) {
                Log.w(TAG, "User not authenticated, cannot call Cloud Function")
                return
            }
            
            // Build request body
            val requestBody = JSONObject().apply {
                put("token", receiverFcmToken)
                put("title", senderName)
                put("body", messagePreview)
                put("data", JSONObject().apply {
                    put("type", "CHAT_MESSAGE")
                    put("senderId", senderId)
                    put("receiverId", receiverId)
                    put("senderName", senderName)
                    put("senderPhotoUrl", senderPhotoUrl ?: "")
                    put("message", message)
                    put("messageType", messageType)
                })
            }
            
            // Get Cloud Functions URL
            val functionsUrl = CLOUD_FUNCTIONS_URL

            if (functionsUrl.contains("YOUR_REGION") || functionsUrl.contains("YOUR_PROJECT_ID")) {
                // Cloud Function URL not configured - just log
                Log.d(TAG, "Chat notification should be sent to $receiverId from $senderName")
                Log.d(TAG, "Note: Set CLOUD_FUNCTIONS_URL in ChatNotificationService to enable notifications")
                return
            }
            
            // Send notification via Cloud Function
            try {
                val request = Request.Builder()
                    .url(functionsUrl)
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $idToken")
                    .addHeader("Content-Type", "application/json")
                    .build()
                
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d(TAG, "Chat notification sent successfully")
                } else {
                    Log.e(TAG, "Failed to send notification: ${response.code} - ${response.message}")
                }
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error calling Cloud Function: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending chat notification: ${e.message}", e)
            // Don't throw - notification failure shouldn't block message sending
        }
    }
}

