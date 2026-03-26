package com.bisu.chickcare.backend.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class LoginSessionData(
    val sessionId: String = "",
    val deviceName: String = "",
    val deviceType: String = "phone", // "phone", "computer", "tablet"
    val location: String = "Location unknown",
    val ipAddress: String = "Unknown IP",
    val isActive: Boolean = true,
    @ServerTimestamp
    val timestamp: Date? = null,
    val lastActive: Long = System.currentTimeMillis()
)
