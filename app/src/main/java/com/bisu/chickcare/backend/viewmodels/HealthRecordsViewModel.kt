package com.bisu.chickcare.backend.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.repository.HealthRecord
import com.bisu.chickcare.backend.repository.HealthRecordsRepository
import com.bisu.chickcare.backend.utils.OfflineAuthHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HealthRecordsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HealthRecordsRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _healthRecords = MutableStateFlow<List<HealthRecord>>(emptyList())
    val healthRecords: StateFlow<List<HealthRecord>> = _healthRecords.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private fun getCurrentUserId(): String? {
        val firebaseUid = auth.currentUser?.uid
        if (firebaseUid != null) return firebaseUid
        return OfflineAuthHelper.getCurrentLocalUserId(getApplication())
    }
    
    init {
        loadHealthRecords()
    }

    private fun loadHealthRecords() {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.getHealthRecords(userId).collect { records ->
                    _healthRecords.value = records
                }
            } catch (_: Exception) {
                // Error handling
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveHealthRecord(record: HealthRecord) {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.saveHealthRecord(userId, record)
            } catch (_: Exception) {
                // Error handling
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteHealthRecord(recordId: String) {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deleteHealthRecord(userId, recordId)
            } catch (_: Exception) {
                // Error handling
            } finally {
                _isLoading.value = false
            }
        }
    }

}

