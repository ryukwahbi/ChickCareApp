package com.bisu.chickcare.backend.service

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import com.bisu.chickcare.backend.repository.DetectionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class DetectionService(
    private val detectionRepository: DetectionRepository,
    private val context: Context
) {
    // Initialize classifiers lazily to prevent crashes on app startup if model loading fails
    private val imageClassifier by lazy { ChickenClassifier(context) }
    private val audioClassifier by lazy { AudioClassifier(context) }
    private val fusionClassifier by lazy { FusionClassifier(context) }
    private val audioConverter by lazy { AudioSpectrogramConverter(context) }

    suspend fun detectIB(userId: String, imageUri: String?, audioUri: String?): Pair<Boolean, String> {
        // Use NonCancellable at the top level to ensure we always return a result
        return kotlinx.coroutines.withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
            detectIBInternal(userId, imageUri, audioUri)
        }
    }
    
    private suspend fun detectIBInternal(userId: String, imageUri: String?, audioUri: String?): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        // Determine which type of detection to perform
        val isImageDetection = imageUri != null
        val isAudioDetection = audioUri != null
        
        if (!isImageDetection && !isAudioDetection) {
            return@withContext false to "No Input Provided"
        }

        try {
            var label: String? = null
            var score = 0f
            var detectedType = ""

            // FUSION DETECTION: Use fusion model when both image and audio are available
            if (isImageDetection && isAudioDetection && fusionClassifier.isModelAvailable()) {
                android.util.Log.d("DetectionService", "Using fusion model (image + audio)")
                
                // Wrap the entire fusion detection in NonCancellable to prevent job cancellation
                // This ensures the process completes even if viewModelScope is cancelled
                var fusionResult: Pair<Boolean, String>? = null
                try {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                        android.util.Log.d("DetectionService", "Step 1: Loading image bitmap...")
                        // Load image bitmap
                        val decodedImageUri = imageUri.toUri()
                        val imageBitmap = if (decodedImageUri.scheme == "file") {
                            BitmapFactory.decodeFile(decodedImageUri.path)
                        } else {
                            context.contentResolver.openInputStream(decodedImageUri)?.use { inputStream ->
                                BitmapFactory.decodeStream(inputStream)
                            }
                        } ?: throw Exception("Failed to decode image bitmap")
                        
                        android.util.Log.d("DetectionService", "Image loaded: ${imageBitmap.width}x${imageBitmap.height}")

                        android.util.Log.d("DetectionService", "Step 2: Validating audio URI...")
                        // Validate audio URI first
                        val audioUriObj = try {
                            audioUri.toUri()
                        } catch (e: Exception) {
                            android.util.Log.e("DetectionService", "Invalid audio URI: $audioUri", e)
                            throw Exception("Invalid audio URI: ${e.message}")
                        }
                        
                        // Check if audio file is accessible
                        val audioFile = try {
                            if (audioUriObj.scheme == "file") {
                                java.io.File(audioUriObj.path ?: "")
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            null
                        }
                        
                        if (audioFile != null && (!audioFile.exists() || audioFile.length() == 0L)) {
                            android.util.Log.e("DetectionService", "Audio file does not exist or is empty: ${audioFile.path}")
                            throw Exception("Audio file not found or empty")
                        }
                        
                        android.util.Log.d("DetectionService", "Step 3: Converting audio to spectrogram...")
                        // Convert audio to spectrogram
                        val specBitmap = try {
                            audioConverter.convertAudioToSpectrogram(audioUri)
                        } catch (e: Exception) {
                            android.util.Log.e("DetectionService", "Audio conversion failed: ${e.message}", e)
                            throw Exception("Failed to convert audio to spectrogram: ${e.message}")
                        }
                        android.util.Log.d("DetectionService", "Spectrogram generated: ${specBitmap.width}x${specBitmap.height}")

                        android.util.Log.d("DetectionService", "Step 4: Running fusion classification...")
                        // Run fusion classification
                        val (fusionLabelResult, fusionScoreResult) = try {
                            fusionClassifier.classifyFusion(imageBitmap, specBitmap)
                        } catch (e: Exception) {
                            android.util.Log.e("DetectionService", "Fusion classification exception: ${e.message}", e)
                            throw Exception("Fusion classification failed: ${e.message}")
                        }
                        android.util.Log.d("DetectionService", "Fusion classification complete: $fusionLabelResult ($fusionScoreResult)")

                        if (fusionLabelResult != "Model Not Loaded" && fusionLabelResult != "Classification Error") {
                            label = fusionLabelResult
                            score = fusionScoreResult
                            detectedType = "Fusion (Image + Audio)"
                            
                            android.util.Log.i("DetectionService", "Fusion prediction: $label (${score * 100}%)")
                            
                            // Prepare result before saving
                            val status = if (label == "Unhealthy") "Infected" else label
                            val isInfected = status.equals("Infected", ignoreCase = true) || 
                                            status.equals("Sick", ignoreCase = true) ||
                                            status.equals("Sick_chicken", ignoreCase = true) ||
                                            status.equals("Unhealthy", ignoreCase = true)
                            
                            val resultString = "$status (${String.format(Locale.US, "%.1f", score * 100)}%)"
                            
                            // Store result - saving will be done manually by user clicking "Save Result"
                            fusionResult = isInfected to resultString
                            
                            android.util.Log.d("DetectionService", "Detection completed - result ready (not saved yet, waiting for user action)")
                            
                            // Clean up bitmaps
                            if (!imageBitmap.isRecycled) imageBitmap.recycle()
                            if (!specBitmap.isRecycled) specBitmap.recycle()
                        } else {
                            android.util.Log.w("DetectionService", "Fusion model returned error: $fusionLabelResult, falling back")
                            // Clean up bitmaps before fallback
                            if (!imageBitmap.isRecycled) imageBitmap.recycle()
                            if (!specBitmap.isRecycled) specBitmap.recycle()
                            // Fall through to individual classifiers
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    android.util.Log.e("DetectionService", "Fusion detection was cancelled (shouldn't happen with NonCancellable): ${e.message}", e)
                    throw Exception("Detection was cancelled. Please try again.")
                } catch (e: Exception) {
                    android.util.Log.e("DetectionService", "Fusion classification failed: ${e.message}", e)
                    android.util.Log.e("DetectionService", "Stack trace:", e)
                    // Return error immediately instead of falling back
                    return@withContext false to "Detection Error: ${e.message ?: "Unknown error during fusion detection"}"
                }
                
                // If fusion detection succeeded, return the stored result
                if (fusionResult != null) {
                    android.util.Log.d("DetectionService", "Returning fusion result: ${fusionResult!!.second}")
                    return@withContext fusionResult!!
                } else if (label != null && detectedType == "Fusion (Image + Audio)") {
                    // Fallback: construct result if fusionResult wasn't set (shouldn't happen)
                    val status = if (label == "Unhealthy") "Infected" else label
                    val isInfected = status.equals("Infected", ignoreCase = true) || 
                                    status.equals("Sick", ignoreCase = true) ||
                                    status.equals("Sick_chicken", ignoreCase = true) ||
                                    status.equals("Unhealthy", ignoreCase = true)
                    
                    val resultString = "$status (${String.format(Locale.US, "%.1f", score * 100)}%)"
                    return@withContext isInfected to resultString
                } else {
                    android.util.Log.w("DetectionService", "Fusion model failed, falling back to individual classifiers")
                    // Fall through to individual classifiers
                }
            } else if (isImageDetection && isAudioDetection && !fusionClassifier.isModelAvailable()) {
                android.util.Log.e("DetectionService", "Fusion model not available but both inputs provided!")
                return@withContext false to "Detection Error: Fusion model is unavailable. Please ensure cnnmlp_fusion.tflite is in assets folder."
            }

            // FALLBACK: Use individual classifiers if fusion is not available or failed
            // Image detection
            if (isImageDetection) {
                android.util.Log.d("DetectionService", "Starting image detection with URI: $imageUri")
                
                // Check if model is available before processing image
                if (!imageClassifier.isModelAvailable()) {
                    android.util.Log.w("DetectionService", "Image classifier not available (expected - using fusion model)")
                    // If fusion model also failed and we have both inputs, return error
                    if (!fusionClassifier.isModelAvailable() && isAudioDetection) {
                        return@withContext false to "Detection Error: Fusion model is unavailable. Please ensure cnnmlp_fusion.tflite is in assets folder."
                    }
                    // If we only have image input but no image model, return specific error
                    if (!isAudioDetection) {
                        return@withContext false to "Image Model Not Available. Please provide both image and audio for fusion detection."
                    }
                    // Skip image detection, will try audio-only if available
                    android.util.Log.d("DetectionService", "Skipping image-only detection, fusion model should be used instead")
                } else {
                    val bitmap = try {
                        val decodedUri = imageUri.toUri()
                        android.util.Log.d("DetectionService", "Decoded URI: $decodedUri")
                    
                        // Handle both file:// and content:// URIs
                        if (decodedUri.scheme == "file") {
                            // For file:// URIs (camera capture)
                            BitmapFactory.decodeFile(decodedUri.path)
                        } else {
                            // For content:// URIs (gallery selection)
                            context.contentResolver.openInputStream(decodedUri)?.use { inputStream ->
                                BitmapFactory.decodeStream(inputStream)
                            }
                        } ?: throw Exception("Failed to decode image bitmap")
                    } catch (e: Exception) {
                        android.util.Log.e("DetectionService", "Error loading image: ${e.message}", e)
                        return@withContext false to "Failed to load image: ${e.message}"
                    }

                    if (bitmap.isRecycled) {
                        android.util.Log.e("DetectionService", "Bitmap already recycled")
                        return@withContext false to "Invalid image"
                    }

                    android.util.Log.d("DetectionService", "Bitmap loaded: ${bitmap.width}x${bitmap.height}")

                    // Get all classification results for validation
                    val allResults = imageClassifier.classifyWithAllResults(bitmap)
                    val (imgLabel, imgScore) = imageClassifier.classify(bitmap)
                    
                    android.util.Log.d("DetectionService", "Classification result: $imgLabel (${imgScore})")
                    android.util.Log.d("DetectionService", "All results: $allResults")
                    
                    if (imgLabel == null || imgLabel == "Model Not Loaded" || imgLabel == "Classification Error") {
                        android.util.Log.e("DetectionService", "Image classification failed: $imgLabel")
                        return@withContext false to "Image Model Not Available"
                    }

                    // VALIDATION: Check if image is actually a chicken
                    // If confidence is too low (< 0.55), it's likely not a chicken image
                    // Also check if both categories have low confidence
                    val isValidChickenImage = validateChickenImage(allResults, imgScore)
                    
                    if (!isValidChickenImage) {
                        android.util.Log.w("DetectionService", "Invalid image detected - not a chicken (confidence: $imgScore)")
                        // Recycle bitmap before returning
                        if (!bitmap.isRecycled) {
                            bitmap.recycle()
                        }
                        return@withContext false to "INVALID_IMAGE: Invalid image, not related to chicken. Please retake again."
                    }

                    label = imgLabel
                    score = imgScore
                    detectedType = "Image"
                    
                    // Recycle bitmap to free memory
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }

            // Audio detection (if no image or as supplement)
            if (isAudioDetection) {
                // Convert audio to spectrogram
                val spectrogram = audioConverter.convertAudioToSpectrogram(audioUri)
                
                try {
                    // Classify spectrogram
                    if (audioClassifier.isModelAvailable()) {
                        val (audioLabel, audioScore) = audioClassifier.classifySpectrogram(spectrogram)
                        
                        if (audioLabel != "Model Not Loaded") {
                            // If we already have image results, combine them
                            if (label != null) {
                                // Weighted combination: 60% image, 40% audio (for logging)
                                val combinedScore = (score * 0.6f + audioScore * 0.4f)
                                android.util.Log.d("DetectionService", "Combined score: $combinedScore (Image: $score, Audio: $audioScore)")
                                
                                // Use the more confident prediction
                                if (audioScore > 0.7f) {
                                    label = audioLabel
                                    score = audioScore
                                    android.util.Log.d("DetectionService", "Using audio prediction (high confidence: $audioScore)")
                                } else {
                                    // Keep image prediction but log combined score
                                    android.util.Log.d("DetectionService", "Using image prediction (combined score: $combinedScore)")
                                }
                                detectedType = "Combined (Image + Audio)"
                            } else {
                                label = audioLabel
                                score = audioScore
                                detectedType = "Audio"
                            }
                        } else {
                            if (label == null) {
                                // Recycle spectrogram before returning
                                if (!spectrogram.isRecycled) {
                                    spectrogram.recycle()
                                }
                                return@withContext false to "Audio Model Not Available"
                            }
                            // Image model worked, but audio failed - continue with image result
                            android.util.Log.w("DetectionService", "Audio model failed, using image result only")
                        }
                    } else {
                        // Audio model not available
                        if (!isImageDetection) {
                            // Recycle spectrogram before returning
                            if (!spectrogram.isRecycled) {
                                spectrogram.recycle()
                            }
                            return@withContext false to "Audio Model Not Available"
                        }
                        // Image detection is available, continue with that
                        android.util.Log.d("DetectionService", "Audio model not available, using image detection only")
                    }
                } finally {
                    // Always recycle spectrogram bitmap to free memory
                    if (!spectrogram.isRecycled) {
                        spectrogram.recycle()
                    }
                }
            }

            if (label == null) {
                return@withContext false to "No Detection Result"
            }

            val status = label
            // Support multiple label formats
            val isInfected = status.equals("Infected", ignoreCase = true) || 
                            status.equals("Sick", ignoreCase = true) ||
                            status.equals("Sick_chicken", ignoreCase = true) ||
                            status.equals("Unhealthy", ignoreCase = true)
            val isHealthy = !isInfected

            val resultString = "$status (${String.format(Locale.US, "%.1f", score * 100)}%)"
            if (detectedType.isNotEmpty()) {
                android.util.Log.i("DetectionService", "Detection Type: $detectedType")
            }

            // Result ready - saving will be done manually by user clicking "Save Result"
            android.util.Log.d("DetectionService", "Detection completed - result ready (not saved yet, waiting for user action)")

            return@withContext isInfected to resultString

        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("DetectionService", "Detection failed: ${e.message}", e)
            return@withContext false to "Analysis Failed: ${e.message}"
        }
    }

    private suspend fun saveDetection(userId: String, result: String, isHealthy: Boolean, confidence: Float, imageUri: String?, audioUri: String?) {
        detectionRepository.saveDetection(userId, result, isHealthy, confidence, imageUri, audioUri)
    }

    suspend fun getRemedySuggestions(isInfected: Boolean): List<String> = withContext(Dispatchers.IO) {
        if (isInfected) {
            listOf(
                "⚠️ IMPORTANT: Go to the veterinarian immediately for proper diagnosis and treatment.",
                "Isolate infected birds immediately to prevent disease spread.",
                "Administer antibiotics only as prescribed by a veterinarian.",
                "Improve ventilation in the coop to reduce infection risk.",
                "Ensure clean water and high-quality feed to support recovery.",
                "Monitor affected chickens closely and report symptoms to the vet."
            )
        } else {
            listOf(
                "Your chicken appears healthy. Continue regular health monitoring.",
                "Maintain clean and dry coop conditions.",
                "Provide balanced nutrition and fresh water.",
                "Schedule regular check-ups with a veterinarian for preventive care."
            )
        }
    }

    suspend fun fetchUserStats(userId: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        detectionRepository.fetchUserStats(userId)
    }

    /**
     * Validate if the image is actually a chicken
     * Returns false if the image doesn't match the trained model (not a chicken)
     * 
     * Validation rules:
     * 1. Top confidence must be >= 0.55 (55%) - if lower, it's likely not a chicken
     * 2. If both categories have low confidence (< 0.5), it's not a chicken
     * 3. If only one category exists with very low confidence, it's suspicious
     */
    private fun validateChickenImage(allResults: List<Pair<String, Float>>, topScore: Float): Boolean {
        // Rule 1: Top confidence must be reasonably high
        // For a well-trained chicken model, we expect at least 55% confidence
        if (topScore < 0.55f) {
            android.util.Log.w("DetectionService", "Validation failed: Low confidence ($topScore < 0.55)")
            return false
        }

        // Rule 2: Check if we have reasonable predictions for both classes
        // If we have results, check if the top result makes sense
        if (allResults.isEmpty()) {
            android.util.Log.w("DetectionService", "Validation failed: No classification results")
            return false
        }

        // Rule 3: If we have both Healthy and Infected results, check their confidence spread
        // If both are very uncertain, it's likely not a chicken
        val healthyResult = allResults.find { it.first.equals("Healthy", ignoreCase = true) }
        val infectedResult = allResults.find { it.first.equals("Infected", ignoreCase = true) }
        
        if (healthyResult != null && infectedResult != null) {
            // Both categories present - check if both are uncertain
            val maxConfidence = maxOf(healthyResult.second, infectedResult.second)
            val minConfidence = minOf(healthyResult.second, infectedResult.second)
            
            // If the difference is very small and both are low, it's not a chicken
            if (maxConfidence < 0.6f && (maxConfidence - minConfidence) < 0.15f) {
                android.util.Log.w("DetectionService", "Validation failed: Both categories uncertain (max: $maxConfidence, min: $minConfidence)")
                return false
            }
        }

        // If we pass all checks, it's likely a valid chicken image
        android.util.Log.d("DetectionService", "Validation passed: Valid chicken image (confidence: $topScore)")
        return true
    }
}
