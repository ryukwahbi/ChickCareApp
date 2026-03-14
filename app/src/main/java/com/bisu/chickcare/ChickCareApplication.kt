package com.bisu.chickcare

import android.app.Application
import android.content.Context
import android.util.Log
import com.bisu.chickcare.backend.utils.LocaleHelper
import com.bisu.chickcare.backend.viewmodels.LanguageViewModel
import com.bisu.chickcare.backend.worker.NotificationWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import java.lang.reflect.Method

class ChickCareApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
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
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
                firestore.firestoreSettings = settings
                Log.d(TAG, "Firestore offline persistence enabled with unlimited cache")
            } catch (e: Exception) {
                Log.w(TAG, "Could not enable Firestore offline persistence: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization error (non-fatal): ${e.message}", e)
        }
        initializeProviderInstaller()
        suppressGooglePlayServicesWarnings()
        
        // Initialize Background Notification Worker
        try {
            val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
                .setInitialDelay(5, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "ChickCareNotificationWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            Log.d(TAG, "NotificationWorker scheduled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule NotificationWorker", e)
        }
        
        // Start Foreground Notification Service if user is logged in
        try {
            if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
                com.bisu.chickcare.backend.service.NotificationForegroundService.start(this)
                Log.d(TAG, "NotificationForegroundService started")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start NotificationForegroundService", e)
        }
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
        private var instance: ChickCareApplication? = null

        fun getInstance(): ChickCareApplication {
            return instance ?: throw IllegalStateException("ChickCareApplication not initialized")
        }
    }
}

