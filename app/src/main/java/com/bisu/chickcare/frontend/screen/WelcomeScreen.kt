package com.bisu.chickcare.frontend.screen

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bisu.chickcare.R
import kotlinx.coroutines.delay
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

@Composable
fun WelcomeScreen(navController: NavController) {
    var isVisible by remember { mutableStateOf(true) }
    val scale = remember { Animatable(0.5f) }
    val progress = remember { Animatable(0f) }

    // Dynamic Gradient with Subtle Animation
    val gradientBackground = remember { mutableStateOf(
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF8B4513),
                Color(0xFFD2B48C)
            )
        )
    )}
    LaunchedEffect(Unit) {
        while (isVisible) {
            gradientBackground.value = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF8B4513).copy(alpha = 0.8f),
                    Color(0xFFD2B48C).copy(alpha = 0.6f)
                ),
                startY = 0f,
                endY = 1000f
            )
            delay(3000)
            gradientBackground.value = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFD2B48C).copy(alpha = 0.6f),
                    Color(0xFF8B4513).copy(alpha = 0.8f)
                ),
                startY = 0f,
                endY = 1000f
            )
            delay(3000)
        }
    }

    // Progress Animation
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = tween(
                durationMillis = 1500,
                easing = { OvershootInterpolator(2f).getInterpolation(it) },
                delayMillis = 500
            )
        )
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 6000,
                easing = LinearEasing,
                delayMillis = 1000
            )
        )
        delay(6000)
        isVisible = false
        navController.navigate("login") {
            popUpTo("welcome") { inclusive = true }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 1000)),
        exit = fadeOut(animationSpec = tween(durationMillis = 500))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBackground.value)
                .padding(16.dp)
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        drawCircle(
                            color = ThemeColorUtils.black(alpha = 0.05f),
                            radius = size.minDimension / 4,
                            center = center
                        )
                    }
                },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.chicken_icon),
                contentDescription = "ChickCare Logo",
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale.value)
            )

            Spacer(modifier = Modifier.size(32.dp))

            val shimmer by animateFloatAsState(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "shimmer"
            )

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(durationMillis = 2000, delayMillis = 1000))
            ) {
                Text(
                    text = "ChickCare",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF2F1801),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.size(16.dp))

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(durationMillis = 2000, delayMillis = 2000))
            ) {
                Text(
                    text = "Catch Problems Before They Spread",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = shimmer),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.size(32.dp))

            // Animated Progress Ring
            CircularProgressIndicator(
                progress = { progress.value },
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 4.dp,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap
            )
        }
    }
}