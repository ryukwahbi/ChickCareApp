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

    // Notification Settings
    fun getPushNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_PUSH_NOTIFICATIONS, true)
    fun setPushNotificationsEnabled(enabled: Boolean) = prefs.edit { putBoolean(KEY_PUSH_NOTIFICATIONS, enabled) }

    fun getDetectionAlertsEnabled(): Boolean = prefs.getBoolean(KEY_DETECTION_ALERTS, true)
    fun setDetectionAlertsEnabled(enabled: Boolean) = prefs.edit { putBoolean(KEY_DETECTION_ALERTS, enabled) }

    fun getReminderNotificationsEnabled(): Boolean = prefs.getBoolean(KEY_REMINDER_NOTIFICATIONS, true)
    fun setReminderNotificationsEnabled(enabled: Boolean) = prefs.edit { putBoolean(KEY_REMINDER_NOTIFICATIONS, enabled) }

    fun getFriendRequestsEnabled(): Boolean = prefs.getBoolean(KEY_FRIEND_REQUESTS, true)
    fun setFriendRequestsEnabled(enabled: Boolean) = prefs.edit { putBoolean(KEY_FRIEND_REQUESTS, enabled) }

    fun getVibrationEnabled(): Boolean = prefs.getBoolean(KEY_VIBRATION, true)
    fun setVibrationEnabled(enabled: Boolean) = prefs.edit { putBoolean(KEY_VIBRATION, enabled) }

    fun getSoundEnabled(): Boolean = prefs.getBoolean(KEY_SOUND, true)
    fun setSoundEnabled(enabled: Boolean) = prefs.edit { putBoolean(KEY_SOUND, enabled) }

    companion object {
        private const val KEY_USE_CELSIUS = "use_celsius"
        private const val KEY_CACHED_WEATHER = "cached_weather"
        
        private const val KEY_PUSH_NOTIFICATIONS = "push_notifications_enabled"
        private const val KEY_DETECTION_ALERTS = "detection_alerts_enabled"
        private const val KEY_REMINDER_NOTIFICATIONS = "reminder_notifications_enabled"
        private const val KEY_FRIEND_REQUESTS = "friend_requests_enabled"
        private const val KEY_VIBRATION = "vibration_enabled"
        private const val KEY_SOUND = "sound_enabled"
    }
}


