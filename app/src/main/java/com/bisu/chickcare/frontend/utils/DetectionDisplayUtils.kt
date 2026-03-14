package com.bisu.chickcare.frontend.utils

import androidx.compose.ui.graphics.Color

object DetectionDisplayUtils {
    const val UNKNOWN_THRESHOLD: Float = 0.60f

    fun isUnknown(confidence: Float): Boolean = confidence < UNKNOWN_THRESHOLD

    fun statusText(isHealthy: Boolean, confidence: Float): String {
        return if (isUnknown(confidence)) {
            "Unknown"
        } else {
            if (isHealthy) "Healthy" else "Infected"
        }
    }

    fun statusColor(isHealthy: Boolean, confidence: Float): Color {
        return if (isUnknown(confidence)) {
            Color(0xFF9E9E9E) // gray for unknown
        } else {
            if (isHealthy) Color(0xFF4CAF50) else Color(0xFFEF5350)
        }
    }
}


