package com.bisu.chickcare.backend.viewmodels

import android.app.Application
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.data.DashboardUiState
import com.bisu.chickcare.backend.data.TrendDataPoint
import com.bisu.chickcare.backend.repository.DetectionEntry
import com.bisu.chickcare.backend.repository.DetectionRepository
import com.bisu.chickcare.backend.repository.NotificationEntry
import com.bisu.chickcare.backend.repository.NotificationRepository
import com.bisu.chickcare.backend.service.DetectionService
import com.bisu.chickcare.backend.service.NotificationService
import com.bisu.chickcare.frontend.utils.DateFormatters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val detectionRepository = DetectionRepository()
    private val notificationRepository = NotificationRepository()
    private val detectionService = DetectionService(detectionRepository, application)
    private val notificationService = NotificationService(notificationRepository)
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private val _detectionHistory = MutableStateFlow<List<DetectionEntry>>(emptyList())
    val detectionHistory: StateFlow<List<DetectionEntry>> = _detectionHistory.asStateFlow()
    private val _newHistoryCount = MutableStateFlow(0)
    val newHistoryCount: StateFlow<Int> = _newHistoryCount.asStateFlow()
    private val _notifications = MutableStateFlow<List<NotificationEntry>>(emptyList())
    val notifications: StateFlow<List<NotificationEntry>> = _notifications.asStateFlow()
    private val _newNotificationCount = MutableStateFlow(0)
    val newNotificationCount: StateFlow<Int> = _newNotificationCount.asStateFlow()
    private val _recentlyDeleted = MutableStateFlow<List<DetectionEntry>>(emptyList())
    val recentlyDeleted: StateFlow<List<DetectionEntry>> = _recentlyDeleted.asStateFlow()
    private val _favoriteDetections = MutableStateFlow<List<DetectionEntry>>(emptyList())
    val favoriteDetections: StateFlow<List<DetectionEntry>> = _favoriteDetections.asStateFlow()
    private val _archivedDetections = MutableStateFlow<List<DetectionEntry>>(emptyList())
    val archivedDetections: StateFlow<List<DetectionEntry>> = _archivedDetections.asStateFlow()

    init {
        auth.addAuthStateListener {
            initializeListeners()
            loadInitialData()
            updateActiveStatus()
        }
        if (auth.currentUser != null) {
            initializeListeners()
            loadInitialData()
            updateActiveStatus()
        }
    }

    private fun initializeListeners() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            listenToDetectionHistory(userId)
            listenToNotifications(userId)
            listenToRecentlyDeleted(userId)
            listenToFavoriteDetections(userId)
            listenToArchivedDetections(userId)
        }
    }
    
    private fun listenToFavoriteDetections(userId: String) {
        viewModelScope.launch {
            detectionRepository.getFavoriteDetections(userId).collect { favorites ->
                _favoriteDetections.value = favorites
            }
        }
    }
    
    private fun listenToArchivedDetections(userId: String) {
        viewModelScope.launch {
            detectionRepository.getArchivedDetections(userId).collect { archived ->
                _archivedDetections.value = archived
            }
        }
    }

    fun loadInitialData() {
        val userId = auth.currentUser?.uid ?: run {
            _uiState.value = DashboardUiState(isLoading = false)
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch(Dispatchers.Default) {
            val profileJob = async { auth.fetchUserProfile() }
            val statsJob = async { detectionService.fetchUserStats(userId) }

            val profileData = profileJob.await()
            val (chickens, _) = statsJob.await()

            // Update state on main thread
            withContext(Dispatchers.Main) {
                _uiState.update { currentState ->
                    currentState.copy(
                        userName = (profileData?.get("fullName") as? String) ?: "User",
                        totalChickens = chickens,
                        isLoading = false
                    )
                }
            }
        }
    }
    
    /**
     * Refresh all dashboard data manually (for pull-to-refresh).
     * Reloads user profile and re-initializes listeners.
     */
    @Suppress("UNUSED")
    fun refreshData() {
        // No need to check userId here, initializeListeners() will handle it
        viewModelScope.launch {
            try {
                // Reload user profile
                val profileData = withContext(Dispatchers.Default) {
                    auth.fetchUserProfile()
                }
                val userName = (profileData?.get("fullName") as? String) ?: "User"
                _uiState.update { it.copy(userName = userName) }
                
                // Force refresh by re-initializing listeners
                initializeListeners()
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error refreshing data: ${e.message}")
            }
        }
    }

    fun onScanNowClicked(imageUri: String?, audioUri: String?) {
        val userId = auth.currentUser?.uid ?: run {
            android.util.Log.e("DashboardViewModel", "User not logged in, cannot perform detection")
            return
        }
        
        android.util.Log.d("DashboardViewModel", "onScanNowClicked called - Image: $imageUri, Audio: $audioUri")
        _uiState.update { it.copy(isDetecting = true) }

        // Use a separate coroutine scope that won't be cancelled when screen is destroyed
        // But still respect lifecycle - use supervisor scope for better error handling
        viewModelScope.launch(kotlinx.coroutines.SupervisorJob()) {
            var detectionResult: Pair<Boolean, String>? = null
            try {
                android.util.Log.d("DashboardViewModel", "Starting detection...")
                
                // Call detection service - it's wrapped in NonCancellable so it will always return
                detectionResult = try {
                    detectionService.detectIB(imageUri, audioUri)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    android.util.Log.e("DashboardViewModel", "Detection service was cancelled (unexpected): ${e.message}", e)
                    throw e // Re-throw to be caught by outer handler
                } catch (e: Exception) {
                    android.util.Log.e("DashboardViewModel", "Detection service failed: ${e.message}", e)
                    throw e // Re-throw to be caught by outer handler
                }
                
                android.util.Log.d("DashboardViewModel", "Detection completed - IsInfected: ${detectionResult.first}, Status: ${detectionResult.second}")
                
                // Update UI state immediately with result in NonCancellable to ensure it completes
                val (isInfected, status) = detectionResult
                withContext(kotlinx.coroutines.NonCancellable) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            isDetecting = false,
                            detectionResult = Pair(isInfected, status),
                            remedySuggestions = emptyList() // Will be updated after
                        )
                    }
                    android.util.Log.d("DashboardViewModel", "UI state updated with detection result: $status")
                }
                
                // Get suggestions in background (non-blocking)
                try {
                    val suggestions = detectionService.getRemedySuggestions(isInfected)
                    android.util.Log.d("DashboardViewModel", "Got ${suggestions.size} remedy suggestions")
                    _uiState.update { currentState ->
                        currentState.copy(remedySuggestions = suggestions)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("DashboardViewModel", "Failed to get suggestions: ${e.message}")
                    // Continue without suggestions
                }
                
                // Send notification in background (non-blocking, won't cancel if it fails)
                try {
                    notificationService.sendDetectionNotification(userId, status)
                    android.util.Log.d("DashboardViewModel", "Notification sent")
                } catch (e: Exception) {
                    android.util.Log.w("DashboardViewModel", "Failed to send notification: ${e.message}")
                    // Continue - notification is not critical
                }
                
            } catch (e: kotlinx.coroutines.CancellationException) {
                android.util.Log.e("DashboardViewModel", "Detection was cancelled: ${e.message}", e)
                // Even if cancelled, try to update UI if we have a result
                if (detectionResult != null) {
                    val (isInfected, status) = detectionResult
                    withContext(kotlinx.coroutines.NonCancellable) {
                        _uiState.update { currentState ->
                            currentState.copy(
                                isDetecting = false,
                                detectionResult = Pair(isInfected, status),
                                remedySuggestions = emptyList()
                            )
                        }
                        android.util.Log.d("DashboardViewModel", "UI state updated with detection result (after cancellation): $status")
                    }
                } else {
                    // No result yet, set error state
                    withContext(kotlinx.coroutines.NonCancellable) {
                        _uiState.update { currentState ->
                            if (currentState.detectionResult == null) {
                                currentState.copy(
                                    isDetecting = false,
                                    detectionResult = Pair(false, "Error: Detection was cancelled. Please try again."),
                                    remedySuggestions = emptyList()
                                )
                            } else {
                                currentState.copy(isDetecting = false)
                            }
                        }
                    }
                }
                // Don't re-throw - we've handled it
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Detection failed with exception: ${e.message}", e)
                withContext(kotlinx.coroutines.NonCancellable) {
                    _uiState.update { currentState ->
                        currentState.copy(
                            isDetecting = false,
                            detectionResult = Pair(false, "Error: ${e.message}"),
                            remedySuggestions = emptyList()
                        )
                    }
                }
            }
        }
    }

    fun clearDetectionResult() {
        _uiState.update { it.copy(detectionResult = null, remedySuggestions = emptyList()) }
    }

    private fun listenToDetectionHistory(userId: String) {
        viewModelScope.launch {
            detectionRepository.getDetectionHistory(userId).collect { history ->
                // Automatic cleanup: Delete old detections when exceeding 300 limit
                if (history.size > 300) {
                    val sortedHistory = history.sortedByDescending { it.timestamp }
                    val detectionsToDelete = sortedHistory.drop(300) // Keep only the 300 most recent
                    
                    // Delete old detections in background
                    viewModelScope.launch(Dispatchers.IO) {
                        detectionsToDelete.forEach { entry ->
                            try {
                                detectionRepository.permanentlyDeleteDetection(userId, entry.id)
                            } catch (e: Exception) {
                                android.util.Log.e("DashboardViewModel", "Error deleting old detection ${entry.id}: ${e.message}")
                            }
                        }
                        android.util.Log.d("DashboardViewModel", "Cleaned up ${detectionsToDelete.size} old detections (kept 300 most recent)")
                    }
                    
                    // Use only the 300 most recent for display
                    val limitedHistory = sortedHistory.take(300)
                    _detectionHistory.value = limitedHistory
                    _newHistoryCount.value = limitedHistory.count { !it.isRead }
                    
                    // Update trend data with limited history
                    val imageTrendData = calculateTrendData(limitedHistory.filter { !it.imageUri.isNullOrEmpty() })
                    val audioTrendData = calculateTrendData(limitedHistory.filter { !it.audioUri.isNullOrEmpty() })
                    
                    // Calculate healthy and unhealthy rates independently
                    val healthyRate = calculateStableHealthyRate(limitedHistory)
                    val unhealthyRate = calculateUnhealthyRate(limitedHistory)
                    
                    _uiState.update { currentState ->
                        currentState.copy(
                            totalDetections = limitedHistory.size,
                            healthyRate = healthyRate,
                            unhealthyRate = unhealthyRate,
                            imageDetections = limitedHistory.count { !it.imageUri.isNullOrEmpty() },
                            audioDetections = limitedHistory.count { !it.audioUri.isNullOrEmpty() },
                            imageTrendData = imageTrendData,
                            audioTrendData = audioTrendData
                        )
                    }
                } else {
                    // Normal flow when under 300 detections
                    _detectionHistory.value = history
                    _newHistoryCount.value = history.count { !it.isRead }
                    
                    // Update trend data when history changes
                    val imageTrendData = calculateTrendData(history.filter { !it.imageUri.isNullOrEmpty() })
                    val audioTrendData = calculateTrendData(history.filter { !it.audioUri.isNullOrEmpty() })
                    
                    // Calculate healthy and unhealthy rates independently
                    val healthyRate = calculateStableHealthyRate(history)
                    val unhealthyRate = calculateUnhealthyRate(history)
                    
                    _uiState.update { currentState ->
                        currentState.copy(
                            totalDetections = history.size,
                            healthyRate = healthyRate,
                            unhealthyRate = unhealthyRate,
                            imageDetections = history.count { !it.imageUri.isNullOrEmpty() },
                            audioDetections = history.count { !it.audioUri.isNullOrEmpty() },
                            imageTrendData = imageTrendData,
                            audioTrendData = audioTrendData
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Calculate trend data for line graphs.
     * Groups detections by 12-hour periods (AM/PM) per day.
     * Each day has 2 data points: AM (0-12 hours) and PM (12-24 hours).
     * Calculates average healthy and unhealthy percentages for each 12-hour period.
     * Always shows last 2 days, starting from 0% on the left.
     */
    private fun calculateTrendData(entries: List<DetectionEntry>): List<TrendDataPoint> {
        val now = System.currentTimeMillis()
        val twoDaysAgo = now - (2 * 24 * 60 * 60 * 1000L) // 2 days window

        // Keep only the detections within the last 2 days and order chronologically
        val recentEntries = entries
            .filter { it.timestamp >= twoDaysAgo }
            .sortedBy { it.timestamp }

        if (recentEntries.isEmpty()) {
            // Return empty list if no detections
            return emptyList()
        }

        val calendar = java.util.Calendar.getInstance()
        val labelFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

        // Group detections by day and 12-hour period (AM/PM)
        // Key: "YYYY-MM-DD-AM" or "YYYY-MM-DD-PM"
        val groupedData = mutableMapOf<String, MutableList<DetectionEntry>>()

        recentEntries.forEach { entry ->
            calendar.timeInMillis = entry.timestamp
            
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            
            // Determine AM (0-11) or PM (12-23)
            val period = if (hour < 12) "AM" else "PM"
            val key = "$year-$month-$day-$period"
            
            groupedData.getOrPut(key) { mutableListOf() }.add(entry)
        }

        val dataPoints = mutableListOf<TrendDataPoint>()

        // Generate all 12-hour periods for the last 2 days (4 periods total: 2 days × 2 periods/day)
        val allPeriods = mutableListOf<Pair<String, Long>>() // (key, timestamp)
        val tempCalendar = java.util.Calendar.getInstance()
        tempCalendar.timeInMillis = twoDaysAgo
        tempCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        tempCalendar.set(java.util.Calendar.MINUTE, 0)
        tempCalendar.set(java.util.Calendar.SECOND, 0)
        tempCalendar.set(java.util.Calendar.MILLISECOND, 0)
        
        // Generate all periods from 2 days ago to now
        while (tempCalendar.timeInMillis <= now) {
            val year = tempCalendar.get(java.util.Calendar.YEAR)
            val month = tempCalendar.get(java.util.Calendar.MONTH) + 1
            val day = tempCalendar.get(java.util.Calendar.DAY_OF_MONTH)
            
            // Add AM period (0-12 hours)
            val amKey = "$year-$month-$day-AM"
            val amTimestamp = tempCalendar.timeInMillis
            allPeriods.add(Pair(amKey, amTimestamp))
            
            // Add PM period (12-24 hours)
            val pmKey = "$year-$month-$day-PM"
            tempCalendar.add(java.util.Calendar.HOUR_OF_DAY, 12)
            val pmTimestamp = tempCalendar.timeInMillis
            allPeriods.add(Pair(pmKey, pmTimestamp))
            
            // Move to next day
            tempCalendar.add(java.util.Calendar.HOUR_OF_DAY, 12)
        }

        // Create data points for all periods, but only show periods with actual detections
        // This prevents showing 0% for periods with no data, which creates confusing drops
        var lastHealthyValue = 0.0
        var lastUnhealthyValue = 0.0
        
        allPeriods.forEach { (key, timestamp) ->
            val entriesInPeriod = groupedData[key] ?: emptyList()
            
            tempCalendar.timeInMillis = timestamp
            val hour = tempCalendar.get(java.util.Calendar.HOUR_OF_DAY)
            val period = if (hour < 12) "AM" else "PM"
            
            // Calculate average healthy and unhealthy percentages for this period
            val avgHealthy: Double
            val avgUnhealthy: Double
            
            if (entriesInPeriod.isNotEmpty()) {
                var totalHealthy = 0.0
                var totalUnhealthy = 0.0
                var healthyCount = 0
                var unhealthyCount = 0
                
                entriesInPeriod.forEach { entry ->
                    val confidencePercent = (entry.confidence * 100.0).coerceIn(0.0, 100.0)
                    if (entry.isHealthy) {
                        totalHealthy += confidencePercent
                        healthyCount++
                    } else {
                        totalUnhealthy += confidencePercent
                        unhealthyCount++
                    }
                }
                
                // Calculate averages
                avgHealthy = if (healthyCount > 0) totalHealthy / healthyCount else 0.0
                avgUnhealthy = if (unhealthyCount > 0) totalUnhealthy / unhealthyCount else 0.0
                
                // Update last known values
                if (avgHealthy > 0.0) lastHealthyValue = avgHealthy
                if (avgUnhealthy > 0.0) lastUnhealthyValue = avgUnhealthy
            } else {
                // For periods with no detections, use last known value (carry forward)
                // This prevents confusing drops to 0% when there's simply no data in that period
                avgHealthy = lastHealthyValue
                avgUnhealthy = lastUnhealthyValue
            }
            
            // Format label: "MMM dd\nAM" or "MMM dd\nPM"
            val dateLabel = labelFormatter.format(tempCalendar.time)
            
            dataPoints.add(
                TrendDataPoint(
                    label = "$dateLabel\n$period",
                    healthyAverage = avgHealthy,
                    unhealthyAverage = avgUnhealthy
                )
            )
        }

        return dataPoints
    }
    
    /**
     * Calculate healthy rate based on actual number of detections.
     * Uses the most recent 300 detections for calculation (if more than 300 exist).
     * Formula: (healthyCount / totalDetections) * 100
     * Example: 7 healthy out of 10 total = 70%
     */
    private fun calculateStableHealthyRate(history: List<DetectionEntry>): Double {
        if (history.isEmpty()) return 0.0
        
        val maxDataPoints = 300
        val sortedHistory = history.sortedByDescending { it.timestamp }
        val dataPointsToUse = sortedHistory.take(maxDataPoints)
        
        if (dataPointsToUse.isEmpty()) return 0.0

        val healthyCount = dataPointsToUse.count { it.isHealthy }
        val totalCount = dataPointsToUse.size

        // FIXED: Use actual total count, not maxDataPoints
        val healthyRate = (healthyCount.toDouble() / totalCount.toDouble()) * 100.0
        
        val roundedRate = (healthyRate * 10.0).roundToInt() / 10.0
        
        android.util.Log.d("DashboardViewModel", "Healthy rate calculated: $roundedRate% ($healthyCount healthy out of $totalCount total detections)")
        
        return roundedRate.coerceIn(0.0, 100.0)
    }
    
    /**
     * Calculate unhealthy rate based on actual number of detections.
     * Uses the most recent 300 detections for calculation (if more than 300 exist).
     * Formula: (unhealthyCount / totalDetections) * 100
     * Example: 3 infected out of 10 total = 30%
     */
    private fun calculateUnhealthyRate(history: List<DetectionEntry>): Double {
        if (history.isEmpty()) return 0.0
        
        // Maximum 300 detections for calculation
        val maxDataPoints = 300
        val sortedHistory = history.sortedByDescending { it.timestamp }
        val dataPointsToUse = sortedHistory.take(maxDataPoints)
        
        if (dataPointsToUse.isEmpty()) return 0.0

        // Count ONLY unhealthy detections (independent calculation)
        val unhealthyCount = dataPointsToUse.count { !it.isHealthy }
        val totalCount = dataPointsToUse.size
        
        // FIXED: Use actual total count, not maxDataPoints
        // Formula: rate = (unhealthyCount / totalCount) * 100
        val unhealthyRate = (unhealthyCount.toDouble() / totalCount.toDouble()) * 100.0
        
        // Round to 1 decimal place for smooth progression (0.1% increments)
        val roundedRate = (unhealthyRate * 10.0).roundToInt() / 10.0
        
        android.util.Log.d("DashboardViewModel", "Unhealthy rate calculated: $roundedRate% ($unhealthyCount unhealthy out of $totalCount total detections)")
        
        return roundedRate.coerceIn(0.0, 100.0)
    }

    private fun listenToNotifications(userId: String) {
        viewModelScope.launch {
            notificationRepository.getNotifications(userId).collect { notifs ->
                _notifications.value = notifs
                _newNotificationCount.value = notifs.count { !it.isRead }
            }
        }
    }
    
    private fun listenToRecentlyDeleted(userId: String) {
        viewModelScope.launch {
            try {
                detectionRepository.cleanupOldDeletedItems(userId)
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error cleaning up old deleted items", e)
            }
            
            detectionRepository.getRecentlyDeleted(userId).collect { deleted ->
                _recentlyDeleted.value = deleted
            }
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            notificationRepository.markAsRead(userId, notificationId)
        }
    }
    
    fun deleteNotification(notificationId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                notificationRepository.deleteNotification(userId, notificationId)
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Failed to delete notification: ${e.message}", e)
            }
        }
    }
    
    fun markAllNotificationsAsRead() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            notificationRepository.markAllAsRead(userId)
        }
    }
    
    fun updateActiveStatus() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .set(hashMapOf("lastActive" to System.currentTimeMillis()), com.google.firebase.firestore.SetOptions.merge())
                    .await()
            } catch (_: Exception) {
            }
        }
    }

    fun formatDate(timestamp: Long): String {
        return DateFormatters.formatDateTime(timestamp)
    }
    
    fun deleteDetection(detectionId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                detectionRepository.deleteDetection(userId, detectionId)
                // History will update automatically via Firestore listener
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error deleting detection", e)
            }
        }
    }
    
    fun markAllDetectionsAsRead() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Get unread detection IDs from current history
                val unreadDetectionIds = _detectionHistory.value
                    .filter { !it.isRead && it.id.isNotEmpty() }
                    .map { it.id }
                
                android.util.Log.d("DashboardViewModel", "Found ${unreadDetectionIds.size} unread detections to mark")
                _newHistoryCount.value = 0
                
                val updatedHistory = _detectionHistory.value.map { entry ->
                    if (!entry.isRead) {
                        entry.copy(isRead = true)
                    } else {
                        entry
                    }
                }
                _detectionHistory.value = updatedHistory
                
                if (unreadDetectionIds.isNotEmpty()) {
                    unreadDetectionIds.forEach { detectionId ->
                        try {
                            firestore.collection("users")
                                .document(userId)
                                .collection("detections")
                                .document(detectionId)
                                .update("isRead", true)
                                .await()
                        } catch (e: Exception) {
                            android.util.Log.w("DashboardViewModel", "Failed to mark detection $detectionId as read: ${e.message}")
                        }
                    }
                    android.util.Log.d("DashboardViewModel", "Updated ${unreadDetectionIds.size} detections in Firestore")
                } else {
                    detectionRepository.markAllDetectionsAsRead(userId)
                }
                
                android.util.Log.d("DashboardViewModel", "Marked all detections as read")
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error marking detections as read", e)
                _newHistoryCount.value = _detectionHistory.value.count { !it.isRead }
            }
        }
    }
    
    fun restoreDetection(detectionId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                detectionRepository.restoreDetection(userId, detectionId)
                // History will update automatically via Firestore listener
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error restoring detection", e)
            }
        }
    }
    
    fun permanentlyDeleteDetection(detectionId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                detectionRepository.permanentlyDeleteDetection(userId, detectionId)
                // Send notification about deletion
                try {
                    notificationService.sendDataDeletedNotification(userId, "Detection")
                } catch (e: Exception) {
                    android.util.Log.w("DashboardViewModel", "Failed to send deletion notification: ${e.message}")
                }
                // History will update automatically via Firestore listener
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error permanently deleting detection", e)
            }
        }
    }
    
    fun restoreAllDetections() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                detectionRepository.restoreAllDetections(userId)
                // History will update automatically via Firestore listener
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error restoring all detections", e)
            }
        }
    }
    
    fun permanentlyDeleteAllTrash() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                detectionRepository.permanentlyDeleteAllTrash(userId)
                // Send notification about bulk deletion
                try {
                    notificationService.sendDataDeletedNotification(userId, "All deleted detections")
                } catch (e: Exception) {
                    android.util.Log.w("DashboardViewModel", "Failed to send bulk deletion notification: ${e.message}")
                }
                // History will update automatically via Firestore listener
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error permanently deleting all trash", e)
            }
        }
    }
    
    fun toggleFavorite(detectionId: String, isFavorite: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                detectionRepository.toggleFavorite(userId, detectionId, isFavorite)
                // History will update automatically via Firestore listener
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error toggling favorite", e)
            }
        }
    }
    
    fun toggleArchive(detectionId: String, isArchived: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                detectionRepository.toggleArchive(userId, detectionId, isArchived)
                // History will update automatically via Firestore listener
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error toggling archive", e)
            }
        }
    }
    
    /**
     * Save detection result to Firebase
     * Called when user clicks "Save Result" button
     */
    fun saveDetectionResult(
        resultString: String,
        isHealthy: Boolean,
        confidence: Float,
        imageUri: String?,
        audioUri: String?,
        context: android.content.Context? = null,
        location: String? = null,
        recommendations: List<String> = emptyList()
    ) {
        val userId = auth.currentUser?.uid ?: run {
            android.util.Log.e("DashboardViewModel", "User not logged in, cannot save detection")
            return
        }
        
        viewModelScope.launch {
            try {
                // Take persistable URI permissions for picker URIs before saving
                val finalImageUri = imageUri?.let { uriString ->
                    takePersistableUriPermissionIfNeeded(context, uriString)
                }
                val finalAudioUri = audioUri?.let { uriString ->
                    takePersistableUriPermissionIfNeeded(context, uriString)
                }
                
                // Get recommendations if not provided
                val finalRecommendations = recommendations.ifEmpty {
                    try {
                        detectionService.getRemedySuggestions(!isHealthy)
                    } catch (e: Exception) {
                        android.util.Log.w("DashboardViewModel", "Failed to get recommendations: ${e.message}")
                        emptyList()
                    }
                }
                
                // Get location if not provided and context is available
                val finalLocation = location ?: run {
                    if (context != null) {
                        try {
                            getCurrentLocation(context)
                        } catch (e: Exception) {
                            android.util.Log.w("DashboardViewModel", "Failed to get location: ${e.message}")
                            null
                        }
                    } else {
                        null
                    }
                }
                
                detectionRepository.saveDetection(
                    userId = userId,
                    result = resultString,
                    isHealthy = isHealthy,
                    confidence = confidence,
                    imageUri = finalImageUri,
                    audioUri = finalAudioUri,
                    location = finalLocation,
                    recommendations = finalRecommendations
                )
                android.util.Log.d("DashboardViewModel", "Detection result saved successfully to Firestore")
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error saving detection result: ${e.message}", e)
                throw e // Re-throw so caller can handle it
            }
        }
    }
    
    /**
     * Get current location as a string address
     */
    private suspend fun getCurrentLocation(context: android.content.Context): String? {
        return try {
            val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            
            // Check location permissions
            val hasLocationPermission = android.content.pm.PackageManager.PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) ||
                android.content.pm.PackageManager.PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            
            if (!hasLocationPermission) {
                android.util.Log.w("DashboardViewModel", "Location permission not granted")
                return null
            }
            
            // Try to get fresh current location first (more accurate)
            var location: Location? = null
            try {
                val currentLocationTask = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                )
                location = currentLocationTask.await()
                android.util.Log.d("DashboardViewModel", "Got fresh location: ${location?.latitude}, ${location?.longitude}")
            } catch (e: SecurityException) {
                android.util.Log.w("DashboardViewModel", "SecurityException getting current location: ${e.message}")
            } catch (e: Exception) {
                android.util.Log.w("DashboardViewModel", "Error getting current location: ${e.message}")
            }
            
            // Fallback to last known location if current location is null
            if (location == null) {
                try {
                    val lastLocationTask = fusedLocationClient.lastLocation
                    location = lastLocationTask.await()
                    android.util.Log.d("DashboardViewModel", "Got last known location: ${location?.latitude}, ${location?.longitude}")
                } catch (e: SecurityException) {
                    android.util.Log.w("DashboardViewModel", "SecurityException getting last location: ${e.message}")
                    return null
                } catch (e: Exception) {
                    android.util.Log.w("DashboardViewModel", "Error getting last location: ${e.message}")
                    return null
                }
            }
            
            if (location != null) {
                // Use Geocoder to get address (simplified - just return coordinates if geocoding fails)
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        val address = addresses[0]
                        // Format: Address Line, City, Country
                        val addressParts = listOfNotNull(
                            address.getAddressLine(0),
                            address.locality,
                            address.countryName
                        )
                        val formattedAddress = addressParts.joinToString(", ")
                        android.util.Log.d("DashboardViewModel", "Location formatted as: $formattedAddress")
                        formattedAddress
                    } else {
                        val coordinates = "${location.latitude}, ${location.longitude}"
                        android.util.Log.d("DashboardViewModel", "Location as coordinates: $coordinates")
                        coordinates
                    }
                } catch (e: Exception) {
                    // Fallback to coordinates if geocoding fails
                    val coordinates = "${location.latitude}, ${location.longitude}"
                    android.util.Log.w("DashboardViewModel", "Geocoding failed, using coordinates: $coordinates - ${e.message}")
                    coordinates
                }
            } else {
                android.util.Log.w("DashboardViewModel", "No location available (both current and last location are null)")
                null
            }
        } catch (e: Exception) {
            android.util.Log.w("DashboardViewModel", "Error getting location: ${e.message}", e)
            null
        }
    }
    
    /**
     * Take persistable URI permission for picker URIs so they can be accessed later
     */
    private fun takePersistableUriPermissionIfNeeded(
        context: android.content.Context?,
        uriString: String
    ): String? {
        if (context == null) return uriString
        
        return try {
            // Decode URL-encoded URI if needed (navigation encodes URIs)
            val decodedUriString = try {
                java.net.URLDecoder.decode(uriString, "UTF-8")
            } catch (_: Exception) {
                uriString // Use original if decoding fails
            }
            val uri = decodedUriString.toUri()
            // Take persistable permission for all content URIs (not just picker URIs)
            // This ensures we can access the URI later, even after the app restarts
            if (uri.scheme == "content") {
                try {
                    // Take persistable permission (FLAG_GRANT_READ_URI_PERMISSION is persistent)
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    android.util.Log.d("DashboardViewModel", "Taken persistable URI permission for: $decodedUriString")
                } catch (e: SecurityException) {
                    android.util.Log.w("DashboardViewModel", "Cannot take persistable permission for URI (may already be taken or not support persistable): $decodedUriString - ${e.message}")
                    // Continue anyway - permission might already be taken or URI might not support persistable
                } catch (e: Exception) {
                    android.util.Log.w("DashboardViewModel", "Error taking persistable permission: ${e.message}")
                }
            }
            uriString // Return original (possibly encoded) string
        } catch (e: Exception) {
            android.util.Log.w("DashboardViewModel", "Error parsing URI for permission: $uriString", e)
            uriString
        }
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
