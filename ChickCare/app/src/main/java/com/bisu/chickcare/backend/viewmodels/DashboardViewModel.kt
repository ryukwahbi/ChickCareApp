package com.bisu.chickcare.backend.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.data.DashboardUiState
import com.bisu.chickcare.backend.repository.DetectionEntry
import com.bisu.chickcare.backend.repository.DetectionRepository
import com.bisu.chickcare.backend.repository.NotificationEntry
import com.bisu.chickcare.backend.repository.NotificationRepository
import com.bisu.chickcare.backend.service.DetectionService
import com.bisu.chickcare.backend.service.NotificationService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardViewModel : ViewModel() {
    private val detectionRepository = DetectionRepository()
    private val notificationRepository = NotificationRepository()
    private val detectionService = DetectionService(detectionRepository)
    private val notificationService = NotificationService(notificationRepository)
    private val auth = FirebaseAuth.getInstance()

    // --- StateFlow for the main UI state ---
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // --- StateFlows for data that updates independently ---
    private val _detectionHistory = MutableStateFlow<List<DetectionEntry>>(emptyList())
    val detectionHistory: StateFlow<List<DetectionEntry>> = _detectionHistory.asStateFlow()

    private val _newHistoryCount = MutableStateFlow(0)
    val newHistoryCount: StateFlow<Int> = _newHistoryCount.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationEntry>>(emptyList())
    val notifications: StateFlow<List<NotificationEntry>> = _notifications.asStateFlow()

    private val _newNotificationCount = MutableStateFlow(0)
    val newNotificationCount: StateFlow<Int> = _newNotificationCount.asStateFlow()

    init {
        auth.addAuthStateListener {
            // Re-initialize listeners and data when user logs in or out
            initializeListeners()
            loadInitialData()
        }
    }

    private fun initializeListeners() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            listenToDetectionHistory(userId)
            listenToNotifications(userId)
        }
    }

    fun loadInitialData() {
        val userId = auth.currentUser?.uid ?: run {
            _uiState.value = DashboardUiState(isLoading = false)
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val profileJob = async { auth.fetchUserProfile() }
            val statsJob = async { detectionService.fetchUserStats(userId) }

            val profileData = profileJob.await()
            val (chickens, alerts) = statsJob.await()

            _uiState.update { currentState ->
                currentState.copy(
                    userName = (profileData?.get("fullName") as? String) ?: "User",
                    totalChickens = chickens,
                    alerts = alerts,
                    isLoading = false
                )
            }
        }
    }

    fun onScanNowClicked(imageUri: String?, audioUri: String?) {
        val userId = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isDetecting = true) }

        viewModelScope.launch {
            val (isInfected, status) = detectionService.detectIB(userId, imageUri, audioUri)
            val suggestions = detectionService.getRemedySuggestions(isInfected)
            notificationService.sendDetectionNotification(userId, status)

            _uiState.update { currentState ->
                currentState.copy(
                    isDetecting = false,
                    detectionResult = Pair(isInfected, status),
                    remedySuggestions = suggestions
                )
            }
        }
    }

    // ### FIX: ADDED THE MISSING FUNCTION ###
    /**
     * Resets the detection result to null to prevent re-triggering navigation.
     * This should be called after navigating to the result screen.
     */
    fun clearDetectionResult() {
        _uiState.update { it.copy(detectionResult = null, remedySuggestions = emptyList()) }
    }

    private fun listenToDetectionHistory(userId: String) {
        viewModelScope.launch {
            detectionRepository.getDetectionHistory(userId).collect { history ->
                _detectionHistory.value = history
                _newHistoryCount.value = history.count { !it.isRead }
            }
        }
    }

    private fun listenToNotifications(userId: String) {
        viewModelScope.launch {
            notificationRepository.getNotifications(userId).collect { notifs ->
                _notifications.value = notifs
                _newNotificationCount.value = notifs.count { !it.isRead }
            }
        }
    }

    fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        return format.format(date)
    }
}

suspend fun FirebaseAuth.fetchUserProfile(): Map<String, Any>? {
    return this.currentUser?.let { user ->
        try {
            FirebaseFirestore.getInstance().collection("users").document(user.uid).get().await().data
        } catch (_: Exception) {
            null
        }
    }
}
