package com.bisu.chickcare.frontend.components

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.SpeakerNotes
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bisu.chickcare.backend.data.UserProfile
import com.bisu.chickcare.backend.repository.FriendSuggestion
import com.bisu.chickcare.backend.repository.TimelinePost
import com.bisu.chickcare.backend.viewmodels.AuthViewModel
import com.bisu.chickcare.frontend.utils.SoundManager
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import kotlinx.coroutines.launch

@Composable
fun CoverPhotoSection(
    coverPhotoUrl: String?,
    isViewingOwnProfile: Boolean,
    onCoverPhotoClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onCoverPhotoClick)
    ) {
        if (coverPhotoUrl != null) {
            AsyncImage(
                model = coverPhotoUrl,
                contentDescription = "Cover Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE1E0E0)),
                contentAlignment = Alignment.Center
            ) {
                if (isViewingOwnProfile) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.offset(y = (-40).dp)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Add cover photo",
                            tint = Color(0xFF3A3939).copy(alpha = 0.65f),
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add cover photo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF464343).copy(alpha = 0.65f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        if (isViewingOwnProfile) {
            IconButton(
                onClick = onCoverPhotoClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = if (coverPhotoUrl != null) "Change cover photo" else "Add cover photo",
                    tint = ThemeColorUtils.white(),
                    modifier = Modifier
                        .size(36.dp)
                        .background(ThemeColorUtils.black(alpha = 0.6f), CircleShape)
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun ProfilePictureAndNameSection(
    userProfile: UserProfile,
    isViewingOwnProfile: Boolean,
    isUploading: Boolean,
    onProfilePhotoClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (userProfile.photoUrl != null) {
                AsyncImage(
                    model = userProfile.photoUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .border(4.dp, ThemeColorUtils.darkGray(Color(0xFF3A3939)), CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE1E0E0))
                        .border(3.dp, ThemeColorUtils.darkGray(Color(0xFF3A3939)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Default profile picture",
                        tint = Color(0xFF464343),
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
            
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(140.dp),
                    color = Color(0xFF3A3939),
                    strokeWidth = 4.dp
                )
            }
            
            if (isViewingOwnProfile) {
                IconButton(
                    onClick = onProfilePhotoClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(44.dp)
                        .background(ThemeColorUtils.white(), CircleShape)
                        .border(2.dp, ThemeColorUtils.darkGray(Color(0xFF3A3939)), CircleShape)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = if (userProfile.photoUrl != null) "Change photo" else "Add photo",
                        tint = Color(0xFF2F2E2E),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = userProfile.fullName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
        )
    }
}

@Composable
fun FriendSuggestionsSection(
    suggestionCount: Int,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .indication(interactionSource, ripple(bounded = true))
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFF2C2C2C)) else ThemeColorUtils.surface(Color.White)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 8.dp else 5.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 6.dp,
            focusedElevation = 6.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Suggestions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
                )
                if (suggestionCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = Color.Red
                            ) {
                                Text(
                                    text = suggestionCount.toString(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    ) {
                        Text("")
                    }
                }
            }
            
            if (suggestionCount == 0) {
                Text(
                    text = "No friend suggestions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black(alpha = 0.7f)
                )
            } else {
                Text(
                    text = "$suggestionCount new friend suggestions available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.darkGray(Color(0xFF464644))
                )
            }
        }
    }
}

@Composable
fun PostsSection(
    userProfile: UserProfile,
    onNavigateToPost: () -> Unit,
    isViewingOwnProfile: Boolean,
    timelineUserId: String,
    navController: NavController? = null
) {
    var postText by remember { mutableStateOf("") }
    var showAudienceDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Instantiate repo here to fetch reaction details
    val postRepository = remember { com.bisu.chickcare.backend.repository.PostRepository() }
    val notificationRepository = remember { com.bisu.chickcare.backend.repository.NotificationRepository() }
    val authViewModel: AuthViewModel = viewModel()
    val currentUserProfile by authViewModel.userProfile.collectAsState()
    
    val currentUserId = authViewModel.getCurrentUserId(context) ?: ""
    val targetTimelineUserId = timelineUserId.ifEmpty { currentUserId }
    val includePrivatePosts = isViewingOwnProfile && targetTimelineUserId == currentUserId
    val timelinePostsFlow = remember(targetTimelineUserId, includePrivatePosts) {
        postRepository.getUserTimelinePosts(
            targetTimelineUserId,
            includePrivate = includePrivatePosts
        )
    }
    val timelinePosts by timelinePostsFlow.collectAsState(initial = emptyList())
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Posts",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
        )
        
        if (isViewingOwnProfile) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFF2C2C2C)) else ThemeColorUtils.surface(Color.White)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        ) {
                            if (currentUserProfile?.photoUrl != null && currentUserProfile?.photoUrl?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = currentUserProfile?.photoUrl,
                                    contentDescription = currentUserProfile?.fullName ?: "User",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color(0xFFE1E0E0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = currentUserProfile?.fullName ?: "User",
                                        tint = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.white() else Color(0xFF464343),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        
                        IconButton(
                            onClick = onNavigateToPost,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(
                                        0xFF3B3E42
                                    ) else ThemeColorUtils.surface(Color(0xFFF5F5F5)),
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                Icons.Default.PostAdd,
                                contentDescription = "Create post from detection history",
                                tint = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFB6C3CC) else Color(0xFF1C1B1B),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        OutlinedTextField(
                            value = postText,
                            onValueChange = { postText = it },
                            placeholder = { 
                                Text(
                                    "Write a post...",
                                    color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black(alpha = 0.6f)
                                ) 
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 40.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0x3B505459) else ThemeColorUtils.surface(Color(0xFFF9F9F9)),
                                unfocusedContainerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0x3B505459) else ThemeColorUtils.surface(Color(0xFFF9F9F9)),
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black(),
                                unfocusedTextColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black(),
                                focusedPlaceholderColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black(alpha = 0.6f),
                                unfocusedPlaceholderColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            singleLine = false,
                            maxLines = 3
                        )
                        
                        Button(
                            onClick = {
                                if (postText.trim().isNotEmpty()) {
                                    showAudienceDialog = true
                                }
                            },
                            modifier = Modifier.heightIn(min = 40.dp),
                            enabled = postText.trim().isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD4B48C),
                                disabledContainerColor = Color(0xFFDA9969).copy(alpha = 0.5f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "Post",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.white() else ThemeColorUtils.black()
                            )
                        }
                    }
                }
            }
        }
        
        if (timelinePosts.isEmpty()) {
            val emptyStateMessage = if (isViewingOwnProfile) {
                "No posts yet. Create your first post!"
            } else {
                val firstName = userProfile.fullName.substringBefore(" ")
                "No posts from ${firstName.ifEmpty { "this user" }} yet."
            }
            Text(
                text = emptyStateMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                timelinePosts.forEach { post ->
                    TimelinePostItem(
                        post = post,
                        onDelete = { postId ->
                            scope.launch {
                                try {
                                    val currentUserId = authViewModel.getCurrentUserId(context) ?: return@launch
                                    postRepository.deletePost(currentUserId, postId)
                                } catch (e: Exception) {
                                    Log.e("PostsSection", "Error deleting post: ${e.message}")
                                }
                            }
                        },
                        onChangeAudience = { postId, newVisibility ->
                            scope.launch {
                                try {
                                    val currentUserId = authViewModel.getCurrentUserId(context) ?: return@launch
                                    postRepository.updatePostVisibility(currentUserId, postId, newVisibility)
                                } catch (e: Exception) {
                                    Log.e("PostsSection", "Error changing audience: ${e.message}")
                                }
                            }
                        },
                        onSavePost = { postId ->
                            scope.launch {
                                try {
                                    val currentUserId = authViewModel.getCurrentUserId(context) ?: return@launch
                                    val postToSave = timelinePosts.find { it.id == postId }
                                    if (postToSave != null) {
                                        val originalUserId = postToSave.userId
                                        if (postToSave.isSaved) {
                                        postRepository.unsavePost(currentUserId, originalUserId, postId)
                                        } else {
                                            postRepository.savePost(currentUserId, originalUserId, postId, postToSave)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("PostsSection", "Error saving post: ${e.message}")
                                }
                            }
                        },
                        onReaction = { postId, postOwnerId, reactionType ->
                            scope.launch {
                                try {
                                    val currentUserId = authViewModel.getCurrentUserId(context) ?: return@launch
                                    postRepository.toggleReaction(postOwnerId, postId, currentUserId, reactionType)
                                } catch (e: Exception) {
                                    Log.e("PostsSection", "Error toggling reaction: ${e.message}")
                                }
                            }
                        },
                        onCommentClick = { postId, postOwnerId ->
                            navController?.navigate("comments/$postId/$postOwnerId")
                        },
                        userId = currentUserId
                    )
                }
            }
        }
    }
    
    LaunchedEffect(Unit) {
    }
    
    if (showAudienceDialog && isViewingOwnProfile) {
        AudienceSelectionDialog(
            onDismiss = { showAudienceDialog = false },
            onPost = { visibility ->
                scope.launch {
                    try {
                        val userId = currentUserId.takeIf { it.isNotEmpty() } ?: return@launch
                        postRepository.createTextPost(
                            userId = userId,
                            userName = currentUserProfile?.fullName ?: "",
                            userPhotoUrl = currentUserProfile?.photoUrl,
                            location = currentUserProfile?.farmLocation ?: "", // Use farm location
                            content = postText.trim(),
                            visibility = visibility
                        )
                        // Play success sound
                        try {
                            val mediaPlayer = android.media.MediaPlayer.create(context, com.bisu.chickcare.R.raw.posted_sound)
                            mediaPlayer?.setOnCompletionListener { it.release() }
                            mediaPlayer?.start()
                        } catch (e: Exception) {
                            Log.e("PostsSection", "Error playing sound: ${e.message}")
                        }
                        postText = ""
                        showAudienceDialog = false
                    } catch (e: Exception) {
                        Log.e("PostsSection", "Error posting: ${e.message}")
                    }
                }
            }
        )
    }
}

@Composable
fun AudienceSelectionDialog(
    onDismiss: () -> Unit,
    onPost: (String) -> Unit
) {
    var selectedVisibility by remember { mutableStateOf("public") }
    
    AlertDialog(
        containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFF2C2C2C)) else ThemeColorUtils.white(),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(16.dp),
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Select audience",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HorizontalDivider()
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedVisibility = "public" },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = selectedVisibility == "public",
                        onClick = { selectedVisibility = "public" },
                        colors = androidx.compose.material3.RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF27ADF5),
                            unselectedColor = Color.Gray
                        )
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Public",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
                        )
                        Text(
                            "Anyone can see this post",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black(alpha = 0.6f)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedVisibility = "private" },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = selectedVisibility == "private",
                        onClick = { selectedVisibility = "private" },
                        colors = androidx.compose.material3.RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF27ADF5),
                            unselectedColor = Color.Gray
                        )
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Private",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
                        )
                        Text(
                            "Only you can see this post",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black(alpha = 0.6f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onPost(selectedVisibility) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF0E0B9)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "Post",
                    color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.white() else ThemeColorUtils.black(),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF959597))
            }
        }
    )
}

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelinePostItem(
    post: TimelinePost,
    onDelete: (String) -> Unit,
    onChangeAudience: (String, String) -> Unit,
    onSavePost: (String) -> Unit,
    onReaction: (postId: String, postOwnerId: String, reactionType: String) -> Unit,
    onCommentClick: (postId: String, postOwnerId: String) -> Unit,
    userId: String
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAudienceDialog by remember { mutableStateOf(false) }

    val postRepository = remember { com.bisu.chickcare.backend.repository.PostRepository() }
    val dateFormat = remember { java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault()) }
    val dateString = remember(post.timestamp) {
        dateFormat.format(java.util.Date(post.timestamp))
    }
    val context = LocalContext.current
    
    // Only show menu if the post belongs to the current user
    val isOwnPost = post.userId == userId
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFF2C2C2C)) else ThemeColorUtils.white()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE1E0E0))
                ) {
                    if (post.userPhotoUrl != null && post.userPhotoUrl.isNotEmpty()) {
                        AsyncImage(
                            model = post.userPhotoUrl,
                            contentDescription = post.userName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = post.userName,
                            tint = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.white() else Color(0xFF464343),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = post.userName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
                        )
                        Icon(
                            imageVector = if (post.visibility == "public") Icons.Default.Public else Icons.Default.Lock,
                            contentDescription = post.visibility,
                            tint = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.white(alpha = 0.6f) else ThemeColorUtils.black(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black(alpha = 0.6f)
                    )
                }
                
                // Three-dot menu button (only show for own posts)
                if (isOwnPost) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else ThemeColorUtils.black(),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(
                                if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFF2C2C2C)) else ThemeColorUtils.white()
                            )
                        ) {
                            // 1. Save Post
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        if (post.isSaved) "Unsave Post" else "Save Post",
                                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onSavePost(post.id)
                                }
                            )

                            // 2. Change Privacy
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        if (post.visibility == "public") "Change to Private" else "Change to Public",
                                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showAudienceDialog = true
                                }
                            )

                            // 3. Delete Post
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        "Delete Post",
                                        color = Color.Red
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
            
            if (post.content.isNotEmpty()) {
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
                )
            }
            
            if (post.detectionResult.isNotEmpty()) {
                val statusText = com.bisu.chickcare.frontend.utils.DetectionDisplayUtils.statusText(post.isHealthy, post.confidence)
                
                // Row wrapping Status/Confidence and Audio Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status and Confidence tightly packed
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        val statusColor = com.bisu.chickcare.frontend.utils.DetectionDisplayUtils.statusColor(post.isHealthy, post.confidence)

                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                        
                        if (post.confidence > 0f) {
                            Text(
                                text = "Confidence: ${String.format("%.1f", post.confidence * 100)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black(alpha = 0.6f)
                            )
                        }
                    }

                    // Play Audio Button
                    val audioUri = post.audioUri
                    val cloudAudioUri = post.cloudAudioUri
                    
                    if (!audioUri.isNullOrEmpty() || !cloudAudioUri.isNullOrEmpty()) {
                        val context = LocalContext.current
                        var isPlaying by remember { mutableStateOf(false) }
                        var player by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
                        
                        DisposableEffect(Unit) {
                            onDispose {
                                player?.release()
                                player = null
                            }
                        }

                        Button(
                            onClick = {
                                if (isPlaying) {
                                    try {
                                        player?.stop()
                                        player?.release()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    player = null
                                    isPlaying = false
                                } else {
                                    try {
                                        // Use smart URI selection (checks local file existence)
                                        val audioSource = com.bisu.chickcare.frontend.utils.getAccessibleUri(context, audioUri, cloudAudioUri)
                                        
                                        if (audioSource == null) {
                                            Log.e("TimelinePostItem", "No accessible audio source found")
                                            isPlaying = false
                                            return@Button
                                        }

                                        player = android.media.MediaPlayer().apply {
                                            if (audioSource is android.net.Uri) {
                                                setDataSource(context, audioSource)
                                            } else if (audioSource is String) {
                                                setDataSource(audioSource)
                                            }
                                            
                                            prepareAsync()
                                            setOnPreparedListener { start() }
                                            setOnCompletionListener { 
                                                isPlaying = false 
                                                try { it.release() } catch (_: Exception) {}
                                                player = null
                                            }
                                            setOnErrorListener { _, what, extra ->
                                                Log.e("TimelinePostItem", "Error playing audio: $what, $extra")
                                                // If we failed with local URI and have cloud backup, try generic cloud fallback
                                                // (Though getAccessibleUri matches existence, file might be corrupted)
                                                if (audioSource is android.net.Uri && !cloudAudioUri.isNullOrEmpty()) {
                                                    Log.d("TimelinePostItem", "Audio source failed, trying cloud fallback...")
                                                    try {
                                                       reset()
                                                       setDataSource(cloudAudioUri)
                                                       prepareAsync()
                                                       return@setOnErrorListener true // Handled!
                                                    } catch (e: Exception) {
                                                        Log.e("TimelinePostItem", "Cloud fallback failed: ${e.message}")
                                                    }
                                                }
                                                // Return false to let completion listener clean up
                                                false 
                                            }
                                        }
                                        isPlaying = true
                                    } catch (e: Exception) {
                                        Log.e("TimelinePostItem", "Error playing audio: ${e.message}")
                                        // Try cloud fallback immediately if setup failed
                                        if (!cloudAudioUri.isNullOrEmpty()) {
                                             try {
                                                player = android.media.MediaPlayer().apply {
                                                    setDataSource(cloudAudioUri)
                                                    prepareAsync()
                                                    setOnPreparedListener { start() }
                                                    setOnCompletionListener { 
                                                        isPlaying = false 
                                                        try { it.release() } catch (_: Exception) {}
                                                        player = null
                                                    }
                                                }
                                                isPlaying = true
                                            } catch (e2: Exception) {
                                                 Log.e("TimelinePostItem", "Cloud fallback setup failed: ${e2.message}")
                                                 isPlaying = false
                                            }
                                        } else {
                                            isPlaying = false
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF89B3C9)),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Stop" else "Play Audio",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPlaying) "Stop" else "Play Audio",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                }
                
                val imageModel = com.bisu.chickcare.frontend.utils.getAccessibleUri(LocalContext.current, post.imageUri, post.cloudImageUri)
                
                if (imageModel != null) {
                    var showImagePreview by remember { mutableStateOf(false) }
                    
                    AsyncImage(
                        model = imageModel,
                        contentDescription = "Detection image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showImagePreview = true },
                        contentScale = ContentScale.Crop
                    )
                    
                    if (showImagePreview) {
                        PhotoPreviewDialog(
                            imageModel = imageModel,
                            onDismiss = { showImagePreview = false },
                            aspectRatio = 1f
                        )
                    }
                }
            }
            
            // Reactions Row: Facebook Style (Like + Comment)
            HorizontalDivider(
                color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFF444444) else Color(0xFFE0E0E0),
                thickness = 1.dp
            )
            
            // Reaction Count and List Logic
            val totalReactions = post.reactions.values.sumOf { it.size }
            var showReactorsDialog by remember { mutableStateOf(false) }

            if (totalReactions > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { showReactorsDialog = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Show icons of reactions present
                    val reactionIcons = post.reactions.filter { it.value.isNotEmpty() }.keys
                    Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                         reactionIcons.forEach { type ->
                             val icon = when(type) {
                                 "heart" -> "❤️"
                                 "chicken" -> "🐔" 
                                 "wow" -> "😮"
                                 "pray" -> "🙏"
                                 else -> "❤️"
                             }
                             Text(text = icon, style = MaterialTheme.typography.bodyMedium)
                         }
                    }
                    Text(
                        text = "$totalReactions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black(alpha = 0.7f)
                    )
                }
            }

            if (showReactorsDialog) {
                Dialog(onDismissRequest = { showReactorsDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFF2C2C2C)) else ThemeColorUtils.white()
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "People who reacted",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            var reactors by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
                            var isLoadingReactors by remember { mutableStateOf(true) }
                            
                            LaunchedEffect(Unit) {
                                val distinctUserIds = post.reactions.values.flatten().distinct()
                                reactors = postRepository.getUsersByIds(distinctUserIds)
                                isLoadingReactors = false
                            }
                            
                            if (isLoadingReactors) {
                                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            } else if (reactors.isEmpty()) {
                                Text("No reactions details available.")
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(reactors) { user ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (user.photoUrl != null) {
                                                AsyncImage(
                                                    model = user.photoUrl,
                                                    contentDescription = user.fullName,
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Gray),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                                                }
                                            }
                                            Text(
                                                text = user.fullName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Determine current user's reaction
            // Logic: Find the first reaction type where the user ID list contains current userId
            val myReactionType = post.reactions.entries.firstOrNull { it.value.contains(userId) }?.key
            
            var showReactionPopup by remember { mutableStateOf(false) }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp)
            ) {
                // Reaction Popup (Floating above)
                androidx.compose.animation.AnimatedVisibility(
                    visible = showReactionPopup,
                    enter = androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopStart) // Anchor relative to the row, offset manually
                        .offset(y = (-60).dp, x = 16.dp) // Float above the Like button
                        .zIndex(1f) // Ensure it's on top
                ) {
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFF333333) else Color.White
                        )
                    ) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val reactionOptions = listOf("❤️" to "heart", "🐔" to "chicken", "😮" to "wow", "🙏" to "pray")
                            
                            reactionOptions.forEach { (emoji, type) ->
                                Text(
                                    text = emoji,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                    // Play sound
                                    SoundManager.playSound(context, com.bisu.chickcare.R.raw.react_sound)
                                    onReaction(post.id, post.userId, type)
                                    showReactionPopup = false
                                        }
                                        .wrapContentSize(Alignment.Center),
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                        }
                    }
                }

                // Main Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, // Space between Like and Comment
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Like / Reaction Button
                    // Uses Box to easier center content and handle gestures
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .combinedClickable(
                                onClick = {
                                    if (myReactionType != null) {
                                        // If already reacted, clicking removes it (toggles off)
                                        onReaction(post.id, post.userId, myReactionType)
                                    } else {
                                        // If not reacted, clicking adds default "Heart"
                                        SoundManager.playSound(context, com.bisu.chickcare.R.raw.react_sound)
                                        onReaction(post.id, post.userId, "heart")
                                    }
                                },
                                onLongClick = {
                                    showReactionPopup = true
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon Logic
                            if (myReactionType != null) {
                                // Show selected reaction
                                val (emoji, label, color) = when(myReactionType) {
                                    "heart" -> Triple("❤️", "Love", Color(0xFFE91E63))
                                    "chicken" -> Triple("🐔", "Chicken", Color(0xFFDA8041)) // Orange-ish
                                    "wow" -> Triple("😮", "Wow", Color(0xFFE6C200)) // Yellow-ish
                                    "pray" -> Triple("🙏", "Pray", Color(0xFF9C27B0))
                                    else -> Triple("❤️", "Like", Color(0xFFE91E63))
                                }
                                
                                Text(text = emoji, style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label, 
                                    color = color, 
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                // Default "Like" State
                                Icon(
                                    imageVector = Icons.Default.ThumbUp, 
                                    contentDescription = "Like",
                                    tint = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B) // Facebook gray
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Like",
                                    color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B),
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    
                    // 2. Comment Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onCommentClick(post.id, post.userId) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.SpeakerNotes, // Chat icon
                                contentDescription = "Comment",
                                tint = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Comment",
                                color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFB0B3B8) else Color(0xFF65676B),
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    // Removed Share button as requested
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        DeletePostConfirmationDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                onDelete(post.id)
                showDeleteDialog = false
            }
        )
    }
    
    // Change audience dialog
    if (showAudienceDialog) {
        ChangeAudienceDialog(
            currentVisibility = post.visibility,
            onDismiss = { showAudienceDialog = false },
            onConfirm = { newVisibility ->
                onChangeAudience(post.id, newVisibility)
                showAudienceDialog = false
            }
        )
    }
}

@Composable
fun DeletePostConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }, // Prevent clicks from dismissing
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ThemeColorUtils.white()
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Title
                    Text(
                        text = "Delete Post?",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = com.bisu.chickcare.ui.theme.FiraSans,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = ThemeColorUtils.black()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Confirmation message
                    Text(
                        text = "Are you sure you want to delete this post? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeColorUtils.darkGray(Color(0xFF666666))
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Delete Button (Red)
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626), 
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Cancel Button (White with Border)
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = ThemeColorUtils.black()
                        ),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChangeAudienceDialog(
    currentVisibility: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
        var selectedVisibility by remember { mutableStateOf(if (currentVisibility == "public") "private" else "public") }

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.surface(
                        Color(0xFF2C2C2C)
                    ) else ThemeColorUtils.white()
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header with title and X button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Change Audience",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(
                                0xFFE3E5E8
                            ) else ThemeColorUtils.darkGray(Color(0xFF231C16))
                        )
                        IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.offset(x = 12.dp)
                    ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.white() else ThemeColorUtils.black()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(16.dp))

                    // Public option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedVisibility = "public" },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedVisibility == "public",
                            onClick = { selectedVisibility = "public" },
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF27ADF5),
                                unselectedColor = Color.Gray
                            )
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Public",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(
                                    0xFFE3E5E8
                                ) else ThemeColorUtils.black()
                            )
                            Text(
                                "Anyone can see this post",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(
                                    0xFFE3E5E8
                                ) else ThemeColorUtils.black(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Private option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedVisibility = "private" },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = selectedVisibility == "private",
                            onClick = { selectedVisibility = "private" },
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF27ADF5),
                                unselectedColor = Color.Gray
                            )
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Private",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(
                                    0xFFE3E5E8
                                ) else ThemeColorUtils.black()
                            )
                            Text(
                                "Only you can see this post",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(
                                    0xFFE3E5E8
                                ) else ThemeColorUtils.black(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Save Button
                    Button(
                        onClick = { onConfirm(selectedVisibility) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = ThemeColorUtils.white()
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

@Composable
fun MutualFriendsSection(
    mutualFriends: List<FriendSuggestion>,
    onViewMore: (() -> Unit)? = null
) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.surface(
                    Color(0xFF2C2C2C)
                ) else ThemeColorUtils.white()
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Friends",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(
                                0xFFE3E5E8
                            ) else ThemeColorUtils.black()
                        )
                        Text(
                            text = "${mutualFriends.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(
                                0xFFE3E5E8
                            ) else ThemeColorUtils.black()
                        )
                    }

                    if (mutualFriends.size > 3) {
                        Text(
                            text = "See more...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            ),
                            fontWeight = FontWeight.Medium,
                            fontStyle = FontStyle.Italic,
                            color = Color(0xFF8B4513),
                            modifier = Modifier.clickable(enabled = onViewMore != null) {
                                onViewMore?.invoke()
                            }
                        )
                    }
                }

                if (mutualFriends.isEmpty()) {
                    Text(
                        text = "No friends yet. Start adding friends!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(
                            0xFFE3E5E8
                        ) else ThemeColorUtils.black(alpha = 0.7f),
                        fontStyle = FontStyle.Italic
                    )
                } else {
                    val displayFriends = mutualFriends.take(3)
                    val rows = displayFriends.chunked(3)

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        rows.forEach { rowFriends ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowFriends.forEach { friend ->
                                    FriendAvatarItem(
                                        friend = friend,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - rowFriends.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
fun FriendAvatarItem(
    friend: FriendSuggestion,
    modifier: Modifier = Modifier
) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF5F5F5))
                    .border(2.dp, ThemeColorUtils.lightGray(Color(0xFFE0E0E0)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = if (friend.photoUrl.isNullOrEmpty()) com.bisu.chickcare.R.drawable.default_avatar else friend.photoUrl,
                    contentDescription = friend.fullName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                text = friend.fullName.split(" ").first(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

@Composable
fun InfoRow(
    label: String,
    value: String,
    fieldName: String = "",
    isEmpty: Boolean = false,
    onPrivacyChange: ((String, String) -> Unit)? = null,
    currentPrivacy: String = "public",
    isViewingOwnProfile: Boolean = true
) {
        var showPrivacyMenu by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label:",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(140.dp)
            )
            // Value text - NOT clickable (editing only through pencil icon)
            Row(
                modifier = Modifier
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) {
                        if (isEmpty) Color(0xFFE3E5E8).copy(alpha = 0.6f) else Color(0xFFE3E5E8)
                    } else {
                        if (isEmpty) ThemeColorUtils.black(alpha = 0.6f) else ThemeColorUtils.black()
                    },
                    fontStyle = if (isEmpty) FontStyle.Italic else FontStyle.Normal,
                    modifier = Modifier.weight(1f)
                )
            }

            // Privacy icon (three dots) - Only show when viewing own profile
            if (onPrivacyChange != null && isViewingOwnProfile) {
                Box {
                    IconButton(
                        onClick = { showPrivacyMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Privacy settings",
                            tint = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else ThemeColorUtils.black(),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showPrivacyMenu,
                        onDismissRequest = { showPrivacyMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Public") },
                            onClick = {
                                onPrivacyChange(fieldName, "public")
                                showPrivacyMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    if (currentPrivacy == "public") Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (currentPrivacy == "public") Color(0xFF6366F1) else ThemeColorUtils.black(
                                        alpha = 0.5f
                                    )
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Friends") },
                            onClick = {
                                onPrivacyChange(fieldName, "friends")
                                showPrivacyMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    if (currentPrivacy == "friends") Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (currentPrivacy == "friends") Color(0xFF6366F1) else ThemeColorUtils.black(
                                        alpha = 0.5f
                                    )
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Private") },
                            onClick = {
                                onPrivacyChange(fieldName, "private")
                                showPrivacyMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    if (currentPrivacy == "private") Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (currentPrivacy == "private") Color(0xFF6366F1) else ThemeColorUtils.black(
                                        alpha = 0.5f
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }


