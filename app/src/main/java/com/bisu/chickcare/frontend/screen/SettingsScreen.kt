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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val authViewModel: AuthViewModel = viewModel()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var searchQuery by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // Helper function to get icon resource with fallback for missing resources
    fun getSettingIcon(title: String): Int {
        return when (title) {
            "Account" -> R.drawable.ic_account_flaticon
            "Security and Privacy" -> R.drawable.ic_security_flaticon
            "Notifications" -> R.drawable.ic_notifications_flaticon
            "Language" -> R.drawable.ic_language_flaticon
            "Theme" -> R.drawable.ic_theme_flaticon
            "About" -> R.drawable.ic_about_flaticon
            "Logout" -> R.drawable.ic_logout_flaticon
            else -> R.drawable.ic_account_flaticon
        }
    }
    
    val allSettingsOptions = listOf(
        SettingOption("Account", "Manage your account details", getSettingIcon("Account")),
        SettingOption("Security and Privacy", "Control your data and privacy options", getSettingIcon("Security and Privacy")),
        SettingOption("Notifications", "Customize alert preferences", getSettingIcon("Notifications")),
        SettingOption("Language", "Change app language", getSettingIcon("Language")),
        SettingOption("Theme", "Switch between light and dark modes", getSettingIcon("Theme")),
        SettingOption("About", "View app version and information", getSettingIcon("About")),
        SettingOption("Logout", "", getSettingIcon("Logout"), isLogout = true)
    )
    
    val filteredOptions = remember(searchQuery) {
        if (searchQuery.isEmpty()) {
            allSettingsOptions
        } else {
            allSettingsOptions.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.subtitle.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ThemeColorUtils.black()
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
                            contentDescription = "Search",
                            tint = ThemeColorUtils.darkGray(Color(0xFF424141))
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = ThemeColorUtils.darkGray(Color(0xFF424141))
                                )
                            }
                        }
                    },
                    placeholder = { Text("Search settings", color = ThemeColorUtils.darkGray(Color(0xFF424141))) },
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
                            text = "No settings found",
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
                                when (option.title) {
                                    "Account" -> navController.navigate("account_settings")
                                    "Security and Privacy" -> navController.navigate("security_privacy_settings")
                                    "Notifications" -> navController.navigate("notification_settings")
                                    "Language" -> navController.navigate("language_settings")
                                    "Theme" -> {
                                        ThemeViewModel.toggleTheme()
                                    }
                                    "About" -> navController.navigate("about_settings")
                                    "Logout" -> {
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
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Sign Out",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out?",
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        authViewModel.auth.signOut()
                        navController.navigate("login") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                        showLogoutDialog = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = ThemeColorUtils.black()
                    )
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = ThemeColorUtils.black()
                    )
                ) {
                    Text("Cancel")
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
                        modifier = Modifier.size(38.dp),
                        colorFilter = if (ThemeViewModel.isDarkMode) {
                            // In dark mode, apply a light grayish-blue tint for better visibility
                            // Using #A1AAB2 (light blue-gray) for softer, more elegant appearance
                            ColorFilter.tint(
                                color = ThemeColorUtils.lightGray(Color(0xFFA1AAB2)),
                                blendMode = BlendMode.SrcAtop
                            )
                        } else {
                            // In light mode, use original colors
                            null
                        }
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
                if (option.title == "Theme" || option.title == "Dark Mode") {
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
