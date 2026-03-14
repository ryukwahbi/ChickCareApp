package com.bisu.chickcare.backend.service

import android.content.Context
import android.util.Log
import com.bisu.chickcare.backend.data.LocalUser
import com.bisu.chickcare.backend.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Service to sync local offline accounts with Firebase when online
 */
class AuthSyncService(context: Context) {
    private val offlineAuthService = OfflineAuthService(context)
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val notificationService = NotificationService(NotificationRepository())
    
    companion object {
        private const val TAG = "AuthSyncService"
    }
    
    /**
     * Sync a specific local user account with Firebase
     * This is called when user logs in with their password while online
     * @param localUser The local user account to sync
     * @param password The user's password (required for Firebase Auth)
     * @return true if sync successful, false otherwise
     */
    suspend fun syncLocalAccountToFirebase(
        localUser: LocalUser,
        password: String
    ): Boolean {
        return try {
            // Create Firebase account
            val authResult = auth.createUserWithEmailAndPassword(localUser.email, password).await()
            val firebaseUserId = authResult.user?.uid ?: ""
            
            if (firebaseUserId.isEmpty()) {
                Log.e(TAG, "Firebase user ID is empty after account creation")
                offlineAuthService.updateSyncError(localUser, "Failed to get Firebase user ID")
                return false
            }
            
            // Create Firestore user document
            val userData = hashMapOf(
                "fullName" to localUser.fullName,
                "email" to localUser.email,
                "birthDate" to localUser.birthDate,
                "gender" to localUser.gender,
                "contact" to localUser.contact,
                "createdAt" to localUser.createdAt,
                "photoUrl" to null
            )
            firestore.collection("users").document(firebaseUserId).set(userData).await()
            
            // Send welcome notification
            try {
                notificationService.sendWelcomeNotification(firebaseUserId, localUser.fullName)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send welcome notification: ${e.message}")
            }
            
            // Mark as synced
            offlineAuthService.markAsSynced(localUser, firebaseUserId)
            
            Log.d(TAG, "Successfully synced local account ${localUser.email} to Firebase")
            true
        } catch (_: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            // Account already exists - user should login instead
            Log.d(TAG, "Account ${localUser.email} already exists in Firebase")
            offlineAuthService.updateSyncError(localUser, "Account already exists. Please login.")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing account ${localUser.email}: ${e.message}", e)
            offlineAuthService.updateSyncError(localUser, e.message ?: "Unknown error")
            false
        }
    }
    
}

