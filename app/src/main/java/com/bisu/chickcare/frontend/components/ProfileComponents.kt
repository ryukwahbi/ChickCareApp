package com.bisu.chickcare.frontend.components

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.bisu.chickcare.backend.data.UserProfile
import com.bisu.chickcare.backend.repository.FriendSuggestion
import com.bisu.chickcare.backend.repository.PostRepository
import com.bisu.chickcare.backend.repository.TimelinePost
import com.bisu.chickcare.backend.viewmodels.AuthViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import kotlinx.coroutines.launch

@Composable
fun CoverPhotoSection(
    coverPhotoUrl: String?,
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

@Composable
fun ProfilePictureAndNameSection(
    userProfile: UserProfile,
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
                                    color = ThemeColorUtils.white(),
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
    timelineUserId: String
) {
    var postText by remember { mutableStateOf("") }
    var showAudienceDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val postRepository = remember { PostRepository() }
    val authViewModel: AuthViewModel = viewModel()
    val auth = authViewModel.auth
    val currentUserId = auth.currentUser?.uid ?: ""
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
                            if (userProfile.photoUrl != null && userProfile.photoUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = userProfile.photoUrl,
                                    contentDescription = userProfile.fullName,
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
                                        contentDescription = userProfile.fullName,
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
                "No posts from ${userProfile.fullName.ifEmpty { "this user" }} yet."
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
                                    val currentUserId = auth.currentUser?.uid ?: return@launch
                                    postRepository.deletePost(currentUserId, postId)
                                } catch (e: Exception) {
                                    Log.e("PostsSection", "Error deleting post: ${e.message}")
                                }
                            }
                        },
                        onChangeAudience = { postId, newVisibility ->
                            scope.launch {
                                try {
                                    val currentUserId = auth.currentUser?.uid ?: return@launch
                                    postRepository.updatePostVisibility(currentUserId, postId, newVisibility)
                                } catch (e: Exception) {
                                    Log.e("PostsSection", "Error changing audience: ${e.message}")
                                }
                            }
                        },
                        onSavePost = { postId ->
                            scope.launch {
                                try {
                                    val currentUserId = auth.currentUser?.uid ?: return@launch
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
                            userName = userProfile.fullName,
                            userPhotoUrl = userProfile.photoUrl,
                            content = postText.trim(),
                            visibility = visibility
                        )
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
                        onClick = { selectedVisibility = "public" }
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
                        onClick = { selectedVisibility = "private" }
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
                Text("Cancel", color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(
                    0xFFBFC2C4
                ) else ThemeColorUtils.black())
            }
        }
    )
}

@SuppressLint("DefaultLocale")
@Composable
fun TimelinePostItem(
    post: TimelinePost,
    onDelete: (String) -> Unit,
    onChangeAudience: (String, String) -> Unit,
    onSavePost: (String) -> Unit,
    userId: String
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAudienceDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault()) }
    val dateString = remember(post.timestamp) {
        dateFormat.format(java.util.Date(post.timestamp))
    }
    
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
                                tint = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.white() else ThemeColorUtils.black(),
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
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        "Delete Post",
                                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.Red
                                    )
                                }
                            )
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
                                },
                                leadingIcon = {
                                    Icon(
                                        if (post.visibility == "public") Icons.Default.Lock else Icons.Default.Public,
                                        contentDescription = null,
                                        tint = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.white() else ThemeColorUtils.black()
                                    )
                                }
                            )
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
                                },
                                leadingIcon = {
                                    Icon(
                                        if (post.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        tint = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.white() else ThemeColorUtils.black()
                                    )
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
                
                if (post.imageUri != null && post.imageUri.isNotEmpty()) {
                    var showImagePreview by remember { mutableStateOf(false) }
                    
                    AsyncImage(
                        model = post.imageUri,
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
                            imageModel = post.imageUri,
                            onDismiss = { showImagePreview = false },
                            aspectRatio = 1f
                        )
                    }
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFF2C2C2C)) else ThemeColorUtils.white()
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
                        text = "Delete Post",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.darkGray(Color(0xFF231C16))
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.white() else ThemeColorUtils.black()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Confirmation message
                Text(
                    text = "Are you sure you want to delete this post? This action cannot be undone. The detection history will remain intact.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.lightGray(Color(0xFF666666))
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Delete Button
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF5350),
                        contentColor = ThemeColorUtils.white()
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
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
                containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFF2C2C2C)) else ThemeColorUtils.white()
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
                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.darkGray(Color(0xFF231C16))
                    )
                    IconButton(onClick = onDismiss) {
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
                        onClick = { selectedVisibility = "public" }
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
                        onClick = { selectedVisibility = "private" }
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
            containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFF2C2C2C)) else ThemeColorUtils.white()
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
                    text = "Friends",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
                )
                Text(
                    text = "${mutualFriends.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
                )
            }
            
            if (mutualFriends.isEmpty()) {
                Text(
                    text = "No friends yet. Start adding friends!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black(alpha = 0.7f),
                    fontStyle = FontStyle.Italic
                )
            } else {
                val displayFriends = mutualFriends.take(6)
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
                
                if (mutualFriends.size > 6) {
                    Text(
                        text = "View more...",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF8B4513),
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable(enabled = onViewMore != null) {
                                onViewMore?.invoke()
                            }
                    )
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
            if (friend.photoUrl != null && friend.photoUrl.isNotEmpty()) {
                AsyncImage(
                    model = friend.photoUrl,
                    contentDescription = friend.fullName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = friend.fullName,
                    tint = Color(0xFF8B4513),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Text(
            text = friend.fullName.split(" ").first(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
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
                        tint = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) ThemeColorUtils.white() else ThemeColorUtils.black(),
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
                                tint = if (currentPrivacy == "public") Color(0xFF6366F1) else ThemeColorUtils.black(alpha = 0.5f)
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
                                tint = if (currentPrivacy == "friends") Color(0xFF6366F1) else ThemeColorUtils.black(alpha = 0.5f)
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
                                tint = if (currentPrivacy == "private") Color(0xFF6366F1) else ThemeColorUtils.black(alpha = 0.5f)
                            )
                        }
                    )
                }
            }
        }
    }
}
