package com.bisu.chickcare.backend.data

import androidx.annotation.DrawableRes

data class SettingOption(
    val title: String,
    val subtitle: String,
    @DrawableRes val icon: Int,
    val isLogout: Boolean = false
)
