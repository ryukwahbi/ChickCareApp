package com.bisu.chickcare.frontend.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatters {
    
    /**
     * Format timestamp to readable date with time
     * Example: "Dec 25, 2024 at 03:45 PM"
     */
    fun formatDateTime(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        return format.format(date)
    }
    
    /**
     * Format timestamp to date only
     * Example: "Dec 25, 2024"
     */
    fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return format.format(date)
    }
    
    /**
     * Format timestamp to month and day only (for graphs)
     * Example: "Dec 25"
     */
    fun formatMonthDay(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("MMM dd", Locale.getDefault())
        return format.format(date)
    }
    
    /**
     * Format timestamp to time only
     * Example: "03:45 PM"
     */
    fun formatTime(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return format.format(date)
    }
    
    /**
     * Get relative time (e.g., "2 hours ago", "Yesterday")
     */
    fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val minutes = diff / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            hours < 24 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            days == 1L -> "Yesterday"
            days < 7 -> "$days days ago"
            else -> formatDate(timestamp)
        }
    }
}

