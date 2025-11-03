package com.bisu.chickcare.backend.service

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import com.yalantis.ucrop.UCrop
import java.io.File

/**
 * Helper class for cropping images using UCrop
 */
object ImageCropHelper {
    private const val TAG = "ImageCropHelper"
    
    /**
     * Get destination URI for cropped profile picture using FileProvider
     */
    fun getProfilePictureCropUri(context: Context): Uri {
        val file = File(context.cacheDir, "cropped_profile_${System.currentTimeMillis()}.jpg")
        val authority = "${context.packageName}.provider"
        return FileProvider.getUriForFile(context, authority, file)
    }
    
    /**
     * Get destination URI for cropped cover photo using FileProvider
     */
    fun getCoverPhotoCropUri(context: Context): Uri {
        val file = File(context.cacheDir, "cropped_cover_${System.currentTimeMillis()}.jpg")
        val authority = "${context.packageName}.provider"
        return FileProvider.getUriForFile(context, authority, file)
    }
    
    /**
     * Start crop for profile picture (square, 1:1 ratio)
     */
    fun startProfilePictureCrop(
        cropLauncher: ActivityResultLauncher<android.content.Intent>,
        sourceUri: Uri,
        context: Context
    ) {
        try {
            val destinationUri = getProfilePictureCropUri(context)
            val options = UCrop.Options().apply {
                setToolbarTitle("Crop Profile Picture")
                setHideBottomControls(false)
                setFreeStyleCropEnabled(false)
                setCompressionQuality(90)
                setMaxBitmapSize(2048)
                setToolbarColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.black))
                setStatusBarColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.black))
                setToolbarWidgetColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.white))
            }
            
            val uCrop = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1f, 1f) // Square for profile picture
                .withMaxResultSize(1024, 1024) // 1:1 ratio, max 1024x1024
                .withOptions(options)
            
            cropLauncher.launch(uCrop.getIntent(context))
        } catch (e: Exception) {
            Log.e(TAG, "Error starting profile picture crop", e)
        }
    }
    
    /**
     * Start crop for cover photo (wide, 16:9 ratio)
     */
    fun startCoverPhotoCrop(
        cropLauncher: ActivityResultLauncher<android.content.Intent>,
        sourceUri: Uri,
        context: Context
    ) {
        try {
            val destinationUri = getCoverPhotoCropUri(context)
            val options = UCrop.Options().apply {
                setToolbarTitle("Crop Cover Photo")
                setHideBottomControls(false)
                setFreeStyleCropEnabled(true) // Allow free-style for cover photos
                setCompressionQuality(85)
                setMaxBitmapSize(4096) // Higher for cover photos
                setToolbarColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.black))
                setStatusBarColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.black))
                setToolbarWidgetColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.white))
            }
            
            val uCrop = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(16f, 9f) // 16:9 for cover photo
                .withMaxResultSize(1920, 1080) // 16:9 ratio, max 1920x1080
                .withOptions(options)
            
            cropLauncher.launch(uCrop.getIntent(context))
        } catch (e: Exception) {
            Log.e(TAG, "Error starting cover photo crop", e)
        }
    }
}

