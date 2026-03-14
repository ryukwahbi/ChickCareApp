package com.bisu.chickcare.backend.service

import android.net.Uri
import android.util.Log
import com.bisu.chickcare.BuildConfig
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
        // SECURITY: Credentials are now loaded from BuildConfig (which reads from local.properties)
        // This prevents credentials from being hardcoded in the source code
        private val CLOUD_NAME = BuildConfig.CLOUDINARY_CLOUD_NAME
        private val API_KEY = BuildConfig.CLOUDINARY_API_KEY
        private val API_SECRET = BuildConfig.CLOUDINARY_API_SECRET
        private const val UPLOAD_URL = "https://api.cloudinary.com/v1_1/%s/%s/upload"
        private const val TAG = "CloudinaryUploadService"
        
        init {
            // Log credential status (without exposing actual values)
            if (CLOUD_NAME.isEmpty() || API_KEY.isEmpty() || API_SECRET.isEmpty()) {
                Log.w(TAG, "⚠️ Cloudinary credentials are EMPTY in BuildConfig!")
                Log.w(TAG, "This usually means the app needs to be REBUILT after adding credentials to local.properties")
                Log.w(TAG, "Solution: Build -> Rebuild Project in Android Studio")
            } else {
                Log.d(TAG, "✓ Cloudinary credentials loaded successfully from BuildConfig")
            }
        }
    }
    
    private val client = OkHttpClient()
    
    /**
     * Upload image to Cloudinary
     */
    suspend fun uploadImage(imageUri: Uri): String? = uploadMedia(imageUri, "image")

    /**
     * Upload audio to Cloudinary
     */
    suspend fun uploadAudio(audioUri: Uri): String? = uploadMedia(audioUri, "video")

    /**
     * Generic upload to Cloudinary
     * @param uri The URI of the file to upload
     * @param resourceType "image", "video" (for audio as well), or "raw"
     * @return The direct URL of the uploaded image, or null if upload failed
     */
    private suspend fun uploadMedia(uri: Uri, resourceType: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting Cloudinary upload ($resourceType) for URI: $uri")
            
            // Validate credentials
            if (CLOUD_NAME.isEmpty() || API_KEY.isEmpty() || API_SECRET.isEmpty()) {
                Log.e(TAG, "Cloudinary credentials not configured!")
                return@withContext null
            }

            // Convert URI to File
            val file = uriToFile(uri) ?: run {
                Log.e(TAG, "Failed to convert URI to File")
                return@withContext null
            }
            
            if (!file.exists()) {
                Log.e(TAG, "File does not exist: ${file.absolutePath}")
                return@withContext null
            }
            
            val timestamp = (System.currentTimeMillis() / 1000).toString()
            val signature = generateSignature(timestamp)
            val uploadUrl = String.format(UPLOAD_URL, CLOUD_NAME, resourceType)
            
            val mediaType = when(resourceType) {
                "image" -> "image/*".toMediaType()
                "video" -> "audio/*".toMediaType() // For audio we use video resource type in Cloudinary
                else -> "application/octet-stream".toMediaType()
            }

            // Create multipart request body
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody(mediaType))
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
            
            val json = JSONObject(responseBody)
            if (json.has("error")) {
                val error = json.optJSONObject("error")?.optString("message") ?: json.optString("error")
                Log.e(TAG, "Upload failed: $error")
                return@withContext null
            }
            
            val secureUrl = json.optString("secure_url")
            val imageUrl = secureUrl.ifEmpty { json.optString("url") }
            
            if (imageUrl.isEmpty()) {
                Log.e(TAG, "No URL in response")
                return@withContext null
            }
            
            Log.d(TAG, "Upload successful! URL: $imageUrl")
            
            // Clean up temp file
            file.delete()
            return@withContext imageUrl
            
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading to Cloudinary", e)
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

