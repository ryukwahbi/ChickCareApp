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
            Log.d(TAG, "ProviderInstaller not available (class not found)")
        } catch (e: NoSuchMethodException) {
            Log.w(TAG, "ProviderInstaller method not found: ${e.message}")
        } catch (e: Exception) {
            val exceptionClass = e.javaClass.name
            when {
                exceptionClass.contains("GooglePlayServicesRepairableException") -> {
                    Log.w(TAG, "ProviderInstaller requires Google Play Services update: ${e.message}")
                }
                exceptionClass.contains("GooglePlayServicesNotAvailableException") -> {
                    Log.w(TAG, "Google Play Services not available: ${e.message}")
                }
                else -> {
                    Log.w(TAG, "ProviderInstaller initialization error (non-fatal): ${e.message}")
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
    }
    
    companion object {
        private const val TAG = "ChickCareApplication"
    }
}

