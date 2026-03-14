package com.bisu.chickcare.frontend.screen

import android.content.Context
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.service.BiometricAuthHelper
import com.bisu.chickcare.backend.service.TwoFactorAuthHelper
import com.bisu.chickcare.backend.viewmodels.AuthViewModel
import com.bisu.chickcare.frontend.components.BiometricAuthSetupDialog
import com.bisu.chickcare.frontend.components.ChangePasswordDialog
import com.bisu.chickcare.frontend.components.TwoFactorAuthSetupDialog
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityPrivacyScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val context = LocalContext.current
    val currentUserId = authViewModel.getCurrentUserId(context)
    
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
    var twoFactorSecret by remember { mutableStateOf<String?>(null) }
    var isBiometricAvailable by remember { mutableStateOf(false) }
    
    // Load initial state
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // Load 2FA status
            if (currentUserId != null) {
                // Check local preferences first
                val local2FAEnabled = TwoFactorAuthHelper.isTwoFactorEnabled(context)
                
                // Sync with Firestore to ensure consistency
                val firestoreSecret = TwoFactorAuthHelper.getTwoFactorSecret(currentUserId)
                val firestore2FAEnabled = firestoreSecret != null
                
                // Use Firestore status if available, otherwise use local
                twoFactorEnabled = firestore2FAEnabled || local2FAEnabled
                
                // If Firestore has secret but local doesn't, sync it
                if (firestoreSecret != null && !local2FAEnabled) {
                    val prefs = context.getSharedPreferences(
                        "two_factor_prefs", 
                        Context.MODE_PRIVATE
                    )
                    prefs.edit {
                        putBoolean("two_factor_enabled", true)
                        putString("two_factor_secret", firestoreSecret)
                    }
                }
            }
            
            // Load biometric status
            biometricEnabled = BiometricAuthHelper.isBiometricEnabled(context)
            isBiometricAvailable = BiometricAuthHelper.isBiometricAvailable(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        androidx.compose.ui.res.stringResource(R.string.security_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ThemeColorUtils.black()
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = androidx.compose.ui.res.stringResource(R.string.back),
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
            // Security Section
            item {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.security_section_security),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black(),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
            }

            // Two-Factor Authentication
            item {
                SecuritySettingCard(
                    icon = R.drawable.ic_two_factor_flaticon,
                    title = androidx.compose.ui.res.stringResource(R.string.security_2fa_title),
                    description = androidx.compose.ui.res.stringResource(R.string.security_2fa_desc),
                    isEnabled = twoFactorEnabled,
                    onToggle = { enabled ->
                        if (enabled) {
                            if (currentUserId != null) {
                                // Generate secret first
                                twoFactorSecret = TwoFactorAuthHelper.generateSecret()
                                showTwoFactorSetupDialog = true
                            } else {
                                dialogMessage = context.getString(R.string.security_login_required)
                                showErrorDialog = true
                            }
                        } else {
                            // Disable 2FA
                            if (currentUserId != null) {
                                authViewModel.viewModelScope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            TwoFactorAuthHelper.disableTwoFactor(currentUserId, context)
                                        }
                                        twoFactorEnabled = false
                                        dialogMessage = context.getString(R.string.security_2fa_disabled)
                                        showSuccessDialog = true
                                    } catch (e: Exception) {
                                        dialogMessage = "${context.getString(R.string.common_error)}: ${e.message}"
                                        showErrorDialog = true
                                    }
                                }
                            }
                        }
                    }
                )
            }

            // Biometric Authentication
            item {
                SecuritySettingCard(
                    icon = R.drawable.ic_biometric_flaticon,
                    title = androidx.compose.ui.res.stringResource(R.string.security_biometric_title),
                    description = if (isBiometricAvailable) {
                        androidx.compose.ui.res.stringResource(R.string.security_biometric_desc_enabled)
                    } else {
                        BiometricAuthHelper.getBiometricStatus(context)
                    },
                    isEnabled = biometricEnabled,
                    onToggle = { enabled ->
                        if (enabled) {
                            if (isBiometricAvailable) {
                                showBiometricSetupDialog = true
                            } else {
                                dialogMessage = BiometricAuthHelper.getBiometricStatus(context)
                                showErrorDialog = true
                            }
                        } else {
                            BiometricAuthHelper.disableBiometric(context)
                            biometricEnabled = false
                            dialogMessage = context.getString(R.string.security_biometric_disabled)
                            showSuccessDialog = true
                        }
                    }
                )
            }

            // Change Password
            item {
                SecurityActionCard(
                    icon = R.drawable.ic_lock_flaticon,
                    title = androidx.compose.ui.res.stringResource(R.string.security_change_password_title),
                    description = androidx.compose.ui.res.stringResource(R.string.security_change_password_desc),
                    onClick = { showChangePasswordDialog = true }
                )
            }

            // Privacy Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.security_section_privacy),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black(),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
            }

            // Data Encryption
            item {
                SecuritySettingCard(
                    icon = R.drawable.ic_shield_encrypt_flaticon,
                    title = androidx.compose.ui.res.stringResource(R.string.security_encryption_title),
                    description = androidx.compose.ui.res.stringResource(R.string.security_encryption_desc),
                    isEnabled = dataEncryptionEnabled,
                    onToggle = { },
                    isToggleable = false
                )
            }

            // Privacy Mode
            item {
                SecuritySettingCard(
                    icon = R.drawable.ic_privacy_mode_flaticon,
                    title = androidx.compose.ui.res.stringResource(R.string.security_privacy_mode_title),
                    description = androidx.compose.ui.res.stringResource(R.string.security_privacy_mode_desc),
                    isEnabled = privacyModeEnabled,
                    onToggle = { privacyModeEnabled = it }
                )
            }

            // Privacy Policy & Terms
            item {
                Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.security_section_legal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black(),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
            }

            item {
                SecurityActionCard(
                    icon = R.drawable.ic_privacy_policy_flaticon,
                    title = androidx.compose.ui.res.stringResource(R.string.security_policy_title),
                    description = androidx.compose.ui.res.stringResource(R.string.security_policy_desc),
                    onClick = { navController.navigate("privacy_policy") }
                )
            }

            item {
                SecurityActionCard(
                    icon = R.drawable.ic_terms_flaticon,
                    title = androidx.compose.ui.res.stringResource(R.string.security_terms_title),
                    description = androidx.compose.ui.res.stringResource(R.string.security_terms_desc),
                    onClick = { navController.navigate("terms_of_service") }
                )
            }
        }
        }
    }
    
    // Dialogs
    if (showTwoFactorSetupDialog && currentUserId != null && twoFactorSecret != null) {
        TwoFactorAuthSetupDialog(
            secretKey = twoFactorSecret,
            onDismiss = { 
                showTwoFactorSetupDialog = false
                twoFactorSecret = null
            },
            onEnable = { code ->
                // Verify code and enable
                authViewModel.viewModelScope.launch {
                    try {
                        val success = withContext(Dispatchers.IO) {
                            TwoFactorAuthHelper.verifyAndEnableTwoFactor(
                                currentUserId,
                                twoFactorSecret!!,
                                code,
                                context
                            )
                        }
                        
                        if (success) {
                            twoFactorEnabled = true
                            dialogMessage = context.getString(R.string.common_success)
                            showSuccessDialog = true
                            showTwoFactorSetupDialog = false
                            twoFactorSecret = null
                        } else {
                            dialogMessage = "Invalid verification code. Please try again." // This might come from Helper, kept hardcoded or move to resource if needed
                            showErrorDialog = true
                        }
                    } catch (e: Exception) {
                        dialogMessage = "${context.getString(R.string.common_error)}: ${e.message}"
                        showErrorDialog = true
                    }
                }
            }
        )
    }
    
    if (showBiometricSetupDialog) {
        val title = context.getString(R.string.security_biometric_prompt_title)
        val subtitle = context.getString(R.string.security_biometric_prompt_subtitle)
        val successMsg = context.getString(R.string.security_biometric_success)
        
        BiometricAuthSetupDialog(
            onDismiss = { showBiometricSetupDialog = false },
            onEnable = {
                // Show biometric prompt to authenticate
                val activity = context as? FragmentActivity
                if (activity != null) {
                    BiometricAuthHelper.authenticate(
                        activity = activity,
                        title = title,
                        subtitle = subtitle,
                        onSuccess = {
                            BiometricAuthHelper.enableBiometric(context)
                            biometricEnabled = true
                            dialogMessage = successMsg
                            showSuccessDialog = true
                            showBiometricSetupDialog = false
                        },
                        onError = { error ->
                            dialogMessage = "Biometric authentication failed: $error"
                            showErrorDialog = true
                        },
                        onCancel = {
                            showBiometricSetupDialog = false
                        }
                    )
                } else {
                    dialogMessage = "Unable to show biometric prompt. Please try again."
                    showErrorDialog = true
                }
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
            title = { Text(androidx.compose.ui.res.stringResource(R.string.common_success), fontWeight = FontWeight.Bold) },
            text = { Text(dialogMessage) },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text(androidx.compose.ui.res.stringResource(R.string.common_ok))
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
            title = { Text(androidx.compose.ui.res.stringResource(R.string.common_error), fontWeight = FontWeight.Bold, color = Color.Red) },
            text = { Text(dialogMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text(androidx.compose.ui.res.stringResource(R.string.common_ok))
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
        border = BorderStroke(
            width = 1.dp,
            color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C))
        ),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.surface(Color(0xFFE5E2DE)),
            contentColor = ThemeColorUtils.black()
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
                modifier = Modifier.size(38.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ThemeColorUtils.black()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeColorUtils.darkGray(Color(0xFF666666))
                )
            }
            if (isToggleable) {
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ThemeColorUtils.white(),
                        checkedTrackColor = ThemeColorUtils.black(),
                        uncheckedThumbColor = ThemeColorUtils.white(),
                        uncheckedTrackColor = ThemeColorUtils.lightGray(Color(0xFF9C9FA1))
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
                modifier = Modifier.size(38.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ThemeColorUtils.black()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeColorUtils.darkGray(Color(0xFF666666))
                )
            }
        }
    }
}