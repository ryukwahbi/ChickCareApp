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
import com.bisu.chickcare.backend.service.DetectionService
import com.bisu.chickcare.backend.service.NotificationService
import com.bisu.chickcare.frontend.utils.DateFormatters
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
import java.util.Calendar
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

        viewModelScope.launch {
            val profileJob = async { auth.fetchUserProfile() }
            val statsJob = async { detectionService.fetchUserStats(userId) }

            val profileData = profileJob.await()
            val (chickens, _) = statsJob.await()

            _uiState.update { currentState ->
                currentState.copy(
                    userName = (profileData?.get("fullName") as? String) ?: "User",
                    totalChickens = chickens,
                    isLoading = false
                )
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
                    detectionService.detectIB(userId, imageUri, audioUri)
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
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
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
                    kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
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
                    kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
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
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
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
                _detectionHistory.value = history
                _newHistoryCount.value = history.count { !it.isRead }
                
                // Update trend data when history changes
                val imageTrendData = calculateTrendData(history.filter { !it.imageUri.isNullOrEmpty() })
                val audioTrendData = calculateTrendData(history.filter { !it.audioUri.isNullOrEmpty() })
                
                // Calculate stable healthy rate using at least 100 data points (or all if less than 100)
                // This makes the rate change more gradually when new detections are added
                val stableHealthyRate = calculateStableHealthyRate(history)
                
                _uiState.update { currentState ->
                    currentState.copy(
                        totalDetections = history.size,
                        healthyRate = stableHealthyRate,
                        imageDetections = history.count { !it.imageUri.isNullOrEmpty() },
                        audioDetections = history.count { !it.audioUri.isNullOrEmpty() },
                        imageTrendData = imageTrendData,
                        audioTrendData = audioTrendData
                    )
                }
            }
        }
    }
    
    private fun calculateTrendData(entries: List<DetectionEntry>): List<TrendDataPoint> {
        if (entries.isEmpty()) {
            // Return empty data points for empty state
            return listOf()
        }
        
        // Group by weeks (last 6 weeks)
        val calendar = Calendar.getInstance()
        val weekGroups = mutableMapOf<String, Pair<Double, Double>>() // label to (healthyPercentage, unhealthyPercentage)
        
        // Get last 6 weeks
        for (i in 0 until 6) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.WEEK_OF_YEAR, -i)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val weekStart = calendar.timeInMillis
            
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
            val weekEnd = calendar.timeInMillis
            
            val label = SimpleDateFormat("MMM dd", Locale.getDefault()).format(weekStart)
            
            val weekEntries = entries.filter { it.timestamp in weekStart until weekEnd }
            
            // Calculate percentage of healthy and unhealthy detections (not average confidence)
            // This shows the trend: what percentage of detections were healthy vs unhealthy each week
            val healthyEntries = weekEntries.filter { it.isHealthy }
            val unhealthyEntries = weekEntries.filter { !it.isHealthy }
            val totalEntries = weekEntries.size
            
            // Calculate percentage: (healthy count / total count) * 100
            val healthyPercentage = if (totalEntries > 0) {
                (healthyEntries.size.toDouble() / totalEntries.toDouble()) * 100.0
            } else 0.0
            
            // Calculate percentage: (unhealthy count / total count) * 100
            val unhealthyPercentage = if (totalEntries > 0) {
                (unhealthyEntries.size.toDouble() / totalEntries.toDouble()) * 100.0
            } else 0.0
            
            weekGroups[label] = Pair(healthyPercentage, unhealthyPercentage)
        }
        
        // Convert to list and reverse so oldest is first
        return weekGroups.map { (label, averages) ->
            TrendDataPoint(label, averages.first, averages.second)
        }.reversed()
    }
    
    /**
     * Calculate stable healthy rate using at least 100 data points (or all if less than 100).
     * Uses weighted average based on confidence values to allow smooth 0.1% increments.
     * This ensures the rate changes gradually (1.0%, 1.1%, 1.2%...) instead of jumping in 1% steps.
     */
    private fun calculateStableHealthyRate(history: List<DetectionEntry>): Double {
        if (history.isEmpty()) return 0.0
        
        // Use at least 100 data points, or all available if less than 100
        val minDataPoints = 100
        val dataPointsToUse = if (history.size >= minDataPoints) {
            // Take the most recent 100+ data points (sorted by timestamp descending)
            history.sortedByDescending { it.timestamp }.take(minDataPoints)
        } else {
            // Use all available data if less than 100
            history
        }
        
        if (dataPointsToUse.isEmpty()) return 0.0
        
        // Calculate weighted average: each detection contributes based on its confidence
        // This allows for smooth 0.1% increments instead of discrete 1% jumps
        var totalWeight = 0.0
        var healthyWeight = 0.0
        
        dataPointsToUse.forEach { entry ->
            // Use confidence as weight (0.0 to 1.0), with minimum weight of 0.01 to ensure all entries contribute
            val weight = entry.confidence.coerceIn(0.01f, 1.0f).toDouble()
            totalWeight += weight
            
            // If healthy, add its weighted contribution
            if (entry.isHealthy) {
                healthyWeight += weight
            }
        }
        
        // Calculate rate with precision that allows 0.1% increments
        val healthyRate = if (totalWeight > 0.0) {
            (healthyWeight / totalWeight) * 100.0
        } else {
            0.0
        }
        
        // Round to 1 decimal place for smooth 0.1% increments
        val roundedRate = (healthyRate * 10.0).roundToInt() / 10.0
        
        android.util.Log.d("DashboardViewModel", "Stable healthy rate calculated: $roundedRate% (using ${dataPointsToUse.size} data points, weighted average: $healthyWeight/$totalWeight)")
        
        return roundedRate
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
            // Cleanup old items before listening
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
                    .update("lastActive", System.currentTimeMillis())
                    .await()
            } catch (_: Exception) {
                // Silent fail - not critical
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
    
    fun deleteAllDetections() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                detectionRepository.deleteAllDetections(userId)
                // History will update automatically via Firestore listener
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error deleting all detections", e)
            }
        }
    }
    
    fun deleteSelectedDetections(detectionIds: List<String>) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                detectionIds.forEach { id ->
                    detectionRepository.deleteDetection(userId, id)
                }
                // History will update automatically via Firestore listener
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error deleting selected detections", e)
            }
        }
    }
    
    fun updateDetection(
        detectionId: String,
        result: String,
        isHealthy: Boolean,
        confidence: Float
    ) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                detectionRepository.updateDetection(userId, detectionId, result, isHealthy, confidence)
                // History will update automatically via Firestore listener
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error updating detection", e)
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
                
                // Optimistically set count to 0 immediately
                _newHistoryCount.value = 0
                
                // Also optimistically update local history state
                val updatedHistory = _detectionHistory.value.map { entry ->
                    if (!entry.isRead) {
                        entry.copy(isRead = true)
                    } else {
                        entry
                    }
                }
                _detectionHistory.value = updatedHistory
                
                // Then mark all unread detections as read in Firestore
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
                    // Use repository method as fallback if no IDs found
                    detectionRepository.markAllDetectionsAsRead(userId)
                }
                
                android.util.Log.d("DashboardViewModel", "Marked all detections as read")
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Error marking detections as read", e)
                // On error, recalculate from current history
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
            
            // Get last known location (faster, but may be stale)
            val location: Location? = try {
                val locationTask = fusedLocationClient.lastLocation
                locationTask.await()
            } catch (e: SecurityException) {
                android.util.Log.w("DashboardViewModel", "SecurityException getting location: ${e.message}")
                return null
            } catch (e: Exception) {
                android.util.Log.w("DashboardViewModel", "Error getting location: ${e.message}")
                return null
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
                        addressParts.joinToString(", ")
                    } else {
                        "${location.latitude}, ${location.longitude}"
                    }
                } catch (_: Exception) {
                    // Fallback to coordinates if geocoding fails
                    "${location.latitude}, ${location.longitude}"
                }
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.w("DashboardViewModel", "Error getting location: ${e.message}")
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
            } catch (e: Exception) {
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
