package com.bisu.chickcare.frontend.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

fun sanitizeUriString(raw: String?, logTag: String? = null): String? {
    if (raw.isNullOrBlank()) return null
    return try {
        Uri.decode(raw)
    } catch (e: Exception) {
        logTag?.let { Log.w(it, "Failed to decode URI: ${e.message}") }
        raw
    }
}

fun sanitizeToUri(raw: String?, logTag: String? = null): Uri? {
    val sanitized = sanitizeUriString(raw, logTag) ?: return null
    return try {
        val uri = Uri.parse(sanitized)
        if (uri.scheme == null) return null
        uri
    } catch (e: Exception) {
        logTag?.let { Log.w(it, "Failed to parse URI: ${e.message}") }
        null
    }
}

/**
 * Checks if a URI is accessible (file exists/can be opened).
 * Returns the local URI if accessible, otherwise fallbackUrl (cloud URL).
 */
fun getAccessibleUri(context: Context, localUriString: String?, fallbackUrl: String?): Any? {
    if (!localUriString.isNullOrEmpty()) {
        val uri = sanitizeToUri(localUriString, "UriUtils")
        if (uri != null) {
            try {
                // For file:// URIs, check file existence directly
                if (uri.scheme == "file") {
                    val file = File(uri.path ?: "")
                    if (file.exists() && file.canRead()) {
                        Log.v("UriUtils", "Local file accessible: ${file.absolutePath}")
                        return uri
                    } else {
                        Log.v("UriUtils", "Local file NOT found or NOT readable: ${file.absolutePath}")
                    }
                } else {
                    // For content:// or others, try to open stream
                    context.contentResolver.openInputStream(uri)?.use { 
                        Log.v("UriUtils", "Local content accessible: $uri")
                        return uri // Accessible!
                    } ?: Log.v("UriUtils", "Local content stream NULL: $uri")
                }
            } catch (e: Exception) {
                 Log.v("UriUtils", "Failed to access local URI $uri: ${e.message}")
                 // Failed to open -> Local file missing or no permission
                 // Fall through to fallback
            }
        }
    }
    
    // Local failed or null, try fallback
    return if (!fallbackUrl.isNullOrEmpty()) {
        Log.v("UriUtils", "Falling back to cloud URL: $fallbackUrl")
        fallbackUrl 
    } else {
        null
    }
}

suspend fun persistUriToAppStorage(
    context: Context,
    sourceUriString: String?,
    subdirectory: String,
    fallbackExtension: String,
    logTag: String = "UriUtils"
): String? = withContext(Dispatchers.IO) {
    if (sourceUriString.isNullOrBlank()) return@withContext null
    try {
        val sourceUri = sourceUriString.toUri()
        if ("content".equals(sourceUri.scheme, ignoreCase = true)) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    sourceUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (se: SecurityException) {
                Log.w(logTag, "persistUriToAppStorage: could not take persistable permission: ${se.message}")
            } catch (e: Exception) {
                Log.w(logTag, "persistUriToAppStorage: error taking persistable permission: ${e.message}")
            }
        }
        val targetDir = File(context.filesDir, subdirectory).apply {
            if (!exists()) mkdirs()
        }

        val extension = sourceUri.path?.substringAfterLast('.', missingDelimiterValue = "")
            ?.takeIf { it.isNotBlank() }
            ?: fallbackExtension

        val targetFile = File(
            targetDir,
            "${System.currentTimeMillis()}_${sourceUri.lastPathSegment?.hashCode() ?: 0}.$extension"
        )

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        } ?: run {
            Log.e(logTag, "persistUriToAppStorage: Input stream null for $sourceUriString")
            return@withContext null
        }

        Log.d(logTag, "persistUriToAppStorage: copied $sourceUriString to ${targetFile.absolutePath}")
        Uri.fromFile(targetFile).toString()
    } catch (e: SecurityException) {
        Log.e(logTag, "persistUriToAppStorage security error: ${e.message}")
        null
    } catch (e: Exception) {
        Log.e(logTag, "persistUriToAppStorage error: ${e.message}", e)
        null
    }
}

fun getTempUri(context: Context): Uri {
    val tempFile = File.createTempFile(
        "temp_image_${System.currentTimeMillis()}",
        ".jpg",
        context.cacheDir
    )
    val authority = "${context.packageName}.provider"
    return androidx.core.content.FileProvider.getUriForFile(context, authority, tempFile)
}