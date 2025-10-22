package com.bisu.chickcare.backend.data

import androidx.compose.ui.graphics.vector.ImageVector

data class SettingOption(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isLogout: Boolean = false
)
