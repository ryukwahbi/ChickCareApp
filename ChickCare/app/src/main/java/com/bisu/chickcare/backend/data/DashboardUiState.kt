package com.bisu.chickcare.backend.data

data class DashboardUiState(
    val isLoading: Boolean = true,
    val userName: String = "User",
    val totalChickens: Int = 0,
    val alerts: Int = 0,
    val isDetecting: Boolean = false,
    val detectionResult: Pair<Boolean, String>? = null,
    val remedySuggestions: List<String> = emptyList()
)
