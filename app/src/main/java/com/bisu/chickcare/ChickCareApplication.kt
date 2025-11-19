package com.bisu.chickcare

import android.app.Application
import android.content.Context
import android.util.Log
import com.bisu.chickcare.backend.utils.LocaleHelper
import com.bisu.chickcare.backend.viewmodels.LanguageViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.lang.reflect.Method

class ChickCareApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize language settings
        LanguageViewModel.initialize(this)
        
        // Initialize theme preference
        com.bisu.chickcare.backend.viewmodels.ThemeViewModel.initialize(this)

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
        } catch (_: NoSuchMethodException) {
        } catch (e: Exception) {
            val exceptionClass = e.javaClass.name
            when {
                exceptionClass.contains("GooglePlayServicesRepairableException") -> {
                    Log.d(TAG, "ProviderInstaller: Google Play Services update recommended")
                }
                exceptionClass.contains("GooglePlayServicesNotAvailableException") -> {
                }
                exceptionClass.contains("SecurityException") -> {
                }
                else -> {
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

        try {
            Log.d(TAG, "Google Play Services warnings suppressed (non-critical)")

        } catch (_: Exception) {
        }
    }
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.attachBaseContext(base))
    }
    
    companion object {
        private const val TAG = "ChickCareApplication"
    }
}

