package com.bisu.chickcare.frontend.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bisu.chickcare.backend.repository.DetectionEntry
import com.bisu.chickcare.backend.repository.PostRepository
import com.bisu.chickcare.backend.viewmodels.AuthViewModel
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetectionHistoryScreen(navController: NavController) {
    val dashboardViewModel: DashboardViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val history by dashboardViewModel.detectionHistory.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()
    
    val postRepository = remember { PostRepository() }
    var expandedMenuId by remember { mutableStateOf<String?>(null) }
    var showSuccessMessage by remember { mutableStateOf<String?>(null) }
    var selectedVisibility by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Detection History",
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
                .padding(paddingValues)
        ) {
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No detection history yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ThemeColorUtils.lightGray(Color.Gray)
                    )
                }
            } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                if (showSuccessMessage != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = showSuccessMessage ?: "",
                                modifier = Modifier.padding(16.dp),
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                items(history) { detection ->
                    DetectionHistoryItem(
                        detection = detection,
                        userName = userProfile?.fullName ?: "User",
                        userPhotoUrl = userProfile?.photoUrl,
                        expandedMenuId = expandedMenuId,
                        selectedVisibility = selectedVisibility[detection.id],
                        onMenuExpandedChange = { expandedMenuId = it },
                        onVisibilitySelected = { visibility ->
                            selectedVisibility = selectedVisibility + (detection.id to visibility)
                            expandedMenuId = null
                        },
                        onPostToTimeline = { visibility ->
                            val context = dashboardViewModel.getApplication<android.app.Application>()
                            val userId = authViewModel.getCurrentUserId(context) ?: return@DetectionHistoryItem
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    postRepository.createPost(
                                        userId = userId,
                                        userName = userProfile?.fullName ?: "User",
                                        userPhotoUrl = userProfile?.photoUrl,
                                        location = userProfile?.farmLocation ?: "", // Use farm location
                                        detectionId = detection.id,
                                        detectionResult = detection.result,
                                        isHealthy = detection.isHealthy,
                                        confidence = detection.confidence,
                                        imageUri = detection.imageUri,
                                        audioUri = detection.audioUri,
                                        visibility = visibility,
                                        cloudImageUri = detection.cloudUrl,
                                        cloudAudioUri = detection.cloudAudioUrl
                                    )
                                    // Play success sound on main thread
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        try {
                                            val mediaPlayer = android.media.MediaPlayer.create(context, com.bisu.chickcare.R.raw.posted_sound)
                                            mediaPlayer?.setOnCompletionListener { it.release() }
                                            mediaPlayer?.start()
                                        } catch (e: Exception) {
                                            android.util.Log.e("PostDetection", "Error playing sound: ${e.message}")
                                        }
                                    }
                                    showSuccessMessage = "Posted to timeline as $visibility!"
                                    expandedMenuId = null
                                    selectedVisibility = selectedVisibility - detection.id
                                    // Hide success message after 3 seconds
                                    kotlinx.coroutines.delay(3000)
                                    showSuccessMessage = null
                                } catch (e: Exception) {
                                    showSuccessMessage = "Failed to post: ${e.message}"
                                    kotlinx.coroutines.delay(3000)
                                    showSuccessMessage = null
                                }
                            }
                        }
                    )
                }
            }
        }
    }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun DetectionHistoryItem(
    detection: DetectionEntry,
    userName: String,
    userPhotoUrl: String?,
    expandedMenuId: String?,
    selectedVisibility: String?,
    onMenuExpandedChange: (String?) -> Unit,
    onVisibilitySelected: (String) -> Unit,
    onPostToTimeline: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(detection.timestamp))
    val isMenuExpanded = expandedMenuId == detection.id
    var showDetectionDetails by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFF2C2C2C)) else ThemeColorUtils.white()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                IconButton(
                    onClick = { onMenuExpandedChange(if (isMenuExpanded) null else detection.id) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = ThemeColorUtils.black(),
                        modifier = Modifier.size(20.dp)
                    )
                }
                

                
                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { onMenuExpandedChange(null) },
                    modifier = Modifier.background(ThemeColorUtils.white())
                ) {
                    DropdownMenuItem(
                        text = { Text("Public") },
                        onClick = {
                            onVisibilitySelected("public")
                        }
                    )
                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                    DropdownMenuItem(
                        text = { Text("Private") },
                        onClick = {
                            onVisibilitySelected("private")
                        }
                    )
                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                    DropdownMenuItem(
                        text = { Text("View Details") },
                        onClick = {
                           onMenuExpandedChange(null)
                           showDetectionDetails = true
                        }
                    )
                }
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .padding(top = 4.dp, end = 2.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE1E0E0))
                    ) {
                        if (userPhotoUrl != null && userPhotoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = userPhotoUrl,
                                contentDescription = userName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = userName,
                                tint = Color(0xFF464343),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        }
                    }
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
                            )
                            selectedVisibility?.let { visibility ->
                                Icon(
                                    imageVector = if (visibility == "public") Icons.Default.Public else Icons.Default.Lock,
                                    contentDescription = visibility,
                                    tint = if (ThemeViewModel.isDarkMode) Color(0xFFB0B3B8) else ThemeColorUtils.lightGray(Color.Gray),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = "- $dateString",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (ThemeViewModel.isDarkMode) Color(0xFFB0B3B8) else ThemeColorUtils.lightGray(Color.Gray)
                        )
                    }
                }
                
                Column(
                    modifier = Modifier
                        .padding(start = 64.dp)
                        .offset(y = (-18).dp)
                ) {
                    Text(
                        text = com.bisu.chickcare.frontend.utils.DetectionDisplayUtils.statusText(detection.isHealthy, detection.confidence),
                        style = MaterialTheme.typography.bodyMedium,
                        color = com.bisu.chickcare.frontend.utils.DetectionDisplayUtils.statusColor(detection.isHealthy, detection.confidence),
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (detection.confidence > 0f) {
                        Text(
                            text = "Confidence: ${String.format("%.1f", detection.confidence * 100)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (ThemeViewModel.isDarkMode) Color(0xFFB0B3B8) else ThemeColorUtils.lightGray(Color.Gray)
                        )
                    }
                }
            }
            
            Box(
                contentAlignment = Alignment.Center
            ){
                Card(
                    modifier = Modifier
                        .height(40.dp)
                        .clickable(
                            enabled = selectedVisibility != null,
                            onClick = {
                                selectedVisibility?.let { visibility ->
                                    onPostToTimeline(visibility)
                                }
                            }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedVisibility != null) Color(0xFFE0C9A8) else Color(0xFFE0C9A8).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (selectedVisibility != null) 4.dp else 0.dp
                    )
                ) {
                    Text(
                        text = "Post",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedVisibility != null) Color(0xFF464343) else Color(0xFF464343).copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }


    if (showDetectionDetails) {
        ViewDetectionDialog(
            entry = detection,
            onDismiss = { showDetectionDetails = false }
        )
    }
}

@Composable
fun ViewDetectionDialog(
    entry: DetectionEntry,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var showFullscreenImage by remember { mutableStateOf(false) }
    val imageModel = com.bisu.chickcare.frontend.utils.getAccessibleUri(context, entry.imageUri, entry.cloudUrl)

    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        isPlaying = false
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { stopPlayback() }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFF2C2C2C)) else ThemeColorUtils.white())
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detection Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.offset(x = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.white() else ThemeColorUtils.black(),
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Spacer(modifier = Modifier.height(10.dp))

                // Image
                if (imageModel != null) {
                    val imageRequest = coil.request.ImageRequest.Builder(context)
                        .data(imageModel)
                        .crossfade(true)
                        .build()

                    AsyncImage(
                        model = imageRequest,
                        contentDescription = "Detection Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showFullscreenImage = true },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Results
                 val statusText = com.bisu.chickcare.frontend.utils.DetectionDisplayUtils.statusText(entry.isHealthy, entry.confidence)
                 val statusColor = com.bisu.chickcare.frontend.utils.DetectionDisplayUtils.statusColor(entry.isHealthy, entry.confidence)
                 
                 Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Result: ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                 }
                 
                 if (entry.confidence > 0f) {
                     Text(
                         text = "Confidence: ${String.format("%.1f", entry.confidence * 100)}%",
                         style = MaterialTheme.typography.bodyMedium,
                         color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
                     )
                 }
                 Spacer(modifier = Modifier.height(8.dp))

                // Play Audio button
                val audioUri = entry.audioUri
                val cloudAudioUri = entry.cloudAudioUrl
                val audioModel = com.bisu.chickcare.frontend.utils.getAccessibleUri(context, audioUri, cloudAudioUri)

                if (audioModel != null) {
                    androidx.compose.material3.Button(
                        onClick = {
                            if (isPlaying) {
                                stopPlayback()
                                return@Button
                            }
                            try {
                                stopPlayback()
                                
                                val player = android.media.MediaPlayer()
                                
                                if (audioModel is android.net.Uri) {
                                     player.setDataSource(context, audioModel)
                                } else if (audioModel is String) {
                                     player.setDataSource(audioModel)
                                }
                                
                                player.prepareAsync()
                                player.setOnPreparedListener { 
                                    it.start() 
                                    mediaPlayer = it
                                    isPlaying = true
                                }
                                player.setOnCompletionListener { 
                                    stopPlayback() 
                                }
                                player.setOnErrorListener { _, what, extra ->
                                     stopPlayback()
                                     false
                                }

                            } catch (_: Exception) {
                                stopPlayback()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(999.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = ThemeColorUtils.surface(Color(0xFFE5E2DE)),
                            contentColor = ThemeColorUtils.black()
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else androidx.compose.material.icons.Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Stop audio" else "Play audio",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPlaying) "Stop Audio" else "Play Audio",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Date
                Text(
                    text = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(
                        Date(entry.timestamp)
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFB0B3B8) else Color.Gray
                )
            }
        }
    }

    // Fullscreen image viewer
    if (showFullscreenImage && imageModel != null) {
        com.bisu.chickcare.frontend.components.FullscreenImageViewer(
            images = listOf(imageModel.toString()),
            initialIndex = 0,
            onDismiss = { showFullscreenImage = false }
        )
    }
}