package com.bisu.chickcare.backend.data

data class UserProfile(
    val fullName: String = "User",
    val email: String = "",
    val contact: String = "",
    val birthDate: String = "",
    val photoUrl: String? = null,
    val coverPhotoUrl: String? = null,
    val lastActive: Long = 0L,
    val gender: String? = null,
    val address: String = "",
    val createdAt: Long = 0L,
    val fcmToken: String? = null,
    val farmName: String = "",
    val farmLocation: String = "",
    val farmType: String = "",
    val specialization: String = "",
    val numberOfBirds: String = "",
    val yearsExperience: String = "",
    // Social/follower fields
    val followers: List<String> = emptyList(),
    val following: List<String> = emptyList(),
    val fieldPrivacy: Map<String, String> = mapOf(
        "email" to "public",
        "contact" to "public",
        "birthDate" to "public",
        "gender" to "public",
        "address" to "public",
        "farmName" to "public",
        "farmLocation" to "public",
        "farmType" to "public",
        "specialization" to "public",
        "numberOfBirds" to "public",
        "yearsExperience" to "public"
    ),
    val showActiveStatus: Boolean = true
)
