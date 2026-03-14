package com.bisu.chickcare.backend.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.repository.EggProductionRecord
import com.bisu.chickcare.backend.repository.EggProductionRepository
import com.bisu.chickcare.backend.utils.OfflineAuthHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EggProductionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EggProductionRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _eggProductionRecords = MutableStateFlow<List<EggProductionRecord>>(emptyList())
    val eggProductionRecords: StateFlow<List<EggProductionRecord>> = _eggProductionRecords.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private fun getCurrentUserId(): String? {
        val firebaseUid = auth.currentUser?.uid
        if (firebaseUid != null) return firebaseUid
        return OfflineAuthHelper.getCurrentLocalUserId(getApplication())
    }
    
    init {
        loadEggProductionRecords()
    }

    private fun loadEggProductionRecords() {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.getEggProductionRecords(userId).collect { records ->
                    _eggProductionRecords.value = records
                }
            } catch (_: Exception) {
                // Error handling
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveEggProductionRecord(record: EggProductionRecord) {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.saveEggProduction(userId, record)
            } catch (_: Exception) {
                // Error handling
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteEggProductionRecord(recordId: String) {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deleteEggProductionRecord(userId, recordId)
            } catch (_: Exception) {
                // Error handling
            } finally {
                _isLoading.value = false
            }
        }
    }

}

