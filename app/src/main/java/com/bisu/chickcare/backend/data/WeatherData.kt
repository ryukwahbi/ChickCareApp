package com.bisu.chickcare.backend.data

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val coord: Coordinates,
    val weather: List<Weather>,
    val main: Main,
    val wind: Wind,
    val sys: Sys,
    val name: String
)

data class Coordinates(
    val lat: Double,
    val lon: Double
)

data class Weather(
    val main: String,
    val description: String,
    val icon: String
)

data class Main(
    val temp: Double,
    @field:SerializedName("feels_like")
    val feelsLike: Double,
    @field:SerializedName("temp_min")
    val tempMin: Double,
    @field:SerializedName("temp_max")
    val tempMax: Double,
    val humidity: Int
)

data class Wind(
    val speed: Double
)

data class Sys(
    val country: String,
    val sunrise: Long? = null,
    val sunset: Long? = null
)

data class WeatherUiState(
    val currentTemp: Double = 0.0,
    val highTemp: Double = 0.0,
    val lowTemp: Double = 0.0,
    val humidity: Int = 0,
    val windSpeed: Double = 0.0,
    val weatherCondition: String = "Unknown",
    val feelsLike: Double = 0.0,
    val location: String = "",
    val sunriseTime: Long? = null,
    val sunsetTime: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ForecastResponse(
    val list: List<ForecastItem>,
    val city: ForecastCity
)

data class ForecastItem(
    @field:SerializedName("dt") val timestamp: Long,
    val main: Main,
    val weather: List<Weather>,
    @field:SerializedName("pop") val precipitationProb: Double
)

data class ForecastCity(
    val name: String,
    val country: String,
    val sunrise: Long?,
    val sunset: Long?
)

