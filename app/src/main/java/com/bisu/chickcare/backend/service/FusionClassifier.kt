package com.bisu.chickcare.backend.service

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import androidx.core.graphics.scale
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Fusion classifier that takes both image and spectrogram as inputs
 * Model expects:
 * - Input 0: Image (1, 224, 224, 3) - float32, normalized 0-1
 * - Input 1: Spectrogram (1, 224, 224, 3) - float32, normalized 0-1
 * - Output: (1, 2) - probabilities [Healthy, Unhealthy]
 */
class FusionClassifier(
    private val context: Context,
    modelPathOverride: String? = null,
) {
    companion object {
        const val PRIMARY_MODEL_PATH = "cnnmlp_fusion_v2.tflite"
    }

    private var interpreter: Interpreter? = null
    private var isModelLoaded = false
    private var inputImageShape: IntArray? = null
    private var inputSpecShape: IntArray? = null
    private var outputShape: IntArray? = null
    private var activeModelPath: String? = null

    private val candidateModelPaths: List<String> = listOf(modelPathOverride ?: PRIMARY_MODEL_PATH)

    init {
        setupInterpreter()
    }

    private fun setupInterpreter() {
        var lastException: Exception? = null

        candidateModelPaths.forEachIndexed { index, path ->
            try {
                val modelBuffer = loadModelFromAssets(context, path)

                if (modelBuffer != null) {
                    val options = Interpreter.Options().apply {
                        setNumThreads(4)
                        setUseXNNPACK(true) // Enable optimized kernels
                    }

                    try {
                        interpreter = Interpreter(modelBuffer, options)

                        // Get input/output details
                        val inputDetails = interpreter?.inputTensorCount ?: 0
                        val outputDetails = interpreter?.outputTensorCount ?: 0

                        if (inputDetails >= 2) {
                            // Get input shapes
                            inputImageShape = interpreter?.getInputTensor(0)?.shape()
                            inputSpecShape = interpreter?.getInputTensor(1)?.shape()
                            outputShape = interpreter?.getOutputTensor(0)?.shape()

                            activeModelPath = path
                            android.util.Log.i("FusionClassifier", "Fusion model loaded successfully from $path")
                            android.util.Log.d("FusionClassifier", "Input count: $inputDetails, Output count: $outputDetails")
                            android.util.Log.d("FusionClassifier", "Input 0 (Image) shape: ${inputImageShape?.contentToString()}")
                            android.util.Log.d("FusionClassifier", "Input 1 (Spectrogram) shape: ${inputSpecShape?.contentToString()}")
                            android.util.Log.d("FusionClassifier", "Output shape: ${outputShape?.contentToString()}")

                            isModelLoaded = true
                            return
                        } else {
                            android.util.Log.e("FusionClassifier", "Model at $path does not have 2 inputs (found $inputDetails)")
                            isModelLoaded = false
                        }
                    } catch (e: IllegalArgumentException) {
                        val errorMsg = e.message ?: ""
                        if (errorMsg.contains("FULLY_CONNECTED") || errorMsg.contains("opcode")) {
                            android.util.Log.e("FusionClassifier", "Model opcode version mismatch for $path: ${e.message}")
                            android.util.Log.e("FusionClassifier", "This usually means the TFLite runtime version doesn't match the model. Please ensure TFLite 2.16.1+ is in build.gradle")
                            android.util.Log.e("FusionClassifier", "Stack trace:", e)
                        } else {
                            android.util.Log.e("FusionClassifier", "Failed to create interpreter for $path: ${e.message}", e)
                        }
                        lastException = e
                    } catch (e: Exception) {
                        android.util.Log.e("FusionClassifier", "Failed to create interpreter for $path: ${e.message}", e)
                        lastException = e
                    }
                } else {
                    android.util.Log.w("FusionClassifier", "Model buffer for $path was null (file missing?)")
                }
            } catch (e: Exception) {
                android.util.Log.w("FusionClassifier", "Failed to load model from $path: ${e.message}")
                lastException = e
            }
        }

        android.util.Log.e(
            "FusionClassifier",
            "Failed to setup fusion classifier with any candidate model. Checked paths: ${candidateModelPaths.joinToString()}",
            lastException
        )
        isModelLoaded = false
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
            android.util.Log.e("FusionClassifier", "Error loading model from assets ($modelPath): ${e.message}")
            null
        }
    }

    /**
     * Convert Bitmap to normalized float32 ByteBuffer (224x224x3)
     */
    private fun bitmapToByteBuffer(bitmap: Bitmap, width: Int = 224, height: Int = 224): ByteBuffer {
        // Resize bitmap to target size if needed
        val needsResize = bitmap.width != width || bitmap.height != height
        @Suppress("DEPRECATION") // createScaledBitmap is standard Android API
        val resizedBitmap = if (needsResize) {
            bitmap.scale(width, height)
        } else {
            bitmap
        }

        // Allocate ByteBuffer: batch_size * height * width * channels * 4 bytes (float32)
        val byteBuffer = ByteBuffer.allocateDirect(1 * height * width * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())

        // Extract pixels and normalize to 0-1 range
        val intValues = IntArray(width * height)
        resizedBitmap.getPixels(intValues, 0, width, 0, 0, width, height)
        
        // Recycle resized bitmap if we created one (prevents memory leak)
        if (needsResize && resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }

        var pixelIndex = 0
        // Iterate through all pixels row by row
        repeat(height) {
            repeat(width) {
                val pixel = intValues[pixelIndex++]
                
                // Extract RGB and normalize to [0, 1]
                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f
                
                byteBuffer.putFloat(r)
                byteBuffer.putFloat(g)
                byteBuffer.putFloat(b)
            }
        }

        return byteBuffer
    }

    /**
     * Classify using both image and spectrogram inputs
     * Returns: (label, confidence) where label is "Healthy" or "Unhealthy"
     */
    fun classifyFusion(imageBitmap: Bitmap, specBitmap: Bitmap): Pair<String, Float> {
        if (!isModelLoaded || interpreter == null) {
            android.util.Log.e("FusionClassifier", "Model not loaded")
            return "Model Not Loaded" to 0.0f
        }

        try {
            android.util.Log.d("FusionClassifier", "Preparing inputs for fusion classification...")
            // Prepare inputs
            val imageBuffer = bitmapToByteBuffer(imageBitmap)
            android.util.Log.d("FusionClassifier", "Image buffer prepared: ${imageBuffer.capacity()} bytes")
            val specBuffer = bitmapToByteBuffer(specBitmap)
            android.util.Log.d("FusionClassifier", "Spectrogram buffer prepared: ${specBuffer.capacity()} bytes")
            
            // Prepare output buffer: [1, 2] for probabilities [Healthy, Unhealthy]
            val outputBuffer = ByteBuffer.allocateDirect(1 * 2 * 4) // 2 floats
            outputBuffer.order(ByteOrder.nativeOrder())
            android.util.Log.d("FusionClassifier", "Output buffer prepared: ${outputBuffer.capacity()} bytes")

            // Prepare input array
            val inputs = arrayOf(imageBuffer, specBuffer)
            @Suppress("UNCHECKED_CAST") // ByteBuffer is compatible with Any for TensorFlow Lite
            val outputs = (hashMapOf<Int, ByteBuffer>().apply {
                put(0, outputBuffer)
            } as MutableMap<Int, Any>)

            android.util.Log.d("FusionClassifier", "Running model inference...")
            // Run inference
            interpreter?.runForMultipleInputsOutputs(inputs, outputs)
            android.util.Log.d("FusionClassifier", "Model inference completed")

            // Extract probabilities
            outputBuffer.rewind()
            val prob0 = outputBuffer.float  // First output (could be Healthy or Unhealthy)
            val prob1 = outputBuffer.float  // Second output

            android.util.Log.d("FusionClassifier", "Raw probabilities - Index 0: $prob0, Index 1: $prob1")
            
            // According to the exported model metadata, output order is [Unhealthy, Healthy]
            // However, earlier revisions mistakenly assumed the opposite which caused healthy
            // samples to be flagged as infected. To keep behaviour consistent with the trained
            // model, map index 1 → Healthy, index 0 → Unhealthy.
            val unhealthyProb = prob0  // Index 0 corresponds to the unhealthy class
            val healthyProb = prob1    // Index 1 corresponds to the healthy class
            
            android.util.Log.d("FusionClassifier", "Interpreted probabilities - Healthy (index1): $healthyProb, Unhealthy (index0): $unhealthyProb")

            // Determine prediction using correct mapping
            val (label, confidence) = if (healthyProb >= unhealthyProb) {
                "Healthy" to healthyProb
            } else {
                "Unhealthy" to unhealthyProb
            }

            android.util.Log.i("FusionClassifier", "Fusion prediction: $label (${confidence * 100}%)")
            return label to confidence

        } catch (e: Exception) {
            android.util.Log.e("FusionClassifier", "Error during fusion classification: ${e.message}", e)
            return "Classification Error" to 0.0f
        }
    }

    /**
     * Check if model is loaded and available
     */
    fun isModelAvailable(): Boolean = isModelLoaded

    @Suppress("unused")
    fun close() {
        interpreter?.close()
        interpreter = null
        isModelLoaded = false
    }

    fun getActiveModelPath(): String? = activeModelPath
}

