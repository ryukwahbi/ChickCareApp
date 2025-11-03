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

class AudioClassifier(
    private val context: Context,
    private val modelPath: String = "audio_model.tflite", 
    private val maxResults: Int = 2
) {
    private lateinit var classifier: ImageClassifier
    private var isModelLoaded = false

    init {
        setupClassifier()
    }

    private fun setupClassifier() {
        val baseOptions = BaseOptions.builder()
            .setNumThreads(4)
            .build()

        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setBaseOptions(baseOptions)
            .setMaxResults(maxResults)
            .build()

        try {
            // Try to load model from assets folder
            val modelBuffer = loadModelFromAssets(context, modelPath)
            
            if (modelBuffer != null) {
                classifier = ImageClassifier.createFromBufferAndOptions(modelBuffer, options)
                isModelLoaded = true
                android.util.Log.i("AudioClassifier", "Audio model loaded successfully from assets")
            } else {
                android.util.Log.w("AudioClassifier", "Audio model file not found in assets: $modelPath")
                isModelLoaded = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isModelLoaded = false
            android.util.Log.e("AudioClassifier", "Failed to load audio model: ${e.message}", e)
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
            android.util.Log.d("AudioClassifier", "Audio model not found in assets (this is okay if audio model doesn't exist): ${e.message}")
            null
        }
    }

    fun classifySpectrogram(bitmap: Bitmap): Pair<String?, Float> {
        if (!isModelLoaded || !::classifier.isInitialized) {
            android.util.Log.e("AudioClassifier", "Classifier not loaded")
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
                    android.util.Log.d("AudioClassifier", "Audio classification: $friendlyLabel (${category.score})")
                    friendlyLabel to category.score
                } ?: ("Unknown" to 0.0f)
            } else {
                "Unknown" to 0.0f
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioClassifier", "Error during audio classification: ${e.message}", e)
            return "Classification Error" to 0.0f
        }
    }

    private fun mapLabelToFriendlyName(label: String): String {
        return when (label.lowercase()) {
            "healthy_spectrogram", "healthy" -> "Healthy"
            "unhealthy_spectrogram", "unhealthy", "infected" -> "Infected"
            else -> label
        }
    }

    fun isModelAvailable(): Boolean = isModelLoaded
}

