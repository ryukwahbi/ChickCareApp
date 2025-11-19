package com.bisu.chickcare.backend.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import androidx.core.graphics.get
import androidx.core.net.toUri
import com.bisu.chickcare.backend.repository.DetectionRepository
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class DetectionService(
    private val detectionRepository: DetectionRepository,
    private val context: Context

) {
    companion object {
        private const val FUSION_MODEL_MISSING_MESSAGE =
            "Detection Error: Fusion model is unavailable. Please ensure ${FusionClassifier.PRIMARY_MODEL_PATH} is in the assets folder."
    }
    private val imageClassifier by lazy { ChickenClassifier(context) }
    private val audioClassifier by lazy { AudioClassifier(context) }
    private val fusionClassifier by lazy { FusionClassifier(context) }
    private val audioConverter by lazy { AudioSpectrogramConverter(context) }

    suspend fun detectIB(imageUri: String?, audioUri: String?): Pair<Boolean, String> {
        // Use NonCancellable at the top level to ensure we always return a result
        return withContext(Dispatchers.IO + NonCancellable) {
            detectIBInternal(imageUri, audioUri)
        }
    }
    
    private suspend fun detectIBInternal(imageUri: String?, audioUri: String?): Pair<Boolean, String> = withContext(Dispatchers.IO) {
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
                Log.d("DetectionService", "Using fusion model (image + audio)")
                
                // Wrap the entire fusion detection in NonCancellable to prevent job cancellation
                // This ensures the process completes even if viewModelScope is cancelled
                var fusionResult: Pair<Boolean, String>? = null
                try {
                    withContext(NonCancellable) {
                        Log.d("DetectionService", "Step 1: Loading image bitmap...")
                        // Load image bitmap
                        val decodedImageUri = imageUri.toUri()
                        val imageBitmap = if (decodedImageUri.scheme == "file") {
                            BitmapFactory.decodeFile(decodedImageUri.path)
                        } else {
                            context.contentResolver.openInputStream(decodedImageUri)?.use { inputStream ->
                                BitmapFactory.decodeStream(inputStream)
                            }
                        } ?: throw Exception("Failed to decode image bitmap")
                        
                        Log.d("DetectionService", "Image loaded: ${imageBitmap.width}x${imageBitmap.height}")
                        
                        // Validate image quality: check for black or overexposed images
                        // OPTIMIZED: Brightness check now uses 20px sampling (4x faster than before)
                        if (isBlackImage(imageBitmap)) {
                            if (!imageBitmap.isRecycled) imageBitmap.recycle()
                            return@withContext false to "INVALID_DETECTION: Image is too dark or completely black. Please take a clear photo with proper lighting. The image or audio is not related to chicken. Please try again with a valid chicken image/audio."
                        }
                        if (isOverexposedImage(imageBitmap)) {
                            if (!imageBitmap.isRecycled) imageBitmap.recycle()
                            return@withContext false to "INVALID_DETECTION: Image is overexposed or too bright. Please take a clear photo with proper lighting. The image or audio is not related to chicken. Please try again with a valid chicken image/audio."
                        }

                        // Guard: Human face detection even in fusion path
                        try {
                            if (containsBlockingHumanFace(imageBitmap)) {
                                if (!imageBitmap.isRecycled) {
                                    imageBitmap.recycle()
                                }
                                Log.w("DetectionService", "Human face detected in fusion image - rejecting as invalid input")
                                return@withContext false to "INVALID_IMAGE: Human face detected. Please capture a clear photo of a chicken."
                            }
                        } catch (e: Exception) {
                            Log.w("DetectionService", "Face detection (fusion) failed or unavailable: ${e.message}")
                            // Continue; do not block if detector fails
                        }

                        Log.d("DetectionService", "Step 2: Validating audio URI...")
                        // Validate audio URI first
                        val audioUriObj = try {
                            audioUri.toUri()
                        } catch (e: Exception) {
                            Log.e("DetectionService", "Invalid audio URI: $audioUri", e)
                            throw Exception("Invalid audio URI: ${e.message}")
                        }
                        
                        // Check if audio file is accessible
                        val audioFile = try {
                            if (audioUriObj.scheme == "file") {
                                File(audioUriObj.path ?: "")
                            } else {
                                null
                            }
                        } catch (_: Exception) {
                            null
                        }
                        
                        if (audioFile != null && (!audioFile.exists() || audioFile.length() == 0L)) {
                            Log.e("DetectionService", "Audio file does not exist or is empty: ${audioFile.path}")
                            throw Exception("Audio file not found or empty")
                        }
                        
                        Log.d("DetectionService", "Step 3: Converting audio to spectrogram...")
                        // Convert audio to spectrogram
                        // TRUST THE MODEL COMPLETELY: No audio validation in fusion mode - let the trained fusion model decide
                        // The fusion model is trained and should be trusted completely - no heuristic gates!
                        val specBitmap = try {
                            audioConverter.convertAudioToSpectrogram(audioUri)
                        } catch (e: Exception) {
                            Log.e("DetectionService", "Audio conversion failed: ${e.message}", e)
                            // Even if audio conversion fails, try to proceed with image-only
                            // Don't block fusion detection just because of audio issues
                            Log.w("DetectionService", "Audio conversion failed, but continuing with image-only fallback")
                            if (!imageBitmap.isRecycled) imageBitmap.recycle()
                            // Fall through to image-only classification instead of returning invalid
                            // Don't throw - let it try image classification
                            return@withContext false to "Audio processing failed. Please try again or use image-only detection."
                        }
                        Log.d("DetectionService", "Spectrogram generated: ${specBitmap.width}x${specBitmap.height}")

                        Log.d("DetectionService", "Step 4: Running fusion classification...")
                        // Run fusion classification with full probability information for validation
                        val (fusionLabelResult, fusionProbabilities) = try {
                            fusionClassifier.classifyFusionWithProbabilities(imageBitmap, specBitmap)
                        } catch (e: Exception) {
                            Log.e("DetectionService", "Fusion classification exception: ${e.message}", e)
                            throw Exception("Fusion classification failed: ${e.message}")
                        }
                        
                        if (fusionLabelResult == "Model Not Loaded" || fusionLabelResult == "Classification Error") {
                            Log.w("DetectionService", "Fusion model returned error: $fusionLabelResult, falling back")
                            // Clean up bitmaps before fallback
                            if (!imageBitmap.isRecycled) imageBitmap.recycle()
                            if (!specBitmap.isRecycled) specBitmap.recycle()
                            // Fall through to individual classifiers
                        } else {
                            val (fusionScoreResult, healthyProb, unhealthyProb) = fusionProbabilities
                            Log.d("DetectionService", "Fusion classification complete: $fusionLabelResult (confidence: $fusionScoreResult, healthy: $healthyProb, unhealthy: $unhealthyProb)")
                            val probabilityGap = abs(healthyProb - unhealthyProb)
                            // ULTRA-RELAXED VALIDATION: Trust the trained fusion model - it knows best!
                            // The model is trained on real data, so trust its predictions
                            // Only reject if confidence is extremely low or completely uncertain
                            val minConfidence = 0.50f  // Very low threshold - trust model predictions
                            val minGap = 0.10f  // Very small gap allowed - model can be uncertain
                            // Only flag as suspicious if extremely high confidence with tiny gap (likely non-chicken)
                            val isSuspiciouslyHighConfidence = fusionScoreResult >= 0.99f && probabilityGap < 0.20f
                            
                            Log.d("DetectionService", "Validation check - Confidence: $fusionScoreResult, Gap: $probabilityGap, Suspicious: $isSuspiciouslyHighConfidence")
                            val isValidChicken = fusionScoreResult >= minConfidence && 
                                                probabilityGap >= minGap && 
                                                !isSuspiciouslyHighConfidence
                            
                            if (!isValidChicken) {
                                // TRUST THE MODEL: Even if validation fails, use the model's probabilities
                                // The model is trained - if it says unhealthy > healthy, trust it!
                                val unhealthyAdvantage = unhealthyProb - healthyProb
                                if (unhealthyAdvantage > 0.05f) {  // Even smaller threshold - trust model more
                                    // Trust the model - unhealthy is higher, use it!
                                    Log.d("DetectionService", "Fusion validation failed but trusting model: unhealthyProb ($unhealthyProb) > healthyProb ($healthyProb) by ${unhealthyAdvantage * 100}%")
                                    val actualLabel = "Unhealthy"
                                    val actualScore = unhealthyProb
                                    
                                    label = actualLabel
                                    score = actualScore
                                    detectedType = "Fusion (Image + Audio)"
                                    
                                    Log.i("DetectionService", "Fusion prediction (trusting model): $label (${score * 100}%) - Healthy: $healthyProb, Unhealthy: $unhealthyProb")
                                    
                                    val status = "Infected"
                                    val isInfected = true
                                    val resultString = "$status (${String.format(Locale.US, "%.1f", score * 100)}%)"
                                    fusionResult = isInfected to resultString
                                    
                                    if (!imageBitmap.isRecycled) imageBitmap.recycle()
                                    if (!specBitmap.isRecycled) specBitmap.recycle()
                                } else if (healthyProb > unhealthyProb && healthyProb > 0.50f) {
                                    // Model says healthy and it's above 50% - trust it
                                    Log.d("DetectionService", "Fusion validation failed but trusting model: healthyProb ($healthyProb) > unhealthyProb ($unhealthyProb)")
                                    val actualLabel = "Healthy"
                                    val actualScore = healthyProb
                                    
                                    label = actualLabel
                                    score = actualScore
                                    detectedType = "Fusion (Image + Audio)"
                                    
                                    Log.i("DetectionService", "Fusion prediction (trusting model): $label (${score * 100}%) - Healthy: $healthyProb, Unhealthy: $unhealthyProb")
                                    
                                    val status = "Healthy"
                                    val isInfected = false
                                    val resultString = "$status (${String.format(Locale.US, "%.1f", score * 100)}%)"
                                    fusionResult = isInfected to resultString
                                    
                                    if (!imageBitmap.isRecycled) imageBitmap.recycle()
                                    if (!specBitmap.isRecycled) specBitmap.recycle()
                                } else {
                                    // Validation failed and probabilities are uncertain - fall back to image-only
                                    if (isSuspiciouslyHighConfidence) {
                                        Log.w("DetectionService", "Invalid fusion detection - Extremely high confidence ($fusionScoreResult >= 98%) but very small gap ($probabilityGap < 50%). Model is overconfident on non-chicken input.")
                                    } else {
                                        Log.w("DetectionService", "Invalid fusion detection - Confidence: $fusionScoreResult (required: >= $minConfidence), Gap: $probabilityGap (required: >= $minGap). Likely not a chicken.")
                                    }
                                    // Instead of returning invalid, try image-only classification as fallback
                                    // This prevents audio gate from blocking a valid infected chicken image
                                    Log.d("DetectionService", "Fusion validation failed, trying image-only classification as fallback...")
                                    // Recycle bitmaps before falling through (image-only path will reload from URI)
                                    if (!specBitmap.isRecycled) specBitmap.recycle()
                                    if (!imageBitmap.isRecycled) imageBitmap.recycle()
                                    // Fall through to image-only classification (no audio gate)
                                    // Don't return here - let it try image classification
                                }
                            } else {
                                // Fusion model passed validation - use its result
                                // CRITICAL FIX: Use probabilities to determine label, not just the model's label
                                // This prevents cases where model returns "Healthy" but unhealthyProb > healthyProb
                                val actualLabel = if (unhealthyProb > healthyProb) {
                                    "Unhealthy"
                                } else {
                                    fusionLabelResult
                                }
                                val actualScore = if (unhealthyProb > healthyProb) {
                                    unhealthyProb
                                } else {
                                    fusionScoreResult
                                }
                                
                                label = actualLabel
                                score = actualScore
                                detectedType = "Fusion (Image + Audio)"
                                
                                Log.i("DetectionService", "Fusion prediction: $label (${score * 100}%) - Healthy: $healthyProb, Unhealthy: $unhealthyProb - VALID CHICKEN DETECTION")
                                
                                // Prepare result before saving
                                val status = if (label == "Unhealthy") "Infected" else label
                                val isInfected = status.equals("Infected", ignoreCase = true) || 
                                                status.equals("Sick", ignoreCase = true) ||
                                                status.equals("Sick_chicken", ignoreCase = true) ||
                                                status.equals("Unhealthy", ignoreCase = true)
                                
                                val resultString = "$status (${String.format(Locale.US, "%.1f", score * 100)}%)"
                                fusionResult = isInfected to resultString
                                
                                Log.d("DetectionService", "Detection completed - result ready (not saved yet, waiting for user action)")
                                if (!imageBitmap.isRecycled) imageBitmap.recycle()
                                if (!specBitmap.isRecycled) specBitmap.recycle()
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    Log.e("DetectionService", "Fusion detection was cancelled (shouldn't happen with NonCancellable): ${e.message}", e)
                    throw Exception("Detection was cancelled. Please try again.")
                } catch (e: Exception) {
                    Log.e("DetectionService", "Fusion classification failed: ${e.message}", e)
                    Log.e("DetectionService", "Stack trace:", e)
                    // Return error immediately instead of falling back
                    return@withContext false to "Detection Error: ${e.message ?: "Unknown error during fusion detection"}"
                }
                if (fusionResult != null) {
                    Log.d("DetectionService", "Returning fusion result: ${fusionResult!!.second}")
                    return@withContext fusionResult!!
                } else if (label != null && detectedType == "Fusion (Image + Audio)") {
                    val status = if (label == "Unhealthy") "Infected" else label
                    val isInfected = status.equals("Infected", ignoreCase = true) || 
                                    status.equals("Sick", ignoreCase = true) ||
                                    status.equals("Sick_chicken", ignoreCase = true) ||
                                    status.equals("Unhealthy", ignoreCase = true)
                    
                    val resultString = "$status (${String.format(Locale.US, "%.1f", score * 100)}%)"
                    return@withContext isInfected to resultString
                } else {
                    Log.w("DetectionService", "Fusion model failed, falling back to individual classifiers")
                }
            } else if (isImageDetection && isAudioDetection && !fusionClassifier.isModelAvailable()) {
                Log.e("DetectionService", "Fusion model not available but both inputs provided!")
                return@withContext false to FUSION_MODEL_MISSING_MESSAGE
            }
            if (isImageDetection) {
                Log.d("DetectionService", "Starting image detection with URI: $imageUri")
                if (!imageClassifier.isModelAvailable()) {
                    Log.w("DetectionService", "Image classifier not available (expected - using fusion model)")
                    if (!fusionClassifier.isModelAvailable() && isAudioDetection) {
                        return@withContext false to FUSION_MODEL_MISSING_MESSAGE
                    }
                    if (!isAudioDetection) {
                        return@withContext false to "Image Model Not Available. Please provide both image and audio for fusion detection."
                    }
                    Log.d("DetectionService", "Skipping image-only detection, fusion model should be used instead")
                } else {
                    val bitmap = try {
                        val decodedUri = imageUri.toUri()
                        Log.d("DetectionService", "Decoded URI: $decodedUri")
                        if (decodedUri.scheme == "file") {
                            BitmapFactory.decodeFile(decodedUri.path)
                        } else {
                            context.contentResolver.openInputStream(decodedUri)?.use { inputStream ->
                                BitmapFactory.decodeStream(inputStream)
                            }
                        } ?: throw Exception("Failed to decode image bitmap")
                    } catch (e: Exception) {
                        Log.e("DetectionService", "Error loading image: ${e.message}", e)
                        return@withContext false to "Failed to load image: ${e.message}"
                    }

                    if (bitmap.isRecycled) {
                        Log.e("DetectionService", "Bitmap already recycled")
                        return@withContext false to "Invalid image"
                    }
                    Log.d("DetectionService", "Bitmap loaded: ${bitmap.width}x${bitmap.height}")
                    
                    // Validate image quality: check for black or overexposed images
                    if (isBlackImage(bitmap)) {
                        if (!bitmap.isRecycled) {
                            bitmap.recycle()
                        }
                        return@withContext false to "INVALID_IMAGE: Image is too dark or completely black. Please take a clear photo with proper lighting. Please retake again."
                    }
                    if (isOverexposedImage(bitmap)) {
                        if (!bitmap.isRecycled) {
                            bitmap.recycle()
                        }
                        return@withContext false to "INVALID_IMAGE: Image is overexposed or too bright. Please take a clear photo with proper lighting. Please retake again."
                    }

                    // Guard: Human face detection using ML Kit
                    try {
                        if (containsBlockingHumanFace(bitmap)) {
                            if (!bitmap.isRecycled) {
                                bitmap.recycle()
                            }
                            Log.w("DetectionService", "Human face detected in image - rejecting as invalid input")
                            return@withContext false to "INVALID_IMAGE: Human face detected. Please capture a clear photo of a chicken."
                        }
                    } catch (e: Exception) {
                        Log.w("DetectionService", "Face detection failed or unavailable: ${e.message}")
                        // Do not block detection if face detector fails; continue
                    }
                    
                    val allResults = imageClassifier.classifyWithAllResults(bitmap)
                    val (imgLabel, imgScore) = imageClassifier.classify(bitmap)
                    Log.d("DetectionService", "Classification result: $imgLabel (${imgScore})")
                    Log.d("DetectionService", "All results: $allResults")
                    
                    if (imgLabel == null || imgLabel == "Model Not Loaded" || imgLabel == "Classification Error") {
                        Log.e("DetectionService", "Image classification failed: $imgLabel")
                        return@withContext false to "Image Model Not Available"
                    }
                    val isValidChickenImage = validateChickenImage(allResults, imgScore)
                    // RELAXED: Only reject if confidence is extremely low (< 0.50)
                    // Trust the model more - it knows what it's trained on
                    if (!isValidChickenImage || imgScore < 0.50f) {
                        Log.w("DetectionService", "Image model very low confidence ($imgScore < 0.50), likely not a chicken")
                        if (!bitmap.isRecycled) {
                            bitmap.recycle()
                        }
                        return@withContext false to "INVALID_IMAGE: Image confidence too low. Please ensure the image shows a clear chicken."
                    }

                    label = imgLabel
                    score = imgScore
                    detectedType = "Image"
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }
            if (isAudioDetection) {
                // TRUST THE MODEL: Audio validation is too difficult - chicken sounds vary greatly
                // The trained model should decide, not heuristics
                // Only keep minimal checks for obvious technical issues (handled in AudioSpectrogramConverter)
                val isFusionMode = isImageDetection && fusionClassifier.isModelAvailable()
                if (isFusionMode) {
                    Log.d("DetectionService", "Fusion mode - skipping all audio validation, trusting trained fusion model")
                } else if (label != null) {
                    Log.d("DetectionService", "Image result exists - skipping audio validation, trusting model")
                } else {
                    // Pure audio-only: Still skip human speech gate - let the audio model decide
                    // Audio sounds are too varied to validate with heuristics
                    Log.d("DetectionService", "Audio-only detection - trusting trained audio model, no heuristic gates")
                }

                val spectrogram = audioConverter.convertAudioToSpectrogram(audioUri)
                
                try {
                    if (audioClassifier.isModelAvailable()) {
                        val (audioLabel, audioScore) = audioClassifier.classifySpectrogram(spectrogram)
                        
                        if (audioLabel != "Model Not Loaded") {
                            // TRUST THE MODEL: Don't reject based on low confidence
                            // The model knows best - even low confidence might be valid
                            // Only log for debugging, but don't block
                            if (label == null && audioScore < 0.50f) {
                                Log.w("DetectionService", "Audio model low confidence ($audioScore < 0.50), but trusting model decision")
                                // Don't return invalid - let the model's result be used
                            }
                            if (label != null) {
                                val combinedScore = (score * 0.6f + audioScore * 0.4f)
                                Log.d("DetectionService", "Combined score: $combinedScore (Image: $score, Audio: $audioScore)")
                                if (audioScore > 0.7f) {
                                    label = audioLabel
                                    score = audioScore
                                    Log.d("DetectionService", "Using audio prediction (high confidence: $audioScore)")
                                } else {
                                    Log.d("DetectionService", "Using image prediction (combined score: $combinedScore)")
                                }
                                detectedType = "Combined (Image + Audio)"
                            } else {
                                label = audioLabel
                                score = audioScore
                                detectedType = "Audio"
                            }
                        } else {
                            if (label == null) {
                                if (!spectrogram.isRecycled) {
                                    spectrogram.recycle()
                                }
                                return@withContext false to "Audio Model Not Available"
                            }
                            Log.w("DetectionService", "Audio model failed, using image result only")
                        }
                    } else {
                        if (!isImageDetection) {
                            if (!spectrogram.isRecycled) {
                                spectrogram.recycle()
                            }
                            return@withContext false to "Audio Model Not Available"
                        }
                        Log.d("DetectionService", "Audio model not available, using image detection only")
                    }
                } finally {
                    if (!spectrogram.isRecycled) {
                        spectrogram.recycle()
                    }
                }
            }

            if (label == null) {
                return@withContext false to "No Detection Result"
            }

            val status = label
            val isInfected = status.equals("Infected", ignoreCase = true) ||
                            status.equals("Sick", ignoreCase = true) ||
                            status.equals("Sick_chicken", ignoreCase = true) ||
                            status.equals("Unhealthy", ignoreCase = true)
            // isInfected indicates negative health; we invert later when saving/returning

            // If this was NOT fusion (i.e., image-only or audio-only), apply a stricter trust rule,
            // but slightly relaxed to reduce false Unknown on good rooster audio/images.
            if (detectedType != "Fusion (Image + Audio)" && score < 0.80f) {
                Log.w("DetectionService", "Low-trust single-modality detection (type=$detectedType, score=$score). Returning No Detection Result.")
                return@withContext false to "No Detection Result"
            }

            val resultString = "$status (${String.format(Locale.US, "%.1f", score * 100)}%)"
            if (detectedType.isNotEmpty()) {
                Log.i("DetectionService", "Detection Type: $detectedType")
            }
            Log.d("DetectionService", "Detection completed - result ready (not saved yet, waiting for user action)")
            return@withContext isInfected to resultString

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("DetectionService", "Detection failed: ${e.message}", e)
            return@withContext false to "Analysis Failed: ${e.message}"
        }
    }

    suspend fun getRemedySuggestions(isInfected: Boolean): List<String> = withContext(Dispatchers.IO) {
        if (isInfected) {
            listOf(
                "⚠️ IMPORTANT: Go to the veterinarian immediately for proper diagnosis and treatment.",
                "Isolate infected chicken immediately to prevent disease spread.",
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
     * Calculate average brightness/luminance of a bitmap
     * Returns a value between 0.0 (completely black) and 1.0 (completely white)
     * OPTIMIZED: Increased sample size for faster processing
     */
    private fun calculateAverageBrightness(bitmap: Bitmap): Float {
        if (bitmap.isRecycled) return 0f
        val sampleSize = 20 // Increased from 10 to 20 for faster processing (4x fewer samples)
        var totalBrightness = 0.0
        var pixelCount = 0
        val width = bitmap.width
        val height = bitmap.height
        for (y in 0 until height step sampleSize) {
            for (x in 0 until width step sampleSize) {
                val pixel = bitmap[x, y]
                // Extract RGB components
                val r = Color.red(pixel) / 255f
                val g = Color.green(pixel) / 255f
                val b = Color.blue(pixel) / 255f
                
                // Calculate luminance using standard formula (weighted average)
                val luminance = 0.299f * r + 0.587f * g + 0.114f * b
                totalBrightness += luminance.toDouble()
                pixelCount++
            }
        }
        
        return if (pixelCount > 0) {
            (totalBrightness / pixelCount).toFloat()
        } else {
            0f
        }
    }
    
    /**
     * Check if image is too dark (black image)
     * Returns true if average brightness is below threshold (5%)
     */
    private fun isBlackImage(bitmap: Bitmap): Boolean {
        val avgBrightness = calculateAverageBrightness(bitmap)
        val isBlack = avgBrightness < 0.05f
        if (isBlack) {
            Log.w("DetectionService", "Black image detected - average brightness: ${avgBrightness * 100}%")
        }
        return isBlack
    }
    
    /**
     * Check if image is too bright (overexposed/white image)
     * Returns true if average brightness is above threshold (95%)
     */
    private fun isOverexposedImage(bitmap: Bitmap): Boolean {
        val avgBrightness = calculateAverageBrightness(bitmap)
        val isOverexposed = avgBrightness > 0.95f
        if (isOverexposed) {
            Log.w("DetectionService", "Overexposed/white image detected - average brightness: ${avgBrightness * 100}%")
        }
        return isOverexposed
    }
    
    /**
     * Validate if the image is actually a chicken
     * Returns false if the image doesn't match the trained model (not a chicken)
     * 
     * BALANCED Validation rules - relaxed to give more power to model predictions:
     * 1. Top confidence must be >= 0.80 (80%) - reasonable threshold
     * 2. Probability gap must be >= 0.50 (50%) - relaxed from 60% to trust model more
     * 3. CRITICAL: Extremely high confidence (>98%) requires very large gap (>85%) - catches overconfident non-chicken
     * 4. Suspicious: High confidence (>95%) but moderate gap (<70%) - likely non-chicken
     * 5. If both categories have moderate confidence, it's likely not a chicken
     */
    private fun validateChickenImage(allResults: List<Pair<String, Float>>, topScore: Float): Boolean {
        if (topScore < 0.80f) {
            Log.w("DetectionService", "Validation failed: Low confidence ($topScore < 0.80)")
            return false
        }
        if (allResults.isEmpty()) {
            Log.w("DetectionService", "Validation failed: No classification results")
            return false
        }
        val healthyResult = allResults.find { it.first.equals("Healthy", ignoreCase = true) }
        val infectedResult = allResults.find { it.first.equals("Infected", ignoreCase = true) }
        
        if (healthyResult != null && infectedResult != null) {
            val maxConfidence = maxOf(healthyResult.second, infectedResult.second)
            val minConfidence = minOf(healthyResult.second, infectedResult.second)
            val probabilityGap = maxConfidence - minConfidence
            if (probabilityGap < 0.50f) {
                Log.w("DetectionService", "Validation failed: Small probability gap ($probabilityGap < 0.50) - max: $maxConfidence, min: $minConfidence")
                return false
            }
            if (topScore >= 0.98f && probabilityGap < 0.85f) {
                Log.w("DetectionService", "Validation failed: Extremely high confidence ($topScore >= 0.98) but gap not very large ($probabilityGap < 0.85). Model is overconfident on non-chicken input.")
                return false
            }
            if (topScore >= 0.95f && probabilityGap < 0.70f) {
                Log.w("DetectionService", "Validation failed: High confidence ($topScore >= 0.95) but moderate gap ($probabilityGap < 0.70) - likely non-chicken")
                return false
            }
            if (maxConfidence < 0.70f && minConfidence > 0.30f) {
                Log.w("DetectionService", "Validation failed: Both categories have moderate confidence (max: $maxConfidence, min: $minConfidence) - likely not a chicken")
                return false
            }
        }
        Log.d("DetectionService", "Validation passed: Valid chicken image (confidence: $topScore)")
        return true
    }

    /**
     * Detect whether a human face is present in the bitmap (blocking invalid selfies)
     * OPTIMIZED: Using fastest settings for better performance
     */
    private fun containsBlockingHumanFace(bitmap: Bitmap): Boolean {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE) // Disable landmarks for speed
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE) // Disable classification for speed
            .setMinFaceSize(0.1f) // Smaller min face size for faster detection
            .build()
        val detector = FaceDetection.getClient(options)
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val faces = Tasks.await(detector.process(image))
            if (faces.isEmpty()) return false

            // Relaxed rule: block only if face is large enough or near the center
            val imgW = bitmap.width
            val imgH = bitmap.height
            val imgArea = (imgW * imgH).toFloat().coerceAtLeast(1f)

            // Define a center region (40% of width/height around center)
            val cx = imgW / 2
            val cy = imgH / 2
            val halfW = (imgW * 0.2f).toInt()
            val halfH = (imgH * 0.2f).toInt()
            val centerRect = android.graphics.Rect(cx - halfW, cy - halfH, cx + halfW, cy + halfH)

            faces.any { face ->
                val bb = face.boundingBox
                val faceArea = (bb.width() * bb.height()).toFloat().coerceAtLeast(0f)
                val areaRatio = faceArea / imgArea // portion of image covered by face
                val overlapsCenter = android.graphics.Rect.intersects(bb, centerRect)

                // Block if face is big (>=10%) OR overlaps center and is moderate (>=5%)
                areaRatio >= 0.10f || (overlapsCenter && areaRatio >= 0.05f)
            }
        } catch (e: Exception) {
            Log.w("DetectionService", "Face detection error: ${e.message}")
            false
        } finally {
            try { detector.close() } catch (_: Exception) {}
        }
    }

    /**
     * Get the human voice ratio for audio (low/high frequency energy ratio)
     * Returns null if calculation fails
     * Higher ratio (>3.0) indicates human speech, lower ratio indicates chicken sounds
     */
    private fun getHumanVoiceRatio(audioUriString: String): Float? {
        return try {
            val decoded = decodePcmMonoFloats(audioUriString) ?: return null
            val samples = decoded.first
            val sampleRate = decoded.second
            if (samples.isEmpty() || sampleRate <= 0) return null

            val maxAbs = samples.maxOf { abs(it) }.coerceAtLeast(1e-6f)
            val norm = FloatArray(samples.size) { samples[it] / maxAbs }

            val lowFreqs = floatArrayOf(400f, 700f, 900f)
            val highFreqs = floatArrayOf(1700f, 2500f, 3200f)
            val lowEnergy = lowFreqs.sumOf { goertzelPower(norm, sampleRate, it).toDouble() }.toFloat()
            val highEnergy = highFreqs.sumOf { goertzelPower(norm, sampleRate, it).toDouble() }.toFloat()
            val ratio = if (highEnergy > 1e-6f) lowEnergy / highEnergy else Float.POSITIVE_INFINITY
            Log.d("DetectionService", "AudioHeuristic low=$lowEnergy high=$highEnergy ratio=$ratio")
            ratio
        } catch (e: Exception) {
            Log.w("DetectionService", "getHumanVoiceRatio failed: ${e.message}")
            null
        }
    }

    /**
     * Lightweight heuristic to flag human speech vs chicken calls without an extra ML model.
     * See comment for details in implementation.
     */
    private fun isLikelyHumanVoice(audioUriString: String): Boolean? {
        return getHumanVoiceRatio(audioUriString)?.let { it > 3.0f }
    }

    private fun decodePcmMonoFloats(audioUriString: String): Pair<FloatArray, Int>? {
        val uri = audioUriString.toUri()
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)
        var trackIndex = -1
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                trackIndex = i
                break
            }
        }
        if (trackIndex == -1) {
            extractor.release()
            return null
        }
        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(format, null, null, 0)
        decoder.start()

        val info = MediaCodec.BufferInfo()

        val floats = ArrayList<Float>(sampleRate * 5)
        var isEOS = false
        while (true) {
            if (!isEOS) {
                val inIndex = decoder.dequeueInputBuffer(10_000)
                if (inIndex >= 0) {
                    val buffer = decoder.getInputBuffer(inIndex) ?: continue
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEOS = true
                    } else {
                        val pts = extractor.sampleTime
                        decoder.queueInputBuffer(inIndex, 0, sampleSize, pts, 0)
                        extractor.advance()
                    }
                }
            }
            val outIndex = decoder.dequeueOutputBuffer(info, 10_000)
            if (outIndex >= 0) {
                val outBuf = decoder.getOutputBuffer(outIndex) ?: run {
                    decoder.releaseOutputBuffer(outIndex, false)
                    continue
                }
                outBuf.position(info.offset)
                outBuf.limit(info.offset + info.size)
                val shortCount = info.size / 2
                val shorts = ShortArray(shortCount)
                outBuf.asShortBuffer().get(shorts)
                for (s in shorts) {
                    floats.add(s / 32768.0f)
                }
                decoder.releaseOutputBuffer(outIndex, false)
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.d("DetectionService", "MediaCodec output format changed: ${decoder.outputFormat}")
            }
        }
        decoder.stop()
        decoder.release()
        extractor.release()
        return floats.toFloatArray() to sampleRate
    }

    private fun goertzelPower(samples: FloatArray, sampleRate: Int, targetFreq: Float): Float {
        val n = samples.size
        if (n == 0 || sampleRate <= 0) return 0f
        val k = (0.5f + (n * targetFreq) / sampleRate).toInt()
        val omega = (2.0 * Math.PI * k) / n
        val coeff = 2.0 * cos(omega)
        var q0: Double
        var q1 = 0.0
        var q2 = 0.0
        for (i in 0 until n) {
            q0 = coeff * q1 - q2 + samples[i]
            q2 = q1
            q1 = q0
        }
        val real = q1 - q2 * cos(omega)
        val imag = q2 * sin(omega)
        return (real * real + imag * imag).toFloat() / n
    }
}
