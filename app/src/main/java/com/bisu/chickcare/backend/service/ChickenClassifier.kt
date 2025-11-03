package com.bisu.chickcare.backend.service

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ChickenClassifier(
    private val context: Context,
    private val modelPath: String = "model.tflite",
    private val maxResults: Int = 2
) {
    private lateinit var classifier: ImageClassifier
    private var isModelLoaded = false

    init {
        setupClassifier()
    }

    private fun setupClassifier() {
        try {
            try {
                context.assets.open(modelPath).close()
            } catch (_: IOException) {
                android.util.Log.d("ChickenClassifier", "Model file '$modelPath' not found in assets (this is expected if using fusion model only)")
                isModelLoaded = false
                return
            }

            val baseOptions = BaseOptions.builder()
                .setNumThreads(4)
                .build()

            val options = ImageClassifier.ImageClassifierOptions.builder()
                .setBaseOptions(baseOptions)
                .setMaxResults(maxResults)
                .build()

            // Load model from assets folder
            val modelBuffer = loadModelFromAssets(context, modelPath)
            
            if (modelBuffer != null) {
                try {
                    classifier = ImageClassifier.createFromBufferAndOptions(modelBuffer, options)
                    isModelLoaded = true
                    android.util.Log.i("ChickenClassifier", "Image model loaded successfully from assets")
                } catch (e: IllegalArgumentException) {
                    // Handle model compatibility issues (e.g., unsupported ops)
                    android.util.Log.w("ChickenClassifier", "Model compatibility error: ${e.message}")
                    android.util.Log.d("ChickenClassifier", "This is expected if the model uses operations not supported by the current runtime")
                    isModelLoaded = false
                } catch (e: IllegalStateException) {
                    // Handle native library loading issues
                    android.util.Log.w("ChickenClassifier", "Native library error: ${e.message}")
                    isModelLoaded = false
                }
            } else {
                android.util.Log.d("ChickenClassifier", "Model buffer is null - model not available (expected if using fusion model)")
                isModelLoaded = false
            }
        } catch (e: Exception) {
            android.util.Log.d("ChickenClassifier", "Image classifier not available (this is expected if using fusion model only): ${e.message}")
            isModelLoaded = false
        }
    }

    /**
     * Load model from assets folder as MappedByteBuffer
     */
    private fun loadModelFromAssets(context: Context, modelPath: String): MappedByteBuffer? {
        return try {
            val assetManager = context.assets
            val fileDescriptor: AssetFileDescriptor = assetManager.openFd(modelPath)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: IOException) {
            android.util.Log.e("ChickenClassifier", "Error loading model from assets: ${e.message}", e)
            null
        }
    }

    /**
     * Classify image and return top result with all categories for validation
     */
    fun classify(bitmap: Bitmap): Pair<String?, Float> {
        if (!isModelLoaded || !::classifier.isInitialized) {
            android.util.Log.e("ChickenClassifier", "Classifier not loaded or initialized")
            return "Model Not Loaded" to 0.0f
        }

        try {
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                .build()

            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))
            val results = classifier.classify(tensorImage)
            val topResult = results.firstOrNull()

            return if (topResult != null) {
                topResult.categories.firstOrNull()?.let { category ->
                    // Map labels to user-friendly names
                    val friendlyLabel = mapLabelToFriendlyName(category.label)
                    android.util.Log.d("ChickenClassifier", "Classification: $friendlyLabel (${category.score})")
                    friendlyLabel to category.score
                } ?: ("Unknown" to 0.0f)
            } else {
                "Unknown" to 0.0f
            }
        } catch (e: Exception) {
            android.util.Log.e("ChickenClassifier", "Error during classification: ${e.message}", e)
            return "Classification Error" to 0.0f
        }
    }

    /**
     * Get all classification results for validation
     * Returns list of (label, confidence) pairs
     */
    fun classifyWithAllResults(bitmap: Bitmap): List<Pair<String, Float>> {
        if (!isModelLoaded || !::classifier.isInitialized) {
            return emptyList()
        }

        try {
            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                .build()

            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))
            val results = classifier.classify(tensorImage)
            
            return results.flatMap { result ->
                result.categories.map { category ->
                    val friendlyLabel = mapLabelToFriendlyName(category.label)
                    friendlyLabel to category.score
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ChickenClassifier", "Error during classification: ${e.message}", e)
            return emptyList()
        }
    }

    private fun mapLabelToFriendlyName(label: String): String {
        return when (label.lowercase()) {
            "healthy", "healthy_chicken", "normal" -> "Healthy"
            "infected", "sick", "sick_chicken", "unhealthy" -> "Infected"
            else -> label
        }
    }

    fun isModelAvailable(): Boolean = isModelLoaded
}
