package com.bisu.chickcare.frontend.screen

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.utils.OnboardingPreferences
import kotlinx.coroutines.launch

data class OnboardingPage(
    val backgroundResId: Int,
    val title: String,
    val description: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPage(
            backgroundResId = R.drawable.onboarding_bg,
            title = "Smart Disease Detection",
            description = "Detect chicken health issues using advanced image and audio analysis powered by AI technology."
        ),
        OnboardingPage(
            backgroundResId = R.drawable.onboarding_bg1,
            title = "Track Your Flock",
            description = "Monitor health trends and detection history over time. Keep a complete record of your flock's wellbeing."
        ),
        OnboardingPage(
            backgroundResId = R.drawable.onboarding_bg2,
            title = "Get Expert Tips",
            description = "Receive personalized recommendations and connect with the poultry farming community."
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    fun navigateToLogin() {
        OnboardingPreferences.setOnboardingCompleted(context)
        navController.navigate("login") {
            popUpTo("onboarding") { inclusive = true }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Horizontal Pager for slides
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingPageContent(page = pages[page])
        }

        // Skip button (top-right)
        Text(
            text = "SKIP",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 24.dp)
                .clickable { navigateToLogin() }
        )

        // Bottom section: Page indicator + Get Started button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page indicator dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width = animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        label = "indicator_width"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width.value)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White
                                else Color.White.copy(alpha = 0.5f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Get Started button (always visible, but can change behavior)
            Button(
                onClick = {
                    if (pagerState.currentPage == pages.size - 1) {
                        // Last page - navigate to login
                        navigateToLogin()
                    } else {
                        // Not last page - go to next page
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF2F1801)
                )
            ) {
                Text(
                    text = if (pagerState.currentPage == pages.size - 1) "GET STARTED" else "NEXT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        Image(
            painter = painterResource(id = page.backgroundResId),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.75f),
                            Color.Black.copy(alpha = 0.88f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Text content at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, end = 24.dp, bottom = 180.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = page.title,
                fontFamily = com.bisu.chickcare.ui.theme.FiraSans,
                fontWeight = FontWeight.ExtraBold, // 800
                color = Color.White,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = page.description,
                fontFamily = com.bisu.chickcare.ui.theme.FiraSans,
                fontWeight = FontWeight.Normal, // 400
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Start
            )
        }
    }
}
