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
        sanitized.toUri()
    } catch (e: Exception) {
        logTag?.let { Log.w(it, "Failed to parse URI: ${e.message}") }
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