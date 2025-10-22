package com.bisu.chickcare.backend.service

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.bisu.chickcare.backend.repository.DetectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DetectionService(
    private val detectionRepository: DetectionRepository,
    private val context: Context // Context is now required
) {
    // Initialize our classifier
    private val classifier = ChickenClassifier(context)

    suspend fun detectIB(userId: String, imageUri: String?, audioUri: String?): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (imageUri == null) {
            return@withContext false to "No Image Provided"
        }

        try {
            val decodedUri = Uri.parse(imageUri)
            val inputStream = context.contentResolver.openInputStream(decodedUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // *** USE THE ACTUAL MODEL FOR CLASSIFICATION ***
            val (label, score) = classifier.classify(bitmap)

            // Assuming your model's labels are "Infected" and "Healthy".
            // Make sure these strings exactly match the labels your model outputs.
            val status = label
            val isInfected = status.equals("Infected", ignoreCase = true)
            val isHealthy = !isInfected

            // Format the result string to include confidence
            val resultString = "$status (${String.format("%.1f", score * 100)}%)"

            // Save the formatted result to your database
            saveDetection(userId, resultString, isHealthy, imageUri, audioUri)

            // Return the boolean and the formatted string to the ViewModel
            return@withContext isInfected to resultString

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false to "Analysis Failed"
        }
    }

    // The rest of your service functions remain the same...
    private suspend fun saveDetection(userId: String, result: String, isHealthy: Boolean, imageUri: String?, audioUri: String?) {
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
    