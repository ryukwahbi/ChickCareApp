// Updated: C:\Users\PC-1\ChickCare-Thesis\ChickCare\app\src\main\java\com\bisu\chickcare\ui\theme\Type.kt
// Minor improvement: Adjusted font sizes and weights for better readability in dashboard.
package com.bisu.chickcare.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bisu.chickcare.R

val FiraSans = FontFamily(
    // Thin
    Font(R.font.fira_sans_condensed_thin, FontWeight.Thin, FontStyle.Normal),
    Font(R.font.fira_sans_condensed_thin_italic, FontWeight.Thin, FontStyle.Italic),
    // ExtraLight
    Font(R.font.fira_sans_condensed_extralight, FontWeight.ExtraLight, FontStyle.Normal),
    Font(R.font.fira_sans_condensed_extralight_italic, FontWeight.ExtraLight, FontStyle.Italic),
    // Light
    Font(R.font.fira_sans_condensed_light, FontWeight.Light, FontStyle.Normal),
    Font(R.font.fira_sans_condensed_light_italic, FontWeight.Light, FontStyle.Italic),
    // Regular (Normal)
    Font(R.font.fira_sans_condensed_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.fira_sans_condensed_italic, FontWeight.Normal, FontStyle.Italic),
    // Medium
    Font(R.font.fira_sans_condensed_medium, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.fira_sans_condensed_medium_italic, FontWeight.Medium, FontStyle.Italic),
    // SemiBold
    Font(R.font.fira_sans_condensed_semibold, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.fira_sans_condensed_semibold_italic, FontWeight.SemiBold, FontStyle.Italic),
    // Bold
    Font(R.font.fira_sans_condensed_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.fira_sans_condensed_bold_italic, FontWeight.Bold, FontStyle.Italic),
    // ExtraBold
    Font(R.font.fira_sans_condensed_extrabold, FontWeight.ExtraBold, FontStyle.Normal),
    Font(R.font.fira_sans_condensed_extrabold_italic, FontWeight.ExtraBold, FontStyle.Italic),
    // Black
    Font(R.font.fira_sans_condensed_black, FontWeight.Black, FontStyle.Normal),
    Font(R.font.fira_sans_condensed_black_italic, FontWeight.Black, FontStyle.Italic)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FiraSans,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FiraSans,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FiraSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FiraSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FiraSans,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FiraSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FiraSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FiraSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FiraSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FiraSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FiraSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    )
)