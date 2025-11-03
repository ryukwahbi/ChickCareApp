package com.bisu.chickcare.backend.viewmodels

import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.data.WeatherUiState
import com.bisu.chickcare.backend.repository.SettingsRepository
import com.bisu.chickcare.backend.repository.WeatherRepository
import com.bisu.chickcare.backend.service.LocationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.bisu.chickcare.backend.data.ForecastItem

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val locationService = LocationService(application)
    private val weatherRepository = WeatherRepository()
    private val settingsRepository = SettingsRepository(application)
    
    private val _weatherState = MutableStateFlow(WeatherUiState(isLoading = true))
    val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()

    private val _useCelsius = MutableStateFlow(true)
    val useCelsius: StateFlow<Boolean> = _useCelsius.asStateFlow()

    private val _hourly = MutableStateFlow<List<ForecastItem>>(emptyList())
    val hourly: StateFlow<List<ForecastItem>> = _hourly.asStateFlow()

    init {
        // Load persisted unit
        _useCelsius.value = settingsRepository.getUseCelsius()
        // Load cached weather immediately (if any)
        settingsRepository.getCachedWeather()?.let { cached ->
            _weatherState.value = cached.copy(isLoading = false, error = null)
        }
    }

    fun setUseCelsius(value: Boolean) {
        _useCelsius.value = value
        settingsRepository.setUseCelsius(value)
        // Refetch with new units
        fetchWeather(useCelsius = value)
    }

    fun fetchWeather(useCelsius: Boolean = true) {
        viewModelScope.launch {
            _weatherState.value = _weatherState.value.copy(isLoading = true, error = null)
            
            try {
                // Get current location
                val location = locationService.getCurrentLocation()
                
                if (location == null) {
                // Fallback to city-based fetch (default to Manila, PH)
                    val byCity = weatherRepository.getWeatherByCity("Manila,PH", useCelsius)
                    _weatherState.value = byCity
                    settingsRepository.saveCachedWeather(byCity)
                    _hourly.value = weatherRepository.getForecastByCity("Manila,PH", useCelsius)
                    return@launch
                }
                
                // Check if location services are enabled
                val locationManager = getApplication<Application>().getSystemService(Context.LOCATION_SERVICE) as LocationManager
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                    !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    _weatherState.value = WeatherUiState(
                        isLoading = false,
                        error = "Location services are disabled. Please enable location services."
                    )
                    return@launch
                }
                
                // Fetch weather data
                val weatherData = weatherRepository.getWeatherByLocation(location, useCelsius)
                _weatherState.value = weatherData
                settingsRepository.saveCachedWeather(weatherData)
                _hourly.value = weatherRepository.getForecastByLocation(location, useCelsius)
                
            } catch (e: Exception) {
                _weatherState.value = WeatherUiState(
                    isLoading = false,
                    error = "Failed to fetch weather: ${e.message}"
                )
            }
        }
    }
    
    fun refreshWeather(useCelsius: Boolean = true) {
        fetchWeather(useCelsius)
    }
}

