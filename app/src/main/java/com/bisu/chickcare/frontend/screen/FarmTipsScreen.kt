package com.bisu.chickcare.frontend.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bisu.chickcare.R
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

data class FarmTip(
    val id: Int,
    val title: String,
    val description: String,
    val imageRes: Int,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmTipsScreen(navController: NavController) {
    val tips = remember {
        listOf(
            FarmTip(
                id = 1,
                title = "Proper Ventilation",
                description = "Maintain adequate airflow in your chicken coop to prevent respiratory diseases. Install ventilation systems that allow fresh air circulation while protecting chickens from drafts. Poor ventilation can lead to ammonia buildup and respiratory infections.",
                imageRes = R.drawable.chicken_coop_indoor_view_2,
                category = "Environment"
            ),
            FarmTip(
                id = 2,
                title = "Clean Water Daily",
                description = "Provide fresh, clean water to your chickens every day. Water containers should be cleaned regularly to prevent bacterial growth. Chickens need constant access to water for proper digestion and health. Dehydration can lead to decreased egg production and health issues.",
                imageRes = R.drawable.chicken_farm_feeding_scene,
                category = "Nutrition"
            ),
            FarmTip(
                id = 3,
                title = "Regular Health Monitoring",
                description = "Use the app's image and audio detection features to monitor your chickens daily. Early detection of health issues can prevent disease spread and reduce losses. Watch for unusual behavior, sounds, or visual symptoms that indicate illness.",
                imageRes = R.drawable.chicken_health_monitoring,
                category = "Health"
            ),
            FarmTip(
                id = 4,
                title = "Clean and Dry Coop",
                description = "Keep the chicken coop clean and dry at all times. Regular cleaning prevents the buildup of harmful bacteria and parasites. Wet bedding can cause foot problems and respiratory issues. Replace bedding material weekly or as needed.",
                imageRes = R.drawable.chicken_coop_clean_spacious,
                category = "Environment"
            ),
            FarmTip(
                id = 5,
                title = "Balanced Nutrition",
                description = "Provide a balanced diet with proper feed composition suitable for your chickens' age and purpose (laying, broiler, etc.). Include necessary vitamins and minerals. Avoid overfeeding or underfeeding, as both can cause health problems.",
                imageRes = R.drawable.chicken_coop_indoor_view_2,
                category = "Nutrition"
            ),
            FarmTip(
                id = 6,
                title = "Isolate Sick Chickens",
                description = "Immediately isolate any chicken showing signs of illness to prevent disease spread to the rest of the flock. Keep a separate quarantine area ready. Monitor isolated chickens closely and consult a veterinarian if symptoms persist.",
                imageRes = R.drawable.chicken_farm_feeding_scene,
                category = "Health"
            ),
            FarmTip(
                id = 7,
                title = "Temperature Control",
                description = "Maintain optimal temperature in the coop (18-24°C). Extreme temperatures can stress chickens and affect their health and productivity. Use heaters or cooling systems as needed, and ensure proper insulation for year-round comfort.",
                imageRes = R.drawable.chicken_health_monitoring,
                category = "Environment"
            ),
            FarmTip(
                id = 8,
                title = "Adequate Space",
                description = "Provide sufficient space for your chickens to move, perch, and nest comfortably. Overcrowding leads to stress, feather picking, and disease spread. Follow recommended space requirements: 2-4 square feet per chicken in the coop.",
                imageRes = R.drawable.chicken_coop_clean_spacious,
                category = "Environment"
            ),
            FarmTip(
                id = 9,
                title = "Regular Vaccination",
                description = "Follow a vaccination schedule recommended by veterinarians to protect your flock from common diseases. Keep vaccination records and ensure all new birds are properly vaccinated before introducing them to the flock.",
                imageRes = R.drawable.chicken_coop_indoor_view_2,
                category = "Health"
            ),
            FarmTip(
                id = 10,
                title = "Predator Protection",
                description = "Secure your coop with proper fencing and locks to protect chickens from predators. Check for gaps, holes, or weak spots regularly. Use motion-activated lights and secure doors at night. Predators can cause stress and injuries to your flock.",
                imageRes = R.drawable.chicken_farm_feeding_scene_2,
                category = "Safety"
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Farm Tips & Best Practices",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ThemeColorUtils.black()
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ThemeColorUtils.black()
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeColorUtils.white(),
                    titleContentColor = ThemeColorUtils.black()
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
        ) {
            HorizontalDivider(
                color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)),
                thickness = 1.dp
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            item {
                // Header with carousel
                TipsCarouselHeader(
                    tips = tips.take(4),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Tips list
            items(tips, key = { it.id }) { tip ->
                FarmTipCard(tip = tip)
            }
        }
        }
    }
}

@Composable
fun TipsCarouselHeader(
    tips: List<FarmTip>,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var slideDirection by remember { mutableIntStateOf(1) }
    
    Card(
        modifier = modifier.height(200.dp),
        border = BorderStroke(
            width = 1.dp,
            color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C))
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.surface(Color(0xFFE5E2DE)),
            contentColor = ThemeColorUtils.black()
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
        ) {
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
                label = "tips_carousel"
            ) { index ->
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = tips[index].imageRes),
                        contentDescription = tips[index].title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Gradient overlay for text readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        ThemeColorUtils.black(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                    
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = tips[index].title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColorUtils.white()
                        )
                        Text(
                            text = tips[index].category,
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeColorUtils.white(alpha = 0.9f)
                        )
                    }
                }
            }
            
            // Navigation arrows
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            ) {
                IconButton(
                    onClick = {
                        val newIndex = if (currentIndex > 0) currentIndex - 1 else tips.size - 1
                        slideDirection = if (newIndex < currentIndex) -1 else 1
                        currentIndex = newIndex
                    },
                    modifier = Modifier
                        .background(ThemeColorUtils.black(alpha = 0.5f), CircleShape)
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
            
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            ) {
                IconButton(
                    onClick = {
                        val newIndex = if (currentIndex < tips.size - 1) currentIndex + 1 else 0
                        slideDirection = if (newIndex > currentIndex) 1 else -1
                        currentIndex = newIndex
                    },
                    modifier = Modifier
                        .background(ThemeColorUtils.black(alpha = 0.5f), CircleShape)
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
            
            // Indicator dots
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tips.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentIndex) 8.dp else 6.dp)
                            .background(
                                if (index == currentIndex) ThemeColorUtils.white() else ThemeColorUtils.white(alpha = 0.5f),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun FarmTipCard(tip: FarmTip) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            width = 1.dp,
            color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C))
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.surface(Color(0xFFE5E2DE)),
            contentColor = ThemeColorUtils.black()
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Image
            Image(
                painter = painterResource(id = tip.imageRes),
                contentDescription = tip.title,
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = tip.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black()
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                when (tip.category) {
                                    "Health" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    "Nutrition" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                    "Environment" -> Color(0xFF2196F3).copy(alpha = 0.2f)
                                    else -> Color(0xFF9C27B0).copy(alpha = 0.2f)
                                },
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tip.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (tip.category) {
                                "Health" -> Color(0xFF4CAF50)
                                "Nutrition" -> Color(0xFFFF9800)
                                "Environment" -> Color(0xFF2196F3)
                                else -> Color(0xFF9C27B0)
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Text(
                    text = tip.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeColorUtils.darkGray(Color(0xFF666666))
                )
            }
        }
    }
}
