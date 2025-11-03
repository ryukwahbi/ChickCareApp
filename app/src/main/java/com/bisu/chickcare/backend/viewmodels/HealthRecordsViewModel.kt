package com.bisu.chickcare.backend.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.repository.HealthRecord
import com.bisu.chickcare.backend.repository.HealthRecordsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HealthRecordsViewModel : ViewModel() {
    private val repository = HealthRecordsRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _healthRecords = MutableStateFlow<List<HealthRecord>>(emptyList())
    val healthRecords: StateFlow<List<HealthRecord>> = _healthRecords.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadHealthRecords()
    }

    private fun loadHealthRecords() {
        val userId = auth.currentUser?.uid ?: return
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
        val userId = auth.currentUser?.uid ?: return
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
        val userId = auth.currentUser?.uid ?: return
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

