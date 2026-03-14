package com.bisu.chickcare.backend.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.repository.Vaccination
import com.bisu.chickcare.backend.repository.VaccinationRepository
import com.bisu.chickcare.backend.utils.OfflineAuthHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VaccinationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VaccinationRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _vaccinations = MutableStateFlow<List<Vaccination>>(emptyList())
    val vaccinations: StateFlow<List<Vaccination>> = _vaccinations.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private fun getCurrentUserId(): String? {
        val firebaseUid = auth.currentUser?.uid
        if (firebaseUid != null) return firebaseUid
        return OfflineAuthHelper.getCurrentLocalUserId(getApplication())
    }
    
    init {
        loadVaccinations()
    }

    private fun loadVaccinations() {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.getVaccinations(userId).collect { vaccinations ->
                    _vaccinations.value = vaccinations
                }
            } catch (_: Exception) {
                // Error handling
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveVaccination(vaccination: Vaccination) {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.saveVaccination(userId, vaccination)
            } catch (_: Exception) {
                // Error handling
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteVaccination(vaccinationId: String) {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deleteVaccination(userId, vaccinationId)
            } catch (_: Exception) {
                // Error handling
            } finally {
                _isLoading.value = false
            }
        }
    }

}

