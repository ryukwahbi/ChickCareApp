package com.bisu.chickcare.backend.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local user entity for offline authentication
 * Stores user credentials and profile data locally
 */
@Entity(tableName = "local_users")
data class LocalUser(
    @PrimaryKey
    val userId: String,
    val email: String,
    val passwordHash: String, // SHA-256 hashed password
    val fullName: String,
    val birthDate: String = "",
    val gender: String? = null,
    val contact: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false, // Whether account has been synced with Firebase
    val firebaseUserId: String? = null, // Firebase UID after sync
    val lastSyncAttempt: Long = 0L,
    val syncError: String? = null
)

