package com.bisu.chickcare.backend.repository

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.bisu.chickcare.backend.data.LoginSessionData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class LoginSessionRepository {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun recordSession(context: Context, userId: String) {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device_id"
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val deviceName = if (model.startsWith(manufacturer, ignoreCase = true)) {
            model.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else {
            "${manufacturer.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} $model"
        }

        // Default type for Android devices is mostly phone/tablet, "phone" is a safe generic term.
        val deviceType = "phone" 

        // Try to fetch IP and Location
        var ipAddress = "Unknown IP"
        var location = "Location unknown"

        try {
            val ipDetails = fetchIpDetails()
            if (ipDetails != null) {
                ipAddress = ipDetails.getString("ip")
                val city = ipDetails.optString("city", "")
                val region = ipDetails.optString("region", "")
                val country = ipDetails.optString("country_name", "")
                
                val locationParts = listOf(city, region, country).filter { it.isNotBlank() }
                if (locationParts.isNotEmpty()) {
                    location = locationParts.joinToString(", ")
                }
            }
        } catch (e: Exception) {
            Log.w("LoginSessionRep", "Failed to fetch IP/Location details: ${e.message}")
        }

        val sessionData = hashMapOf(
            "sessionId" to deviceId,
            "deviceName" to deviceName,
            "deviceType" to deviceType,
            "location" to location,
            "ipAddress" to ipAddress,
            "isActive" to true,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "lastActive" to System.currentTimeMillis()
        )

        try {
            firestore.collection("users")
                .document(userId)
                .collection("sessions")
                .document(deviceId)
                .set(sessionData)
                .await()
            Log.d("LoginSessionRep", "Login session recorded for device: $deviceId")
        } catch (e: Exception) {
            Log.e("LoginSessionRep", "Error recording login session", e)
        }
    }

    suspend fun revokeSession(userId: String, sessionId: String) {
        try {
            firestore.collection("users")
                .document(userId)
                .collection("sessions")
                .document(sessionId)
                .update("isActive", false)
                .await()
        } catch (e: Exception) {
            Log.e("LoginSessionRep", "Error revoking session", e)
        }
    }

    suspend fun revokeAllOtherSessions(userId: String, currentSessionId: String) {
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("sessions")
                .whereEqualTo("isActive", true)
                .get()
                .await()
                
            val batch = firestore.batch()
            for (document in snapshot.documents) {
                if (document.id != currentSessionId) {
                    batch.update(document.reference, "isActive", false)
                }
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("LoginSessionRep", "Error revoking all other sessions", e)
        }
    }

    suspend fun markSessionInactiveLocally(context: Context, userId: String) {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: return
        try {
            firestore.collection("users")
                .document(userId)
                .collection("sessions")
                .document(deviceId)
                .update("isActive", false)
                .await()
        } catch (e: Exception) {
            Log.e("LoginSessionRep", "Error marking current session inactive", e)
        }
    }

    private suspend fun fetchIpDetails(): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://ipapi.co/json/")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000 // 5 seconds
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                return@withContext JSONObject(response)
            }
        } catch (e: Exception) {
            Log.e("LoginSessionRep", "API request failed: ${e.message}")
        }
        return@withContext null
    }
}
