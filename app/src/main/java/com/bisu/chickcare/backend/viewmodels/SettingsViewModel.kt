package com.bisu.chickcare.backend.viewmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

object ThemeViewModel : ViewModel() {
    private const val PREFS_NAME = "chickcare_theme_prefs"
    private const val KEY_DARK_MODE = "is_dark_mode"
    
    private var prefs: SharedPreferences? = null
    
    var isDarkMode by mutableStateOf(false)
        private set

    /**
     * Initialize the theme preference from SharedPreferences
     * Should be called when the app starts (e.g., in Application.onCreate or MainActivity.onCreate)
     */
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Load saved dark mode preference, default to false (light mode)
        isDarkMode = prefs?.getBoolean(KEY_DARK_MODE, false) ?: false
    }

    fun toggleTheme() {
        isDarkMode = !isDarkMode
        // Save the preference immediately when toggled
        savePreference()
    }
    
    /**
     * Save the current dark mode preference to SharedPreferences
     */
    private fun savePreference() {
        prefs?.edit {
            putBoolean(KEY_DARK_MODE, isDarkMode)
        }
    }
}
