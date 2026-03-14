package com.bisu.chickcare.frontend.screen

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isVisible by remember { mutableStateOf(true) }
    val scale = remember { Animatable(0.5f) }
    val progress = remember { Animatable(0f) }

    // Check if user is authenticated
    val isAuthenticated = remember { FirebaseAuth.getInstance().currentUser != null }
    
    // Check if user has seen onboarding
    val hasSeenOnboarding = remember { com.bisu.chickcare.backend.utils.OnboardingPreferences.hasSeenOnboarding(context) }
    
    // Debug logging - check these values in Logcat with tag "WelcomeScreen"
    android.util.Log.d("WelcomeScreen", "isAuthenticated: $isAuthenticated, hasSeenOnboarding: $hasSeenOnboarding")

    // Shorter duration when already logged in (2-3 seconds), full duration when not (6 seconds)
    val scaleDuration = if (isAuthenticated) 800 else 1500
    val scaleDelay = if (isAuthenticated) 200 else 500
    val progressDuration = if (isAuthenticated) 1500 else 6000
    val progressDelay = if (isAuthenticated) 300 else 1000
    val finalDelay = if (isAuthenticated) 1500L else 6000L

    // Static Horizontal Gradient (Steady, from Left to Right)
    // Dark theme uses gray/slate gradient from the palette
    val gradientBrush = if (ThemeViewModel.isDarkMode) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF1A1D21),  // Dark black
                Color(0xFF2D3239),  // Dark slate
                Color(0xFF3D444D),  // Medium slate
                Color(0xFF4D5560)   // Lighter slate
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xA9CB894A),
                Color(color = 0xA9C99F7B),
                Color(0xFFDAC29F)
            )
        )
    }

    // Progress Animation
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = tween(
                durationMillis = scaleDuration,
                easing = { OvershootInterpolator(2f).getInterpolation(it) },
                delayMillis = scaleDelay
            )
        )
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = progressDuration,
                easing = LinearEasing,
                delayMillis = progressDelay
            )
        )
        delay(finalDelay)
        isVisible = false

        // Navigate based on authentication and onboarding status
        when {
            isAuthenticated -> {
                // User is logged in - go to dashboard
                navController.navigate("dashboard") {
                    popUpTo("welcome") { inclusive = true }
                }
            }
            !hasSeenOnboarding -> {
                // First-time user - show onboarding
                navController.navigate("onboarding") {
                    popUpTo("welcome") { inclusive = true }
                }
            }
            else -> {
                // Returning user who has seen onboarding - go to login
                navController.navigate("login") {
                    popUpTo("welcome") { inclusive = true }
                }
            }
        }
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 1000)),
        exit = fadeOut(animationSpec = tween(durationMillis = 500))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Center Content (Icon + Logo)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Box( // Icon Box
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(400.dp)
                        .offset(y = (-140).dp)
                ) {
                    var showGlow by remember { mutableStateOf(false) }

                    // Trigger glow after zoom
                    LaunchedEffect(Unit) {
                        delay(2000)
                        showGlow = true
                    }

                    // Pumping Animation
                    val infiniteTransition = rememberInfiniteTransition(label = "glow_pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )

                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val boxSizeDp = 400.dp

                    // Keep glow radius same as before (based on 300dp size)
                    val glowRadius = with(density) { 150.dp.toPx() }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showGlow,
                        enter = fadeIn(animationSpec = tween(500))
                    ) {
                        Box(
                            modifier = Modifier
                                .size(boxSizeDp)
                                .scale(pulseScale)
                                .alpha(pulseAlpha)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = if (ThemeViewModel.isDarkMode) {
                                            // White glow for dark theme
                                            listOf(
                                                Color.White.copy(alpha = 0.4f),
                                                Color.White.copy(alpha = 0.25f),
                                                Color.White.copy(alpha = 0.1f),
                                                Color.Transparent
                                            )
                                        } else {
                                            // Brown glow for light theme
                                            listOf(
                                                Color(0xFFB9946D).copy(alpha = 0.4f),
                                                Color(0xFF936038).copy(alpha = 0.3f),
                                                Color(0xFF834C10).copy(alpha = 0.1f),
                                                Color.Transparent
                                            )
                                        },
                                        center = androidx.compose.ui.geometry.Offset.Unspecified,
                                        radius = glowRadius
                                    ),
                                    shape = CircleShape
                                )
                        )
                    }

                    Image(
                        painter = painterResource(id = R.drawable.chicken_icon),
                        contentDescription = "ChickCare Icon",
                        modifier = Modifier
                            .size(170.dp)
                            .scale(scale.value)
                    )
                }

                // Floating Logo & Loader (Front Layer)
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = 50.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo with Fade In
                    val logoAlpha = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        delay(1000)
                        logoAlpha.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 2000)
                        )
                    }

                    Image(
                        painter = painterResource(id = R.drawable.chickcare),
                        contentDescription = "ChickCare Logo",
                        modifier = Modifier
                            .size(width = 190.dp, height = 80.dp)
                            .alpha(logoAlpha.value)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Animated Progress Ring
                    CircularProgressIndicator(
                        progress = { progress.value },
                        modifier = Modifier.size(36.dp),
                        color = Color(0xFFFAF7F2),
                        strokeWidth = 4.dp,
                        trackColor = Color(0xFFFAF7F2).copy(alpha = 0.2f),
                        strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(bottom = 38.dp)
            ) {


                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(durationMillis = 2000, delayMillis = 2000))
                ) {
                    Text(
                        text = "Catch Problems Before They Spread",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFFAF7F2),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
