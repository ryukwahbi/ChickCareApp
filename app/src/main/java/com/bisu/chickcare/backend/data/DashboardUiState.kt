package com.bisu.chickcare.backend.data


data class TrendDataPoint(
    val label: String,
    val healthyAverage: Double,
    val unhealthyAverage: Double
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val userName: String = "User",
    val totalChickens: Int = 0,
    val totalDetections: Int = 0,
    val healthyRate: Double = 0.0,
    val unhealthyRate: Double = 0.0, // Independent unhealthy rate calculation
    val imageDetections: Int = 0,
    val audioDetections: Int = 0,
    val imageTrendData: List<TrendDataPoint> = emptyList(),
    val audioTrendData: List<TrendDataPoint> = emptyList(),
    val isDetecting: Boolean = false,
    val detectionResult: Pair<Boolean, String>? = null,
    val remedySuggestions: List<String> = emptyList()
)
