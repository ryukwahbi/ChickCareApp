package com.bisu.chickcare.frontend.screen

import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.backend.data.UserProfile
import com.bisu.chickcare.backend.service.ImageCropHelper
import com.bisu.chickcare.backend.viewmodels.AuthViewModel
import com.bisu.chickcare.backend.viewmodels.FriendViewModel
import com.bisu.chickcare.frontend.components.AboutTabContent
import com.bisu.chickcare.frontend.components.AddInfoDialog
import com.bisu.chickcare.frontend.components.AudiosTabContent
import com.bisu.chickcare.frontend.components.ChangePhotoBottomSheet
import com.bisu.chickcare.frontend.components.CoverPhotoSection
import com.bisu.chickcare.frontend.components.EditProfileDialog
import com.bisu.chickcare.frontend.components.MoreTabContent
import com.bisu.chickcare.frontend.components.PhotoPreviewDialog
import com.bisu.chickcare.frontend.components.PhotosTabContent
import com.bisu.chickcare.frontend.components.ProfilePictureAndNameSection
import com.bisu.chickcare.frontend.components.TimelineTabContent
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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
    val auth = authViewModel.auth
    val currentUserId = auth.currentUser?.uid
    val isViewingOwnProfile = viewUserId == null || viewUserId == currentUserId
    val profileUserId = viewUserId ?: currentUserId.orEmpty()
    var displayedProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoadingProfile by remember { mutableStateOf(false) }
    val friendSuggestions by friendViewModel.suggestions.collectAsState()
    val friendsList by friendViewModel.friends.collectAsState()
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
    val context = LocalContext.current

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
        }
    }

    LaunchedEffect(Unit) {
        if (isViewingOwnProfile) {
            // Load friends FIRST, then suggestions, so filtering works correctly
            friendViewModel.loadFriends()
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
    LaunchedEffect(viewUserId) {
        if (!isViewingOwnProfile) {
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
    

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                // Start cropping instead of direct upload
                ImageCropHelper.startProfilePictureCrop(cropLauncherProfile, it, context)
            }
        }
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
                        text = if (isViewingOwnProfile) "Profile" else displayedProfile?.fullName ?: "Profile",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ThemeColorUtils.darkGray(Color(0xFF231C16))
                        )
                    }
                },
                actions = {
                    // Only show settings when viewing own profile
                    if (isViewingOwnProfile) {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = ThemeColorUtils.darkGray(Color(0xFF231C16)))
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
        if (displayedProfile == null && !isLoadingProfile) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (isLoadingProfile) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val tabs = listOf("Timeline", "About", "Photos", "Audios", "More")

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
                    .padding(innerPadding)
                    .imePadding()
            ) {
                item {
                    Box {
                        CoverPhotoSection(
                            coverPhotoUrl = displayedProfile?.coverPhotoUrl,
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
                                containerColor = Color(0xFFFFF0DB)
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
                                    text = "You have blocked ${displayedProfile!!.fullName}",
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
                                    Text("Unblock", color = Color.White)
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
                            timelineUserId = profileUserId
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
                        4 -> MoreTabContent()
                    }
                }
            }

            if (showChangePhotoSheet) {
                ChangePhotoBottomSheet(
                    onDismiss = { showChangePhotoSheet = false },
                    onTakePhoto = {
                        cameraLauncher.launch(tempUri)
                        showChangePhotoSheet = false
                    },
                    onChooseFromGallery = {
                        galleryLauncher.launch("image/*")
                        showChangePhotoSheet = false
                    },
                    canRemove = displayedProfile?.photoUrl != null,
                    onRemove = {
                        authViewModel.removeProfileImage { _, _ ->
                            showChangePhotoSheet = false
                        }
                    },
                    onViewCurrent = {
                        showChangePhotoSheet = false
                        previewingCoverPhoto = false
                        showPhotoPreview = true
                    }
                )
            }

            if (showChangeCoverSheet) {
                ChangePhotoBottomSheet(
                    onDismiss = { showChangeCoverSheet = false },
                    onTakePhoto = {
                        cameraLauncherCover.launch(tempCoverUri)
                        showChangeCoverSheet = false
                    },
                    onChooseFromGallery = {
                        galleryLauncherCover.launch("image/*")
                        showChangeCoverSheet = false
                    },
                    canRemove = displayedProfile?.coverPhotoUrl != null,
                    onRemove = {
                        authViewModel.removeCoverPhoto { _, _ ->
                            showChangeCoverSheet = false
                        }
                    },
                    onViewCurrent = {
                        showChangeCoverSheet = false
                        previewingCoverPhoto = true
                        showPhotoPreview = true
                    }
                )
            }

            if (showPhotoPreview) {
                PhotoPreviewDialog(
                    imageModel = if (previewingCoverPhoto) {
                        displayedProfile?.coverPhotoUrl ?: ""
                    } else {
                        displayedProfile?.photoUrl ?: ""
                    },
                    aspectRatio = if (previewingCoverPhoto) 16f / 9f else 1f,
                    onDismiss = {
                        showPhotoPreview = false
                        previewingCoverPhoto = false
                    }
                )
            }

            if (showEditDialog && displayedProfile != null) {
                EditProfileDialog(
                    userProfile = displayedProfile!!,
                    onDismiss = { showEditDialog = false },
                    onSave = { updatedFields ->
                        // Update all fields
                        updatedFields.forEach { (fieldName, value) ->
                            authViewModel.updateProfileField(fieldName, value) { success, message ->
                                if (!success) {
                                    Log.e("ProfileScreen", "Failed to update $fieldName: $message")
                                }
                            }
                        }
                        showEditDialog = false
                    }
                )
            }

            if (showAddInfoDialog) {
                AddInfoDialog(
                    onDismiss = { showAddInfoDialog = false },
                    onSave = { fieldName, fieldValue ->
                        authViewModel.updateProfileField(fieldName, fieldValue) { _, _ ->
                            showAddInfoDialog = false
                        }
                    }
                )
            }
            
            // Unblock Dialog
            if (showUnblockDialog && viewUserId != null && displayedProfile != null) {
                AlertDialog(
                    onDismissRequest = { showUnblockDialog = false },
                    title = { 
                        Text(
                            "Unblock ${displayedProfile!!.fullName}?",
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    text = { 
                        Text("Are you sure you want to unblock ${displayedProfile!!.fullName}? You'll be able to see their posts and send them messages again.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                friendViewModel.unblockUser(viewUserId) { success, message ->
                                    showUnblockDialog = false
                                    if (success) {
                                        isBlocked = false
                                        // Show success message (you can add a snackbar here if needed)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("Unblock", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUnblockDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

fun getTempUri(context: Context): Uri {
    // Create temp file in cache directory for camera capture
    // File.createTempFile already creates the file, so no need to call createNewFile()
    val tempFile = File.createTempFile(
        "temp_image_${System.currentTimeMillis()}",
        ".jpg",
        context.cacheDir
    )
    // Note: Don't use deleteOnExit() here as the file needs to exist
    // until it's used by the camera launcher
    val authority = "${context.packageName}.provider"
    return FileProvider.getUriForFile(context, authority, tempFile)
}