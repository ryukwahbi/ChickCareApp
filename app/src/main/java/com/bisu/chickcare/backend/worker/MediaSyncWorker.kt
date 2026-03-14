package com.bisu.chickcare.backend.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bisu.chickcare.backend.service.CloudinaryUploadService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.core.net.toUri

class MediaSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val cloudinaryService = CloudinaryUploadService(appContext)
    private val firestore = FirebaseFirestore.getInstance()

    override suspend fun doWork(): Result {
        val userId = inputData.getString("userId") ?: return Result.failure()
        val detectionId = inputData.getString("detectionId") ?: return Result.failure()
        val postId = inputData.getString("postId")

        Log.d("MediaSyncWorker", "Starting sync for detection: $detectionId (User: $userId, Post: $postId)")

        try {
            // 1. Fetch detection details from Firestore
            val detectionDoc = firestore.collection("users").document(userId)
                .collection("detections").document(detectionId).get().await()

            if (!detectionDoc.exists()) {
                Log.e("MediaSyncWorker", "Detection document not found: $detectionId")
                return Result.failure()
            }

            val localImageUri = detectionDoc.getString("localImageUri")
            val localAudioUri = detectionDoc.getString("localAudioUri")
            var currentCloudUrl = detectionDoc.getString("cloudUrl")
            var currentCloudAudioUrl = detectionDoc.getString("cloudAudioUrl")

            val detectionUpdates = hashMapOf<String, Any>()

            // 2. Sync Image if needed
            if (!localImageUri.isNullOrEmpty() && currentCloudUrl.isNullOrEmpty()) {
                Log.d("MediaSyncWorker", "Uploading image to Cloudinary...")
                val cloudUrl = cloudinaryService.uploadImage(localImageUri.toUri())
                if (cloudUrl != null) {
                    detectionUpdates["cloudUrl"] = cloudUrl
                    currentCloudUrl = cloudUrl
                } else {
                    Log.w("MediaSyncWorker", "Image upload failed, will retry later")
                    return Result.retry()
                }
            }

            // 3. Sync Audio if needed
            if (!localAudioUri.isNullOrEmpty() && currentCloudAudioUrl.isNullOrEmpty()) {
                Log.d("MediaSyncWorker", "Uploading audio to Cloudinary...")
                val cloudAudioUrl = cloudinaryService.uploadAudio(localAudioUri.toUri())
                if (cloudAudioUrl != null) {
                    detectionUpdates["cloudAudioUrl"] = cloudAudioUrl
                    currentCloudAudioUrl = cloudAudioUrl
                } else {
                    Log.w("MediaSyncWorker", "Audio upload failed, will retry later")
                    return Result.retry()
                }
            }

            // 4. Update Detection Firestore
            if (detectionUpdates.isNotEmpty()) {
                firestore.collection("users").document(userId)
                    .collection("detections").document(detectionId)
                    .update(detectionUpdates).await()
                Log.d("MediaSyncWorker", "Detection sync completed successfully for $detectionId")
            }

            // 5. Update Post Firestore if provided
            if (postId != null) {
                val postUpdates = hashMapOf<String, Any>()
                if (currentCloudUrl != null) postUpdates["cloudImageUri"] = currentCloudUrl
                if (currentCloudAudioUrl != null) postUpdates["cloudAudioUri"] = currentCloudAudioUrl

                if (postUpdates.isNotEmpty()) {
                    firestore.collection("users").document(userId)
                        .collection("timelinePosts").document(postId)
                        .update(postUpdates).await()
                    Log.d("MediaSyncWorker", "Post sync completed successfully for $postId")
                }
            }

            return Result.success()

        } catch (e: Exception) {
            Log.e("MediaSyncWorker", "Error during media sync: ${e.message}", e)
            return Result.retry()
        }
    }
}
