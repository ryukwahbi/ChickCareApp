package com.bisu.chickcare.backend.service

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

class CloudinaryUploadService(private val context: android.content.Context) {
    
    companion object {
        // TODO: Replace these with your actual credentials!
        private const val CLOUD_NAME = "dt5zfjcut"
        private const val API_KEY = "851529128471813"
        private const val API_SECRET = "G6L6U6S_4q9RBNfpLp7gXnidJaQ"
        private const val UPLOAD_URL = "https://api.cloudinary.com/v1_1/%s/image/upload"
        private const val TAG = "CloudinaryUploadService"
    }
    
    private val client = OkHttpClient()
    
    /**
     * Upload image to Cloudinary and return the direct URL
     * @param imageUri The URI of the image to upload
     * @return The direct URL of the uploaded image, or null if upload failed
     */
    suspend fun uploadImage(imageUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting Cloudinary upload for URI: $imageUri")
            
            // Validate credentials
            if (CLOUD_NAME.isEmpty() || API_KEY.isEmpty() || API_SECRET.isEmpty()) {
                Log.e(TAG, "Cloudinary credentials not configured! Please update CLOUD_NAME, API_KEY, and API_SECRET in CloudinaryUploadService.kt")
                return@withContext null
            }

            // Convert URI to File
            val imageFile = uriToFile(imageUri) ?: run {
                Log.e(TAG, "Failed to convert URI to File")
                return@withContext null
            }
            
            if (!imageFile.exists()) {
                Log.e(TAG, "Image file does not exist: ${imageFile.absolutePath}")
                return@withContext null
            }
            
            Log.d(TAG, "Image file size: ${imageFile.length()} bytes")
            
            // Create timestamp for signature
            val timestamp = (System.currentTimeMillis() / 1000).toString()
            
            // Generate signature (required for authenticated uploads)
            // Signature format: SHA1(timestamp + API_SECRET)
            val signature = generateSignature(timestamp)
            
            // Build upload URL
            val uploadUrl = String.format(UPLOAD_URL, CLOUD_NAME)
            
            // Create multipart request body
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", imageFile.name, imageFile.asRequestBody("image/*".toMediaType()))
                .addFormDataPart("api_key", API_KEY)
                .addFormDataPart("timestamp", timestamp)
                .addFormDataPart("signature", signature)
                .build()
            
            // Create request
            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build()
            
            // Execute request
            val response = client.newCall(request).execute()
            val responseBody = response.body.string()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Upload failed: ${response.code} - $responseBody")
                return@withContext null
            }
            
            // Parse response
            val json = JSONObject(responseBody)
            
            // Check for errors
            if (json.has("error")) {
                val error = json.optJSONObject("error")?.optString("message") ?: json.optString("error")
                Log.e(TAG, "Upload failed: $error")
                return@withContext null
            }
            
            // Extract secure URL (HTTPS) or regular URL
            val secureUrl = json.optString("secure_url")
            val regularUrl = json.optString("url")
            val imageUrl = secureUrl.ifEmpty { regularUrl }
            
            if (imageUrl.isEmpty()) {
                Log.e(TAG, "No image URL in response")
                return@withContext null
            }
            
            Log.d(TAG, "Upload successful! Image URL: $imageUrl")
            
            // Clean up temp file
            imageFile.delete()
            
            return@withContext imageUrl
            
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image to Cloudinary", e)
            return@withContext null
        }
    }
    
    /**
     * Generate signature for Cloudinary authenticated upload
     */
    private fun generateSignature(timestamp: String): String {
        try {
            // Create the string to sign: timestamp + API_SECRET
            val stringToSign = "timestamp=$timestamp$API_SECRET"
            
            // Create SHA-1 hash
            val digest = MessageDigest.getInstance("SHA-1")
            val hash = digest.digest(stringToSign.toByteArray())
            
            // Convert to hex string
            return hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating signature", e)
            return ""
        }
    }
    
    /**
     * Convert URI to File
     */
    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream ?: return null
            
            val tempFile = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(tempFile)
            
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error converting URI to File", e)
            null
        }
    }
}

