package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
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
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetectionHistoryScreen(navController: NavController) {
    val dashboardViewModel: DashboardViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val history by dashboardViewModel.detectionHistory.collectAsState()
    val userProfile by authViewModel.userProfile.collectAsState()
    val auth = FirebaseAuth.getInstance()
    
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
                    containerColor = ThemeColorUtils.white()
                )
            )
        }
    ) { paddingValues ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
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
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
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
                            val userId = auth.currentUser?.uid ?: return@DetectionHistoryItem
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    postRepository.createPost(
                                        userId = userId,
                                        userName = userProfile?.fullName ?: "User",
                                        userPhotoUrl = userProfile?.photoUrl,
                                        detectionId = detection.id,
                                        detectionResult = detection.result,
                                        isHealthy = detection.isHealthy,
                                        confidence = detection.confidence,
                                        imageUri = detection.imageUri,
                                        audioUri = detection.audioUri,
                                        visibility = visibility
                                    )
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
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(detection.timestamp))
    val isMenuExpanded = expandedMenuId == detection.id
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.white()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box {
                IconButton(
                    onClick = { onMenuExpandedChange(if (isMenuExpanded) null else detection.id) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = Color(0xFF100E0E),
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
                }
            }
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
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
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColorUtils.black()
                        )
                        selectedVisibility?.let { visibility ->
                            Icon(
                                imageVector = if (visibility == "public") Icons.Default.Public else Icons.Default.Lock,
                                contentDescription = visibility,
                                tint = ThemeColorUtils.lightGray(Color.Gray),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeColorUtils.lightGray(Color.Gray)
                    )
                }
                
                Text(
                    text = detection.result,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (detection.isHealthy) Color(0xFF4CAF50) else Color(0xFFEF5350),
                    fontWeight = FontWeight.Medium
                )
                
                if (detection.confidence > 0f) {
                    Text(
                        text = "Confidence: ${(detection.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeColorUtils.lightGray(Color.Gray)
                    )
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
                    shape = RoundedCornerShape(6.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (selectedVisibility != null) 4.dp else 0.dp
                    )
                ) {
                    Text(
                        text = "Post",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedVisibility != null) Color(0xFF464343) else Color(0xFF464343).copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
