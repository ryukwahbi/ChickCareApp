package com.bisu.chickcare.backend.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.repository.DetectionRepository
import com.bisu.chickcare.backend.repository.EggProductionRepository
import com.bisu.chickcare.backend.repository.ExpenseRepository
import com.bisu.chickcare.backend.repository.FeedingScheduleRepository
import com.bisu.chickcare.backend.repository.HealthRecordsRepository
import com.bisu.chickcare.backend.repository.MedicationsRepository
import com.bisu.chickcare.backend.repository.ReportCategory
import com.bisu.chickcare.backend.repository.ReportEntry
import com.bisu.chickcare.backend.repository.ReportsRepository
import com.bisu.chickcare.backend.repository.VaccinationRepository
import com.bisu.chickcare.backend.utils.OfflineAuthHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ReportsRepository()
    private val auth = FirebaseAuth.getInstance()
    
    // Repositories for fetching real data
    private val healthRecordsRepo = HealthRecordsRepository()
    private val vaccinationRepo = VaccinationRepository()
    private val medicationsRepo = MedicationsRepository()
    private val eggProductionRepo = EggProductionRepository()
    private val feedingScheduleRepo = FeedingScheduleRepository()
    private val expenseRepo = ExpenseRepository()
    private val detectionRepo = DetectionRepository()

    private val _reports = MutableStateFlow<List<ReportEntry>>(emptyList())
    val reports: StateFlow<List<ReportEntry>> = _reports.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private fun getCurrentUserId(): String? {
        val firebaseUid = auth.currentUser?.uid
        if (firebaseUid != null) return firebaseUid
        return OfflineAuthHelper.getCurrentLocalUserId(getApplication())
    }

    init {
        initialize()
    }

    private fun initialize() {
        val userId = getCurrentUserId() ?: return
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
        val userId = getCurrentUserId() ?: return
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
        val userId = getCurrentUserId() ?: return
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
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            repository.updateLastGenerated(userId, reportId)
        }
    }

    fun createEmptyReport(): ReportEntry = ReportEntry(type = ReportCategory.HEALTH)
    
    // Fetch all data for report generation
    suspend fun getAllReportData(userId: String): ReportData {
        return ReportData(
            healthRecords = healthRecordsRepo.getHealthRecords(userId).first(),
            vaccinations = vaccinationRepo.getVaccinations(userId).first(),
            medications = medicationsRepo.getMedications(userId).first(),
            eggProduction = eggProductionRepo.getEggProductionRecords(userId).first(),
            feedingSchedules = feedingScheduleRepo.getSchedules(userId).first(),
            expenses = expenseRepo.getExpenses(userId).first(),
            detections = detectionRepo.getDetectionHistory(userId).first()
        )
    }
}

// Data class to hold all report data
data class ReportData(
    val healthRecords: List<com.bisu.chickcare.backend.repository.HealthRecord>,
    val vaccinations: List<com.bisu.chickcare.backend.repository.Vaccination>,
    val medications: List<com.bisu.chickcare.backend.repository.MedicationEntry>,
    val eggProduction: List<com.bisu.chickcare.backend.repository.EggProductionRecord>,
    val feedingSchedules: List<com.bisu.chickcare.backend.repository.FeedingScheduleEntry>,
    val expenses: List<com.bisu.chickcare.backend.repository.Expense>,
    val detections: List<com.bisu.chickcare.backend.repository.DetectionEntry>
)

