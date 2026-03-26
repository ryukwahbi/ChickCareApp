package com.bisu.chickcare.backend.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.data.UserProfile
import com.bisu.chickcare.backend.repository.AccountManager
import com.bisu.chickcare.backend.repository.NotificationRepository
import com.bisu.chickcare.backend.service.CloudinaryUploadService
import com.bisu.chickcare.backend.service.NotificationService
import com.bisu.chickcare.backend.service.NetworkConnectivityHelper
import com.bisu.chickcare.backend.data.LocalUser
import com.bisu.chickcare.backend.utils.OfflineAuthHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val notificationRepository = NotificationRepository()
    private val notificationService = NotificationService(notificationRepository)
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    // Method to get the current user's ID
    fun getCurrentUserId(context: Context? = null): String? {
        // First check Firebase Auth
        val firebaseUid = auth.currentUser?.uid
        if (firebaseUid != null) return firebaseUid

        // Fallback to offline session if context is available
        return context?.let { OfflineAuthHelper.getCurrentLocalUserId(it) }
    }

    // Method to check if a user is authenticated
    fun isAuthenticated(context: Context? = null): Boolean {
        if (auth.currentUser != null) return true
        
        // Check offline session
        return context?.let { OfflineAuthHelper.hasOfflineSession(it) } ?: false
    }

    init {
        if (auth.currentUser != null) {
            fetchUserProfile()
        }
    }

    fun interface AuthCallback {
        fun onResult(success: Boolean, message: String)
    }

    fun login(identifier: String, password: String, context: Context? = null, callback: AuthCallback) {
        viewModelScope.launch {
            try {
                var emailToUse = identifier

                // 1. Check if input is a valid email
                if (!isValidEmail(identifier)) {
                    // Not an email - try to resolve as phone number from Firestore
                    try {
                         // Sanitize identifier for phone lookup (remove spaces, etc)
                        val cleanIdentifier = identifier.replace(Regex("[\\s\\-\\(\\)]"), "")
                        
                        // Try exact match first
                        var snapshot = firestore.collection("users")
                            .whereEqualTo("contact", cleanIdentifier)
                            .get()
                            .await()
                        
                        if (snapshot.isEmpty) {
                            // Try Philippines format variations
                            if (cleanIdentifier.startsWith("09") && cleanIdentifier.length == 11) {
                                // 09... -> +639...
                                val phFormat = "+63" + cleanIdentifier.substring(1)
                                snapshot = firestore.collection("users")
                                    .whereEqualTo("contact", phFormat)
                                    .get()
                                    .await()
                            } else if (cleanIdentifier.startsWith("+63")) {
                                // +63... -> 09... (fallback)
                                val localFormat = "0" + cleanIdentifier.substring(3)
                                snapshot = firestore.collection("users")
                                    .whereEqualTo("contact", localFormat)
                                    .get()
                                    .await()
                            }
                        }

                        if (!snapshot.isEmpty) {
                            emailToUse = snapshot.documents[0].getString("email") ?: ""
                            if (emailToUse.isEmpty()) {
                                callback.onResult(false, "Account found but no email associated.")
                                return@launch
                            }
                        } else {
                            callback.onResult(false, "Please enter a valid email or mobile number")
                            return@launch
                        }
                    } catch (e: Exception) {
                        // Network error handling for phone lookup
                         val isOnline = context?.let { NetworkConnectivityHelper.isOnline(it) } ?: false
                         if (!isOnline) {
                             // If offline and trying phone login, we might fail unless we have local data.
                             // But let's proceed to offline check below, maybe 'identifier' IS the email used last time?
                             // If user typed phone number, and we are offline, we can't look it up to get email.
                             // So offline login with phone number WON'T work unless we cached "phone -> email" mapping.
                             // We currently don't.
                             // We'll let it fall through to offline check with 'identifier', which will likely fail if it's not the email.
                         } else {
                             throw e
                         }
                    }
                }

                if (password.length < 6) {
                    callback.onResult(false, "Password must be at least 6 characters")
                    return@launch
                }

                // Check if online
                val isOnline = context?.let { NetworkConnectivityHelper.isOnline(it) } ?: false
                if (!isOnline) {
                    // Try offline login
                    if (context != null) {
                        val lastEmail = OfflineAuthHelper.getLastEmail(context)
                        val lastPasswordHash = OfflineAuthHelper.getLastPasswordHash(context)
                        val currentPasswordHash = OfflineAuthHelper.hashPassword(password)

                        // Note: If user is logging in with Phone, 'emailToUse' will be the phone number (if lookup failed) 
                        // or matched email (if lookup succeeded/cached?). 
                        // Firestore lookup fails if offline.
                        // So offline login expects EMAIL.
                        
                        if (lastEmail == emailToUse && lastPasswordHash == currentPasswordHash) {
                            val lastUserId = OfflineAuthHelper.getLastUserId(context)
                            if (lastUserId != null) {
                                OfflineAuthHelper.setCurrentLocalUserId(context, lastUserId)
                                fetchUserProfile(context)
                                callback.onResult(true, "Offline login successful")
                                return@launch
                            }
                        }
                    }
                    callback.onResult(false, "No internet connection and offline login failed.")
                    return@launch
                }

                // Try Firebase Auth
                auth.signInWithEmailAndPassword(emailToUse, password).await()
                val userId = auth.currentUser?.uid ?: ""
                
                context?.let {
                    val accountManager = AccountManager(it)
                    accountManager.updateAccountInfo(userId)
                    
                    // Save credentials for offline login
                    OfflineAuthHelper.setLastCredentials(it, emailToUse, OfflineAuthHelper.hashPassword(password), userId)
                    OfflineAuthHelper.setCurrentLocalUserId(it, userId)
                    
                    // Record Login Session
                    try {
                        com.bisu.chickcare.backend.repository.LoginSessionRepository().recordSession(it, userId)
                    } catch (e: Exception) {
                        Log.e("AuthViewModel", "Failed to record session", e)
                    }
                }
                saveFCMToken()
                // ... rest of logic
                if (userId.isNotEmpty()) {
                    try {
                        val userDoc = firestore.collection("users").document(userId).get().await()
                        val createdAt = userDoc.getLong("createdAt") ?: 0L
                        val now = System.currentTimeMillis()
                        val hoursSinceCreation = (now - createdAt) / (1000 * 60 * 60)
                        if (hoursSinceCreation < 24) {
                            val notificationsSnapshot = firestore.collection("users")
                                .document(userId)
                                .collection("notifications")
                                .limit(1)
                                .get()
                                .await()
                            if (notificationsSnapshot.isEmpty) {
                                val userName = userDoc.getString("fullName") ?: "there"
                                notificationService.sendWelcomeNotification(userId, userName)
                                Log.d("AuthViewModel", "Welcome notification sent on first login: $userId")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("AuthViewModel", "Failed to check/send welcome notification: ${e.message}")
                    }
                }

                fetchUserProfile()
                callback.onResult(true, "Login successful")

            } catch (_: FirebaseAuthInvalidCredentialsException) {
                callback.onResult(false, "Invalid email or password")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Unexpected error during login: ${e.message}", e)
                callback.onResult(false, "Login failed: ${e.message}")
            }
        }
    }

    fun logout(context: Context? = null) {
        val userId = auth.currentUser?.uid
        if (userId != null && context != null) {
            viewModelScope.launch {
                try {
                    com.bisu.chickcare.backend.repository.LoginSessionRepository().markSessionInactiveLocally(context, userId)
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error removing session locally", e)
                }
                performLocalLogout(context)
            }
        } else {
            performLocalLogout(context)
        }
    }

    private fun performLocalLogout(context: Context?) {
        auth.signOut()
        _userProfile.value = null
        context?.let { OfflineAuthHelper.clearCurrentLocalUserId(it) }
    }

    fun signup(
        email: String,
        password: String,
        fullName: String,
        birthDate: String,
        gender: String,
        contact: String,
        context: Context? = null,
        callback: AuthCallback
    ) {
        viewModelScope.launch {
            try {
                if (!isValidEmail(email)) {
                    callback.onResult(false, "Please enter a valid email address")
                    return@launch
                }
                if (!isValidSignupPassword(password)) {
                    callback.onResult(
                        false,
                        "Password must be at least 8 characters, include uppercase, lowercase, number, and special character"
                    )
                    return@launch
                }

                // Check if online
                val isOnline = context?.let { NetworkConnectivityHelper.isOnline(it) } ?: false
                if (!isOnline) {
                    callback.onResult(false, "No internet connection. Please check your network settings.")
                    return@launch
                }

                // Try Firebase Auth
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val userId = result.user?.uid ?: ""
                val userData = hashMapOf(
                    "fullName" to fullName,
                    "email" to email,
                    "birthDate" to birthDate,
                    "gender" to gender,
                    "contact" to contact,
                    "createdAt" to System.currentTimeMillis(),
                    "photoUrl" to null
                )
                firestore.collection("users").document(userId).set(userData).await()

                try {
                    notificationService.sendWelcomeNotification(userId, fullName)
                    Log.d("AuthViewModel", "Welcome notification sent to new user: $userId")
                } catch (e: Exception) {
                    Log.w("AuthViewModel", "Failed to send welcome notification: ${e.message}")
                }

                fetchUserProfile()
                callback.onResult(true, "Signup successful! Please check your email for verification.")

            } catch (_: FirebaseAuthUserCollisionException) {
                callback.onResult(false, "Email already in use")
            } catch (_: FirebaseAuthWeakPasswordException) {
                callback.onResult(
                    false,
                    "Weak password. Use at least 8 characters, uppercase, lowercase, number, and special character"
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Unexpected error during signup: ${e.message}", e)
                callback.onResult(false, "Signup failed: ${e.message}")
            }
        }
    }

    fun resetPassword(email: String, callback: (Boolean, String) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, "Password reset email sent successfully!")
                } else {
                    callback(
                        false,
                        "Failed to send password reset email: ${task.exception?.message}"
                    )
                }
            }
    }

    fun changePassword(
        oldPassword: String,
        newPassword: String,
        callback: (Boolean, String) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(false, "User not logged in")
            return
        }

        viewModelScope.launch {
            try {
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(
                    currentUser.email ?: "",
                    oldPassword
                )
                currentUser.reauthenticate(credential).await()
                currentUser.updatePassword(newPassword).await()
                callback(true, "Password changed successfully")
            } catch (_: FirebaseAuthInvalidCredentialsException) {
                callback(false, "Current password is incorrect")
            } catch (_: FirebaseAuthWeakPasswordException) {
                callback(false, "New password is too weak. Use at least 8 characters with uppercase, lowercase, number, and special character")
            } catch (e: Exception) {
                callback(false, "Failed to change password: ${e.message}")
            }
        }
    }

    private var profileListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    fun fetchUserProfile(context: Context? = null) {
        val userId = getCurrentUserId(context)
        
        // Remove existing listener if any
        profileListenerRegistration?.remove()
        
        if (userId != null) {
            profileListenerRegistration = firestore.collection("users").document(userId)
                .addSnapshotListener { snapshot, _ ->
                    _userProfile.value = snapshot?.toObject(UserProfile::class.java)
                }
        } else {
            _userProfile.value = null
        }
    }

    private var sessionListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    fun observeCurrentSession(context: Context) {
        val userId = auth.currentUser?.uid ?: return
        val deviceId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: return
        
        sessionListenerRegistration?.remove()
        sessionListenerRegistration = firestore.collection("users").document(userId)
            .collection("sessions").document(deviceId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val isActive = snapshot.getBoolean("isActive") ?: true
                    if (!isActive) {
                        Log.w("AuthViewModel", "Session flagged as inactive remotely! Forcing logout.")
                        logout(context)
                    }
                }
            }
    }

    fun stopObservingCurrentSession() {
        sessionListenerRegistration?.remove()
        sessionListenerRegistration = null
    }

    suspend fun fetchUserProfileById(userId: String): UserProfile? {
        return try {
            val snapshot = firestore.collection("users").document(userId).get().await()
            snapshot.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error fetching user profile: ${e.message}", e)
            null
        }
    }

    fun uploadProfileImage(uri: Uri, context: Context? = null, callback: (Boolean, String) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in.")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Starting profile image upload to Firebase Storage for user: $userId")

                // Use Firebase Storage instead of Cloudinary
                val filename = "profile.jpg"
                val profileRef = storage.reference.child("users/$userId/$filename")
                
                // Upload file
                profileRef.putFile(uri).await()
                
                // Get download URL
                val downloadUrl = profileRef.downloadUrl.await().toString()
                
                Log.d("AuthViewModel", "Profile image upload completed. URL: $downloadUrl")

                firestore.collection("users").document(userId).update("photoUrl", downloadUrl).await()
                Log.d("AuthViewModel", "Profile image URL updated in Firestore")

                callback(true, "Profile picture updated successfully.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Profile image upload failed", e)
                callback(false, "Upload failed: ${e.message}")
            }
        }
    }

    fun uploadCoverPhoto(uri: Uri, context: Context? = null, callback: (Boolean, String) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in.")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Starting cover photo upload to Firebase Storage for user: $userId")

                // Use Firebase Storage instead of Cloudinary
                val filename = "cover.jpg"
                val coverRef = storage.reference.child("users/$userId/$filename")
                
                // Upload file
                coverRef.putFile(uri).await()
                
                // Get download URL
                val downloadUrl = coverRef.downloadUrl.await().toString()
                
                Log.d("AuthViewModel", "Cover photo upload completed. URL: $downloadUrl")

                firestore.collection("users").document(userId).update("coverPhotoUrl", downloadUrl).await()
                Log.d("AuthViewModel", "Cover photo URL updated in Firestore")

                callback(true, "Cover photo updated successfully.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Cover photo upload failed", e)
                callback(false, "Upload failed: ${e.message}")
            }
        }
    }

    fun removeCoverPhoto(callback: (Boolean, String) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in.")
            return
        }

        val storageRef = storage.reference.child("cover_photos/$userId.jpg")

        viewModelScope.launch {
            try {
                try {
                    storageRef.delete().await()
                } catch (_: Exception) {
                }
                firestore.collection("users").document(userId).update("coverPhotoUrl", null).await()
                callback(true, "Cover photo removed.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to remove cover photo", e)
                callback(false, "Failed to remove cover photo: ${e.message}")
            }
        }
    }

    fun removeProfileImage(callback: (Boolean, String) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in.")
            return
        }

        val storageRef = storage.reference.child("profile_pictures/$userId.jpg")

        viewModelScope.launch {
            try {
                // Try to delete from storage; ignore failures if file doesn\'t exist
                try {
                    storageRef.delete().await()
                } catch (_: Exception) {
                    // proceed even if not present
                }
                // Set photoUrl to null in Firestore
                firestore.collection("users").document(userId).update("photoUrl", null).await()
                callback(true, "Profile picture removed.")
            } catch (e: Exception) {
                callback(false, "Failed to remove photo: ${e.message}")
            }
        }
    }

    fun updateProfileField(fieldName: String, value: String, context: Context? = null, callback: (Boolean, String) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in.")
            return
        }

        viewModelScope.launch {
            try {
                firestore.collection("users").document(userId).update(fieldName, value).await()
                // Send notification about profile update
                try {
                    notificationService.sendProfileUpdateNotification(userId, fieldName)
                } catch (e: Exception) {
                    Log.w("AuthViewModel", "Failed to send profile update notification: ${e.message}")
                }
                callback(true, "Profile updated successfully.")
            } catch (e: Exception) {
                callback(false, "Failed to update profile: ${e.message}")
            }
        }
    }

    fun updateUserProfile(updates: Map<String, Any>, context: Context? = null, callback: AuthCallback) {
        val userId = auth.currentUser?.uid ?: run {
            callback.onResult(false, "User not logged in.")
            return
        }

        viewModelScope.launch {
            try {
                firestore.collection("users").document(userId).update(updates).await()
                callback.onResult(true, "Profile updated successfully.")
            } catch (e: Exception) {
                callback.onResult(false, "Failed to update profile: ${e.message}")
            }
        }
    }

    fun updateFieldPrivacy(fieldName: String, privacy: String, callback: (Boolean, String) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in.")
            return
        }

        viewModelScope.launch {
            try {
                // Get current fieldPrivacy map
                val userDoc = firestore.collection("users").document(userId).get().await()
                val fieldPrivacyData = userDoc.get("fieldPrivacy")
                val currentPrivacy = if (fieldPrivacyData is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    (fieldPrivacyData as Map<String, String>)
                } else {
                    emptyMap()
                }

                val updatedPrivacy = currentPrivacy.toMutableMap()
                updatedPrivacy[fieldName] = privacy

                firestore.collection("users").document(userId).update("fieldPrivacy", updatedPrivacy).await()
                callback(true, "Privacy setting updated successfully.")
            } catch (e: Exception) {
                callback(false, "Failed to update privacy setting: ${e.message}")
            }
        }
    }

    fun toggleActiveStatus(enabled: Boolean, callback: (Boolean, String) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in.")
            return
        }

        viewModelScope.launch {
            try {
                firestore.collection("users").document(userId)
                    .update("showActiveStatus", enabled)
                    .await()
                
                // If disabling, also update lastActive to 0 immediately so friends see it instantly
                if (!enabled) {
                    firestore.collection("users").document(userId)
                        .update("lastActive", 0L)
                        .await()
                } else {
                    // If enabling, update to now
                    firestore.collection("users").document(userId)
                        .update("lastActive", System.currentTimeMillis())
                        .await()
                }
                
                callback(true, "Active status updated.")
            } catch (e: Exception) {
                callback(false, "Failed to update active status: ${e.message}")
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidSignupPassword(password: String): Boolean {
        return password.length >= 8 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isDigit() } &&
                password.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) }
    }

    private fun saveFCMToken() {
        val userId = auth.currentUser?.uid ?: return

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("AuthViewModel", "FCM token obtained: $token")

                firestore.collection("users")
                    .document(userId)
                    .update("fcmToken", token)
                    .addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            Log.d("AuthViewModel", "FCM token saved to Firestore")
                        } else {
                            Log.e("AuthViewModel", "Failed to save FCM token", updateTask.exception)
                        }
                    }
            } else {
                Log.e("AuthViewModel", "Failed to get FCM token", task.exception)
            }
        }
    }

    private var storedVerificationCode: String? = null

    fun sendAccountDeletionVerificationCode(callback: AuthCallback) {
        val user = auth.currentUser ?: run {
            callback.onResult(false, "User not logged in.")
            return
        }

        viewModelScope.launch {
            try {
                val code = (100000..999999).random().toString()
                storedVerificationCode = code

                val userId = user.uid
                firestore.collection("users").document(userId)
                    .update("deletionVerificationCode", code)
                    .await()

                user.sendEmailVerification().await()

                Log.d("AuthViewModel", "Verification code sent: $code")

                callback.onResult(true, "Verification code sent to your email.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to send verification code: ${e.message}", e)
                callback.onResult(false, "Failed to send verification code: ${e.message}")
            }
        }
    }

    fun deleteAccount(verificationCode: String, callback: AuthCallback) {
        val user = auth.currentUser ?: run {
            callback.onResult(false, "User not logged in.")
            return
        }

        val userId = user.uid

        viewModelScope.launch {
            try {
                // Verify the code
                val storedCode = storedVerificationCode
                val userDoc = firestore.collection("users").document(userId).get().await()
                val firestoreCode = userDoc.getString("deletionVerificationCode")

                if (verificationCode != storedCode && verificationCode != firestoreCode) {
                    callback.onResult(false, "Invalid verification code.")
                    return@launch
                }

                val batch = firestore.batch()

                val detectionsSnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("detections")
                    .get()
                    .await()
                detectionsSnapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                val postsSnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("timelinePosts")
                    .get()
                    .await()
                postsSnapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                val savedPostsSnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("savedPosts")
                    .get()
                    .await()
                savedPostsSnapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                val notificationsSnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .get()
                    .await()
                notificationsSnapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                val friendsSnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("friends")
                    .get()
                    .await()
                friendsSnapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                val friendRequestsSnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("friendRequests")
                    .get()
                    .await()
                friendRequestsSnapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                val blockedSnapshot = firestore.collection("users")
                    .document(userId)
                    .collection("blocked")
                    .get()
                    .await()
                blockedSnapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                batch.delete(firestore.collection("users").document(userId))

                batch.commit().await()

                try {
                    val profilePhotoRef = storage.reference.child("users/$userId/profile.jpg")
                    profilePhotoRef.delete().await()
                } catch (e: Exception) {
                    Log.w("AuthViewModel", "Failed to delete profile photo: ${e.message}")
                }

                try {
                    val coverPhotoRef = storage.reference.child("users/$userId/cover.jpg")
                    coverPhotoRef.delete().await()
                } catch (e: Exception) {
                    Log.w("AuthViewModel", "Failed to delete cover photo: ${e.message}")
                }

                user.delete().await()

                _userProfile.value = null
                storedVerificationCode = null

                auth.signOut()

                callback.onResult(true, "Account deleted successfully.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to delete account: ${e.message}", e)
                callback.onResult(false, "Failed to delete account: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        profileListenerRegistration?.remove()
        stopObservingCurrentSession()
    }
}
