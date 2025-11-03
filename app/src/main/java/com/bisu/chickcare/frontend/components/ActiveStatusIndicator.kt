package com.bisu.chickcare.frontend.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Displays a green dot indicator if the user was active within the last 5 minutes
 * @param lastActiveTimestamp Timestamp of when user was last active (milliseconds)
 */
@Composable
fun ActiveStatusIndicator(lastActiveTimestamp: Long, modifier: Modifier = Modifier) {
    val isActive = isUserActive(lastActiveTimestamp)
    
    if (isActive) {
        Box(
            modifier = modifier
                .size(12.dp)
                .background(Color(0xFF4CAF50), CircleShape)
        )
    }
}

private fun isUserActive(lastActiveTimestamp: Long): Boolean {
    if (lastActiveTimestamp == 0L) return false
    
    val now = System.currentTimeMillis()
    val timeDiff = now - lastActiveTimestamp
    val fiveMinutesInMillis = 5 * 60 * 1000
    
    return timeDiff <= fiveMinutesInMillis
}

