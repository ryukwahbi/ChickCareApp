package com.bisu.chickcare.backend.repository

import android.content.Context
import androidx.core.content.edit
import com.bisu.chickcare.backend.data.WeatherUiState
import com.google.gson.Gson

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("chickcare_settings", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getUseCelsius(): Boolean {
        return prefs.getBoolean(KEY_USE_CELSIUS, true)
    }

    fun setUseCelsius(useCelsius: Boolean) {
        prefs.edit { putBoolean(KEY_USE_CELSIUS, useCelsius) }
    }

    fun saveCachedWeather(state: WeatherUiState) {
        // Do not cache loading/error-only states
        if (state.error != null) return
        val json = gson.toJson(state)
        prefs.edit { putString(KEY_CACHED_WEATHER, json) }
    }

    fun getCachedWeather(): WeatherUiState? {
        val json = prefs.getString(KEY_CACHED_WEATHER, null) ?: return null
        return try {
            gson.fromJson(json, WeatherUiState::class.java)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val KEY_USE_CELSIUS = "use_celsius"
        private const val KEY_CACHED_WEATHER = "cached_weather"
    }
}


