package com.bisu.chickcare.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8B4513),
    secondary = Color(0xFFDAA520),
    tertiary = Color(0xFFF5F5DC),
    background = Color(0xFFFFF7E6),
    surface = Color(0xFFD2B48C),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color(0xFF8B4513),
    onBackground = Color(0xFF2F1801),
    onSurface = Color.Black
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFDAB9),
    secondary = Color(0xFFDAA520),
    tertiary = Color(0xFF8B5A2B),
    background = Color(0xFF2F1801),
    surface = Color(0xFF5C4033),
    onPrimary = Color(0xFF4A2C0A),
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun ChickCareAppTheme(
    content: @Composable () -> Unit
) {
    // Directly use the singleton ThemeViewModel to get the theme state
    val isDarkTheme = ThemeViewModel.isDarkMode

    val colorScheme = when {
        isDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(24.dp)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDarkTheme
            insetsController.isAppearanceLightNavigationBars = !isDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = shapes,
        content = content
    )
}
