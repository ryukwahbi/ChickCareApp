package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.bisu.chickcare.R
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

data class FarmTip(
    val id: Int,
    val titleRes: Int,
    val descriptionRes: Int,
    val imageRes: Int,
    val category: String,
    val categoryRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmTipsScreen(navController: NavController) {
    val tips = remember {
        listOf(
            FarmTip(
                id = 1,
                titleRes = R.string.tip_1_title,
                descriptionRes = R.string.tip_1_desc,
                imageRes = R.drawable.chicken_coop_indoor_view_2,
                category = "Environment",
                categoryRes = R.string.tip_cat_env
            ),
            FarmTip(
                id = 2,
                titleRes = R.string.tip_2_title,
                descriptionRes = R.string.tip_2_desc,
                imageRes = R.drawable.chicken_farm_feeding_scene,
                category = "Nutrition",
                categoryRes = R.string.tip_cat_nut
            ),
            FarmTip(
                id = 3,
                titleRes = R.string.tip_3_title,
                descriptionRes = R.string.tip_3_desc,
                imageRes = R.drawable.chicken_health_monitoring,
                category = "Health",
                categoryRes = R.string.tip_cat_health
            ),
            FarmTip(
                id = 4,
                titleRes = R.string.tip_4_title,
                descriptionRes = R.string.tip_4_desc,
                imageRes = R.drawable.chicken_coop_clean_spacious,
                category = "Environment",
                categoryRes = R.string.tip_cat_env
            ),
            FarmTip(
                id = 5,
                titleRes = R.string.tip_5_title,
                descriptionRes = R.string.tip_5_desc,
                imageRes = R.drawable.chicken_coop_indoor_view_2,
                category = "Nutrition",
                categoryRes = R.string.tip_cat_nut
            ),
            FarmTip(
                id = 6,
                titleRes = R.string.tip_6_title,
                descriptionRes = R.string.tip_6_desc,
                imageRes = R.drawable.chicken_farm_feeding_scene,
                category = "Health",
                categoryRes = R.string.tip_cat_health
            ),
            FarmTip(
                id = 7,
                titleRes = R.string.tip_7_title,
                descriptionRes = R.string.tip_7_desc,
                imageRes = R.drawable.chicken_health_monitoring,
                category = "Environment",
                categoryRes = R.string.tip_cat_env
            ),
            FarmTip(
                id = 8,
                titleRes = R.string.tip_8_title,
                descriptionRes = R.string.tip_8_desc,
                imageRes = R.drawable.chicken_coop_clean_spacious,
                category = "Environment",
                categoryRes = R.string.tip_cat_env
            ),
            FarmTip(
                id = 9,
                titleRes = R.string.tip_9_title,
                descriptionRes = R.string.tip_9_desc,
                imageRes = R.drawable.chicken_coop_indoor_view_2,
                category = "Health",
                categoryRes = R.string.tip_cat_health
            ),
            FarmTip(
                id = 10,
                titleRes = R.string.tip_10_title,
                descriptionRes = R.string.tip_10_desc,
                imageRes = R.drawable.chicken_farm_feeding_scene_2,
                category = "Safety",
                categoryRes = R.string.tip_cat_safety
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.farm_tips_title),
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
                            contentDescription = androidx.compose.ui.res.stringResource(R.string.back),
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
                // Video Header
                VideoHeader(
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
fun VideoHeader(
    modifier: Modifier = Modifier
) {
    var isMuted by remember { androidx.compose.runtime.mutableStateOf(false) }
    var isPlaying by remember { androidx.compose.runtime.mutableStateOf(true) }
    var mediaPlayer by remember { androidx.compose.runtime.mutableStateOf<android.media.MediaPlayer?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                try {
                    if (isPlaying) {
                        mediaPlayer?.pause()
                    }
                } catch (_: Exception) {}
            } else if (event == Lifecycle.Event.ON_RESUME) {
                try {
                    if (isPlaying) {
                        mediaPlayer?.start()
                    }
                } catch (_: Exception) {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.stop()
                }
                mediaPlayer?.release()
                mediaPlayer = null
            } catch (_: Exception) {}
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f),
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
            @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    android.widget.VideoView(ctx).apply {
                        val uri = "android.resource://${ctx.packageName}/${R.raw.farm_tips_video}".toUri()
                        setVideoURI(uri)
                        setOnPreparedListener { mp ->
                            mediaPlayer = mp
                            mp.isLooping = true
                            mp.setVolume(1f, 1f)
                            start()
                        }
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        setOnClickListener {
                            isPlaying = !isPlaying
                        }
                    }
                },
                update = { videoView ->
                    if (isPlaying && !videoView.isPlaying) {
                        videoView.start()
                    } else if (!isPlaying && videoView.isPlaying) {
                        videoView.pause()
                    }
                    
                    try {
                        val volume = if (isMuted) 0f else 1f
                        mediaPlayer?.setVolume(volume, volume)
                    } catch (_: Exception) {
                        // Ignore volume setting errors if player not ready
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Play Button Overlay (only visible when paused)
            if (!isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ThemeColorUtils.black(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = androidx.compose.ui.res.stringResource(R.string.tip_play),
                        tint = ThemeColorUtils.white(),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Mute/Unmute Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                IconButton(
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier
                        .background(
                            ThemeColorUtils.black(alpha = 0.5f),
                            CircleShape
                        )
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted) androidx.compose.ui.res.stringResource(R.string.tip_unmute) else androidx.compose.ui.res.stringResource(R.string.tip_mute),
                        tint = ThemeColorUtils.white(),
                        modifier = Modifier.size(20.dp)
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
                contentDescription = androidx.compose.ui.res.stringResource(tip.titleRes),
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
                        text = androidx.compose.ui.res.stringResource(tip.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black()
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                Color(0xFF8B4513).copy(alpha = 0.1f), // Light brown background
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(tip.categoryRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8B4513), // Brown text
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Text(
                    text = "        ${androidx.compose.ui.res.stringResource(tip.descriptionRes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeColorUtils.darkGray(Color(0xFF666666)),
                    textAlign = TextAlign.Justify
                )
            }
        }
    }
}
