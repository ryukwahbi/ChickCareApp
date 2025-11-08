package com.bisu.chickcare.frontend.utils

import androidx.compose.ui.graphics.Color
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel

/**
 * Provides utilities to adapt commonly used neutral colors (white, black, grays, beige)
 * to the active app theme. This centralises the dark-mode palette rules requested by design:
 * - Whites convert to #141617
 * - Blacks convert to #FFFFFF
 * - Light grays convert to #1F1D1D
 * - Dark grays convert to #DCDBDB
 * - Light beige backgrounds convert to #020E1A
 *
 * Other accent colours (green, red, blue, yellow, orange, etc.) intentionally remain unchanged.
 */
object ThemeColorUtils {

    private val DarkWhite = Color(0xFF141617)
    private val DarkSurface = Color(0xFF141617)
    private val DarkBlack = Color(0xFFFFFFFF)
    private val DarkLightGray = Color(0xFF1F1D1D)
    private val DarkDarkGray = Color(0xFFDCDBDB)
    private val DarkBeige = Color(0xFF020E1A)

    enum class NeutralRole {
        White,
        Black,
        LightGray,
        DarkGray,
        Beige,
        None
    }

    /**
     * Returns a colour adjusted for the current theme given a light-mode colour and its neutral role.
     *
     * @param lightColor The colour used in light mode.
     * @param role The neutral role that determines how the colour should transform in dark mode.
     * @param darkOverride Optional explicit colour to use in dark mode instead of the role default.
     * @param preserveLightAlpha When true, copies the alpha value from the light colour to the resolved colour.
     */
    fun themedColor(
        lightColor: Color,
        role: NeutralRole = NeutralRole.None,
        darkOverride: Color? = null,
        preserveLightAlpha: Boolean = true
    ): Color {
        if (!ThemeViewModel.isDarkMode) {
            return lightColor
        }

        val darkColor = when {
            darkOverride != null -> darkOverride
            else -> when (role) {
                NeutralRole.White -> DarkWhite
                NeutralRole.Black -> DarkBlack
                NeutralRole.LightGray -> DarkLightGray
                NeutralRole.DarkGray -> DarkDarkGray
                NeutralRole.Beige -> DarkBeige
                NeutralRole.None -> lightColor
            }
        }

        return if (preserveLightAlpha) {
            darkColor.copy(alpha = lightColor.alpha)
        } else {
            darkColor
        }
    }

    fun white(alpha: Float = 1f): Color =
        if (ThemeViewModel.isDarkMode) DarkWhite else Color.White.copy(alpha = alpha)

    fun black(alpha: Float = 1f): Color =
        if (ThemeViewModel.isDarkMode) Color.White.copy(alpha = alpha) else Color.Black.copy(alpha = alpha)

    fun lightGray(base: Color): Color =
        themedColor(base, role = NeutralRole.LightGray)

    fun darkGray(base: Color): Color =
        themedColor(base, role = NeutralRole.DarkGray)

    fun beige(base: Color): Color =
        themedColor(base, role = NeutralRole.Beige)

    fun inverted(lightColor: Color, darkColor: Color): Color =
        themedColor(lightColor, role = NeutralRole.None, darkOverride = darkColor)

    fun surface(lightColor: Color, darkColor: Color = DarkSurface): Color =
        inverted(lightColor, darkColor)
}

