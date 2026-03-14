package com.bisu.chickcare.backend.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Utility class to manage onboarding state using SharedPreferences.
 * Tracks whether the user has completed the onboarding flow.
 */
object OnboardingPreferences {
    private const val PREFS_NAME = "chickcare_onboarding_prefs"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Check if the user has seen and completed the onboarding flow.
     */
    fun hasSeenOnboarding(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    /**
     * Mark the onboarding as completed. Call this when user taps "Get Started" or "Skip".
     */
    fun setOnboardingCompleted(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
    }

    /**
     * Reset onboarding state (useful for testing or if user wants to see onboarding again).
     */
    fun resetOnboarding(context: Context) {
        getPrefs(context).edit().remove(KEY_ONBOARDING_COMPLETED).apply()
    }
}
