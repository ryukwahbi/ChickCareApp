package com.bisu.chickcare.frontend.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bisu.chickcare.backend.data.UserProfile
import com.bisu.chickcare.backend.repository.FriendSuggestion
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import com.bisu.chickcare.frontend.utils.sanitizeToUri
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TimelineTabContent(
    userProfile: UserProfile,
    suggestionCount: Int,
    onFriendSuggestions: () -> Unit,
    onNavigateToPost: () -> Unit,
    isViewingOwnProfile: Boolean,
    timelineUserId: String,
    navController: NavController? = null
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .imePadding()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Dismiss keyboard when clicking outside input fields
                focusManager.clearFocus()
                keyboardController?.hide()
            },
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isViewingOwnProfile) {
            FriendSuggestionsSection(
                suggestionCount = suggestionCount,
                onClick = onFriendSuggestions
            )
        }
        
        PostsSection(
            userProfile = userProfile,
            onNavigateToPost = onNavigateToPost,
            isViewingOwnProfile = isViewingOwnProfile,
            timelineUserId = timelineUserId,
            navController = navController
        )
    }
}

@Composable
fun AboutTabContent(
    userProfile: UserProfile,
    mutualFriends: List<FriendSuggestion>,
    onEditInfo: () -> Unit,
    onPrivacyChange: (String, String) -> Unit,
    isViewingOwnProfile: Boolean = true,
    onViewFriends: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = ThemeColorUtils.white()
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Personal Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // Only show edit icon when viewing own profile
                    if (isViewingOwnProfile) {
                        IconButton(onClick = onEditInfo) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Info",
                                tint = Color(0xFF2F2F2F)
                            )
                        }
                    }
                }
                
                ProfileInfo(
                    userProfile = userProfile,
                    onPrivacyChange = if (isViewingOwnProfile) onPrivacyChange else null,
                    isViewingOwnProfile = isViewingOwnProfile
                )
            }
        }
        
        MutualFriendsSection(
            mutualFriends = mutualFriends,
            onViewMore = if (mutualFriends.size > 3) onViewFriends else null
        )
    }
}

@Composable
fun ProfileInfo(
    userProfile: UserProfile,
    onPrivacyChange: ((String, String) -> Unit)?,
    isViewingOwnProfile: Boolean = true
) {
    val memberSince = if (userProfile.createdAt > 0L) {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        dateFormat.format(Date(userProfile.createdAt))
    } else {
        "Not available"
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ========== PERSONAL INFORMATION SECTION ==========
        InfoRow(
            label = "Email",
            value = userProfile.email,
            fieldName = "email",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["email"] ?: "public",
            isViewingOwnProfile = isViewingOwnProfile
        )
        InfoRow(
            label = "Contact Number",
            value = userProfile.contact,
            fieldName = "contact",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["contact"] ?: "public",
            isViewingOwnProfile = isViewingOwnProfile
        )
        InfoRow(
            label = "Birth Date",
            value = userProfile.birthDate,
            fieldName = "birthDate",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["birthDate"] ?: "public",
            isViewingOwnProfile = isViewingOwnProfile
        )
        InfoRow(
            label = "Gender",
            value = userProfile.gender ?: "Not set",
            fieldName = "gender",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["gender"] ?: "public",
            isEmpty = userProfile.gender.isNullOrEmpty(),
            isViewingOwnProfile = isViewingOwnProfile
        )
        InfoRow(
            label = "Address",
            value = userProfile.address.ifEmpty { "Not set" },
            fieldName = "address",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["address"] ?: "public",
            isEmpty = userProfile.address.isEmpty(),
            isViewingOwnProfile = isViewingOwnProfile
        )
        
        // Line divider between Personal Info and Farm Details
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = Color(0xFFE5E7EB)
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        // ========== FARM DETAILS SECTION ==========
        InfoRow(
            label = "Farm Name",
            value = userProfile.farmName.ifEmpty { "Not set" },
            fieldName = "farmName",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["farmName"] ?: "public",
            isEmpty = userProfile.farmName.isEmpty(),
            isViewingOwnProfile = isViewingOwnProfile
        )
        InfoRow(
            label = "Farm Location",
            value = userProfile.farmLocation.ifEmpty { "Not set" },
            fieldName = "farmLocation",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["farmLocation"] ?: "public",
            isEmpty = userProfile.farmLocation.isEmpty(),
            isViewingOwnProfile = isViewingOwnProfile
        )
        InfoRow(
            label = "Farm Type",
            value = userProfile.farmType.ifEmpty { "Not set" },
            fieldName = "farmType",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["farmType"] ?: "public",
            isEmpty = userProfile.farmType.isEmpty(),
            isViewingOwnProfile = isViewingOwnProfile
        )
        InfoRow(
            label = "Specialization",
            value = userProfile.specialization.ifEmpty { "Not set" },
            fieldName = "specialization",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["specialization"] ?: "public",
            isEmpty = userProfile.specialization.isEmpty(),
            isViewingOwnProfile = isViewingOwnProfile
        )
        
        // Line divider between Farm Details and Statistics
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = Color(0xFFE5E7EB)
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        // ========== STATISTICS SECTION ==========
        InfoRow(
            label = "Number of Chickens",
            value = userProfile.numberOfBirds.ifEmpty { "Not set" },
            fieldName = "numberOfBirds",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["numberOfBirds"] ?: "public",
            isEmpty = userProfile.numberOfBirds.isEmpty(),
            isViewingOwnProfile = isViewingOwnProfile
        )
        InfoRow(
            label = "Years of Experience",
            value = userProfile.yearsExperience.ifEmpty { "Not set" },
            fieldName = "yearsExperience",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["yearsExperience"] ?: "public",
            isEmpty = userProfile.yearsExperience.isEmpty(),
            isViewingOwnProfile = isViewingOwnProfile
        )
        
        // Line divider between Statistics and Account Info
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = Color(0xFFE5E7EB)
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        // ========== ACCOUNT INFORMATION SECTION ==========
        InfoRow(
            label = "Member Since",
            value = memberSince,
            fieldName = "memberSince",
            onPrivacyChange = null,
            currentPrivacy = "public",
            isViewingOwnProfile = isViewingOwnProfile
        )
    }
}

@Composable
fun PhotosTabContent(
    userId: String,
    isViewingOwnProfile: Boolean = true
) {
    val postRepository = remember { com.bisu.chickcare.backend.repository.PostRepository() }
    val postsFlow = remember(userId, isViewingOwnProfile) {
        postRepository.getUserTimelinePosts(userId, includePrivate = isViewingOwnProfile)
    }
    val posts by postsFlow.collectAsState(initial = emptyList())
    
    // Filter posts to only those with images, and respect privacy
    val photoPosts = remember(posts, isViewingOwnProfile) {
        posts.filter { post ->
            !post.imageUri.isNullOrEmpty() && 
            (isViewingOwnProfile || post.visibility == "public")
        }
    }
    
    // State for fullscreen image viewer
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }
    val imageUrls = remember(photoPosts) {
        photoPosts.mapNotNull { it.imageUri }
    }
    
    // Show fullscreen image viewer when an image is selected
    selectedImageIndex?.let { index ->
        FullscreenImageViewer(
            images = imageUrls,
            initialIndex = index,
            onDismiss = { selectedImageIndex = null }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Photos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (photoPosts.isEmpty()) {
            Text(
                "No photos yet.", 
                color = ThemeColorUtils.lightGray(Color.Gray),
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            // Grid layout for photos - ONLY images, NO captions
            // Use manual grid with Rows to avoid nested scrollable (LazyVerticalGrid inside LazyColumn)
            val rows = photoPosts.chunked(3) // Split into rows of 3
            var imageIndex = 0 // Track overall image index for fullscreen viewer
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                rows.forEach { rowPosts ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        rowPosts.forEach { post ->
                            val imageUri = post.imageUri
                            val currentIndex = imageIndex // Capture current index for click
                            if (!imageUri.isNullOrEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clickable { selectedImageIndex = currentIndex }
                                ) {
                                    AsyncImage(
                                        model = if (!imageUri.isNullOrEmpty()) imageUri else post.cloudImageUri,
                                        contentDescription = "Photo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(0.dp)), // No rounded corners for grid style
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                imageIndex++
                            }
                        }
                        // Fill remaining space if row has less than 3 items
                        repeat(3 - rowPosts.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun AudiosTabContent(
    userId: String,
    isViewingOwnProfile: Boolean = true
) {
    val postRepository = remember { com.bisu.chickcare.backend.repository.PostRepository() }
    val postsFlow = remember(userId, isViewingOwnProfile) {
        postRepository.getUserTimelinePosts(userId, includePrivate = isViewingOwnProfile)
    }
    val posts by postsFlow.collectAsState(initial = emptyList())
    
    // Filter posts to only those with audio, and respect privacy
    val audioPosts = remember(posts, isViewingOwnProfile) {
        posts.filter { post ->
            (!post.audioUri.isNullOrEmpty() || !post.cloudAudioUri.isNullOrEmpty()) && 
            (isViewingOwnProfile || post.visibility == "public")
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Audios",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        if (audioPosts.isEmpty()) {
            Text(
                "No audio recordings yet.", 
                color = ThemeColorUtils.lightGray(Color.Gray),
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            // Shared media player state - only one audio can play at a time
            val context = LocalContext.current
            var currentPlayingIndex by remember { mutableStateOf<Int?>(null) }
            var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
            
            // Stop any currently playing audio
            fun stopCurrentAudio() {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                currentPlayingIndex = null
            }
            
            // List of audio players with captions
            // Use regular Column instead of LazyColumn to avoid nested scrollable
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                audioPosts.forEachIndexed { index, post ->
                    val audioUri = post.audioUri
                    val cloudAudioUri = post.cloudAudioUri
                    
                    // Add caption above each audio player
                    Text(
                        text = "Audio ${index + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = ThemeColorUtils.black(),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    AudioPlayerItem(
                        isPlaying = currentPlayingIndex == index,
                        onPlayClick = {
                            // Stop currently playing audio if any
                            if (currentPlayingIndex != null && currentPlayingIndex != index) {
                                stopCurrentAudio()
                            }
                            
                            if (currentPlayingIndex == index) {
                                // Stop if clicking the same audio
                                stopCurrentAudio()
                            } else {
                                // Play new audio
                                try {
                                    val player = android.media.MediaPlayer()
                                    // Try local first, then cloud
                                    val uriToUse = if (!audioUri.isNullOrEmpty()) {
                                        sanitizeToUri(audioUri, "AudiosTabContent") ?: audioUri.toUri()
                                    } else if (!cloudAudioUri.isNullOrEmpty()) {
                                        cloudAudioUri.toUri()
                                    } else {
                                        null
                                    }
                                    
                                    if (uriToUse != null) {
                                        // Handle different URI schemes
                                        if (uriToUse.scheme == "file" || uriToUse.scheme == "content") {
                                             player.setDataSource(context, uriToUse)
                                        } else {
                                             // Network URL
                                             player.setDataSource(uriToUse.toString())
                                        }
                                        
                                        player.prepareAsync() // Use prepareAsync for network/content possibilities
                                        player.setOnPreparedListener { mp ->
                                            mp.start()
                                            mediaPlayer = mp
                                            currentPlayingIndex = index
                                        }
                                        player.setOnCompletionListener {
                                            it.release()
                                            currentPlayingIndex = null
                                            mediaPlayer = null
                                        }
                                        player.setOnErrorListener { mp, what, extra ->
                                            android.util.Log.e("AudiosTabContent", "MSP Error: $what, $extra")
                                             // If local failed and we have cloud, try cloud fallback (if we didn't already try it)
                                            if (!audioUri.isNullOrEmpty() && !cloudAudioUri.isNullOrEmpty() && uriToUse.toString() != cloudAudioUri) {
                                                android.util.Log.d("AudiosTabContent", "Local failed, trying cloud fallback...")
                                                try {
                                                    mp?.reset()
                                                    mp?.setDataSource(cloudAudioUri)
                                                    mp?.prepareAsync()
                                                    // Don't return true yet, let listeners handle it
                                                } catch (e: Exception) {
                                                    android.util.Log.e("AudiosTabContent", "Cloud fallback failed: ${e.message}")
                                                }
                                            }
                                            false // Allow completion listener to run if needed, or default error handling
                                        }
                                    } else {
                                        android.widget.Toast.makeText(context, "Audio file not found", android.widget.Toast.LENGTH_SHORT).show()
                                    }

                                } catch (e: Exception) {
                                    android.util.Log.e("AudiosTabContent", "Error playing audio: ${e.message}")
                                    stopCurrentAudio()
                                    // Try cloud fallback immediately if caught exception on setup
                                    if (!cloudAudioUri.isNullOrEmpty()) {
                                         try {
                                            val player = android.media.MediaPlayer()
                                            player.setDataSource(cloudAudioUri)
                                            player.prepareAsync()
                                            player.setOnPreparedListener { mp ->
                                                mp.start()
                                                mediaPlayer = mp
                                                currentPlayingIndex = index
                                            }
                                             player.setOnCompletionListener {
                                                it.release()
                                                currentPlayingIndex = null
                                                mediaPlayer = null
                                            }
                                        } catch (e2: Exception) {
                                            android.util.Log.e("AudiosTabContent", "Cloud fallback completely failed: ${e2.message}")
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
            
            // Cleanup on dispose
            DisposableEffect(Unit) {
                onDispose {
                    stopCurrentAudio()
                }
            }
        }
    }
}

@Composable
private fun AudioPlayerItem(
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.white()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onPlayClick
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Stop" else "Play",
                    tint = ThemeColorUtils.black()
                )
            }
            
            // Audio waveform indicator
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ThemeColorUtils.lightGray(Color.Gray))
            )
        }
    }
}

@Composable
fun MoreTabContent(userId: String) {
    var selectedSection by remember { mutableStateOf<String?>(null) } // "favorites" or "guides" or "login_session"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (selectedSection == null) {
            Text(
                text = "More",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MoreMenuItem(
                    icon = Icons.Default.Favorite,
                    title = "Favorites / Saved",
                    subtitle = "View your saved posts and detections",
                    onClick = { selectedSection = "favorites" }
                )
                
                MoreMenuItem(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "Guides & Resources",
                    subtitle = "How-to guides for poultry farming",
                    onClick = { selectedSection = "guides" }
                )
                
                MoreMenuItem(
                    icon = Icons.Default.LocationOn,
                    title = "Login Session",
                    subtitle = "View the location of where you logged in",
                    onClick = { selectedSection = "login_session" }
                )
            }
        } else {
            // Header with Back Arrow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                IconButton(
                    onClick = { selectedSection = null },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ThemeColorUtils.black()
                    )
                }
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text(
                    text = when (selectedSection) {
                        "favorites" -> "Favorites / Saved"
                        "login_session" -> "Login Session"
                        else -> "Guides & Resources"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            when (selectedSection) {
                "favorites" -> FavoritesSection(userId = userId)
                "login_session" -> LoginSessionSection()
                else -> GuidesSection()
            }
        }
    }
}

@Composable
fun MoreMenuItem(
    icon:  androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(ThemeColorUtils.primary().copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ThemeColorUtils.primary(),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.padding(start = 16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColorUtils.black()
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeColorUtils.lightGray(Color.Gray)
                )
            }
        }
    }
}

@Composable
fun FavoritesSection(userId: String) {
    val postRepository = remember { com.bisu.chickcare.backend.repository.PostRepository() }
    val savedPostsFlow = remember(userId) { postRepository.getSavedPosts(userId) }
    val savedPosts by savedPostsFlow.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    if (savedPosts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No saved items yet.",
                color = ThemeColorUtils.lightGray(Color.Gray)
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            savedPosts.forEach { post ->
                TimelinePostItem(
                    post = post,
                    userId = userId,
                    onDelete = { },
                    onChangeAudience = { _, _ -> },
                    onSavePost = { postId ->
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {

                           if (post.isSaved) {
                               postRepository.unsavePost(userId, post.userId, postId)
                           } else {
                               postRepository.savePost(userId, post.userId, postId, post)
                           }
                        }
                    },
                    onReaction = { postId, postOwnerId, reactionType ->
                         coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            postRepository.toggleReaction(postOwnerId, postId, userId, reactionType)
                         }
                    },
                    onCommentClick = { _, _ ->
                         android.widget.Toast.makeText(context, "Comments coming soon", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun GuidesSection() {
    val guides = listOf(
        GuideItem("Poultry Disease Prevention", "Learn how to keep your flock healthy and prevent common outbreaks.", "10 min read"),
        GuideItem("Best Feed Mixes", "Optimal nutrition guides for different stages of chicken growth.", "8 min read"),
        GuideItem("Housing & Hygiene", "Building the perfect coop and maintaining sanitation.", "12 min read"),
        GuideItem("Vaccination Schedule", "Essential timeline for vaccinating your chickens.", "5 min read")
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        guides.forEach { guide ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ThemeColorUtils.white()
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                 Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = guide.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black()
                    )
                    Spacer(modifier = Modifier.padding(top = 4.dp))
                    Text(
                        text = guide.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeColorUtils.black().copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.padding(top = 8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                             imageVector = Icons.Default.AccessTime,
                             contentDescription = null,
                             modifier = Modifier.size(14.dp),
                             tint = ThemeColorUtils.lightGray(Color.Gray)
                        )
                        Spacer(modifier = Modifier.padding(start = 4.dp))
                        Text(
                            text = guide.readTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeColorUtils.lightGray(Color.Gray)
                        )
                    }
                }
            }
        }
    }
}

data class GuideItem(val title: String, val description: String, val readTime: String)
