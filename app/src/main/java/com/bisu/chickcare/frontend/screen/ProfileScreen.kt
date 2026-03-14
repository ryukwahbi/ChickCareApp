package com.bisu.chickcare.frontend.screen

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.data.UserProfile
import com.bisu.chickcare.backend.service.ImageCropHelper
import com.bisu.chickcare.backend.service.NetworkConnectivityHelper
import com.bisu.chickcare.backend.viewmodels.AuthViewModel
import com.bisu.chickcare.backend.viewmodels.FriendViewModel
import com.bisu.chickcare.frontend.components.AboutTabContent
import com.bisu.chickcare.frontend.components.AddInfoDialog
import com.bisu.chickcare.frontend.components.AudiosTabContent
import com.bisu.chickcare.frontend.components.ChangePhotoBottomSheet
import com.bisu.chickcare.frontend.components.CoverPhotoSection
import com.bisu.chickcare.frontend.components.EditProfileDialog
import com.bisu.chickcare.frontend.components.MoreTabContent
import com.bisu.chickcare.frontend.components.OfflineIndicator
import com.bisu.chickcare.frontend.components.PhotoPreviewDialog
import com.bisu.chickcare.frontend.components.PhotosTabContent
import com.bisu.chickcare.frontend.components.ProfilePictureAndNameSection
import com.bisu.chickcare.frontend.components.TimelineTabContent
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import com.bisu.chickcare.frontend.utils.getTempUri
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ProfileScreen(
    navController: NavController, 
    viewUserId: String? = null,
    initialTab: Int = 0
) {
    val authViewModel: AuthViewModel = viewModel()
    val friendViewModel: FriendViewModel = viewModel()
    val currentUserProfile by authViewModel.userProfile.collectAsState()
    val context = LocalContext.current
    val currentUserId = authViewModel.getCurrentUserId(context)
    val isViewingOwnProfile = viewUserId == null || viewUserId == currentUserId
    val profileUserId = viewUserId ?: currentUserId.orEmpty()
    var displayedProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoadingProfile by remember { mutableStateOf(false) }
    val friendSuggestions by friendViewModel.suggestions.collectAsState()
    val friendsList by friendViewModel.friends.collectAsState()
    val pendingRequests by friendViewModel.pendingRequests.collectAsState()
    // Filter out users who are already friends (same logic as FriendSuggestionsScreen)
    val friendIds = friendsList.map { it.userId }.toSet()
    val filteredSuggestions = friendSuggestions.filter { it.userId !in friendIds }
    val suggestionCount = filteredSuggestions.size
    // Store friends list for the viewed user (when viewing someone else's profile)
    var viewedUserFriends by remember { mutableStateOf<List<com.bisu.chickcare.backend.repository.FriendSuggestion>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var showChangePhotoSheet by remember { mutableStateOf(false) }
    var showChangeCoverSheet by remember { mutableStateOf(false) }
    var showPhotoPreview by remember { mutableStateOf(false) }
    var previewingCoverPhoto by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddInfoDialog by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var isBlocked by remember { mutableStateOf(false) }
    var showUnblockDialog by remember { mutableStateOf(false) }
    var isFollowing by remember { mutableStateOf(false) }
    var isFollowLoading by remember { mutableStateOf(false) }
    
    // Friend Management State
    var showFriendMenu by remember { mutableStateOf(false) }
    var showUnfriendDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    val postRepository = remember { com.bisu.chickcare.backend.repository.PostRepository() }
    val notificationRepository = remember { com.bisu.chickcare.backend.repository.NotificationRepository() }

    val isOnline by NetworkConnectivityHelper.connectivityFlow(context)
        .collectAsState(initial = NetworkConnectivityHelper.isOnline(context))
    val isOffline = !isOnline

    // Load profile based on viewUserId
    LaunchedEffect(viewUserId, currentUserProfile) {
        if (isViewingOwnProfile) {
            displayedProfile = currentUserProfile
        } else {
            isLoadingProfile = true
            displayedProfile = withContext(Dispatchers.IO) {
                authViewModel.fetchUserProfileById(viewUserId)
            }
            isLoadingProfile = false
        }
    }

    LaunchedEffect(currentUserProfile, isViewingOwnProfile) {
        if (isViewingOwnProfile) {
            displayedProfile = currentUserProfile
            if (currentUserProfile == null) {
                authViewModel.fetchUserProfile(context)
            }
        }
    }

    LaunchedEffect(Unit) {
        // Always load friends to know relationship status
        // Always load friends to know relationship status
        friendViewModel.loadFriends()
        friendViewModel.loadPendingFriendRequests()
        
        if (isViewingOwnProfile) {
            // Load suggestions, so filtering works correctly
            friendViewModel.loadFriendSuggestions()
        } else {
            friendViewModel.checkIfBlocked(viewUserId) { blocked ->
                isBlocked = blocked
            }
            // Load the viewed user's friends list
            try {
                val friendRepo = com.bisu.chickcare.backend.repository.FriendRepository()
                viewedUserFriends = withContext(Dispatchers.IO) {
                    friendRepo.getFriends(viewUserId)
                }
            } catch (e: Exception) {
                Log.e("ProfileScreen", "Failed to load viewed user's friends: ${e.message}", e)
                viewedUserFriends = emptyList()
            }
        }
    }
    
    // Reload friends and suggestions when viewing own profile and screen is visible
    LaunchedEffect(isViewingOwnProfile) {
        if (isViewingOwnProfile) {
            friendViewModel.loadFriends()
            friendViewModel.loadFriendSuggestions()
        }
    }
    
    // Reload viewed user's friends when viewUserId changes
    // Reload viewed user's friends when viewUserId changes
    var isFriend by remember { mutableStateOf(false) } // Add local state for quick check
    var requestStatus by remember { mutableStateOf<String?>(null) }
    
    // ... imports and other vars
    
    // Reload viewed user's friends when viewUserId changes
    LaunchedEffect(viewUserId) {
        if (!isViewingOwnProfile) {
            // Check explicit friendship status first (faster/more reliable)
            friendViewModel.checkIsFriend(viewUserId) { friendStatus ->
                isFriend = friendStatus
                // If we are friends, ensure the friendship is mutual (repair if needed)
                if (friendStatus) {
                    friendViewModel.repairFriendship(viewUserId)
                }
            }
            friendViewModel.checkRequestStatus(viewUserId) { status ->
                requestStatus = status
            }
            
            try {
                val friendRepo = com.bisu.chickcare.backend.repository.FriendRepository()
                viewedUserFriends = withContext(Dispatchers.IO) {
                    friendRepo.getFriends(viewUserId)
                }
            } catch (e: Exception) {
                Log.e("ProfileScreen", "Failed to load viewed user's friends: ${e.message}", e)
                viewedUserFriends = emptyList()
            }
        }
    }
    
    // Check if current user is following the viewed user
    LaunchedEffect(viewUserId, currentUserId) {
        if (!isViewingOwnProfile && currentUserId != null) {
            try {
                isFollowing = withContext(Dispatchers.IO) {
                    postRepository.isFollowing(currentUserId, viewUserId)
                }
            } catch (e: Exception) {
                Log.e("ProfileScreen", "Failed to check follow status: ${e.message}", e)
            }
        }
    }
    
    val scope = rememberCoroutineScope()

    val tempUri = remember { getTempUri(context) }
    val tempCoverUri = remember { getTempUri(context) }

    // Crop launcher for profile picture
    val cropLauncherProfile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val resultCode = result.resultCode
        val data = result.data
        
        if (resultCode == android.app.Activity.RESULT_OK && data != null) {
            val resultUri = UCrop.getOutput(data)
            resultUri?.let { croppedUri ->
                // Upload the cropped image
                isUploading = true
                authViewModel.uploadProfileImage(croppedUri, context) { success, message ->
                    isUploading = false
                    if (!success) {
                        Log.e("ProfileScreen", "Profile picture upload failed: $message")
                    }
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR && data != null) {
            val cropError = UCrop.getError(data)
            Log.e("ProfileScreen", "Crop error: ${cropError?.message}")
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                // Start cropping instead of direct upload
                ImageCropHelper.startProfilePictureCrop(cropLauncherProfile, tempUri, context)
            }
        }
    )
    

    @Suppress("UNCHECKED_CAST") val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                // Start cropping instead of direct upload
                ImageCropHelper.startProfilePictureCrop(cropLauncherProfile, it, context)
            }
        } as (Uri?) -> Unit
    )

    // Crop launcher for cover photo
    val cropLauncherCover = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val resultCode = result.resultCode
        val data = result.data
        
        if (resultCode == android.app.Activity.RESULT_OK && data != null) {
            val resultUri = UCrop.getOutput(data)
            resultUri?.let { croppedUri ->
                // Upload the cropped image
                isUploading = true
                authViewModel.uploadCoverPhoto(croppedUri, context) { success, message ->
                    isUploading = false
                    if (!success) {
                        Log.e("ProfileScreen", "Cover photo upload failed: $message")
                    }
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR && data != null) {
            val cropError = UCrop.getError(data)
            Log.e("ProfileScreen", "Crop error: ${cropError?.message}")
        }
    }

    val cameraLauncherCover = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                // Start cropping instead of direct upload
                ImageCropHelper.startCoverPhotoCrop(cropLauncherCover, tempCoverUri, context)
            }
        }
    )

    val galleryLauncherCover = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                // Start cropping instead of direct upload
                ImageCropHelper.startCoverPhotoCrop(cropLauncherCover, it, context)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isViewingOwnProfile) stringResource(R.string.profile_title) else displayedProfile?.fullName ?: stringResource(R.string.profile_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = ThemeColorUtils.darkGray(Color(0xFF231C16))
                        )
                    }
                },
                actions = {
                    // Only show settings when viewing own profile
                    if (isViewingOwnProfile) {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title), tint = ThemeColorUtils.darkGray(Color(0xFF231C16)))
                        }
                    } else {
                        // Show "More Options" menu for other users
                        Box {
                            IconButton(onClick = { showFriendMenu = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.profile_more_options),
                                    tint = ThemeColorUtils.darkGray(Color(0xFF231C16))
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showFriendMenu,
                                onDismissRequest = { showFriendMenu = false },
                                modifier = Modifier.background(ThemeColorUtils.white())
                            ) {
                                val isFriend = friendIds.contains(viewUserId)
                                val firstName = displayedProfile?.fullName?.substringBefore(" ") ?: "User"
                                
                                if (isFriend) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.profile_unfriend, firstName), color = ThemeColorUtils.black()) },
                                        onClick = {
                                            showFriendMenu = false
                                            showUnfriendDialog = true
                                        }
                                    )
                                }
                                
                                if (!isBlocked) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.profile_block, firstName), color = Color.Red) },
                                        onClick = {
                                            showFriendMenu = false
                                            showBlockDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeColorUtils.white(),
                    titleContentColor = ThemeColorUtils.darkGray(Color(0xFF231C16))
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            OfflineIndicator(isOffline = isOffline)
            if (isLoadingProfile) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (displayedProfile == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isOffline) stringResource(R.string.profile_offline_error) else stringResource(R.string.profile_load_error), 
                        color = Color.Gray
                    )
                }
            } else {
                val tabs = listOf(
                    stringResource(R.string.profile_tab_timeline),
                    stringResource(R.string.profile_tab_about),
                    stringResource(R.string.profile_tab_photos),
                    stringResource(R.string.profile_tab_audios),
                    stringResource(R.string.profile_tab_more)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
                        .imePadding()
                ) {
                    item {
                        Box {
                            CoverPhotoSection(
                                coverPhotoUrl = displayedProfile?.coverPhotoUrl,
                                isViewingOwnProfile = isViewingOwnProfile,
                                onCoverPhotoClick = {
                                    if (isViewingOwnProfile) {
                                        showChangeCoverSheet = true
                                    } else {
                                        previewingCoverPhoto = true
                                        showPhotoPreview = true
                                    }
                                }
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset(y = 85.dp)
                            ) {
                                ProfilePictureAndNameSection(
                                    userProfile = displayedProfile!!,
                                    isViewingOwnProfile = isViewingOwnProfile,
                                    isUploading = isUploading,
                                    onProfilePhotoClick = {
                                        if (isViewingOwnProfile) {
                                            showChangePhotoSheet = true
                                        } else {
                                            previewingCoverPhoto = false
                                            showPhotoPreview = true
                                        }
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(95.dp))
                    }

                    // Show unblock button if user is blocked
                    if (!isViewingOwnProfile && isBlocked && displayedProfile != null) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFF7E6)
                                ),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.profile_blocked_message, displayedProfile!!.fullName),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ThemeColorUtils.black()
                                    )
                                    Button(
                                        onClick = { showUnblockDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4CAF50)
                                        ),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                                    ) {
                                        Text(stringResource(R.string.profile_unblock_button), color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                    
                    // Friend Actions (Friends/Chat) OR Follow Button
                    if (!isViewingOwnProfile && !isBlocked && displayedProfile != null) {
                        // Use either the explicit check result OR the list-based check
                        val effectiveIsFriend = isFriend || (friendIds.contains(viewUserId))
                        
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (effectiveIsFriend) {
                                    // Friends & Chat Buttons (Side by Side)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                    // Friends Button
                                    Button(
                                        onClick = {}, 
                                        enabled = false, // Make it non-interactive
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFD5A358),
                                            contentColor = ThemeColorUtils.black(),
                                            disabledContainerColor = Color(0xFFD5A358), // Match enabled color
                                            disabledContentColor = ThemeColorUtils.black() // Match enabled color
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), // Rectangular with rounded corners
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.Handshake, // Or Check
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = ThemeColorUtils.black()
                                        )
                                        Spacer(modifier = Modifier.run { width(8.dp) })
                                        Text(
                                            stringResource(R.string.profile_friends_status),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    // Chat Button
                                    Button(
                                        onClick = { 
                                            // Navigate to chat with this user
                                            navController.navigate("chat?userId=${viewUserId}&userName=${displayedProfile?.fullName ?: "User"}")
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ThemeColorUtils.white(),
                                            contentColor = ThemeColorUtils.black()
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = stringResource(R.string.profile_chat_button),
                                            modifier = Modifier.size(20.dp),
                                            tint = ThemeColorUtils.black()
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            stringResource(R.string.profile_chat_button),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                // Add/Follow Buttons Row for non-friends
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Add/Requested/Confirm Button
                                    val isIncomingRequest = pendingRequests.any { it.fromUserId == viewUserId }
                                    
                                    val addText = when {
                                        isIncomingRequest -> "Confirm"
                                        requestStatus == "pending" -> stringResource(R.string.profile_requested_status)
                                        else -> stringResource(R.string.profile_add_button)
                                    }
                                    
                                    val addColor = when {
                                        isIncomingRequest -> Color(0xFF4CAF50) // Green
                                        requestStatus == "pending" -> Color(0xFF6F7073) // Gray
                                        else -> Color(0xFF4CAF50) // Green
                                    }
                                    
                                    val addIcon = when {
                                        isIncomingRequest -> Icons.Default.Check
                                        requestStatus == "pending" -> Icons.Default.Check
                                        else -> Icons.Default.PersonAdd
                                    }

                                    Button(
                                        onClick = {
                                            if (isIncomingRequest) {
                                                // Accept Friend Request
                                                val request = pendingRequests.firstOrNull { it.fromUserId == viewUserId }
                                                if (request != null) {
                                                    // OPTIMISTIC UPDATE: Set as friend immediately
                                                    isFriend = true // This will switch the UI to Friends/Chat buttons
                                                    requestStatus = null
                                                    
                                                    friendViewModel.acceptFriendRequest(
                                                        requestId = request.id,
                                                        friendUserId = request.fromUserId,
                                                        friendName = request.fromUserName,
                                                        callback = { success, _ ->
                                                            if (!success) {
                                                                // Revert if failed
                                                                isFriend = false
                                                                friendViewModel.loadPendingFriendRequests() // Reload to get request back
                                                            }
                                                        }
                                                    )
                                                }
                                            } else if (requestStatus == "pending") {
                                                // OPTIMISTIC: Update UI immediately
                                                val previousStatus = requestStatus
                                                requestStatus = null
                                                
                                                if (currentUserId != null) {
                                                    friendViewModel.cancelFriendRequest(viewUserId) { success, _ ->
                                                        if (!success) {
                                                            // Revert if failed
                                                            requestStatus = previousStatus
                                                        }
                                                    }
                                                }
                                            } else {
                                                // OPTIMISTIC: Update UI immediately
                                                requestStatus = "pending"
                                                
                                                if (currentUserId != null && displayedProfile != null) {
                                                    friendViewModel.sendFriendRequest(viewUserId, displayedProfile!!.fullName) { success, _ ->
                                                        if (!success) {
                                                            // Revert if failed
                                                            requestStatus = null
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = addColor,
                                            contentColor = Color.White
                                        ),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            addIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            addText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Follow Button
                                    Button(
                                        onClick = {
                                            if (currentUserId != null) {
                                                // OPTIMISTIC: Update UI immediately
                                                val wasFollowing = isFollowing
                                                isFollowing = !isFollowing
                                                
                                                isFollowLoading = true
                                                scope.launch {
                                                    try {
                                                        withContext(Dispatchers.IO) {
                                                            if (wasFollowing) {
                                                                postRepository.unfollowUser(currentUserId, viewUserId)
                                                            } else {
                                                                postRepository.followUser(currentUserId, viewUserId)
                                                                // Send notification when following
                                                                notificationRepository.notifyFollow(
                                                                    targetUserId = viewUserId,
                                                                    followerUserId = currentUserId,
                                                                    followerUserName = currentUserProfile?.fullName ?: "Someone",
                                                                    followerUserPhotoUrl = currentUserProfile?.photoUrl
                                                                )
                                                            }
                                                        }
                                                        // Success - state already updated optimistically
                                                    } catch (e: Exception) {
                                                        Log.e("ProfileScreen", "Error toggling follow: ${e.message}", e)
                                                        // Revert on failure
                                                        isFollowing = wasFollowing
                                                    } finally {
                                                        isFollowLoading = false
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFDA6552),
                                            contentColor = Color.White
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDA6552)),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isFollowLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                if (isFollowing) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = if (isFollowing) stringResource(R.string.profile_unfollow_button) else stringResource(R.string.profile_follow_button),
                                                modifier = Modifier.size(20.dp),
                                                tint = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                if (isFollowing) stringResource(R.string.profile_unfollow_button) else stringResource(R.string.profile_follow_button),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            }
                        }
                    }

                    item {
                        HorizontalDivider(color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)).copy(alpha = 0.4f))
                    }

                    item {
                        PrimaryTabRow(
                            selectedTabIndex = selectedTab,
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = ThemeColorUtils.white(),
                            contentColor = ThemeColorUtils.black(),
                            indicator = { }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = {
                                        Text(
                                            title,
                                            color = ThemeColorUtils.black()
                                        )
                                    },
                                    selectedContentColor = Color(0xFFFA954D),
                                    unselectedContentColor = ThemeColorUtils.black()
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                        ) {
                            tabs.forEachIndexed { index, _ ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(3.dp)
                                        .background(if (index == selectedTab) Color(0xFFFA954D) else Color.Transparent)
                                )
                            }
                        }
                    }

                    item {
                        HorizontalDivider(color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)).copy(alpha = 0.4f))
                    }

                    item {
                        when (selectedTab) {
                            0 -> TimelineTabContent(
                                userProfile = displayedProfile!!,
                                suggestionCount = if (isViewingOwnProfile) suggestionCount else 0,
                                onFriendSuggestions = {
                                    if (isViewingOwnProfile) {
                                        navController.navigate("friend_suggestions")
                                    }
                                },
                                onNavigateToPost = {
                                    if (isViewingOwnProfile) {
                                        navController.navigate("post_detection_history")
                                    }
                                },
                                isViewingOwnProfile = isViewingOwnProfile,
                                timelineUserId = profileUserId,
                                navController = navController
                            )

                            1 -> AboutTabContent(
                                userProfile = displayedProfile!!,
                                mutualFriends = if (isViewingOwnProfile) friendsList else viewedUserFriends,
                                onEditInfo = {
                                    if (isViewingOwnProfile) {
                                        showEditDialog = true
                                    }
                                },
                                onPrivacyChange = { fieldName, privacy ->
                                    if (isViewingOwnProfile) {
                                        authViewModel.updateFieldPrivacy(fieldName, privacy) { success, message ->
                                            if (!success) {
                                                Log.e("ProfileScreen", "Failed to update privacy: $message")
                                            }
                                        }
                                    }
                                },
                                isViewingOwnProfile = isViewingOwnProfile,
                                onViewFriends = {
                                    if (isViewingOwnProfile) {
                                        navController.navigate("friends")
                                    } else {
                                        navController.navigate("friends?userId=${viewUserId}")
                                    }
                                }
                            )

                            2 -> PhotosTabContent(
                                userId = profileUserId,
                                isViewingOwnProfile = isViewingOwnProfile
                            )

                            3 -> AudiosTabContent(
                                userId = profileUserId,
                                isViewingOwnProfile = isViewingOwnProfile
                            )

                            4 -> MoreTabContent(
                                userId = profileUserId
                            )
                        }
                    }
                }
            }
        }
    }

    if (showChangePhotoSheet) {
        ChangePhotoBottomSheet(
            onDismiss = { showChangePhotoSheet = false },
            onTakePhoto = {
                scope.launch {
                    val uri = getTempUri(context)
                    cameraLauncher.launch(uri)
                    showChangePhotoSheet = false
                }
            },
            onChooseFromGallery = {
                galleryLauncher.launch("image/*")
                showChangePhotoSheet = false
            },
            canRemove = !displayedProfile?.photoUrl.isNullOrEmpty(),
            onRemove = {
                authViewModel.removeProfileImage { success, _ ->
                    if (success) showChangePhotoSheet = false
                }
            },
            onViewCurrent = {
                previewingCoverPhoto = false
                showPhotoPreview = true
                showChangePhotoSheet = false
            }
        )
    }

    if (showChangeCoverSheet) {
        ChangePhotoBottomSheet(
            onDismiss = { showChangeCoverSheet = false },
            onTakePhoto = {
                scope.launch {
                    val uri = getTempUri(context)
                    cameraLauncherCover.launch(uri)
                    showChangeCoverSheet = false
                }
            },
            onChooseFromGallery = {
                galleryLauncherCover.launch("image/*")
                showChangeCoverSheet = false
            },
            canRemove = !displayedProfile?.coverPhotoUrl.isNullOrEmpty(),
            onRemove = {
                authViewModel.removeCoverPhoto { success, _ ->
                    if (success) showChangeCoverSheet = false
                }
            },
            onViewCurrent = {
                previewingCoverPhoto = true
                showPhotoPreview = true
                showChangeCoverSheet = false
            }
        )
    }
    
    if (showEditDialog && displayedProfile != null) {
        EditProfileDialog(
            userProfile = displayedProfile!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedProfile ->
                authViewModel.updateUserProfile(updatedProfile, context) { success, message ->
                    if (success) {
                        Log.d("ProfileScreen", "Profile updated successfully")
                    } else {
                        Log.e("ProfileScreen", "Failed to update profile: $message")
                    }
                    showEditDialog = false
                }
            }
        )
    }

    if (showAddInfoDialog && displayedProfile != null) {
        AddInfoDialog(
            onDismiss = { showAddInfoDialog = false },
            onSave = { key, value ->
                authViewModel.updateProfileField(key, value, context) { success, message ->
                    if (success) {
                        Log.d("ProfileScreen", "Profile updated successfully")
                    } else {
                        Log.e("ProfileScreen", "Failed to update profile: $message")
                    }
                    showAddInfoDialog = false
                }
            }
        )
    }
    if (showPhotoPreview && displayedProfile != null) {
        val imageUrl = if (previewingCoverPhoto) displayedProfile!!.coverPhotoUrl else displayedProfile!!.photoUrl
        imageUrl?.let {
            PhotoPreviewDialog(
                imageModel = it,
                aspectRatio = if (previewingCoverPhoto) null else 1f,
                onDismiss = { showPhotoPreview = false }
            )
        }
    }

    // Dialogs for Unfriend, Block, Unblock
    if (showUnfriendDialog && displayedProfile != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUnfriendDialog = false },
            title = { Text(stringResource(R.string.friends_dialog_unfriend_title, displayedProfile!!.fullName)) },
            text = { Text(stringResource(R.string.friends_dialog_unfriend_msg, displayedProfile!!.fullName)) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        friendViewModel.unfriend(viewUserId ?: "") { success, message ->
                             android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                             showUnfriendDialog = false
                             if (success) {
                             }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showUnfriendDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showBlockDialog && displayedProfile != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text(stringResource(R.string.friends_dialog_block_title, displayedProfile!!.fullName)) },
            text = { Text(stringResource(R.string.friends_dialog_block_msg, displayedProfile!!.fullName)) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        friendViewModel.blockUser(viewUserId ?: "", displayedProfile!!.fullName) { success, message ->
                             android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                             showBlockDialog = false
                             if (success) {
                                 isBlocked = true
                                 navController.popBackStack()
                             }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text(stringResource(R.string.profile_block, ""))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showBlockDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showUnblockDialog && displayedProfile != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUnblockDialog = false },
            title = { Text(stringResource(R.string.blocked_dialog_title, displayedProfile!!.fullName)) },
            text = { Text(stringResource(R.string.blocked_dialog_msg, displayedProfile!!.fullName)) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        friendViewModel.unblockUser(viewUserId ?: "") { success, message ->
                             android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                             showUnblockDialog = false
                             if (success) {
                                 isBlocked = false
                             }
                        }
                    }
                ) {
                    Text(stringResource(R.string.blocked_unblock_btn))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showUnblockDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}