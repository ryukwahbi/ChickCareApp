package com.bisu.chickcare.frontend.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

/**
 * Fullscreen Image Viewer with pinch-to-zoom and swipe navigation
 * 
 * @param images List of image URIs to display
 * @param initialIndex Starting index of the image to show
 * @param onDismiss Callback when the viewer is closed
 */
@Composable
fun FullscreenImageViewer(
    images: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    if (images.isEmpty()) {
        onDismiss()
        return
    }
    
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, images.size - 1),
        pageCount = { images.size }
    )
    
    // Track visibility for controls (hide after tap)
    var showControls by remember { mutableStateOf(true) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showControls = !showControls }
                    )
                }
        ) {
            // Horizontal Pager for swipe navigation
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                ZoomableImage(
                    imageUrl = images[page],
                    onTap = { showControls = !showControls }
                )
            }
            
            // Top controls - Close button and counter
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Image counter
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} of ${images.size}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    
                    // Close button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }
            
            // Bottom indicator dots
            AnimatedVisibility(
                visible = showControls && images.size > 1,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    images.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (index == pagerState.currentPage) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == pagerState.currentPage) 
                                        Color.White 
                                    else 
                                        Color.White.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Zoomable image component with pinch-to-zoom and double-tap zoom
 */
@Composable
fun ZoomableImage(
    imageUrl: String,
    onTap: () -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    // Minimum and maximum zoom levels
    val minScale = 1f
    val maxScale = 5f
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // Toggle between zoomed and normal
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                    onTap = { onTap() }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    do {
                        val event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val pastTouchSlop = event.changes.any { it.positionChange() != Offset.Zero } // Simplification

                        if (zoomChange != 1f || scale > 1f) {
                            // We are zooming or already zoomed, handle gestures
                            val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)
                            
                            // Update offset (for panning when zoomed)
                            // Only update offset if we are actually scaled > 1 (or transitioning)
                            if (newScale > 1f) {
                                val maxX = (size.width * (newScale - 1)) / 2
                                val maxY = (size.height * (newScale - 1)) / 2
                                offset = Offset(
                                    x = (offset.x + panChange.x * newScale).coerceIn(-maxX, maxX),
                                    y = (offset.y + panChange.y * newScale).coerceIn(-maxY, maxY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                            scale = newScale
                            
                            // Consume the changes so parent doesn't see them
                            event.changes.forEach { 
                                if (it.positionChange() != Offset.Zero) it.consume() 
                            }
                        } 
                        // If scale == 1f and not zooming, we do NOT consume the event, 
                        // letting the parent HorizontalPager handle the swipe.
                        
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Full size image",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            contentScale = ContentScale.Fit
        )
    }
}
