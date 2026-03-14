package com.bisu.chickcare.backend.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.repository.FeedingScheduleEntry
import com.bisu.chickcare.backend.repository.FeedingScheduleRepository
import com.bisu.chickcare.backend.service.FeedingScheduleNotificationManager
import com.bisu.chickcare.backend.utils.OfflineAuthHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeedingScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FeedingScheduleRepository()
    private val auth = FirebaseAuth.getInstance()
    private val notificationManager = FeedingScheduleNotificationManager(application)

    private val _schedules = MutableStateFlow<List<FeedingScheduleEntry>>(emptyList())
    val schedules: StateFlow<List<FeedingScheduleEntry>> = _schedules.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private fun getCurrentUserId(): String? {
        val firebaseUid = auth.currentUser?.uid
        if (firebaseUid != null) return firebaseUid
        return OfflineAuthHelper.getCurrentLocalUserId(getApplication())
    }

    init {
        loadSchedules()
    }

    private fun loadSchedules() {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.getSchedules(userId).collect { items ->
                    _schedules.value = items
                    refreshNotifications(items)
                }
            } catch (_: Exception) {
                // Error handled upstream if needed
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveSchedule(entry: FeedingScheduleEntry) {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val documentId = repository.saveSchedule(userId, entry)
                val resolvedEntry = entry.copy(id = entry.id.ifBlank { documentId })
                notificationManager.schedule(resolvedEntry)
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleCompletion(scheduleId: String, completed: Boolean) {
        val userId = getCurrentUserId() ?: return
        val schedule = _schedules.value.firstOrNull { it.id == scheduleId }
        viewModelScope.launch {
            try {
                repository.updateCompletion(userId, scheduleId, completed)
            } catch (_: Exception) {
            }
        }
        if (completed) {
            notificationManager.cancel(scheduleId)
        } else {
            schedule?.copy(isCompleted = false)?.let { notificationManager.schedule(it) }
        }
    }

    fun deleteSchedule(scheduleId: String) {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deleteSchedule(userId, scheduleId)
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
        notificationManager.cancel(scheduleId)
    }

    private fun refreshNotifications(entries: List<FeedingScheduleEntry>) {
        entries.forEach { entry ->
            if (entry.isCompleted) {
                notificationManager.cancel(entry.id)
            } else {
                notificationManager.schedule(entry)
            }
        }
    }
}


