package com.bisu.chickcare.frontend.screen

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.data.SettingOption
import com.bisu.chickcare.backend.viewmodels.AuthViewModel
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import com.bisu.chickcare.frontend.utils.ThemeColorUtils.NeutralRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchQuery by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDataStorageDialog by remember { mutableStateOf(false) }

    // Helper function to get icon resource with fallback for missing resources
    fun getSettingIcon(title: String): Int {
        return when (title) {
            "Account" -> R.drawable.ic_account_flaticon
            "Security and Privacy" -> R.drawable.ic_security_flaticon
            "Emergency Contacts" -> R.drawable.ic_security_flaticon
            "Notifications" -> R.drawable.ic_notifications_flaticon
            "Language" -> R.drawable.ic_language_flaticon
            "Theme" -> R.drawable.ic_theme_flaticon
            "Blocked Users" -> R.drawable.ic_security_flaticon
            "Data and Storage Info" -> R.drawable.ic_about_flaticon
            "About" -> R.drawable.ic_about_flaticon
            "Logout" -> R.drawable.ic_logout_flaticon
            else -> R.drawable.ic_account_flaticon
        }
    }

    // Create settings list using resources
    val accountTitle = stringResource(R.string.settings_account_title)
    val securityTitle = stringResource(R.string.settings_security_title)
    val emergencyTitle = stringResource(R.string.settings_emergency_title)
    val notificationsTitle = stringResource(R.string.settings_notifications_title)
    val languageTitle = stringResource(R.string.settings_language_title)
    val themeTitle = stringResource(R.string.settings_theme_title)
    val blockedTitle = stringResource(R.string.settings_blocked_title)
    val aboutTitle = stringResource(R.string.settings_about_title)
    val dataStorageTitle = stringResource(R.string.settings_data_storage_title)
    val logoutTitle = stringResource(R.string.settings_logout_title)

    val allSettingsOptions = listOf(
        SettingOption(
            title = accountTitle,
            subtitle = stringResource(R.string.settings_account_subtitle),
            icon = getSettingIcon("Account"),
            keywords = listOf(
                "full name", "name", "email", "email address", "birthdate", "birthday", "birth date",
                "date of birth", "gender", "contact", "contact number", "phone", "phone number",
                "mobile", "mobile number", "account information", "profile", "personal info",
                "personal information", "edit profile", "update profile", "change name", "change email",
                "farm location", "location", "address", "farm name", "bio", "about me",
                "delete account", "remove account", "deactivate account"
            )
        ),
        SettingOption(
            title = securityTitle,
            subtitle = stringResource(R.string.settings_security_subtitle),
            icon = getSettingIcon("Security and Privacy"),
            keywords = listOf(
                "password", "change password", "update password", "reset password", "old password",
                "new password", "confirm password", "security",
                "privacy", "privacy settings", "who can see", "visibility", "public", "private",
                "friends only", "profile visibility", "post visibility",
                "2fa", "two-factor", "authentication", "verify", "verification",
                "data", "download data", "export data", "backup", "data privacy"
            )
        ),
        SettingOption(
            title = emergencyTitle,
            subtitle = stringResource(R.string.settings_emergency_subtitle),
            icon = getSettingIcon("Emergency Contacts"),
            keywords = listOf(
                "emergency", "emergency contact", "emergency contacts", "veterinarian", "vet",
                "vet contact", "veterinary", "animal doctor", "doctor", "clinic", "animal clinic",
                "rescue", "hotline", "help", "urgent", "add contact", "add vet", "add veterinarian",
                "hospital", "animal hospital"
            )
        ),
        SettingOption(
            title = notificationsTitle,
            subtitle = stringResource(R.string.settings_notifications_subtitle),
            icon = getSettingIcon("Notifications"),
            keywords = listOf(
                "notification", "notifications", "alert", "alerts", "push notification",
                "push notifications", "notify", "reminder", "reminders",
                "sound", "notification sound", "ring", "ringtone", "tone",
                "vibration", "vibrate", "vibration pattern", "haptic", "haptic feedback",
                "silent", "mute", "do not disturb", "dnd",
                "preferences", "preference", "customize", "settings",
                "disease alert", "health alert", "detection alert", "friend request",
                "message notification", "chat notification", "activity", "updates"
            )
        ),
        SettingOption(
            title = languageTitle,
            subtitle = stringResource(R.string.settings_language_subtitle),
            icon = getSettingIcon("Language"),
            keywords = listOf(
                "language", "language settings", "app language", "change language",
                "english", "tagalog", "filipino", "cebuano", "bisaya", "visayan",
                "hiligaynon", "ilonggo", "waray", "kapampangan", "pangasinan",
                "ilocano", "bicolano", "locale", "region", "translation", "translate"
            )
        ),
        SettingOption(
            title = themeTitle,
            subtitle = stringResource(R.string.settings_theme_subtitle),
            icon = getSettingIcon("Theme"),
            keywords = listOf(
                "theme", "themes", "dark mode", "light mode", "dark theme", "light theme",
                "night mode", "day mode", "appearance", "color scheme", "colors",
                "mode", "display", "display settings", "brightness", "dark", "light",
                "switch mode", "toggle theme"
            )
        ),
        SettingOption(
            title = blockedTitle,
            subtitle = stringResource(R.string.settings_blocked_subtitle),
            icon = getSettingIcon("Blocked Users"),
            keywords = listOf(
                "blocked", "blocked users", "block user", "block", "unblock",
                "unblock user", "blocked list", "block list", "blacklist",
                "restrict", "restricted", "mute user", "hide user"
            )
        ),
        SettingOption(
            title = dataStorageTitle,
            subtitle = stringResource(R.string.settings_data_storage_subtitle),
            icon = getSettingIcon("Data and Storage Info"),
            keywords = listOf(
                "data", "storage", "photos", "images", "clear data", "delete", "warning",
                "privacy", "local storage", "backup", "lost photos", "missing images"
            )
        ),
        SettingOption(
            title = aboutTitle,
            subtitle = stringResource(R.string.settings_about_subtitle),
            icon = getSettingIcon("About"),
            keywords = listOf(
                "about", "about app", "app info", "app information", "version",
                "app version", "build", "build number", "credits", "developers",
                "team", "contact us", "support", "help", "faq", "terms", "terms of service",
                "privacy policy", "license", "licenses", "open source", "chickcare",
                "chick care", "bisu", "application", "feedback"
            )
        ),
        SettingOption(
            title = logoutTitle,
            subtitle = "",
            icon = getSettingIcon("Logout"),
            isLogout = true,
            keywords = listOf(
                "logout", "log out", "sign out", "signout", "exit", "leave"
            )
        )
    )

    val filteredOptions = remember(searchQuery) {
        if (searchQuery.isEmpty()) {
            allSettingsOptions
        } else {
            allSettingsOptions.filter { option ->
                option.title.contains(searchQuery, ignoreCase = true) ||
                        option.subtitle.contains(searchQuery, ignoreCase = true) ||
                        option.keywords.any { keyword ->
                            keyword.contains(searchQuery, ignoreCase = true)
                        }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ThemeColorUtils.black()
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.navigate("profile") {
                                popUpTo("profile") { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    ) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    searchQuery = ""
                }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { newValue -> searchQuery = newValue },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.settings_search_hint),
                                tint = ThemeColorUtils.darkGray(Color(0xFF424141))
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.cancel),
                                        tint = ThemeColorUtils.darkGray(Color(0xFF424141))
                                    )
                                }
                            }
                        },
                        placeholder = { Text(stringResource(R.string.settings_search_hint), color = ThemeColorUtils.darkGray(Color(0xFF424141))) },
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ThemeColorUtils.themedColor(
                                lightColor = Color(0xCDFFFFFF),
                                role = NeutralRole.White
                            ),
                            unfocusedContainerColor = ThemeColorUtils.white(),
                            focusedTextColor = ThemeColorUtils.darkGray(Color(0xFF212121)),
                            unfocusedTextColor = ThemeColorUtils.darkGray(Color(0xFF212121)),
                            focusedBorderColor = ThemeColorUtils.black(),
                            unfocusedBorderColor = ThemeColorUtils.black(),
                            focusedLabelColor = ThemeColorUtils.darkGray(Color(0xFF424242)),
                            unfocusedLabelColor = ThemeColorUtils.lightGray(Color(0xFFB9B5B5))
                        ),
                        singleLine = true
                    )
                }

                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)),
                        thickness = 1.dp
                    )
                }

                // Settings Options
                if (filteredOptions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.settings_no_results),
                                color = ThemeColorUtils.lightGray(Color.Gray),
                                fontSize = 16.sp
                            )
                        }
                    }
                } else {
                    items(filteredOptions) { option ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { /* Prevent parent click handler */ }
                        ) {
                            SettingItem(
                                option = option,
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    searchQuery = ""
                                    // Use the title string resource logic by comparing with the resolved string
                                    when (option.title) {
                                        accountTitle -> navController.navigate("account_settings")
                                        securityTitle -> navController.navigate("security_privacy_settings")
                                        emergencyTitle -> navController.navigate("emergency_contacts")
                                        notificationsTitle -> navController.navigate("notification_settings")
                                        languageTitle -> navController.navigate("language_settings")
                                        themeTitle -> {
                                            ThemeViewModel.toggleTheme()
                                        }
                                        blockedTitle -> navController.navigate("blocked_users")
                                        dataStorageTitle -> showDataStorageDialog = true
                                        aboutTitle -> navController.navigate("about_settings")
                                        logoutTitle -> {
                                            showLogoutDialog = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        Dialog(onDismissRequest = { showLogoutDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.white()),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_logout_dialog_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = ThemeColorUtils.black()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_logout_dialog_message),
                        fontSize = 16.sp,
                        color = ThemeColorUtils.black()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(x = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showLogoutDialog = false },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = ThemeColorUtils.black()
                            )
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(
                            onClick = {
                                authViewModel.logout(context)
                                navController.navigate("login") {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                                showLogoutDialog = false
                            },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = ThemeColorUtils.black()
                            )
                        ) {
                            Text(stringResource(R.string.settings_logout_confirm), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Data Storage Warning Dialog
    if (showDataStorageDialog) {
        AlertDialog(
            onDismissRequest = { showDataStorageDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.data_storage_dialog_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            },
            text = {
                Text(
                    text = stringResource(R.string.data_storage_dialog_message),
                    fontSize = 16.sp,
                    color = ThemeColorUtils.black()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showDataStorageDialog = false },
                    modifier = Modifier.offset(x = 12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = ThemeColorUtils.black()
                    )
                ) {
                    Text(stringResource(R.string.common_ok), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = ThemeColorUtils.white(),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun SettingItem(option: SettingOption, onClick: () -> Unit) {
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
        if (option.isLogout) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ThemeColorUtils.darkGray(Color(0xFF151414))
                )
            }
        } else {
            // Regular layout for other options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = option.icon),
                        contentDescription = option.title,
                        modifier = Modifier.size(38.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ThemeColorUtils.black()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = option.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeColorUtils.darkGray(Color(0xFF666666))
                        )
                    }
                }
                if (option.title == stringResource(R.string.settings_theme_title)) {
                    Switch(
                        checked = ThemeViewModel.isDarkMode,
                        onCheckedChange = { onClick() },
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
}
