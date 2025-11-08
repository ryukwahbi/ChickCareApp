package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.viewmodels.AuthViewModel
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.bisu.chickcare.frontend.components.BiometricAuthSetupDialog
import com.bisu.chickcare.frontend.components.ChangePasswordDialog
import com.bisu.chickcare.frontend.components.TwoFactorAuthSetupDialog
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityPrivacyScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    var twoFactorEnabled by remember { mutableStateOf(false) }
    var biometricEnabled by remember { mutableStateOf(false) }
    var dataEncryptionEnabled by remember { mutableStateOf(true) }
    var privacyModeEnabled by remember { mutableStateOf(false) }
    var showTwoFactorSetupDialog by remember { mutableStateOf(false) }
    var showBiometricSetupDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Security & Privacy",
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
            // Security Section
            item {
                Text(
                    text = "Security",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColorUtils.darkGray(Color(0xFF231C16)),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Two-Factor Authentication
            item {
                SecuritySettingCard(
                    icon = R.drawable.ic_two_factor_flaticon,
                    title = "Two-Factor Authentication",
                    description = "Add an extra layer of security to your account",
                    isEnabled = twoFactorEnabled,
                    onToggle = { enabled ->
                        if (enabled) {
                            showTwoFactorSetupDialog = true
                        } else {
                            twoFactorEnabled = false
                        }
                    }
                )
            }

            // Biometric Authentication
            item {
                SecuritySettingCard(
                    icon = R.drawable.ic_biometric_flaticon,
                    title = "Biometric Authentication",
                    description = "Use fingerprint or face recognition to unlock",
                    isEnabled = biometricEnabled,
                    onToggle = { enabled ->
                        if (enabled) {
                            showBiometricSetupDialog = true
                        } else {
                            biometricEnabled = false
                        }
                    }
                )
            }

            // Change Password
            item {
                SecurityActionCard(
                    icon = R.drawable.ic_lock_flaticon,
                    title = "Change Password",
                    description = "Update your account password",
                    onClick = { showChangePasswordDialog = true }
                )
            }

            // Privacy Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Privacy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColorUtils.darkGray(Color(0xFF231C16)),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Data Encryption
            item {
                SecuritySettingCard(
                    icon = R.drawable.ic_shield_encrypt_flaticon,
                    title = "Data Encryption",
                    description = "Encrypt your data for enhanced security",
                    isEnabled = dataEncryptionEnabled,
                    onToggle = { },
                    isToggleable = false
                )
            }

            // Privacy Mode
            item {
                SecuritySettingCard(
                    icon = R.drawable.ic_privacy_mode_flaticon,
                    title = "Privacy Mode",
                    description = "Hide your activity from others",
                    isEnabled = privacyModeEnabled,
                    onToggle = { privacyModeEnabled = it }
                )
            }

            // Privacy Policy & Terms
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Legal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColorUtils.darkGray(Color(0xFF231C16)),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SecurityActionCard(
                    icon = R.drawable.ic_privacy_policy_flaticon,
                    title = "Privacy Policy",
                    description = "Read our privacy policy",
                    onClick = { navController.navigate("privacy_policy") }
                )
            }

            item {
                SecurityActionCard(
                    icon = R.drawable.ic_terms_flaticon,
                    title = "Terms of Service",
                    description = "Read our terms of service",
                    onClick = { navController.navigate("terms_of_service") }
                )
            }
        }
        }
    }
    
    // Dialogs
    if (showTwoFactorSetupDialog) {
        TwoFactorAuthSetupDialog(
            onDismiss = { showTwoFactorSetupDialog = false },
            onEnable = { code ->
                // TODO: Implement two-factor authentication setup
                twoFactorEnabled = true
                dialogMessage = "Two-factor authentication enabled successfully"
                showSuccessDialog = true
                showTwoFactorSetupDialog = false
            }
        )
    }
    
    if (showBiometricSetupDialog) {
        BiometricAuthSetupDialog(
            onDismiss = { showBiometricSetupDialog = false },
            onEnable = {
                // TODO: Implement biometric authentication setup
                biometricEnabled = true
                dialogMessage = "Biometric authentication enabled successfully"
                showSuccessDialog = true
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
    
    // Success Dialog
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
    
    // Error Dialog
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

@Composable
fun SecuritySettingCard(
    @androidx.annotation.DrawableRes icon: Int,
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    isToggleable: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.beige(Color(0xFFE5E2DE))
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 5.dp,
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
            if (isToggleable) {
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ThemeColorUtils.white(),
                        checkedTrackColor = Color(0xFF131211),
                        uncheckedThumbColor = ThemeColorUtils.white(),
                        uncheckedTrackColor = Color(0xFFA9A9A9)
                    )
                )
            }
        }
    }
}

@Composable
fun SecurityActionCard(
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
                    color = Color(0xFF171311)
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