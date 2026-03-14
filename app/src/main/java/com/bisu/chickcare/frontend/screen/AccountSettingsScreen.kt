package com.bisu.chickcare.frontend.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.service.ImageCropHelper
import com.bisu.chickcare.backend.viewmodels.AuthViewModel
import androidx.compose.ui.window.Dialog
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.bisu.chickcare.frontend.components.ChangePasswordDialog
import com.bisu.chickcare.frontend.components.DeleteAccountVerificationDialog
import com.bisu.chickcare.frontend.components.EditEmailDialog
import com.bisu.chickcare.frontend.components.EditFullNameDialog
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import com.bisu.chickcare.frontend.utils.getTempUri
import com.yalantis.ucrop.UCrop
import com.bisu.chickcare.frontend.components.ProfilePicturePickerDialog as ProfilePictureDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val userProfile by authViewModel.userProfile.collectAsState()
    val context = LocalContext.current
    val currentUserId = authViewModel.getCurrentUserId(context)
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditEmailDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showProfilePictureDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showVerificationDialog by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }
    val tempUri = remember { getTempUri(context) }
    
    // Resources needed inside callback/logic
    val cropErrorFormat = stringResource(R.string.account_crop_error)
    val emailEditError = stringResource(R.string.account_email_edit_error)
    
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
            dialogMessage = String.format(cropErrorFormat, cropError?.message)
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
                        stringResource(R.string.account_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ThemeColorUtils.black()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
        ) {
            // Divider below top bar
                    HorizontalDivider(
                        color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)),
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
                        modifier = Modifier
                            .fillMaxWidth(),
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
                                        color = ThemeColorUtils.black(),
                                        strokeWidth = 4.dp
                                    )
                                } else if (userProfile?.photoUrl != null && userProfile!!.photoUrl!!.isNotEmpty()) {
                                    AsyncImage(
                                        model = userProfile!!.photoUrl,
                                        contentDescription = stringResource(R.string.account_photo_desc),
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
                                            contentDescription = stringResource(R.string.account_photo_desc),
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
                                    text = userProfile?.fullName ?: "User", // Keep as is, User name is dynamic
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeColorUtils.black()
                                )
                                Text(
                                    text = userProfile?.email ?: stringResource(R.string.account_no_email),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ThemeColorUtils.darkGray(Color(0xFF666666))
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
                        text = stringResource(R.string.account_basic_info_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black(),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Full Name
                item {
                    AccountInfoCard(
                        icon = R.drawable.ic_person_flaticon,
                        title = stringResource(R.string.account_full_name),
                        value = userProfile?.fullName ?: stringResource(R.string.account_not_set),
                        onClick = { showEditNameDialog = true }
                    )
                }

                // Email
                item {
                    AccountInfoCard(
                        icon = R.drawable.ic_email_flaticon,
                        title = stringResource(R.string.account_email),
                        value = userProfile?.email ?: stringResource(R.string.account_not_set),
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
                        text = stringResource(R.string.account_actions_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black(),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Edit Profile
                item {
                    AccountActionCard(
                        icon = R.drawable.ic_edit_flaticon,
                        title = stringResource(R.string.account_edit_profile_title),
                        description = stringResource(R.string.account_edit_profile_desc),
                        onClick = { navController.navigate("profile?initialTab=1") }
                    )
                }

                // Delete Account
                item {
                    AccountActionCardWithIcon(
                        iconVector = Icons.Default.Delete,
                        title = stringResource(R.string.account_delete_title),
                        description = stringResource(R.string.account_delete_desc),
                        onClick = { showDeleteAccountDialog = true },
                        isDestructive = true
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
                currentEmail = userProfile?.email ?: "",
                onDismiss = { showEditEmailDialog = false },
                onSave = { _ ->
                    dialogMessage = emailEditError
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
                title = { Text(stringResource(R.string.common_success), fontWeight = FontWeight.Bold) },
                text = { Text(dialogMessage) },
                confirmButton = {
                    TextButton(onClick = { showSuccessDialog = false }) {
                        Text(stringResource(R.string.common_ok))
                    }
                },
                containerColor = ThemeColorUtils.white(),
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = { Text(stringResource(R.string.common_error), fontWeight = FontWeight.Bold, color = Color.Red) },
                text = { Text(dialogMessage) },
                confirmButton = {
                    TextButton(onClick = { showErrorDialog = false }) {
                        Text(stringResource(R.string.common_ok))
                    }
                },
                containerColor = ThemeColorUtils.white(),
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Delete Account Confirmation Dialog
        if (showDeleteAccountDialog) {
            Dialog(
                onDismissRequest = { if (!isDeletingAccount) showDeleteAccountDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Background scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable(onClick = { if (!isDeletingAccount) showDeleteAccountDialog = false })
                    )
                    
                    // Bottom sheet content
                    androidx.compose.material3.Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        color = Color.White,
                        tonalElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 28.dp)
                        ) {
                            // Title
                            Text(
                                text = stringResource(R.string.account_delete_dialog_title),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = com.bisu.chickcare.ui.theme.FiraSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                ),
                                color = Color(0xFF1A1A1A)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Message
                            Text(
                                text = stringResource(R.string.account_delete_dialog_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF545454),
                                lineHeight = 22.sp
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Confirm Button (Red, full width)
                            Button(
                                onClick = {
                                    showDeleteAccountDialog = false
                                    showVerificationDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isDeletingAccount
                            ) {
                                Text(
                                    text = stringResource(R.string.account_delete_continue),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Cancel Button (White with border, full width)
                            androidx.compose.material3.OutlinedButton(
                                onClick = { showDeleteAccountDialog = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                enabled = !isDeletingAccount
                            ) {
                                Text(
                                    text = stringResource(R.string.account_delete_cancel),
                                    color = Color(0xFF1A1A1A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // Verification Code Dialog
        if (showVerificationDialog) {
            DeleteAccountVerificationDialog(
                userEmail = userProfile?.email ?: "",
                userPhone = userProfile?.contact ?: "",
                onDismiss = { 
                    if (!isDeletingAccount) {
                        showVerificationDialog = false
                    }
                },
                onVerify = { verificationCode ->
                    isDeletingAccount = true
                    authViewModel.deleteAccount(verificationCode) { success, message ->
                        isDeletingAccount = false
                        if (success) {
                            // Navigate to login screen after successful deletion
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        } else {
                            dialogMessage = message
                            showErrorDialog = true
                            showVerificationDialog = false
                        }
                    }
                },
                onResendCode = {
                    authViewModel.sendAccountDeletionVerificationCode { success, message ->
                        if (!success) {
                            dialogMessage = message
                            showErrorDialog = true
                        }
                    }
                },
                isDeleting = isDeletingAccount
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

    val elevation = if (isPressed) 8.dp else 5.dp
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .indication(interactionSource, ripple(bounded = true))
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource
            ),
        border = BorderStroke(
            width = 1.dp,
            color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C))
        ),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.surface(Color(0xFFE5E2DE)),
            contentColor = ThemeColorUtils.black()
        ),
        shape = shape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation,
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
                modifier = Modifier.size(38.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeColorUtils.darkGray(Color(0xFF666666))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ThemeColorUtils.black()
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
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val shape = RoundedCornerShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val elevation = if (isPressed) 8.dp else 5.dp
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .indication(interactionSource, ripple(bounded = true))
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource
            ),
        border = BorderStroke(
            width = 1.dp,
            color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C))
        ),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.surface(Color(0xFFE5E2DE)),
            contentColor = ThemeColorUtils.black()
        ),
        shape = shape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation,
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
                modifier = Modifier.size(38.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDestructive) Color(0xFFD32F2F) else ThemeColorUtils.black()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDestructive) Color(0xFFD32F2F).copy(alpha = 0.8f) else ThemeColorUtils.darkGray(Color(0xFF666666))
                )
            }
        }
    }
}

@Composable
fun AccountActionCardWithIcon(
    iconVector: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val shape = RoundedCornerShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val elevation = if (isPressed) 8.dp else 5.dp
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .indication(interactionSource, ripple(bounded = true))
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource
            ),
        border = BorderStroke(
            width = 1.dp,
            color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C))
        ),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.surface(Color(0xFFE5E2DE)),
            contentColor = ThemeColorUtils.black()
        ),
        shape = shape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation,
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
            Icon(
                imageVector = iconVector,
                contentDescription = title,
                modifier = Modifier.size(38.dp),
                tint = if (isDestructive) Color(0xFFD32F2F) else Color.Unspecified
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDestructive) Color(0xFFD32F2F) else ThemeColorUtils.darkGray(Color(0xFF231C16))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDestructive) Color(0xFFD32F2F).copy(alpha = 0.8f) else ThemeColorUtils.lightGray(Color(0xFF666666))
                )
            }
        }
    }
}