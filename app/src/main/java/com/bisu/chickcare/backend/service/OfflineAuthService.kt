package com.bisu.chickcare.backend.service

import android.content.Context
import android.util.Log
import com.bisu.chickcare.backend.data.AppDatabase
import com.bisu.chickcare.backend.data.LocalUser
import java.security.MessageDigest
import java.util.UUID

/**
 * Service for handling offline authentication
 * Allows users to create accounts and login without internet connection
 */
class OfflineAuthService(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val userDao = database.localUserDao()
    
    companion object {
        private const val TAG = "OfflineAuthService"
        
        /**
         * Hash password using SHA-256
         */
        fun hashPassword(password: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray())
            return hash.joinToString("") { "%02x".format(it) }
        }
    }
    
    /**
     * Create a new user account offline
     * Returns LocalUser if successful, null if email already exists
     */
    suspend fun createOfflineAccount(
        email: String,
        password: String,
        fullName: String,
        birthDate: String = "",
        gender: String? = null,
        contact: String = ""
    ): LocalUser? {
        return try {
            // Check if email already exists
            val existingUser = userDao.getUserByEmail(email)
            if (existingUser != null) {
                Log.w(TAG, "Email already exists: $email")
                return null
            }
            
            // Create new local user
            val userId = UUID.randomUUID().toString()
            val passwordHash = hashPassword(password)
            
            val localUser = LocalUser(
                userId = userId,
                email = email,
                passwordHash = passwordHash,
                fullName = fullName,
                birthDate = birthDate,
                gender = gender,
                contact = contact,
                createdAt = System.currentTimeMillis(),
                isSynced = false
            )
            
            userDao.insertUser(localUser)
            Log.d(TAG, "Offline account created: $email (userId: $userId)")
            localUser
        } catch (e: Exception) {
            Log.e(TAG, "Error creating offline account: ${e.message}", e)
            null
        }
    }
    
    /**
     * Authenticate user offline
     * Returns LocalUser if credentials are valid, null otherwise
     */
    suspend fun authenticateOffline(email: String, password: String): LocalUser? {
        return try {
            val passwordHash = hashPassword(password)
            val user = userDao.authenticateUser(email, passwordHash)
            
            if (user != null) {
                Log.d(TAG, "Offline authentication successful: $email")
            } else {
                Log.w(TAG, "Offline authentication failed: $email")
            }
            
            user
        } catch (e: Exception) {
            Log.e(TAG, "Error authenticating offline: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get user by email
     */
    suspend fun getUserByEmail(email: String): LocalUser? {
        return try {
            userDao.getUserByEmail(email)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by email: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: String): LocalUser? {
        return try {
            userDao.getUserById(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by ID: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get all unsynced users (for syncing when online)
     */
    suspend fun getUnsyncedUsers(): List<LocalUser> {
        return try {
            userDao.getUnsyncedUsers()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unsynced users: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Mark user as synced
     */
    suspend fun markAsSynced(user: LocalUser, firebaseUserId: String) {
        try {
            val updatedUser = user.copy(
                isSynced = true,
                firebaseUserId = firebaseUserId,
                lastSyncAttempt = System.currentTimeMillis(),
                syncError = null
            )
            userDao.updateUser(updatedUser)
            Log.d(TAG, "User marked as synced: ${user.email} -> $firebaseUserId")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking user as synced: ${e.message}", e)
        }
    }
    
    /**
     * Update sync error
     */
    suspend fun updateSyncError(user: LocalUser, error: String) {
        try {
            val updatedUser = user.copy(
                lastSyncAttempt = System.currentTimeMillis(),
                syncError = error
            )
            userDao.updateUser(updatedUser)
            Log.w(TAG, "Sync error updated for user: ${user.email} - $error")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating sync error: ${e.message}", e)
        }
    }
}

