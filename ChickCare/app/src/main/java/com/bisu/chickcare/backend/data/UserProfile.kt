package com.bisu.chickcare.backend.data

data class UserProfile(
    val fullName: String = "User",
    val email: String = "",
    val contact: String = "",
    val birthDate: String = "",
    val photoUrl: String? = null
)
