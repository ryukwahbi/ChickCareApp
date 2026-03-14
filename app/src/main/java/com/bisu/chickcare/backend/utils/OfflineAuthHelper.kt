package com.bisu.chickcare.backend.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Helper to track current offline user ID
 * Used by DashboardViewModel and other components to get current user ID
 */
object OfflineAuthHelper {
    private const val PREFS_NAME = "offline_auth_prefs"
    private const val KEY_CURRENT_USER_ID = "current_local_user_id"
    private const val KEY_LAST_EMAIL = "last_email"
    private const val KEY_LAST_PASSWORD_HASH = "last_password_hash"
    private const val KEY_LAST_USER_ID = "last_user_id"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun setCurrentLocalUserId(context: Context, userId: String?) {
        getPrefs(context).edit().putString(KEY_CURRENT_USER_ID, userId).apply()
    }
    
    fun getCurrentLocalUserId(context: Context): String? {
        return getPrefs(context).getString(KEY_CURRENT_USER_ID, null)
    }
    
    fun clearCurrentLocalUserId(context: Context) {
        getPrefs(context).edit().remove(KEY_CURRENT_USER_ID).apply()
    }

    fun setLastCredentials(context: Context, email: String, passwordHash: String, userId: String) {
        getPrefs(context).edit()
            .putString(KEY_LAST_EMAIL, email)
            .putString(KEY_LAST_PASSWORD_HASH, passwordHash)
            .putString(KEY_LAST_USER_ID, userId)
            .apply()
    }

    fun getLastEmail(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_EMAIL, null)
    }

    fun getLastPasswordHash(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_PASSWORD_HASH, null)
    }
    
    fun getLastUserId(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_USER_ID, null)
    }

    fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun hasOfflineSession(context: Context): Boolean {
        return getCurrentLocalUserId(context) != null
    }
}

