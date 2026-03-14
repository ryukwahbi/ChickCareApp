package com.bisu.chickcare.backend.viewmodels

import android.app.Application
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.data.DashboardUiState
import com.bisu.chickcare.backend.data.TrendDataPoint
import com.bisu.chickcare.backend.repository.DetectionEntry
import com.bisu.chickcare.backend.repository.DetectionRepository
import com.bisu.chickcare.backend.repository.NotificationEntry
import com.bisu.chickcare.backend.repository.NotificationRepository
import com.bisu.chickcare.backend.repository.NotificationType
import com.bisu.chickcare.backend.repository.AlertRepository
import com.bisu.chickcare.backend.data.UserProfile
import com.bisu.chickcare.backend.service.DetectionService
import com.bisu.chickcare.backend.service.NetworkConnectivityHelper
import com.bisu.chickcare.backend.service.NotificationService
import com.bisu.chickcare.frontend.utils.DateFormatters
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val detectionRepository = DetectionRepository()
    private val notificationRepository = NotificationRepository()
    private val detectionService = DetectionService(detectionRepository, application)
    private val notificationService = NotificationService(notificationRepository)
    private val alertRepository = AlertRepository()
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    // Global exception handler to prevent crashes from Firestore errors
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("DashboardViewModel", "Coroutine exception caught: ${throwable.message}", throwable)
        // Don't crash - just log the error
    }
    
    // Store listener jobs for cleanup
    private val listenerJobs = mutableMapOf<String, Job>()
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private val _detectionHistory = MutableStateFlow<List<DetectionEntry>>(emptyList())
    val detectionHistory: StateFlow<List<DetectionEntry>> = _detectionHistory.asStateFlow()
    private val _newHistoryCount = MutableStateFlow(0)
    val newHistoryCount: StateFlow<Int> = _newHistoryCount.asStateFlow()
    private val _notifications = MutableStateFlow<List<NotificationEntry>>(emptyList())
    private val _alerts = MutableStateFlow<List<NotificationEntry>>(emptyList())
    
    // Combine standard notifications and disease alerts
    val notifications: StateFlow<List<NotificationEntry>> = combine(_notifications, _alerts) { notifs, alerts ->
        (notifs + alerts).sortedByDescending { it.timestamp }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )
    private val _newNotificationCount = MutableStateFlow(0)
    val newNotificationCount: StateFlow<Int> = _newNotificationCount.asStateFlow()
    private val _recentlyDeleted = MutableStateFlow<List<DetectionEntry>>(emptyList())
    
    // Track notification IDs that have been locally marked as read (to prevent Firestore overwriting)
    private val locallyMarkedAsReadIds = mutableSetOf<String>()
    private val locallyDeletedNotificationIds = mutableSetOf<String>()
    val recentlyDeleted: StateFlow<List<DetectionEntry>> = _recentlyDeleted.asStateFlow()
    private val _favoriteDetections = MutableStateFlow<List<DetectionEntry>>(emptyList())
    val favoriteDetections: StateFlow<List<DetectionEntry>> = _favoriteDetections.asStateFlow()
    private val _archivedDetections = MutableStateFlow<List<DetectionEntry>>(emptyList())
    val archivedDetections: StateFlow<List<DetectionEntry>> = _archivedDetections.asStateFlow()
    
    // Sync status for offline operations
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    // Track timestamps of queued detections to detect when they sync
    private val queuedDetectionTimestamps = mutableListOf<Long>()
    
    data class SyncStatus(
        val isQueued: Boolean = false,
        val queuedCount: Int = 0,
        val lastSyncTime: Long? = null,
        val isSyncing: Boolean = false
    ) {
        companion object {
            val Idle = SyncStatus()
        }
    }


    // Track AuthStateListener for proper cleanup
    private val authStateListener = FirebaseAuth.AuthStateListener {
        initializeListeners()
        loadInitialData()
        updateActiveStatus()
    }


    init {
        auth.addAuthStateListener(authStateListener)
        
        // Monitor network connectivity to update sync status
        monitorNetworkConnectivity()
    }

    
    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
        listenerJobs.values.forEach { it.cancel() }
    }

    
    /**
     * Monitor network connectivity and update sync status accordingly
     */
    private fun monitorNetworkConnectivity() {
        viewModelScope.launch(exceptionHandler) {
            val context = getApplication<Application>()
            NetworkConnectivityHelper.connectivityFlow(context).collect { isOnline ->
                val currentStatus = _syncStatus.value
                
                if (isOnline && currentStatus.isQueued && currentStatus.queuedCount > 0) {
                    // Device came back online with queued items - start syncing
                    android.util.Log.d("DashboardViewModel", "Device is online, syncing ${currentStatus.queuedCount} queued items...")
                    _syncStatus.update { it.copy(isSyncing = true) }
                    
                    // Firestore will automatically sync, but we'll clear the syncing flag after a delay
                    // The actual sync completion will be detected when items appear in history
                    kotlinx.coroutines.delay(2000) // Give Firestore time to start syncing
                    
                    // If still syncing after delay, keep the flag (sync might take longer)
                    // The flag will be cleared when we detect items in history
                } else if (!isOnline && currentStatus.isSyncing) {
                    // Device went offline while syncing
                    android.util.Log.d("DashboardViewModel", "Device went offline during sync")
                    _syncStatus.update { it.copy(isSyncing = false) }
                }
            }
        }
    }

    /**
     * Get current user ID for Firestore operations.
     * IMPORTANT: Must use Firebase Auth UID because Firestore security rules
     * validate against request.auth.uid. Using a different ID (e.g., from
     * AccountManager) will cause PERMISSION_DENIED errors.
     */
    private fun getCurrentUserId(): String? {
        // Always prioritize Firebase Auth UID for Firestore operations
        // Security rules check request.auth.uid, so we MUST use the authenticated user's ID
        val firebaseUserId = auth.currentUser?.uid
        if (!firebaseUserId.isNullOrEmpty()) {
            return firebaseUserId
        }
        
        // Fallback to offline auth only when Firebase is not available
        val context = getApplication<Application>()
        return com.bisu.chickcare.backend.utils.OfflineAuthHelper.getCurrentLocalUserId(context)
    }
    
    private fun initializeListeners() {
        // Get userId from either Firebase or local auth
        val userId = getCurrentUserId()
        val context = getApplication<Application>()
        val isOnline = NetworkConnectivityHelper.isOnline(context)
        val isFirebaseUserReady = auth.currentUser != null

        // Prevent PERMISSION_DENIED errors when using a local Offline ID while Online.
        // Instead of skipping, we force Firestore to OFFLINE mode so it reads from cache.
        if (isOnline && !isFirebaseUserReady) {
            android.util.Log.w("DashboardViewModel", "Device is online but Firebase Auth is not ready. Enabling offline mode to view cached data.")
            firestore.disableNetwork()
            _uiState.update { it.copy(isLoading = false) }
        } else {
            // Ensure network is enabled if we are properly authenticated or truly offline (standard behavior)
            firestore.enableNetwork()
        }

        if (userId != null) {
            listenToDetectionHistory(userId)
            listenToNotifications(userId)
            listenToRecentlyDeleted(userId)
            listenToFavoriteDetections(userId)
            listenToArchivedDetections(userId)
        }
    }
    
    private fun listenToFavoriteDetections(userId: String) {
        listenerJobs["favorites"]?.cancel()
        listenerJobs["favorites"] = viewModelScope.launch(exceptionHandler) {
            try {
                detectionRepository.getFavoriteDetections(userId).collect { favorites ->
                    _favoriteDetections.value = favorites
                }
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    android.util.Log.e("DashboardViewModel", "PERMISSION DENIED (Favorites): Please deploy Firestore Security Rules.")
                } else {
                    android.util.Log.e("DashboardViewModel", "Error listening to favorites: ${e.message}")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Ignore cancellations
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error listening to favorites: ${e.message}")
                // Don't crash - just log and continue with empty list
            }
        }
    }
    
    private fun listenToArchivedDetections(userId: String) {
        listenerJobs["archived"]?.cancel()
        listenerJobs["archived"] = viewModelScope.launch(exceptionHandler) {
            try {
                detectionRepository.getArchivedDetections(userId).collect { archived ->
                    _archivedDetections.value = archived
                }
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    android.util.Log.e("DashboardViewModel", "PERMISSION DENIED (Archived): Please deploy Firestore Security Rules.")
                } else {
                    android.util.Log.e("DashboardViewModel", "Error listening to archived: ${e.message}")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Ignore cancellations
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error listening to archived: ${e.message}")
                // Don't crash - just log and continue with empty list
            }
        }
    }

    fun loadInitialData() {
        // Get userId from either Firebase or local auth
        val userId = getCurrentUserId() ?: run {
            _uiState.value = DashboardUiState(isLoading = false)
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch(exceptionHandler + Dispatchers.Default) {
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
        val userId = getCurrentUserId() ?: run {
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
                    notificationService.sendDetectionNotification(userId, status, imageUri, audioUri)
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
        listenerJobs["history"]?.cancel()
        listenerJobs["history"] = viewModelScope.launch(exceptionHandler) {
            try {
                detectionRepository.getDetectionHistory(userId).collect { history ->
                    // Check if any queued detections have been synced by matching timestamps
                    // Firestore preserves timestamps when syncing, so we can match them
                    if (queuedDetectionTimestamps.isNotEmpty() && history.isNotEmpty()) {
                        val historyTimestamps = history.map { it.timestamp }.toSet()
                        val syncedTimestamps = queuedDetectionTimestamps.filter { queuedTimestamp ->
                            // Match if timestamp is within 5 seconds (allowing for slight differences)
                            historyTimestamps.any { historyTimestamp ->
                                kotlin.math.abs(historyTimestamp - queuedTimestamp) < 5000
                            }
                        }
                        
                        if (syncedTimestamps.isNotEmpty()) {
                            // Some queued items have been synced
                            queuedDetectionTimestamps.removeAll(syncedTimestamps)
                            
                            // Calculate new queued count before updating status
                            val currentQueuedCount = _syncStatus.value.queuedCount
                            val newQueuedCount = (currentQueuedCount - syncedTimestamps.size).coerceAtLeast(0)
                            
                            _syncStatus.update { currentStatus ->
                                currentStatus.copy(
                                    queuedCount = newQueuedCount,
                                    isQueued = newQueuedCount > 0,
                                    isSyncing = newQueuedCount > 0,
                                    lastSyncTime = System.currentTimeMillis()
                                )
                            }
                            android.util.Log.d("DashboardViewModel", "Detected ${syncedTimestamps.size} queued detections have been synced. Remaining: $newQueuedCount")
                            
                            // If all items synced, clear syncing flag after a short delay
                            if (newQueuedCount == 0) {
                                kotlinx.coroutines.delay(1000)
                                _syncStatus.update { it.copy(isSyncing = false) }
                            }
                        }
                    }
                    
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
                                audioTrendData = audioTrendData,
                                combinedTrendData = calculateTrendData(limitedHistory)
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
                                audioTrendData = audioTrendData,
                                combinedTrendData = calculateTrendData(history)
                            )
                        }
                    }
                }
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    android.util.Log.e("DashboardViewModel", "PERMISSION DENIED (History): Please deploy Firestore Security Rules.")
                } else {
                    android.util.Log.e("DashboardViewModel", "Error listening to detection history: ${e.message}")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Ignore cancellations
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error listening to detection history: ${e.message}")
                // Don't crash - just log and continue with empty list
            }
        }
    }
    
    /**
     * Calculate trend data for line graphs.
     * Modified to show individual detections interacting with a "drop to zero" logic.
     * If there is a gap of > 12 hours between detections, the line drops to 0%
     * shortly after the previous detection, effectively resetting the visual trend.
     */
    private fun calculateTrendData(entries: List<DetectionEntry>): List<TrendDataPoint> {
        if (entries.isEmpty()) {
            return emptyList()
        }

        val now = System.currentTimeMillis()
        val twoDaysAgo = now - (48 * 60 * 60 * 1000L)

        // Keep only the detections within the last 48 hours and sort by timestamp ascending
        val recentEntries = entries
            .filter { it.timestamp >= twoDaysAgo }
            .sortedBy { it.timestamp }

        if (recentEntries.isEmpty()) return emptyList()

        // Added space after day: "Jan 22 6:53 PM" (newline replaced with space for better single-line fit or consistent multi-line)
        val labelFormatter = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())
        val resultPoints = mutableListOf<TrendDataPoint>()
        
        // Threshold for dropping to zero (12 hours in milliseconds)
        val gapThreshold = 12 * 60 * 60 * 1000L

        recentEntries.forEachIndexed { index, entry ->
            val confidencePercent = (entry.confidence * 100.0).coerceIn(0.0, 100.0)
            
            // Map current entry to data point
            val healthyVal = if (entry.isHealthy) confidencePercent else 0.0
            val unhealthyVal = if (!entry.isHealthy) confidencePercent else 0.0
            
            val currentPoint = TrendDataPoint(
                label = labelFormatter.format(java.util.Date(entry.timestamp)),
                healthyAverage = healthyVal,
                unhealthyAverage = unhealthyVal
            )

            // Check gap from previous point
            if (index > 0) {
                val prevTimestamp = recentEntries[index - 1].timestamp
                val timeDiff = entry.timestamp - prevTimestamp

                if (timeDiff > gapThreshold) {
                    // Gap > 12 hours detected. Insert a "zero" point to force the drop.
                    // We place this "drop point" slightly after the previous point to make it look like
                    // it dropped after that session ended.
                    
                    // Note: We don't add a label to this intermediate point to avoid clutter
                    resultPoints.add(TrendDataPoint(
                        label = "", 
                        healthyAverage = 0.0, 
                        unhealthyAverage = 0.0
                    ))
                }
            }

            resultPoints.add(currentPoint)
        }

        return resultPoints
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
        listenerJobs["notifications"]?.cancel()
        listenerJobs["notifications"] = viewModelScope.launch(exceptionHandler) {
            try {
                notificationRepository.getNotifications(userId).collect { notifs ->
                    // Filter out locally deleted notifications and preserve locally-marked read states
                    val mergedNotifs = notifs
                        .filter { it.id !in locallyDeletedNotificationIds }
                        .map { notification ->
                            if (notification.id in locallyMarkedAsReadIds) {
                                notification.copy(isRead = true)
                            } else {
                                notification
                            }
                        }
                    _notifications.value = mergedNotifs
                    _newNotificationCount.value = mergedNotifs.count { !it.isRead }
                }
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    android.util.Log.e("DashboardViewModel", "PERMISSION DENIED: Please deploy Firestore Security Rules. See firestore_rules.txt.")
                } else {
                    android.util.Log.e("DashboardViewModel", "Error listening to notifications: ${e.message}")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Ignore cancellations
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error listening to notifications: ${e.message}")
            }
        }
        listenToAlerts(userId)
    }

    private fun listenToAlerts(userId: String) {
        listenerJobs["alerts"]?.cancel()
        listenerJobs["alerts"] = viewModelScope.launch(exceptionHandler) {
            try {
                // Fetch user profile to get location
                val userDoc = firestore.collection("users").document(userId).get().await()
                val userProfile = userDoc.toObject(UserProfile::class.java)
                val location = userProfile?.farmLocation ?: ""
                
                if (location.isNotEmpty()) {
                    alertRepository.getAlerts(location).collect { alerts ->
                        val alertEntries = alerts.map { alert ->
                            NotificationEntry(
                                id = alert.id,
                                type = NotificationType.DISEASE_ALERT.name,
                                title = "Disease Alert: ${alert.disease}",
                                message = "${alert.count} cases of ${alert.disease} detected in $location recently.",
                                timestamp = alert.timestamp,
                                isRead = false // Alerts are always fresh for now
                            )
                        }
                        _alerts.value = alertEntries
                    }
                }
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    android.util.Log.e("DashboardViewModel", "PERMISSION DENIED (Alerts): Please deploy Firestore Security Rules.")
                } else {
                    android.util.Log.e("DashboardViewModel", "Error listening to alerts: ${e.message}")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Ignore cancellations
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error listening to alerts: ${e.message}")
            }
        }
    }
    
    private fun listenToRecentlyDeleted(userId: String) {
        listenerJobs["recentlyDeleted"]?.cancel()
        listenerJobs["recentlyDeleted"] = viewModelScope.launch(exceptionHandler) {
            try {
                detectionRepository.cleanupOldDeletedItems(userId)
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error cleaning up old deleted items", e)
            }
            
            try {
                detectionRepository.getRecentlyDeleted(userId).collect { deleted ->
                    _recentlyDeleted.value = deleted
                }
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    android.util.Log.e("DashboardViewModel", "PERMISSION DENIED (Deleted): Please deploy Firestore Security Rules.")
                } else {
                     android.util.Log.e("DashboardViewModel", "Error listening to recently deleted: ${e.message}")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Ignore cancellations
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error listening to recently deleted: ${e.message}")
                // Don't crash - just log and continue with empty list
            }
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        val userId = getCurrentUserId() ?: return
        
        // Track this ID as locally marked to prevent Firestore from resetting it
        locallyMarkedAsReadIds.add(notificationId)
        
        // Optimistically update local state immediately (for UI responsiveness)
        _notifications.update { currentList ->
            currentList.map { notification ->
                if (notification.id == notificationId) {
                    notification.copy(isRead = true)
                } else {
                    notification
                }
            }
        }
        _newNotificationCount.value = _notifications.value.count { !it.isRead }
        
        // Then update Firestore (may fail if permissions denied)
        viewModelScope.launch {
            notificationRepository.markAsRead(userId, notificationId)
        }
    }
    
    fun deleteNotification(notificationId: String) {
        val userId = getCurrentUserId() ?: return
        
        // Track this ID as locally deleted to prevent Firestore from adding it back
        locallyDeletedNotificationIds.add(notificationId)
        
        // Optimistically remove from local state immediately
        _notifications.update { currentList ->
            currentList.filter { it.id != notificationId }
        }
        _newNotificationCount.value = _notifications.value.count { !it.isRead }
        
        viewModelScope.launch {
            try {
                notificationRepository.deleteNotification(userId, notificationId)
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Failed to delete notification: ${e.message}", e)
            }
        }
    }
    
    fun markAllNotificationsAsRead() {
        val userId = getCurrentUserId() ?: return
        
        // Track all current notification IDs as locally marked
        _notifications.value.forEach { locallyMarkedAsReadIds.add(it.id) }
        
        // Optimistically update local state immediately (for UI responsiveness)
        _notifications.update { currentList ->
            currentList.map { it.copy(isRead = true) }
        }
        _newNotificationCount.value = 0
        
        // Then update Firestore (may fail if permissions denied)
        viewModelScope.launch {
            notificationRepository.markAllAsRead(userId)
        }
    }
    
    fun updateActiveStatus() {
        val userId = getCurrentUserId() ?: return
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
        val userId = getCurrentUserId() ?: return
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
        val userId = getCurrentUserId() ?: return
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
        val userId = getCurrentUserId() ?: return
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
        val userId = getCurrentUserId() ?: return
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
        val userId = getCurrentUserId() ?: return
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
        val userId = getCurrentUserId() ?: return
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
        val userId = getCurrentUserId() ?: return
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
        val userId = getCurrentUserId() ?: return
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
        val userId = getCurrentUserId() ?: run {
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
                
                val saveResult = detectionRepository.saveDetection(
                    userId = userId,
                    result = resultString,
                    isHealthy = isHealthy,
                    confidence = confidence,
                    imageUri = finalImageUri,
                    audioUri = finalAudioUri,
                    location = finalLocation,
                    recommendations = finalRecommendations
                )
                
                val currentTimestamp = System.currentTimeMillis()
                
                when (saveResult) {
                    is DetectionRepository.SaveResult.Success -> {
                        android.util.Log.d("DashboardViewModel", "Detection result saved successfully to Firestore with ID: ${saveResult.documentId}")
                        // Remove from queued timestamps if it was there
                        queuedDetectionTimestamps.remove(currentTimestamp)
                        _syncStatus.update { currentStatus ->
                            val newQueuedCount = (currentStatus.queuedCount - 1).coerceAtLeast(0)
                            currentStatus.copy(
                                isQueued = newQueuedCount > 0,
                                queuedCount = newQueuedCount,
                                isSyncing = false
                            )
                        }
                    }
                    is DetectionRepository.SaveResult.Queued -> {
                        android.util.Log.d("DashboardViewModel", "Detection result queued for sync when online")
                        // Track the timestamp so we can detect when it syncs
                        queuedDetectionTimestamps.add(currentTimestamp)
                        _syncStatus.update { 
                            it.copy(
                                isQueued = true, 
                                queuedCount = it.queuedCount + 1,
                                isSyncing = false
                            ) 
                        }
                    }
                    is DetectionRepository.SaveResult.Error -> {
                        android.util.Log.e("DashboardViewModel", "Error saving detection result: ${saveResult.message}")
                        // Still update sync status to show error
                        _syncStatus.update { it.copy(isQueued = true, queuedCount = it.queuedCount + 1, isSyncing = false) }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Unexpected error saving detection result: ${e.message}", e)
                // Treat as queued since Firestore persistence should handle it
                _syncStatus.update { it.copy(isQueued = true, queuedCount = it.queuedCount + 1) }
            }
        }
    }
    
    /**
     * Get current location as a string address
     */
    private suspend fun getCurrentLocation(context: android.content.Context): String? = withContext(Dispatchers.IO) {
        try {
            val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            
            // Check location permissions
            val hasLocationPermission = android.content.pm.PackageManager.PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) ||
                android.content.pm.PackageManager.PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            
            if (!hasLocationPermission) {
                android.util.Log.w("DashboardViewModel", "Location permission not granted")
                return@withContext null
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
                    return@withContext null
                } catch (e: Exception) {
                    android.util.Log.w("DashboardViewModel", "Error getting last location: ${e.message}")
                    return@withContext null
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
