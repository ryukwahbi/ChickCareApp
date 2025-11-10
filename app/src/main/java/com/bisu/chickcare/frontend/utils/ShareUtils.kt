package com.bisu.chickcare.frontend.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.bisu.chickcare.R
import java.io.File

object ShareUtils {
    
    /**
     * Share the app download link via text message or other apps
     * @param context The context to use for sharing
     * @param downloadUrl The URL where the APK can be downloaded (e.g., from your server or Play Store)
     * @param googleDriveUrl Optional Google Drive download link
     */
    fun shareAppDownloadLink(
        context: Context, 
        downloadUrl: String? = null,
        googleDriveUrl: String? = null
    ) {
        val appName = try {
            context.getString(R.string.app_name)
        } catch (_: Exception) {
            context.packageManager.getApplicationLabel(context.applicationInfo).toString()
        }

        val defaultGoogleDriveUrl = googleDriveUrl ?: "https://drive.google.com/file/d/18tv97VNrGv7Uz4a81GylBtGEQxHTBSKT/view?usp=sharing"
        
        downloadUrl ?: defaultGoogleDriveUrl
        
        val linksSection = "📥 Download Link: $defaultGoogleDriveUrl\n\n"
        
        val shareText = """
            Download $appName - Your Chicken Health Management App! 🐔
            
            $linksSection
            📱 Para sa mga naka-install na:
            I-click ang link: chickcare://app/dashboard
            
            Features:
            • Health detection for chickens
            • Vaccination schedules
            • Feeding reminders
            • Expense tracking
            • And much more!
            
            Get it now!
        """.trimIndent()
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Download $appName")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        try {
            context.startActivity(Intent.createChooser(shareIntent, "Share $appName Download Link"))
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Share the APK file directly (if available)
     * Note: This requires the APK to be accessible on the device
     * @param context The context to use for sharing
     * @param apkFile The APK file to share (optional, will try to find it if not provided)
     */
    @Suppress("unused")
    fun shareApkFile(context: Context, apkFile: File? = null) {
        try {
            val packageName = context.packageName
            val appName = try {
                context.getString(R.string.app_name)
            } catch (_: Exception) {
                context.packageManager.getApplicationLabel(context.applicationInfo).toString()
            }
            
            val apk = apkFile ?: findApkFile(context)
            
            if (apk == null || !apk.exists()) {
                shareAppDownloadLink(context)
                return
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "$packageName.provider",
                apk
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$appName APK")
                putExtra(Intent.EXTRA_TEXT, "Install $appName on your device!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share $appName APK"))
        } catch (_: Exception) {
            shareAppDownloadLink(context)
        }
    }
    
    /**
     * Try to find the APK file on the device
     * This is a best-effort approach and may not always work
     */
    private fun findApkFile(context: Context): File? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val sourceDir = packageInfo.applicationInfo?.sourceDir
            if (sourceDir != null) {
                File(sourceDir)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
    
    /**
     * Generate a deep link for the app
     * @param path The path to navigate to (e.g., "dashboard", "profile", "download")
     * @return The deep link URI
     */
    @Suppress("unused")
    fun generateDeepLink(path: String = "app"): Uri {
        return "chickcare://$path".toUri()
    }
    
    /**
     * Generate an HTTPS deep link
     * @param path The path to navigate to
     * @return The HTTPS deep link URI
     */
    @Suppress("unused")
    fun generateHttpsDeepLink(path: String = "app"): Uri {
        return "https://chickcare.app/$path".toUri()
    }
}

