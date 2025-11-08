package com.bisu.chickcare.backend.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import com.bisu.chickcare.backend.utils.LocaleHelper
import java.util.Locale

object LanguageViewModel : ViewModel() {
    private const val PREFS_NAME = "ChickCarePrefs"
    private const val KEY_LANGUAGE = "selected_language"
    
    var currentLanguage by mutableStateOf("en")
        private set
    
    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentLanguage = prefs.getString(KEY_LANGUAGE, Locale.getDefault().language) ?: "en"
    }
    
    fun setLanguage(context: Context, languageCode: String) {
        currentLanguage = languageCode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_LANGUAGE, languageCode) }
        
        // Update app locale
        LocaleHelper.setLocale(context, languageCode)
    }
    
    fun getLocale(): Locale {
        return LocaleHelper.getLocale(currentLanguage)
    }
}

