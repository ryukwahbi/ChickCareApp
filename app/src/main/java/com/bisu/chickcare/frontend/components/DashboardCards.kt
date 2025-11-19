package com.bisu.chickcare.frontend.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.repository.DetectionEntry
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel
import com.bisu.chickcare.frontend.utils.Dimens
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import androidx.compose.foundation.ExperimentalFoundationApi as ExperimentalFoundationApiImport

@OptIn(ExperimentalFoundationApiImport::class)
@Composable
fun ChickenGalleryCard() {
    val images = remember {
        listOf(
            R.drawable.chicken_background_1,
            R.drawable.chicken_background_2,
            R.drawable.chicken_background_3,
            R.drawable.chicken_background_4
        )
    }
    
    var currentIndex by remember { mutableIntStateOf(0) }
    var slideDirection by remember { mutableIntStateOf(1) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingLarge),
        colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White.copy(alpha = 0.9f))),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            // Animated image transition
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth * slideDirection },
                        animationSpec = tween(durationMillis = 300)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth * slideDirection },
                        animationSpec = tween(durationMillis = 300)
                    )
                },
                label = "image_carousel"
            ) { index ->
                Image(
                    painter = painterResource(id = images[index]),
                    contentDescription = "Chicken Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Left navigation arrow (Previous)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            ) {
                androidx.compose.material3.IconButton(
                    onClick = {
                        val newIndex = if (currentIndex > 0) currentIndex - 1 else images.size - 1
                        slideDirection = if (newIndex < currentIndex) -1 else 1
                        currentIndex = newIndex
                    },
                    modifier = Modifier
                        .background(
                            ThemeColorUtils.black(alpha = 0.5f),
                            androidx.compose.foundation.shape.CircleShape
                        )
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous",
                        tint = ThemeColorUtils.white(),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            // Right navigation arrow (Next)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            ) {
                androidx.compose.material3.IconButton(
                    onClick = {
                        val newIndex = if (currentIndex < images.size - 1) currentIndex + 1 else 0
                        slideDirection = if (newIndex > currentIndex) 1 else -1
                        currentIndex = newIndex
                    },
                    modifier = Modifier
                        .background(
                            ThemeColorUtils.black(alpha = 0.5f),
                            androidx.compose.foundation.shape.CircleShape
                        )
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next",
                        tint = ThemeColorUtils.white(),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun StatsSummaryCard(
    totalDetections: Int,
    healthyRate: Double,
    unhealthyRate: Double = 0.0,
    imageDetections: Int,
    audioDetections: Int,
    detectionHistory: List<DetectionEntry> = emptyList()
) {
    val calendar = java.util.Calendar.getInstance()
    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
    calendar.set(java.util.Calendar.MINUTE, 0)
    calendar.set(java.util.Calendar.SECOND, 0)
    calendar.set(java.util.Calendar.MILLISECOND, 0)
    val startOfDay = calendar.timeInMillis
    
    // Filter today's detections and calculate sum of all confidence percentages
    val todaysDetections = remember(detectionHistory, startOfDay) {
        detectionHistory.filter { it.timestamp >= startOfDay }
    }
    
    val dailySum = remember(todaysDetections) {
        if (todaysDetections.isNotEmpty()) {
            // Sum all confidence percentages from today's detections
            // Each confidence is multiplied by 100 to convert to percentage, then summed
            todaysDetections.sumOf { it.confidence * 100.0 }
        } else {
            0.0
        }
    }
    
    Column(
        modifier = Modifier.padding(horizontal = Dimens.PaddingLarge),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp),
                colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White.copy(alpha = 0.9f))),
                elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevation)
            ) {
                Column(
                    modifier = Modifier
                        .padding(Dimens.PaddingLarge)
                        .fillMaxWidth()
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Daily Average",
                        style = MaterialTheme.typography.titleSmall,
                        color = ThemeColorUtils.darkGray(Color(0xFF666666))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (dailySum == dailySum.toInt().toDouble()) {
                            "${dailySum.toInt()}%"
                        } else {
                            "${String.format("%.1f", dailySum)}%"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (todaysDetections.isNotEmpty()) ThemeColorUtils.black() else ThemeColorUtils.darkGray(Color(0xFF666666))
                    )
                }
            }
            StatCard(
                title = "Today's Detection",
                value = todaysDetections.size.toString(),
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
            )
        }
        StatCard(
            title = "Overall Detections",
            value = (imageDetections + audioDetections).toString(),
            modifier = Modifier.fillMaxWidth()
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White.copy(alpha = 0.9f))),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HealthyRatePieChart(
                    healthyRate = healthyRate,
                    unhealthyRate = unhealthyRate,
                    chartType = ChartType.HEALTHY_ONLY,
                    modifier = Modifier.weight(1f)
                )
                
                HealthyRatePieChart(
                    healthyRate = healthyRate,
                    unhealthyRate = unhealthyRate,
                    chartType = ChartType.UNHEALTHY_ONLY,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun DetectionHistoryCard(
    detectionHistory: List<DetectionEntry>,
    onViewAllClicked: () -> Unit,
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingLarge),
        elevation = CardDefaults.cardElevation(Dimens.CardElevation),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.white()
        )
    ) {
        Column(modifier = Modifier.padding(Dimens.PaddingLarge)) {
            Text(
                "Recent Detections",
                style = MaterialTheme.typography.titleLarge,
                color = ThemeColorUtils.black()
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (detectionHistory.isEmpty()) {
                Text(
                    "No recent detections found.",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center,
                    color = ThemeColorUtils.black()
                )
            } else {
                detectionHistory.take(3).forEach { entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(vertical = Dimens.PaddingSmall)
                    ) {
                        Icon(
                            imageVector = if (entry.isHealthy) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = "Status",
                            tint = if (entry.isHealthy) Color.Green else Color.Red
                        )
                        Spacer(modifier = Modifier.width(Dimens.PaddingMedium))
                        Text(
                            dashboardViewModel.formatDate(entry.timestamp),
                            modifier = Modifier.weight(1f),
                            color = ThemeColorUtils.black()
                        )
                        Text(
                            text = formatDetectionResult(entry.result),
                            fontWeight = FontWeight.Bold,
                            color = if (entry.isHealthy) Color(0xFF4CAF50) else Color(0xFFEF5350)
                        )
                    }
                }
                if (detectionHistory.size > 3) {
                    Text(
                        text = "View all...",
                        color = ThemeColorUtils.darkGray(Color(0xFF666666)),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(top = Dimens.PaddingMedium)
                            .align(Alignment.End)
                            .clickable { onViewAllClicked() }
                    )
                }
            }
        }
    }
}

private fun formatDetectionResult(rawResult: String): String {
    val trimmed = rawResult.trim()
    val parts = trimmed.split(" ", limit = 2)
    return if (parts.size == 2) {
        val label = parts[0].trim().trimEnd(':')
        val percentage = parts[1]
            .replace("%%", "%")
            .replace("�", "%")
            .replace("%", "")
            .trim()
            .trimStart('(')
            .trimEnd(')')
        "$label: $percentage%"
    } else {
        trimmed.replace("�", "%")
    }
}

@Composable
fun FarmTipsCard(navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.PaddingLarge),
        colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White.copy(alpha = 0.9f))),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevation),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(Dimens.PaddingLarge)) {
            Text(
                "Best Practices",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ThemeColorUtils.black()
            )
            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
            
            // Tips list (non-scrollable, shows first few tips)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "1. Ensure proper ventilation to prevent respiratory issues.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeColorUtils.black()
                )
                Text(
                    "2. Provide clean water daily to maintain chicken health.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeColorUtils.black()
                )
                Text(
                    "3. Monitor chicken behavior daily using image and audio detection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeColorUtils.black()
                )
                Text(
                    "4. Keep the coop clean and dry to prevent diseases.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeColorUtils.black()
                )
            }
            
            Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
            
            Text(
                text = "See more...",
                color = ThemeColorUtils.darkGray(Color(0xFF666666)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clickable { navController.navigate("farm_tips") }
                    .align(Alignment.End)
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White.copy(alpha = 0.9f))),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevation)
    ) {
        Column(
            modifier = Modifier
                .padding(Dimens.PaddingLarge)
                .fillMaxWidth()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = ThemeColorUtils.darkGray(Color(0xFF666666))
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (valueColor == Color.Unspecified) ThemeColorUtils.black() else valueColor
            )
        }
    }
}

