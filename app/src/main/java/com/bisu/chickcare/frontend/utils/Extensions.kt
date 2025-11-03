package com.bisu.chickcare.frontend.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * Extension to capitalize first letter of each word
 */
fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercaseChar() }
    }
}

/**
 * Extension to get initials from full name
 */
fun String.getInitials(): String {
    return split(" ").take(2).joinToString("") { word ->
        word.firstOrNull()?.uppercaseChar().toString()
    }
}

/**
 * Extension to format file size
 */
fun Long.formatFileSize(): String {
    val kb = this / 1024
    val mb = kb / 1024
    return when {
        mb >= 1 -> String.format("%.2f MB", mb.toFloat())
        kb >= 1 -> String.format("%.2f KB", kb.toFloat())
        else -> "$this bytes"
    }
}

/**
 * Extension to format large numbers with K, M suffixes
 */
fun Int.formatCompact(): String {
    return when {
        this >= 1_000_000 -> String.format("%.1fM", this / 1_000_000.0)
        this >= 1_000 -> String.format("%.1fK", this / 1_000.0)
        else -> "$this"
    }
}

/**
 * Extension to create styled text with highlighted parts
 */
fun String.highlightText(query: String): AnnotatedString {
    if (query.isEmpty()) return AnnotatedString(this)
    
    return buildAnnotatedString {
        var startIndex = 0
        var currentIndex = indexOf(query, startIndex, ignoreCase = true)
        
        while (currentIndex != -1) {
            // Add text before highlight
            if (currentIndex > startIndex) {
                append(substring(startIndex, currentIndex))
            }
            
            // Add highlighted text
            withStyle(style = SpanStyle()) {
                append(substring(currentIndex, currentIndex + query.length))
            }
            
            startIndex = currentIndex + query.length
            currentIndex = indexOf(query, startIndex, ignoreCase = true)
        }
        
        // Add remaining text
        if (startIndex < length) {
            append(substring(startIndex))
        }
    }
}

