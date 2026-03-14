package com.bisu.chickcare.backend.viewmodels

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.repository.ChatMessage
import com.bisu.chickcare.backend.repository.MessageRepository
import com.bisu.chickcare.backend.service.NetworkConnectivityHelper
import com.bisu.chickcare.backend.utils.OfflineAuthHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val messageRepository = MessageRepository()
    private val auth = FirebaseAuth.getInstance()
    @Suppress("StaticFieldLeak")
    private val context = application.applicationContext

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private var currentOtherUserId: String? = null

    private fun getCurrentUserId(): String? {
        val firebaseUid = auth.currentUser?.uid
        if (firebaseUid != null) return firebaseUid
        return OfflineAuthHelper.getCurrentLocalUserId(getApplication())
    }

    /**
     * Load messages for a conversation
     */
    fun loadMessages(otherUserId: String) {
        currentOtherUserId = otherUserId
        val currentUserId = getCurrentUserId() ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                messageRepository.getMessages(currentUserId, otherUserId).collect { messageList ->
                    _messages.value = messageList
                    _isLoading.value = false

                    // Mark messages as read when viewing
                    if (messageList.isNotEmpty()) {
                        messageRepository.markMessagesAsRead(currentUserId, otherUserId)
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading messages: ${e.message}", e)
                _isLoading.value = false
            }
        }
    }

    /**
     * Send a text message
     */
    fun sendMessage(message: String, otherUserId: String) {
        if (!NetworkConnectivityHelper.isOnline(context)) {
            Toast.makeText(context, "You must be online to send messages", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = getCurrentUserId() ?: return

        if (message.isBlank()) return

        viewModelScope.launch {
            try {
                _isSending.value = true
                messageRepository.sendMessage(
                    senderId = currentUserId,
                    receiverId = otherUserId,
                    message = message.trim()
                )
                _isSending.value = false
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending message: ${e.message}", e)
                _isSending.value = false
            }
        }
    }

    /**
     * Send an existing image URL (no upload)
     */
    fun sendImageUrl(imageUrl: String, otherUserId: String) {
        if (!NetworkConnectivityHelper.isOnline(context)) {
            Toast.makeText(context, "You must be online to send images", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = getCurrentUserId() ?: return
        if (imageUrl.isBlank()) return

        viewModelScope.launch {
            try {
                _isSending.value = true
                messageRepository.sendMessage(
                    senderId = currentUserId,
                    receiverId = otherUserId,
                    message = "📷 Image",
                    imageUrl = imageUrl.trim()
                )
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending image URL: ${e.message}", e)
            } finally {
                _isSending.value = false
            }
        }
    }

    /**
     * Send an existing audio URL (no upload)
     */
    fun sendAudioUrl(audioUrl: String, otherUserId: String) {
        if (!NetworkConnectivityHelper.isOnline(context)) {
            Toast.makeText(context, "You must be online to send audio", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = getCurrentUserId() ?: return
        if (audioUrl.isBlank()) return

        viewModelScope.launch {
            try {
                _isSending.value = true
                messageRepository.sendMessage(
                    senderId = currentUserId,
                    receiverId = otherUserId,
                    message = "🎵 Audio",
                    audioUrl = audioUrl.trim()
                )
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error sending audio URL: ${e.message}", e)
            } finally {
                _isSending.value = false
            }
        }
    }

    /**
     * Format timestamp for display
     */
    @Suppress("unused")
    fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Just now" // Less than 1 minute
            diff < 3600000 -> "${diff / 60000}m ago" // Less than 1 hour
            diff < 86400000 -> "${diff / 3600000}h ago" // Less than 1 day
            else -> {
                val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
        }
    }

    /**
     * Format timestamp for message display (e.g., "8:06 PM")
     */
    fun formatMessageTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Check if message is from current user
     */
    fun isCurrentUser(senderId: String): Boolean {
        return senderId == getCurrentUserId()
    }

    /**
     * Delete a message (only if sent by current user).
     */
    fun deleteMessage(message: ChatMessage) {
        if (!NetworkConnectivityHelper.isOnline(context)) {
            Toast.makeText(context, "You must be online to delete messages", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = getCurrentUserId() ?: return
        val otherUserId = currentOtherUserId ?: return

        // Only allow deleting own messages
        if (message.senderId != currentUserId) return
        if (message.id.isBlank()) return

        viewModelScope.launch {
            val deleted = try {
                messageRepository.deleteMessage(
                    userId = currentUserId,
                    otherUserId = otherUserId,
                    messageId = message.id
                )
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error deleting message: ${e.message}", e)
                false
            }

            if (deleted) {
                // Optimistic local update; Firestore listener will also refresh
                _messages.value = _messages.value.filterNot { it.id == message.id }
            }
        }
    }
    /**
     * Toggle a reaction on a message.
     */
    fun toggleReaction(message: ChatMessage, reaction: String) {
        if (!NetworkConnectivityHelper.isOnline(context)) {
            Toast.makeText(context, "You must be online to react to messages", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = getCurrentUserId() ?: return
        val otherUserId = currentOtherUserId ?: return
        if (message.id.isBlank()) return

        viewModelScope.launch {
            try {
                messageRepository.toggleReaction(
                    userId = currentUserId,
                    otherUserId = otherUserId,
                    messageId = message.id,
                    reaction = reaction
                )
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error toggling reaction: ${e.message}", e)
            }
        }
    }
}
