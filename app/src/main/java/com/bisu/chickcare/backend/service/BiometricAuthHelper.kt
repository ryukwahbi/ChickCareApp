package com.bisu.chickcare.backend.service

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity

object BiometricAuthHelper {
    private const val PREFS_NAME = "biometric_prefs"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    
    /**
     * Check if biometric authentication is available on the device
     */
    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.d("BiometricAuthHelper", "No biometric hardware available")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.d("BiometricAuthHelper", "Biometric hardware unavailable")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.d("BiometricAuthHelper", "No biometric enrolled")
                false
            }
            else -> false
        }
    }
    
    /**
     * Get biometric availability status message
     */
    fun getBiometricStatus(context: Context): String {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Biometric authentication is available"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No biometric hardware available on this device"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware is currently unavailable"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No biometric credentials enrolled. Please set up fingerprint or face recognition in device settings"
            else -> "Biometric authentication is not available"
        }
    }
    
    /**
     * Check if biometric is enabled for the user
     */
    fun isBiometricEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }
    
    /**
     * Enable biometric authentication
     */
    fun enableBiometric(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_BIOMETRIC_ENABLED, true) }
        Log.d("BiometricAuthHelper", "Biometric authentication enabled")
    }
    
    /**
     * Disable biometric authentication
     */
    fun disableBiometric(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_BIOMETRIC_ENABLED, false) }
        Log.d("BiometricAuthHelper", "Biometric authentication disabled")
    }
    
    /**
     * Show biometric prompt for authentication
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Biometric Authentication",
        subtitle: String = "Use your fingerprint or face to authenticate",
        negativeButtonText: String = "Cancel",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d("BiometricAuthHelper", "Biometric authentication succeeded")
                    onSuccess()
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e("BiometricAuthHelper", "Biometric authentication error: $errString")
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || 
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onCancel()
                    } else {
                        onError(errString.toString())
                    }
                }
                
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.e("BiometricAuthHelper", "Biometric authentication failed")
                    onError("Authentication failed. Please try again.")
                }
            }
        )
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
}

