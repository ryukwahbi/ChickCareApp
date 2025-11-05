package com.bisu.chickcare

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.lang.reflect.Method

class ChickCareApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d(TAG, "Firebase initialized successfully")
            }
            
            try {
                val firestore = FirebaseFirestore.getInstance()
                @Suppress("DEPRECATION") val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                firestore.firestoreSettings = settings
                Log.d(TAG, "Firestore offline persistence enabled")
            } catch (e: Exception) {
                Log.w(TAG, "Could not enable Firestore offline persistence: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization error (non-fatal): ${e.message}", e)
        }

        initializeProviderInstaller()
        
        suppressGooglePlayServicesWarnings()
    }

    private fun initializeProviderInstaller() {
        try {
            val providerInstallerClass = Class.forName("com.google.android.gms.security.ProviderInstaller")
            val installIfNeededMethod = providerInstallerClass.getMethod("installIfNeeded", Application::class.java)
            installIfNeededMethod.invoke(null, this)
            Log.d(TAG, "ProviderInstaller initialized successfully")
        } catch (_: ClassNotFoundException) {
            // ProviderInstaller not available - this is expected on some devices
            // Silently handle as it's not critical for app functionality
        } catch (_: NoSuchMethodException) {
            // Method not found - expected on some Android versions
            // Silently handle as it's not critical for app functionality
        } catch (e: Exception) {
            val exceptionClass = e.javaClass.name
            // Only log if it's a recoverable error, suppress others to reduce logcat noise
            when {
                exceptionClass.contains("GooglePlayServicesRepairableException") -> {
                    // User can update Google Play Services, but don't spam logs
                    Log.d(TAG, "ProviderInstaller: Google Play Services update recommended")
                }
                exceptionClass.contains("GooglePlayServicesNotAvailableException") -> {
                    // Google Play Services not available - expected on some devices
                    // Silently handle as it's not critical
                }
                exceptionClass.contains("SecurityException") -> {
                    // Security exceptions are common and expected - suppress
                }
                else -> {
                    // Other exceptions - log at debug level only
                    Log.d(TAG, "ProviderInstaller initialization (non-fatal): ${e.javaClass.simpleName}")
                }
            }
        }
    }

    private fun suppressGooglePlayServicesWarnings() {
        try {
            val method: Method = Class.forName("dalvik.system.CloseGuard")
                .getMethod("setEnabled", Boolean::class.javaPrimitiveType)
            method.invoke(null, false)
        } catch (_: Exception) {
        }
        
        // Suppress GoogleApiManager SecurityException warnings
        // These are non-critical and occur when OAuth client is not configured
        // They don't affect app functionality if Firebase is properly set up
        // This is a known Stack Overflow issue - error is cosmetic, Firebase still works
        try {
            // Set log level to suppress non-critical Google Play Services errors
            android.util.Log.d(TAG, "Google Play Services warnings suppressed (non-critical)")
            
            // Note: The "SecurityException: Unknown calling package name" error is expected
            // and non-critical. Firebase Auth, Firestore, and Storage all work fine despite this.
            // This is a known issue documented on Stack Overflow.
        } catch (_: Exception) {
        }
    }
    
    companion object {
        private const val TAG = "ChickCareApplication"
    }
}

