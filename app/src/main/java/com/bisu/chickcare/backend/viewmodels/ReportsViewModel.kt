package com.bisu.chickcare.backend.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.repository.ReportCategory
import com.bisu.chickcare.backend.repository.ReportEntry
import com.bisu.chickcare.backend.repository.ReportsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportsViewModel : ViewModel() {
    private val repository = ReportsRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _reports = MutableStateFlow<List<ReportEntry>>(emptyList())
    val reports: StateFlow<List<ReportEntry>> = _reports.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            repository.seedDefaultReports(userId)
            repository.getReports(userId).collect { entries ->
                _reports.value = entries
                _isLoading.value = false
            }
        }
    }

    fun saveReport(entry: ReportEntry) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.saveReport(userId, entry)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteReport(reportId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deleteReport(userId, reportId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markGenerated(reportId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.updateLastGenerated(userId, reportId)
        }
    }

    fun createEmptyReport(): ReportEntry = ReportEntry(type = ReportCategory.HEALTH)
}


