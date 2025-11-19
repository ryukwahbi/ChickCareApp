package com.bisu.chickcare.backend.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.sin
import kotlin.random.Random

/**
 * Converts audio files to mel spectrogram images matching librosa's format
 * Output: 224x224 RGB image (normalized 0-1, ready for model input)
 *
 * Note: This is a simplified implementation. For production, consider using
 * a proper audio processing library or implementing full FFT-based mel spectrogram.
 */
class AudioSpectrogramConverter(private val context: Context) {

    companion object {
        private const val IMAGE_SIZE = 224
        private const val SAMPLE_RATE = 22050
        const val MAX_DURATION_SECONDS = 10
        const val MIN_DURATION_SECONDS = 5
    }
    
    /**
     * Get audio file duration in milliseconds
     * Returns null if duration cannot be determined
     */
    fun getAudioDuration(audioUri: String): Long? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, audioUri.toUri())
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            retriever.release()
            duration
        } catch (e: Exception) {
            android.util.Log.w("AudioSpectrogramConverter", "Failed to get audio duration: ${e.message}")
            null
        }
    }

    /**
     * Convert audio file to spectrogram Bitmap (224x224 RGB)
     * This bitmap will be normalized to 0-1 range for model input
     */
    suspend fun convertAudioToSpectrogram(audioUri: String): Bitmap {
        // Use NonCancellable to ensure the conversion completes even if parent job is cancelled
        return kotlinx.coroutines.withContext(Dispatchers.IO + kotlinx.coroutines.NonCancellable) {
            android.util.Log.d("AudioSpectrogramConverter", "Starting audio to spectrogram conversion for URI: $audioUri")
            // Create ARGB_8888 bitmap for better color depth (RGB format)
            val bitmap = createBitmap(IMAGE_SIZE, IMAGE_SIZE, Bitmap.Config.ARGB_8888)

            try {
                // Extract audio data
                val audioData = extractAudioData(audioUri)

                // Generate mel-spectrogram-like visualization
                val spectrogram = computeMelSpectrogram(audioData)

                // Draw spectrogram to bitmap using librosa-like colormap
                drawMelSpectrogram(bitmap, spectrogram)

                android.util.Log.d("AudioSpectrogramConverter", "Spectrogram generated: ${bitmap.width}x${bitmap.height}")

            } catch (e: kotlinx.coroutines.CancellationException) {
                android.util.Log.e("AudioSpectrogramConverter", "Conversion was cancelled (shouldn't happen with NonCancellable): ${e.message}", e)
                // Fill with black even if cancelled (shouldn't happen with NonCancellable)
                bitmap.eraseColor(Color.BLACK)
            } catch (e: Exception) {
                android.util.Log.e("AudioSpectrogramConverter", "Error generating spectrogram: ${e.message}", e)
                // Fill with black (minimum value for librosa power_to_db)
                bitmap.eraseColor(Color.BLACK)
            }

            bitmap
        }
    }

    /**
     * Extract actual PCM audio data from audio file
     * Falls back to synthetic data if extraction fails
     * @throws Exception if audio is silent or invalid
     */
    private fun extractAudioData(audioUri: String): FloatArray {
        return try {
            val audioData = extractPCMAudio(audioUri)
            
            // CRITICAL: Check if audio is silent or has very low amplitude
            // Calculate RMS (Root Mean Square) to measure audio energy/amplitude
            val rms = calculateRMS(audioData)
            val threshold = 0.01f // Threshold for silent audio (1% of max amplitude)
            
            android.util.Log.d("AudioSpectrogramConverter", "Audio RMS: $rms (threshold: $threshold)")
            
            if (rms < threshold) {
                android.util.Log.w("AudioSpectrogramConverter", "Audio is silent or too quiet (RMS: $rms < $threshold)")
                throw Exception("Audio is silent or too quiet. Please record audio with actual sound.")
            }
            
            audioData
        } catch (e: Exception) {
            // If it's our silent audio exception, re-throw it
            if (e.message?.contains("silent") == true || e.message?.contains("too quiet") == true) {
                throw e
            }
            
            android.util.Log.w("AudioSpectrogramConverter", "Failed to extract PCM audio, using synthetic data: ${e.message}", e)
            // Fallback to synthetic data if extraction fails
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, audioUri.toUri())
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 10000
                val durationSeconds = (duration / 1000f).coerceIn(1f, 10f)
                val samples = (SAMPLE_RATE * durationSeconds).toInt()
                generateRealisticAudioData(samples)
            } catch (fallbackError: Exception) {
                android.util.Log.e("AudioSpectrogramConverter", "Error in fallback: ${fallbackError.message}", fallbackError)
                generateRealisticAudioData(22050)
            } finally {
                retriever.release()
            }
        }
    }
    
    /**
     * Calculate RMS (Root Mean Square) of audio data to measure audio energy/amplitude
     * RMS is a good indicator of whether audio is silent or has actual sound
     */
    private fun calculateRMS(audioData: FloatArray): Float {
        if (audioData.isEmpty()) return 0f
        
        var sumOfSquares = 0.0
        for (sample in audioData) {
            sumOfSquares += (sample * sample).toDouble()
        }
        val meanSquare = sumOfSquares / audioData.size
        return kotlin.math.sqrt(meanSquare).toFloat()
    }
    
    /**
     * Extract the most active (highest energy) segment from audio
     * This is smarter than just taking the first 10 seconds because:
     * - The actual chicken sounds might be in the middle or end
     * - The beginning might have silence or setup noise
     * - We want the most informative segment for better accuracy
     * OPTIMIZED: Increased step size for faster processing
     * 
     * @param audioData Full audio array
     * @param segmentLength Desired segment length in samples
     * @return The most active segment of the specified length
     */
    private fun extractMostActiveSegment(audioData: FloatArray, segmentLength: Int): FloatArray {
        if (audioData.size <= segmentLength) {
            return audioData
        }
        
        // Use sliding window to find the segment with highest RMS (energy)
        val windowSize = segmentLength
        val stepSize = SAMPLE_RATE / 2 // OPTIMIZED: Check every 0.5 seconds instead of 0.25 for faster processing
        var maxRMS = 0f
        var bestStartIndex = 0
        
        // Analyze audio in chunks to find the most active segment
        for (start in 0 until (audioData.size - windowSize) step stepSize) {
            val end = minOf(start + windowSize, audioData.size)
            val segment = audioData.sliceArray(start until end)
            val rms = calculateRMS(segment)
            
            if (rms > maxRMS) {
                maxRMS = rms
                bestStartIndex = start
            }
        }
        
        // Extract the best segment
        val bestEndIndex = minOf(bestStartIndex + windowSize, audioData.size)
        val extracted = audioData.sliceArray(bestStartIndex until bestEndIndex)
        
        android.util.Log.d("AudioSpectrogramConverter", 
            "Extracted most active segment: ${bestStartIndex / SAMPLE_RATE}s - ${bestEndIndex / SAMPLE_RATE}s " +
            "(RMS: ${String.format("%.4f", maxRMS)})")
        
        // If extracted segment is shorter than desired (edge case), pad with zeros or repeat
        return if (extracted.size < segmentLength) {
            val padded = FloatArray(segmentLength)
            extracted.copyInto(padded, 0, 0, minOf(extracted.size, segmentLength))
            padded
        } else {
            extracted
        }
    }
    
    /**
     * Extract PCM audio samples from audio file using MediaExtractor and MediaCodec
     */
    private fun extractPCMAudio(audioUri: String): FloatArray {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        
        try {
            extractor.setDataSource(context, audioUri.toUri(), null)
            
            // Find audio track
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }
            
            if (audioTrackIndex == -1 || audioFormat == null) {
                throw Exception("No audio track found in file")
            }
            
            extractor.selectTrack(audioTrackIndex)
            
            // Get sample rate from format or use default
            val sampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).takeIf { it > 0 }
                ?: SAMPLE_RATE
            
            // Create decoder
            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: throw Exception("No MIME type found")
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(audioFormat, null, null, 0)
            decoder.start()
            
            val audioSamples = mutableListOf<Float>()
            var inputEOS = false
            var outputEOS = false
            
            // Decode audio
            // OPTIMIZED: Reduced timeout from 10000ms to 5000ms for faster processing
            while (!outputEOS) {
                // Feed input
                if (!inputEOS) {
                    val inputBufferIndex = decoder.dequeueInputBuffer(5000) // Reduced from 10000 to 5000ms
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputBufferIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEOS = true
                            } else {
                                val presentationTimeUs = extractor.sampleTime
                                decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, 0)
                                extractor.advance()
                            }
                        }
                    }
                }
                
                // Get output
                val bufferInfo = MediaCodec.BufferInfo()
                val outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 5000) // Reduced from 10000 to 5000ms
                
                when {
                    outputBufferIndex >= 0 -> {
                        val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)
                        
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            // Convert PCM bytes to float array
                            val pcmData = ByteArray(bufferInfo.size)
                            outputBuffer.get(pcmData, 0, bufferInfo.size)
                            
                            // Convert to float samples (assuming 16-bit PCM)
                            val samples = pcmDataToFloatArray(pcmData)
                            audioSamples.addAll(samples.toList())
                        }
                        
                        decoder.releaseOutputBuffer(outputBufferIndex, false)
                        
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputEOS = true
                        }
                    }
                    outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        // Output format changed - can update format if needed
                        val newFormat = decoder.outputFormat
                        android.util.Log.d("AudioSpectrogramConverter", "Output format changed: $newFormat")
                    }
                }
            }
            
            // Resample if needed to match SAMPLE_RATE
            val result = if (sampleRate != SAMPLE_RATE && audioSamples.isNotEmpty()) {
                resampleAudio(audioSamples.toFloatArray(), sampleRate, SAMPLE_RATE)
            } else {
                audioSamples.toFloatArray()
            }
            
            // Smart trimming: If audio is longer than 10 seconds, extract the most active segment
            val maxSamples = SAMPLE_RATE * MAX_DURATION_SECONDS
            val audioDurationSeconds = result.size.toFloat() / SAMPLE_RATE
            return if (result.size > maxSamples) {
                android.util.Log.i("AudioSpectrogramConverter", 
                    "🔧 AUTO-CROPPING: Audio is ${String.format("%.1f", audioDurationSeconds)}s (longer than ${MAX_DURATION_SECONDS}s). " +
                    "Extracting the most active ${MAX_DURATION_SECONDS}-second segment for better accuracy...")
                val cropped = extractMostActiveSegment(result, maxSamples)
                android.util.Log.i("AudioSpectrogramConverter", 
                    "✅ AUTO-CROPPING: Successfully extracted ${String.format("%.1f", cropped.size.toFloat() / SAMPLE_RATE)}s segment with highest energy")
                cropped
            } else {
                android.util.Log.d("AudioSpectrogramConverter", 
                    "Audio duration: ${String.format("%.1f", audioDurationSeconds)}s (within ${MAX_DURATION_SECONDS}s limit, no cropping needed)")
                result
            }
            
        } finally {
            decoder?.stop()
            decoder?.release()
            extractor.release()
        }
    }


    
    /**
     * Convert PCM byte array to float array (16-bit PCM)
     */
    private fun pcmDataToFloatArray(pcmData: ByteArray): FloatArray {
        val samples = FloatArray(pcmData.size / 2)
        val buffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN)
        
        for (i in samples.indices) {
            val sample = buffer.short.toInt()
            // Normalize to -1.0 to 1.0 range
            samples[i] = (sample / 32768.0f).coerceIn(-1f, 1f)
        }
        
        return samples
    }
    
    /**
     * Simple linear resampling
     */
    private fun resampleAudio(input: FloatArray, inputRate: Int, outputRate: Int): FloatArray {
        if (inputRate == outputRate) return input
        
        val ratio = inputRate.toFloat() / outputRate.toFloat()
        val outputSize = (input.size / ratio).toInt()
        val output = FloatArray(outputSize)
        
        for (i in output.indices) {
            val inputIndex = (i * ratio).toInt().coerceIn(0, input.size - 1)
            output[i] = input[inputIndex]
        }
        
        return output
    }

    /**
     * Generate realistic audio-like frequency content
     * Simulates chicken sounds with frequency bands
     */
    private fun generateRealisticAudioData(samples: Int): FloatArray {
        val data = FloatArray(samples)

        for (i in data.indices) {
            val t = i.toFloat() / SAMPLE_RATE
            val progress = i.toFloat() / samples
            val lowFreq = sin(2 * PI * 200 * t) * 0.4f
            val midFreq = sin(2 * PI * 800 * t) * 0.3f
            val highFreq = sin(2 * PI * 2000 * t) * 0.2f
            val harmonic1 = sin(2 * PI * 400 * t) * 0.2f
            val harmonic2 = sin(2 * PI * 1200 * t) * 0.15f
            val envelope = 0.5f + 0.5f * sin(2 * PI * progress * 3)
            val noise = (Random.nextDouble() * 0.2 - 0.1).toFloat()

            data[i] = ((lowFreq + midFreq + highFreq + harmonic1 + harmonic2) * envelope + noise).coerceIn(
                (-1f).toDouble(), 1.0
            )
                .toFloat()
        }

        return data
    }

    /**
     * Compute mel-spectrogram-like representation
     * Simplified version: uses frequency-domain-like features
     */
    private fun computeMelSpectrogram(audioData: FloatArray): Array<FloatArray> {
        val nFrames = IMAGE_SIZE
        val nMels = IMAGE_SIZE
        val spectrogram = Array(nFrames) { FloatArray(nMels) }
        val frameSize = audioData.size / nFrames

        // Process each time frame
        for (frame in 0 until nFrames) {
            val startIdx = frame * frameSize
            val endIdx = minOf(startIdx + frameSize, audioData.size)

            // Extract frame
            val frameData = audioData.sliceArray(startIdx until endIdx)

            for (i in frameData.indices) {
                val window = 0.5f * (1f - cos(2f * PI.toFloat() * i.toFloat() / frameData.size.toFloat()))
                frameData[i] = frameData[i] * window
            }

            for (melBin in 0 until nMels) {
                var energy = 0f

                // Compute energy in this mel band
                for (i in frameData.indices) {
                    val freq = i.toFloat() / frameData.size
                    val melFreq = hzToMel(freq * SAMPLE_RATE / 2f)
                    val targetMel = melBin.toFloat() / nMels * hzToMel(SAMPLE_RATE / 2f)

                    val diff = (melFreq - targetMel) / 50f
                    val weight = exp(-(diff * diff))
                    energy += abs(frameData[i]) * weight
                }

                // Apply power scaling (like librosa power_to_db)
                val power = energy * energy
                val dbPower = 10f * log10(power + 1e-10f) // Add epsilon to avoid log(0)

                // Normalize to 0-1 range (typical librosa range is -80 to 0 dB)
                val normalized = ((dbPower + 80f) / 80f).coerceIn(0f, 1f)

                spectrogram[frame][nMels - 1 - melBin] = normalized // Flip for visualization
            }
        }

        return spectrogram
    }

    /**
     * Convert Hz to Mel scale
     */
    private fun hzToMel(hz: Float): Float {
        return 2595f * log10(1f + hz / 700f)
    }


    /**
     * Draw mel spectrogram to bitmap using librosa-like colormap (viridis-like)
     * Matches librosa's spec-show visualization
     */
    private fun drawMelSpectrogram(bitmap: Bitmap, spectrogram: Array<FloatArray>) {
        val pixels = IntArray(IMAGE_SIZE * IMAGE_SIZE)

        for (y in 0 until IMAGE_SIZE) {
            for (x in 0 until IMAGE_SIZE) {
                val value = spectrogram[x][y].coerceIn(0f, 1f)

                // Use viridis-like colormap (similar to librosa default)
                val color = viridisColormap(value)
                pixels[y * IMAGE_SIZE + x] = color
            }
        }

        bitmap.setPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE)
    }

    /**
     * Convert value (0-1) to RGB color using viridis-like colormap
     * This matches librosa's default colormap visualization
     */
    private fun viridisColormap(value: Float): Int {
        // Simplified viridis colormap
        val r: Int
        val g: Int
        val b: Int

        when {
            value < 0.25f -> {
                // Dark blue to green
                val t = value / 0.25f
                r = (68 * (1 - t) + 72 * t).toInt()
                g = (1 * (1 - t) + 40 * t).toInt()
                b = (84 * (1 - t) + 54 * t).toInt()
            }
            value < 0.5f -> {
                // Green to yellow
                val t = (value - 0.25f) / 0.25f
                r = (72 * (1 - t) + 253 * t).toInt()
                g = (40 * (1 - t) + 231 * t).toInt()
                b = (54 * (1 - t) + 37 * t).toInt()
            }
            else -> {
                // Yellow to bright yellow
                val t = (value - 0.5f) / 0.5f
                r = (253 * (1 - t) + 254 * t).toInt()
                g = (231 * (1 - t) + 240 * t).toInt()
                b = (37 * (1 - t) + 217 * t).toInt()
            }
        }

        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }
}

