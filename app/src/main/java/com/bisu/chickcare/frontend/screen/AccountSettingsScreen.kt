package com.bisu.chickcare.frontend.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.service.ImageCropHelper
import com.bisu.chickcare.backend.viewmodels.AuthViewModel
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.bisu.chickcare.frontend.components.ChangePasswordDialog
import com.bisu.chickcare.frontend.components.EditEmailDialog
import com.bisu.chickcare.frontend.components.EditFullNameDialog
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import com.yalantis.ucrop.UCrop
import com.bisu.chickcare.frontend.components.ProfilePicturePickerDialog as ProfilePictureDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val userProfile by authViewModel.userProfile.collectAsState()
    val currentUser = authViewModel.auth.currentUser
    val context = LocalContext.current
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditEmailDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showProfilePictureDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    val tempUri = remember { getTempUri(context) }
    val cropLauncherProfile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: androidx.activity.result.ActivityResult ->
        val resultCode = result.resultCode
        val data = result.data

        if (resultCode == android.app.Activity.RESULT_OK && data != null) {
            val resultUri = UCrop.getOutput(data)
            resultUri?.let { croppedUri ->
                isUploading = true
                authViewModel.uploadProfileImage(croppedUri, context) { success, message ->
                    isUploading = false
                    if (success) {
                        dialogMessage = message
                        showSuccessDialog = true
                    } else {
                        dialogMessage = message
                        showErrorDialog = true
                    }
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR && data != null) {
            val cropError = UCrop.getError(data)
            dialogMessage = "Image crop error: ${cropError?.message}"
            showErrorDialog = true
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                ImageCropHelper.startProfilePictureCrop(cropLauncherProfile, tempUri, context)
            }
        }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                ImageCropHelper.startProfilePictureCrop(cropLauncherProfile, it, context)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Account",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ThemeColorUtils.darkGray(Color(0xFF231C16))
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFFFFF),
                    titleContentColor = ThemeColorUtils.darkGray(Color(0xFF231C16))
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
        ) {
            // Divider below top bar
            HorizontalDivider(
                color = ThemeColorUtils.lightGray(Color(0xFF7E7C7C)),
                thickness = 1.dp
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Header Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = ThemeColorUtils.white()
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Profile Picture (actual or placeholder)
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .clickable { showProfilePictureDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(80.dp),
                                        color = ThemeColorUtils.darkGray(Color(0xFF231C16)),
                                        strokeWidth = 4.dp
                                    )
                                } else if (userProfile?.photoUrl != null && userProfile!!.photoUrl!!.isNotEmpty()) {
                                    AsyncImage(
                                        model = userProfile!!.photoUrl,
                                        contentDescription = "Profile Picture",
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
                                            contentDescription = "Profile Picture",
                                            tint = Color(0xFF464343),
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                            }

                            // Name and Email (left-aligned)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = userProfile?.fullName ?: "User",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                                )
                                Text(
                                    text = currentUser?.email ?: "No email",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ThemeColorUtils.lightGray(Color(0xFF666666))
                                )
                            }
                        }
                    }
                }

                // Divider after profile card
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = ThemeColorUtils.lightGray(Color(0xFF7E7C7C)),
                        thickness = 1.dp
                    )
                }

                // Account Information Section
                item {
                    Text(
                        text = "Account Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.darkGray(Color(0xFF231C16)),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Full Name
                item {
                    AccountInfoCard(
                        icon = R.drawable.ic_person_flaticon,
                        title = "Full Name",
                        value = userProfile?.fullName ?: "Not set",
                        onClick = { showEditNameDialog = true }
                    )
                }

                // Email
                item {
                    AccountInfoCard(
                        icon = R.drawable.ic_email_flaticon,
                        title = "Email",
                        value = currentUser?.email ?: "Not set",
                        onClick = { showEditEmailDialog = true }
                    )
                }

                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = ThemeColorUtils.lightGray(Color(0xFF7E7C7C)),
                        thickness = 1.dp
                    )
                }

                // Account Actions Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Account Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.darkGray(Color(0xFF231C16)),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Edit Profile
                item {
                    AccountActionCard(
                        icon = R.drawable.ic_edit_flaticon,
                        title = "Edit Profile",
                        description = "Update your personal information",
                        onClick = { navController.navigate("profile?initialTab=1") }
                    )
                }
            }
        }

        if (showEditNameDialog) {
            EditFullNameDialog(
                currentName = userProfile?.fullName ?: "",
                onDismiss = { showEditNameDialog = false },
                onSave = { newName ->
                    authViewModel.updateProfileField("fullName", newName) { success, message ->
                        if (success) {
                            dialogMessage = message
                            showSuccessDialog = true
                            showEditNameDialog = false
                        } else {
                            dialogMessage = message
                            showErrorDialog = true
                        }
                    }
                }
            )
        }

        if (showEditEmailDialog) {
            EditEmailDialog(
                currentEmail = currentUser?.email ?: "",
                onDismiss = { showEditEmailDialog = false },
                onSave = { newEmail ->
                    dialogMessage =
                        "Email change requires verification. Please use password reset feature."
                    showErrorDialog = true
                    showEditEmailDialog = false
                }
            )
        }

        if (showChangePasswordDialog) {
            ChangePasswordDialog(
                onDismiss = { showChangePasswordDialog = false },
                onSave = { oldPassword, newPassword, _ ->
                    authViewModel.changePassword(oldPassword, newPassword) { success, message ->
                        if (success) {
                            dialogMessage = message
                            showSuccessDialog = true
                            showChangePasswordDialog = false
                        } else {
                            dialogMessage = message
                            showErrorDialog = true
                        }
                    }
                }
            )
        }

        if (showProfilePictureDialog) {
            ProfilePictureDialog(
                onDismiss = { showProfilePictureDialog = false },
                onTakePhoto = {
                    cameraLauncher.launch(tempUri)
                },
                onChooseFromGallery = {
                    galleryLauncher.launch("image/*")
                },
                onRemovePhoto = {
                    authViewModel.removeProfileImage { success, message ->
                        if (success) {
                            dialogMessage = message
                            showSuccessDialog = true
                        } else {
                            dialogMessage = message
                            showErrorDialog = true
                        }
                    }
                },
                hasPhoto = userProfile?.photoUrl != null
            )
        }

        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                title = { Text("Success", fontWeight = FontWeight.Bold) },
                text = { Text(dialogMessage) },
                confirmButton = {
                    TextButton(onClick = { showSuccessDialog = false }) {
                        Text("OK")
                    }
                },
                containerColor = ThemeColorUtils.white(),
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = { Text("Error", fontWeight = FontWeight.Bold, color = Color.Red) },
                text = { Text(dialogMessage) },
                confirmButton = {
                    TextButton(onClick = { showErrorDialog = false }) {
                        Text("OK")
                    }
                },
                containerColor = ThemeColorUtils.white(),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun AccountInfoCard(
    @androidx.annotation.DrawableRes icon: Int,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
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
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.beige(Color(0xFFE5E2DE))
        ),
        shape = shape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 8.dp else 5.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 6.dp,
            focusedElevation = 6.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = title,
                modifier = Modifier.size(38.dp),
                colorFilter = if (ThemeViewModel.isDarkMode) {
                    ColorFilter.tint(
                        color = ThemeColorUtils.lightGray(Color(0xFFA1AAB2)),
                        blendMode = BlendMode.SrcAtop
                    )
                } else {
                    null
                }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeColorUtils.lightGray(Color(0xFF666666))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                )
            }
        }
    }
}

@Composable
fun AccountActionCard(
    @androidx.annotation.DrawableRes icon: Int,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
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
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.beige(Color(0xFFE5E2DE))
        ),
        shape = shape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 8.dp else 5.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 6.dp,
            focusedElevation = 6.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = title,
                modifier = Modifier.size(38.dp),
                colorFilter = if (ThemeViewModel.isDarkMode) {
                    ColorFilter.tint(
                        color = ThemeColorUtils.lightGray(Color(0xFFA1AAB2)),
                        blendMode = BlendMode.SrcAtop
                    )
                } else {
                    null
                }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeColorUtils.lightGray(Color(0xFF666666))
                )
            }
        }
    }
}