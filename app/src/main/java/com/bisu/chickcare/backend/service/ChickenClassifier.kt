package com.bisu.chickcare.backend.service

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.add
import androidx.privacysandbox.tools.core.generator.build
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import kotlin.reflect.KMutableProperty0

private val KMutableProperty0<ImageClassifier>.isInitialized: Boolean

class ChickenClassifier(
    private val context: Context,
    private val modelPath: String = "model.tflite", // Should match the filename in assets
    private val maxResults: Int = 2 // Number of classes (e.g., Healthy, Infected)
) {
    private lateinit var classifier: ImageClassifier

    init {
        setupClassifier()
    }

    private fun setupClassifier() {
        val baseOptions = BaseOptions.builder().setNumThreads(4).build()
        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setBaseOptions(baseOptions)
            .setMaxResults(maxResults)
            .build()

        try {
            classifier = ImageClassifier.createFromFileAndOptions(context, modelPath, options)
        } catch (e: Exception) {
            // Handle model loading errors, e.g., file not found
            e.printStackTrace()
        }
    }

    fun classify(bitmap: Bitmap): Pair<String, Float> {
        if (!::classifier.isInitialized) {
            return "Uninitialized" to 0.0f
        }

        // IMPORTANT: The image dimensions (224, 224) must match your model's input size.
        // Your Colab output confirms your model expects 224x224 images.
        val imageProcessor = ImageProcessor.builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))
        val results = classifier.classify(tensorImage)
        val topResult = results.firstOrNull()?.categories?.firstOrNull()

        return if (topResult != null) {
            // Return the label (e.g., "Infected") and its confidence score
            topResult.label to topResult.score
        } else {
            // Return a default value if no classification was made
            "Unknown" to 0.0f
        }
    }
}
    