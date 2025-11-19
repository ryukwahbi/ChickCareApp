package com.bisu.chickcare.backend.repository

import android.util.Log
import com.bisu.chickcare.backend.data.ForecastItem
import com.bisu.chickcare.backend.data.ForecastResponse
import com.bisu.chickcare.backend.data.WeatherResponse
import com.bisu.chickcare.backend.data.WeatherUiState
import com.bisu.chickcare.backend.service.LocationData
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class WeatherRepository {
    private val apiKey = "0d1953493019c75dd39741fa0785a2dd"
    private val baseUrl = "https://api.openweathermap.org/data/2.5"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()

    suspend fun getWeatherByLocation(location: LocationData, useCelsius: Boolean = true): WeatherUiState {
        return withContext(Dispatchers.IO) {
            try {
                val unitsParam = if (useCelsius) "metric" else "imperial"
                val url = "$baseUrl/weather?lat=${location.latitude}&lon=${location.longitude}&units=$unitsParam&appid=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    Log.e("WeatherRepository", "API call failed: ${response.code}")
                    return@withContext WeatherUiState(error = "Failed to fetch weather: ${response.code}")
                }

                val responseBody = response.body.string()
                val weatherResponse = gson.fromJson(responseBody, WeatherResponse::class.java)
                val currentTemp = weatherResponse.main.temp
                val highTemp = weatherResponse.main.tempMax
                val lowTemp = weatherResponse.main.tempMin
                val feelsLike = weatherResponse.main.feelsLike
                val windSpeed = if (useCelsius) weatherResponse.wind.speed * 3.6 else weatherResponse.wind.speed

                return@withContext WeatherUiState(
                    currentTemp = currentTemp,
                    highTemp = highTemp,
                    lowTemp = lowTemp,
                    humidity = weatherResponse.main.humidity,
                    windSpeed = windSpeed,
                    weatherCondition = weatherResponse.weather.firstOrNull()?.main ?: "Unknown",
                    feelsLike = feelsLike,
                    location = "${weatherResponse.name}, ${weatherResponse.sys.country}",
                    sunriseTime = weatherResponse.sys.sunrise,
                    sunsetTime = weatherResponse.sys.sunset,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                Log.e("WeatherRepository", "Error fetching weather", e)
                return@withContext WeatherUiState(error = "Error: ${e.message}")
            }
        }
    }
    
    suspend fun getWeatherByCity(city: String, useCelsius: Boolean = true): WeatherUiState {
        return withContext(Dispatchers.IO) {
            try {
                val unitsParam = if (useCelsius) "metric" else "imperial"
                val url = "$baseUrl/weather?q=$city&units=$unitsParam&appid=$apiKey"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.e("WeatherRepository", "API call failed: ${response.code}")
                    return@withContext WeatherUiState(error = "Failed to fetch weather: ${response.code}")
                }
                val responseBody = response.body.string()
                val weatherResponse = gson.fromJson(responseBody, WeatherResponse::class.java)
                val currentTemp = weatherResponse.main.temp
                val highTemp = weatherResponse.main.tempMax
                val lowTemp = weatherResponse.main.tempMin
                val feelsLike = weatherResponse.main.feelsLike
                val windSpeed = if (useCelsius) weatherResponse.wind.speed * 3.6 else weatherResponse.wind.speed
                return@withContext WeatherUiState(
                    currentTemp = currentTemp,
                    highTemp = highTemp,
                    lowTemp = lowTemp,
                    humidity = weatherResponse.main.humidity,
                    windSpeed = windSpeed,
                    weatherCondition = weatherResponse.weather.firstOrNull()?.main ?: "Unknown",
                    feelsLike = feelsLike,
                    location = "${weatherResponse.name}, ${weatherResponse.sys.country}",
                    sunriseTime = weatherResponse.sys.sunrise,
                    sunsetTime = weatherResponse.sys.sunset,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                Log.e("WeatherRepository", "Error fetching weather", e)
                return@withContext WeatherUiState(error = "Error: ${e.message}")
            }
        }
    }

    suspend fun getForecastByLocation(location: LocationData, useCelsius: Boolean = true): List<ForecastItem> {
        return withContext(Dispatchers.IO) {
            try {
                val unitsParam = if (useCelsius) "metric" else "imperial"
                val url = "$baseUrl/forecast?lat=${location.latitude}&lon=${location.longitude}&units=$unitsParam&appid=$apiKey"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.e("WeatherRepository", "Forecast call failed: ${response.code}")
                    return@withContext emptyList()
                }
                val responseBody = response.body.string()
                val forecastResponse = gson.fromJson(responseBody, ForecastResponse::class.java)
                return@withContext forecastResponse.list.take(8)
            } catch (e: Exception) {
                Log.e("WeatherRepository", "Error fetching forecast", e)
                return@withContext emptyList()
            }
        }
    }

    suspend fun getForecastByCity(city: String, useCelsius: Boolean = true): List<ForecastItem> {
        return withContext(Dispatchers.IO) {
            try {
                val unitsParam = if (useCelsius) "metric" else "imperial"
                val url = "$baseUrl/forecast?q=$city&units=$unitsParam&appid=$apiKey"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.e("WeatherRepository", "Forecast call failed: ${response.code}")
                    return@withContext emptyList()
                }
                val responseBody = response.body.string()
                val forecastResponse = gson.fromJson(responseBody, ForecastResponse::class.java)
                return@withContext forecastResponse.list.take(8)
            } catch (e: Exception) {
                Log.e("WeatherRepository", "Error fetching forecast", e)
                return@withContext emptyList()
            }
        }
    }
}

