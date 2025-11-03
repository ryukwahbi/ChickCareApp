package com.bisu.chickcare.frontend.screen

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, viewUserId: String? = null) {
    val authViewModel: AuthViewModel = viewModel()
    val friendViewModel: FriendViewModel = viewModel()
    val currentUserProfile by authViewModel.userProfile.collectAsState()
    val auth = authViewModel.auth
    val isViewingOwnProfile = viewUserId == null || viewUserId == auth.currentUser?.uid
    var displayedProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoadingProfile by remember { mutableStateOf(false) }
    val friendSuggestions by friendViewModel.suggestions.collectAsState()
    val suggestionCount = friendSuggestions.size
    val mutualFriends by friendViewModel.friends.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showChangePhotoSheet by remember { mutableStateOf(false) }
    var showChangeCoverSheet by remember { mutableStateOf(false) }
    var showPhotoPreview by remember { mutableStateOf(false) }
    var previewingCoverPhoto by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddInfoDialog by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
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

    // Update displayed profile when current user profile changes (only if viewing own profile)
    LaunchedEffect(currentUserProfile, isViewingOwnProfile) {
        if (isViewingOwnProfile) {
            displayedProfile = currentUserProfile
        }
    }

    LaunchedEffect(Unit) {
        if (isViewingOwnProfile) {
            friendViewModel.loadFriendSuggestions()
            friendViewModel.loadFriends()
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
                            tint = Color(0xFF231C16)
                        )
                    }
                },
                actions = {
                    // Only show settings when viewing own profile
                    if (isViewingOwnProfile) {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF231C16))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFFFFF),
                    titleContentColor = Color(0xFF231C16)
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
                    .padding(innerPadding)
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

                item {
                    HorizontalDivider()
                }

                item {
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        indicator = { }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        title,
                                        color = Color.Black
                                    )
                                },
                                selectedContentColor = Color(0xFFFA954D),
                                unselectedContentColor = Color.Black
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
                    HorizontalDivider()
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
                            }
                        )
                        1 -> AboutTabContent(
                            userProfile = displayedProfile!!,
                            mutualFriends = if (isViewingOwnProfile) mutualFriends else emptyList(),
                            onEditInfo = {
                                if (isViewingOwnProfile) {
                                    showEditDialog = true
                                }
                            },
                            onAddInfo = {
                                // No longer used - keeping for backward compatibility
                            },
                            onEditField = { _, _ ->
                                // No longer used - editing only through pencil icon
                            },
                            onPrivacyChange = { fieldName, privacy ->
                                if (isViewingOwnProfile) {
                                    authViewModel.updateFieldPrivacy(fieldName, privacy) { success, message ->
                                        if (!success) {
                                            Log.e("ProfileScreen", "Failed to update privacy: $message")
                                        }
                                    }
                                }
                            }
                        )
                        2 -> PhotosTabContent()
                        3 -> AudiosTabContent()
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
        }
    }
}

private fun getTempUri(context: Context): Uri {
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
