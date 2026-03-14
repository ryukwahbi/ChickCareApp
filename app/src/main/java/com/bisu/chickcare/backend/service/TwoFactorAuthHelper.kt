package com.bisu.chickcare.backend.service

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TwoFactorAuthHelper {
    private const val PREFS_NAME = "two_factor_prefs"
    private const val KEY_2FA_ENABLED = "two_factor_enabled"
    private const val KEY_2FA_SECRET = "two_factor_secret"
    
    // Use lazy initialization to avoid static field memory leak warning
    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()
    
    /**
     * Generate a random secret key for TOTP
     */
    fun generateSecret(): String {
        val random = SecureRandom()
        val bytes = ByteArray(20)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
    
    /**
     * Generate TOTP code from secret
     */
    fun generateTOTP(secret: String, timeStep: Long = System.currentTimeMillis() / 30000): String {
        try {
            val key = Base64.decode(secret, Base64.NO_WRAP)
            val keySpec = SecretKeySpec(key, "HmacSHA1")
            val mac = Mac.getInstance("HmacSHA1")
            mac.init(keySpec)
            
            val timeBytes = ByteArray(8)
            var time = timeStep
            for (i in 7 downTo 0) {
                timeBytes[i] = (time and 0xFF).toByte()
                time = time shr 8
            }
            
            val hash = mac.doFinal(timeBytes)
            val offset = hash[hash.size - 1].toInt() and 0x0F
            val code = ((hash[offset].toInt() and 0x7F) shl 24) or
                    ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                    ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                    (hash[offset + 3].toInt() and 0xFF)
            
            val otp = code % 1000000
            return String.format(Locale.ROOT, "%06d", otp)
        } catch (e: Exception) {
            Log.e("TwoFactorAuthHelper", "Error generating TOTP: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Verify TOTP code
     */
    fun verifyTOTP(secret: String, code: String, window: Int = 1): Boolean {
        val currentTimeStep = System.currentTimeMillis() / 30000
        
        // Check current time step and adjacent time steps (for clock skew tolerance)
        for (i in -window..window) {
            val timeStep = currentTimeStep + i
            val expectedCode = generateTOTP(secret, timeStep)
            if (expectedCode == code) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Check if 2FA is enabled for the user
     */
    fun isTwoFactorEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_2FA_ENABLED, false)
    }
    
    /**
     * Get the user's 2FA secret from Firestore
     */
    suspend fun getTwoFactorSecret(userId: String): String? {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            doc.getString("twoFactorSecret")
        } catch (e: Exception) {
            Log.e("TwoFactorAuthHelper", "Error getting 2FA secret: ${e.message}", e)
            null
        }
    }
    
    /**
     * Save 2FA secret to Firestore
     */
    suspend fun saveTwoFactorSecret(userId: String, secret: String) {
        try {
            firestore.collection("users")
                .document(userId)
                .update("twoFactorSecret", secret)
                .await()
            
            Log.d("TwoFactorAuthHelper", "2FA secret saved for user: $userId")
        } catch (e: Exception) {
            Log.e("TwoFactorAuthHelper", "Error saving 2FA secret: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Enable 2FA for the user
     */
    suspend fun enableTwoFactor(userId: String, secret: String, context: Context) {
        try {
            // Save secret to Firestore
            saveTwoFactorSecret(userId, secret)
            
            // Update Firestore to mark 2FA as enabled
            firestore.collection("users")
                .document(userId)
                .update("twoFactorEnabled", true, "twoFactorEnabledAt", System.currentTimeMillis())
                .await()
            
            // Save to local preferences
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit {
                putBoolean(KEY_2FA_ENABLED, true)
                    .putString(KEY_2FA_SECRET, secret)
            }
            
            Log.d("TwoFactorAuthHelper", "2FA enabled for user: $userId")
        } catch (e: Exception) {
            Log.e("TwoFactorAuthHelper", "Error enabling 2FA: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Disable 2FA for the user
     */
    suspend fun disableTwoFactor(userId: String, context: Context) {
        try {
            // Update Firestore to mark 2FA as disabled
            firestore.collection("users")
                .document(userId)
                .update("twoFactorEnabled", false)
                .await()
            
            // Clear local preferences
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit {
                putBoolean(KEY_2FA_ENABLED, false)
                    .remove(KEY_2FA_SECRET)
            }
            
            Log.d("TwoFactorAuthHelper", "2FA disabled for user: $userId")
        } catch (e: Exception) {
            Log.e("TwoFactorAuthHelper", "Error disabling 2FA: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Verify and enable 2FA with a verification code
     */
    suspend fun verifyAndEnableTwoFactor(
        userId: String,
        secret: String,
        verificationCode: String,
        context: Context
    ): Boolean {
        return try {
            // Verify the code
            if (!verifyTOTP(secret, verificationCode)) {
                Log.e("TwoFactorAuthHelper", "Invalid verification code")
                return false
            }
            
            // Enable 2FA
            enableTwoFactor(userId, secret, context)
            true
        } catch (e: Exception) {
            Log.e("TwoFactorAuthHelper", "Error verifying and enabling 2FA: ${e.message}", e)
            false
        }
    }
}

