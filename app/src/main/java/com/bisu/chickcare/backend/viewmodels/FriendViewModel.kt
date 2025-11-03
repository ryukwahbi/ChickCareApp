package com.bisu.chickcare.backend.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.repository.FriendRepository
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
    
    fun sendFriendRequest(toUserId: String, toUserName: String, callback: (Boolean, String) -> Unit) {
        val fromUserId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in")
            return
        }
        val fromUserName = auth.currentUser?.displayName ?: "User"
        
        viewModelScope.launch {
            try {
                friendRepository.sendFriendRequest(fromUserId, fromUserName, toUserId)
                callback(true, "Friend request sent")
            } catch (e: Exception) {
                callback(false, "Failed to send friend request: ${e.message}")
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
                friendRepository.acceptFriendRequest(
                    requestId, 
                    currentUserId, 
                    friendUserId, 
                    friendName,
                    notificationRepository
                )
                // Reload friends list after accepting
                loadFriends()
                // Reload suggestions to update the list
                loadFriendSuggestions()
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
    
    fun getPendingFriendRequests(callback: (Boolean, List<com.bisu.chickcare.backend.repository.FriendRequest>, String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: run {
            callback(false, emptyList(), "User not logged in")
            return
        }
        
        viewModelScope.launch {
            try {
                val requests = friendRepository.getPendingFriendRequests(currentUserId)
                callback(true, requests, "")
            } catch (e: Exception) {
                android.util.Log.e("FriendViewModel", "Error loading pending requests: ${e.message}", e)
                callback(false, emptyList(), "Failed to load friend requests: ${e.message}")
            }
        }
    }
}

