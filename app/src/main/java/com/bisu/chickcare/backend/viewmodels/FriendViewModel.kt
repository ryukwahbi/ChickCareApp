package com.bisu.chickcare.backend.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.repository.FriendRepository
import com.bisu.chickcare.backend.repository.FriendRequest
import com.bisu.chickcare.backend.repository.FriendSuggestion
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FriendViewModel : ViewModel() {
    private val friendRepository = FriendRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _suggestions = MutableStateFlow<List<FriendSuggestion>>(emptyList())
    val suggestions = _suggestions.asStateFlow()
    
    private val _friends = MutableStateFlow<List<FriendSuggestion>>(emptyList())
    val friends = _friends.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _pendingRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val pendingRequests = _pendingRequests.asStateFlow()

    fun loadFriendSuggestions() {
        val userId = auth.currentUser?.uid ?: return
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val suggestionsList = friendRepository.getFriendSuggestions(userId)
                // Update suggestions - will be empty list when offline
                _suggestions.value = suggestionsList
            } catch (e: Exception) {
                android.util.Log.e("FriendViewModel", "Error loading suggestions (likely offline): ${e.message}", e)
                // Clear suggestions when offline - show empty state
                _suggestions.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadFriends() {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                val friendsList = friendRepository.getFriends(userId)
                _friends.value = friendsList
            } catch (_: Exception) {
                // Handle error silently
            }
        }
    }
    
    fun sendFriendRequest(
        toUserId: String, 
        toUserName: String, 
        callback: (Boolean, String) -> Unit
    ) {
        val fromUserId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in")
            return
        }
        val fromUserName = auth.currentUser?.displayName ?: "User"
        
        viewModelScope.launch {
            try {
                val notificationRepository = com.bisu.chickcare.backend.repository.NotificationRepository()
                friendRepository.sendFriendRequest(
                    fromUserId = fromUserId, 
                    fromUserName = fromUserName, 
                    toUserId = toUserId,
                    notificationRepository = notificationRepository
                )
                // Optimistically remove the user from the suggestions list so the UI badge updates immediately
                _suggestions.value = _suggestions.value.filterNot { it.userId == toUserId }
                // Reload pending requests after sending (to update if user sent request to someone)
                loadPendingFriendRequests()
                callback(true, "Friend request sent to $toUserName")
            } catch (e: Exception) {
                callback(false, "Failed to send friend request to $toUserName: ${e.message}")
            }
        }
    }
    
    fun acceptFriendRequest(
        requestId: String, 
        friendUserId: String, 
        friendName: String, 
        callback: (Boolean, String) -> Unit,
        notificationRepository: com.bisu.chickcare.backend.repository.NotificationRepository? = null
    ) {
        val currentUserId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in")
            return
        }
        
        viewModelScope.launch {
            try {
                // Always create notification repository if not provided
                val notificationRepo = notificationRepository ?: com.bisu.chickcare.backend.repository.NotificationRepository()
                
                friendRepository.acceptFriendRequest(
                    requestId, 
                    currentUserId, 
                    friendUserId, 
                    friendName,
                    notificationRepo
                )
                // Reload friends list after accepting
                loadFriends()
                // Reload suggestions to update the list
                loadFriendSuggestions()
                // Reload pending requests to update the list
                loadPendingFriendRequests()
                callback(true, "Friend request accepted")
            } catch (e: Exception) {
                android.util.Log.e("FriendViewModel", "Error accepting friend request: ${e.message}", e)
                callback(false, "Failed to accept friend request: ${e.message}")
            }
        }
    }
    
    fun declineFriendRequest(requestId: String, callback: (Boolean, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in")
            return
        }
        
        viewModelScope.launch {
            try {
                friendRepository.declineFriendRequest(requestId, currentUserId)
                // Reload suggestions after declining
                loadFriendSuggestions()
                // Reload pending requests after declining
                loadPendingFriendRequests()
                callback(true, "Friend request declined")
            } catch (e: Exception) {
                android.util.Log.e("FriendViewModel", "Error declining friend request: ${e.message}", e)
                callback(false, "Failed to decline friend request: ${e.message}")
            }
        }
    }

    /**
     * Check request status for a specific user
     */
    fun checkRequestStatus(targetUserId: String, callback: (String?) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: run {
            callback(null)
            return
        }
        
        viewModelScope.launch {
            try {
                val status = friendRepository.getRequestStatus(currentUserId, targetUserId)
                callback(status)
            } catch (e: Exception) {
                android.util.Log.e("FriendViewModel", "Error checking request status: ${e.message}", e)
                callback(null)
            }
        }
    }
    
    /**
     * Load pending friend requests into StateFlow for reactive UI updates
     */
    fun loadPendingFriendRequests() {
        val currentUserId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                val requests = friendRepository.getPendingFriendRequests(currentUserId)
                _pendingRequests.value = requests
            } catch (e: Exception) {
                android.util.Log.e("FriendViewModel", "Error loading pending requests: ${e.message}", e)
                _pendingRequests.value = emptyList()
            }
        }
    }
    
    /**
     * Get pending friend requests with callback support
     * Also updates the StateFlow for reactive UI updates
     */
    fun getPendingFriendRequests(callback: (Boolean, List<FriendRequest>, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: run {
            callback(false, emptyList(), "User not logged in")
            return
        }
        
        viewModelScope.launch {
            try {
                val requests = friendRepository.getPendingFriendRequests(currentUserId)
                _pendingRequests.value = requests
                callback(true, requests, "")
            } catch (e: Exception) {
                android.util.Log.e("FriendViewModel", "Error loading pending requests: ${e.message}", e)
                _pendingRequests.value = emptyList()
                callback(false, emptyList(), "Failed to load friend requests: ${e.message}")
            }
        }
    }
    
    /**
     * Unfriend a user
     */
    fun unfriend(friendUserId: String, callback: (Boolean, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in")
            return
        }
        
        viewModelScope.launch {
            try {
                friendRepository.unfriend(currentUserId, friendUserId)
                loadFriends() // Reload friends list
                callback(true, "Friend removed successfully")
            } catch (e: Exception) {
                android.util.Log.e("FriendViewModel", "Error unfriending: ${e.message}", e)
                callback(false, "Failed to remove friend: ${e.message}")
            }
        }
    }
    
    /**
     * Block a user
     */
    fun blockUser(blockedUserId: String, blockedUserName: String, callback: (Boolean, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in")
            return
        }
        
        viewModelScope.launch {
            try {
                friendRepository.blockUser(currentUserId, blockedUserId, blockedUserName)
                loadFriends() // Reload friends list
                callback(true, "User blocked successfully")
            } catch (e: Exception) {
                android.util.Log.e("FriendViewModel", "Error blocking user: ${e.message}", e)
                callback(false, "Failed to block user: ${e.message}")
            }
        }
    }
    
    /**
     * Pin a friend
     */
    fun pinFriend(friendUserId: String, callback: (Boolean, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in")
            return
        }
        
        viewModelScope.launch {
            try {
                friendRepository.pinFriend(currentUserId, friendUserId)
                loadFriends() // Reload to update UI with new pin status
                callback(true, "Friend pinned")
            } catch (e: Exception) {
                android.util.Log.e("FriendViewModel", "Error pinning friend: ${e.message}", e)
                callback(false, "Failed to pin friend: ${e.message}")
            }
        }
    }
    
    /**
     * Unpin a friend
     */
    fun unpinFriend(friendUserId: String, callback: (Boolean, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in")
            return
        }
        
        viewModelScope.launch {
            try {
                friendRepository.unpinFriend(currentUserId, friendUserId)
                loadFriends()
                callback(true, "Friend unpinned")
            } catch (e: Exception) {
                android.util.Log.e("FriendViewModel", "Error unpinning friend: ${e.message}", e)
                callback(false, "Failed to unpin friend: ${e.message}")
            }
        }
    }
    
    /**
     * Mute notifications from a friend
     */
    fun muteFriendNotifications(friendUserId: String, callback: (Boolean, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in")
            return
        }
        
        viewModelScope.launch {
            try {
                friendRepository.muteFriendNotifications(currentUserId, friendUserId)
                loadFriends() // Reload to update UI with new mute status
                callback(true, "Notifications muted")
            } catch (e: Exception) {
                android.util.Log.e("FriendViewModel", "Error muting notifications: ${e.message}", e)
                callback(false, "Failed to mute notifications: ${e.message}")
            }
        }
    }
    
    /**
     * Unmute notifications from a friend
     */
    fun unmuteFriendNotifications(friendUserId: String, callback: (Boolean, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in")
            return
        }
        
        viewModelScope.launch {
            try {
                friendRepository.unmuteFriendNotifications(currentUserId, friendUserId)
                loadFriends() // Reload to update UI with new mute status
                callback(true, "Notifications unmuted")
            } catch (e: Exception) {
                android.util.Log.e("FriendViewModel", "Error unmuting notifications: ${e.message}", e)
                callback(false, "Failed to unmute notifications: ${e.message}")
            }
        }
    }
    
    /**
     * Report a user
     */
    fun reportUser(
        reportedUserId: String,
        reportedUserName: String,
        reason: String,
        description: String? = null,
        callback: (Boolean, String) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in")
            return
        }
        
        viewModelScope.launch {
            try {
                friendRepository.reportUser(
                    currentUserId,
                    reportedUserId,
                    reportedUserName,
                    reason,
                    description
                )
                callback(true, "User reported successfully. Thank you for keeping our community safe.")
            } catch (e: Exception) {
                android.util.Log.e("FriendViewModel", "Error reporting user: ${e.message}", e)
                callback(false, "Failed to report user: ${e.message}")
            }
        }
    }
    
    /**
     * Get mutual friends
     */
    fun getMutualFriends(friendUserId: String, callback: (Boolean, List<FriendSuggestion>, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: run {
            callback(false, emptyList(), "User not logged in")
            return
        }
        
        viewModelScope.launch {
            try {
                val mutualFriends = friendRepository.getMutualFriends(currentUserId, friendUserId)
                callback(true, mutualFriends, "")
            } catch (e: Exception) {
                android.util.Log.e("FriendViewModel", "Error getting mutual friends: ${e.message}", e)
                callback(false, emptyList(), "Failed to load mutual friends: ${e.message}")
            }
        }
    }
    
    /**
     * Unblock a user
     */
    fun unblockUser(blockedUserId: String, callback: (Boolean, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in")
            return
        }
        
        viewModelScope.launch {
            try {
                friendRepository.unblockUser(currentUserId, blockedUserId)
                callback(true, "User unblocked successfully")
            } catch (e: Exception) {
                android.util.Log.e("FriendViewModel", "Error unblocking user: ${e.message}", e)
                callback(false, "Failed to unblock user: ${e.message}")
            }
        }
    }
    
    /**
     * Check if a user is blocked
     */
    fun checkIfBlocked(targetUserId: String, callback: (Boolean) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: run {
            callback(false)
            return
        }
        
        viewModelScope.launch {
            try {
                val isBlocked = friendRepository.isBlocked(currentUserId, targetUserId)
                callback(isBlocked)
            } catch (e: Exception) {
                android.util.Log.e("FriendViewModel", "Error checking if blocked: ${e.message}", e)
                callback(false)
            }
        }
    }
    
    /**
     * Get blocked users list
     */
    fun loadBlockedUsers(callback: (Boolean, List<Pair<String, String>>, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: run {
            callback(false, emptyList(), "User not logged in")
            return
        }
        
        viewModelScope.launch {
            try {
                val blockedUsers = friendRepository.getBlockedUsers(currentUserId)
                callback(true, blockedUsers, "")
            } catch (e: Exception) {
                android.util.Log.e("FriendViewModel", "Error loading blocked users: ${e.message}", e)
                callback(false, emptyList(), "Failed to load blocked users: ${e.message}")
            }
        }
    }
}

