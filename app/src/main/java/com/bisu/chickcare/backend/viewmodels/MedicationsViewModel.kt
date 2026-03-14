package com.bisu.chickcare.backend.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.repository.MedicationEntry
import com.bisu.chickcare.backend.repository.MedicationsRepository
import com.bisu.chickcare.backend.utils.OfflineAuthHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MedicationsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MedicationsRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _medications = MutableStateFlow<List<MedicationEntry>>(emptyList())
    val medications: StateFlow<List<MedicationEntry>> = _medications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private fun getCurrentUserId(): String? {
        val firebaseUid = auth.currentUser?.uid
        if (firebaseUid != null) return firebaseUid
        return OfflineAuthHelper.getCurrentLocalUserId(getApplication())
    }

    init {
        loadMedications()
    }

    private fun loadMedications() {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.getMedications(userId).collect { items ->
                    _medications.value = items
                }
            } catch (_: Exception) {
                // Handle upstream if needed
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveMedication(entry: MedicationEntry) {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.saveMedication(userId, entry)
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleMedicationStatus(id: String, isActive: Boolean) {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                repository.updateStatus(userId, id, isActive)
            } catch (_: Exception) {
            }
        }
    }

    fun deleteMedication(id: String) {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deleteMedication(userId, id)
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }
}

