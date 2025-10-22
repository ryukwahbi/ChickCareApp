package com.bisu.chickcare.backend.service

import com.bisu.chickcare.backend.repository.DetectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class DetectionService(private val detectionRepository: DetectionRepository) {
    suspend fun detectIB(userId: String, imageUri: String?, audioUri: String?): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        delay(2500)
        val isInfected = Math.random() < 0.5
        val status = if (isInfected) "Infected" else "Healthy"
        val isHealthy = !isInfected

        // Save the detection result for the specific user
        saveDetection(userId, status, isHealthy, imageUri, audioUri)

        return@withContext Pair(isInfected, status)
    }

    private suspend fun saveDetection(userId: String, result: String, isHealthy: Boolean, imageUri: String?, audioUri: String?) {
        // FIX: The order of arguments was incorrect. Swapped 'result' and 'isHealthy'.
        detectionRepository.saveDetection(userId, result, isHealthy, imageUri, audioUri)
    }


    suspend fun getRemedySuggestions(isInfected: Boolean): List<String> = withContext(Dispatchers.IO) {
        if (isInfected) {
            listOf(
                "Isolate infected birds immediately.",
                "Administer antibiotics as prescribed by a vet.",
                "Improve ventilation in the coop.",
                "Ensure clean water and high-quality feed.",
                "Consult a veterinarian for further tests."
            )
        } else {
            listOf(
                "Continue regular health monitoring.",
                "Maintain clean and dry coop conditions.",
                "Provide balanced nutrition and fresh water."
            )
        }
    }

    suspend fun fetchUserStats(userId: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        detectionRepository.fetchUserStats(userId)
    }
}
