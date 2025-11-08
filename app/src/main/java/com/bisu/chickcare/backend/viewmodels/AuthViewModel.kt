package com.bisu.chickcare.backend.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.data.UserProfile
import com.bisu.chickcare.backend.repository.AccountManager
import com.bisu.chickcare.backend.service.CloudinaryUploadService
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

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    init {
        // Automatically fetch the user profile if a user is already logged in
        if (auth.currentUser != null) {
            fetchUserProfile()
        }
    }

    fun interface AuthCallback {
        fun onResult(success: Boolean, message: String)
    }

    fun login(email: String, password: String, context: Context? = null, callback: AuthCallback) {
        viewModelScope.launch {
            try {
                if (!isValidEmail(email)) {
                    callback.onResult(false, "Please enter a valid email address")
                    return@launch
                }
                if (password.length < 6) {
                    callback.onResult(false, "Password must be at least 6 characters")
                    return@launch
                }

                auth.signInWithEmailAndPassword(email, password).await()

                // Save account to AccountManager if context is provided
                context?.let {
                    val accountManager = AccountManager(it)
                    val userId = auth.currentUser?.uid ?: ""
                    accountManager.updateAccountInfo(userId)
                }

                // Get and save FCM token after successful login
                saveFCMToken()

                fetchUserProfile() // Fetch profile after successful login
                callback.onResult(true, "Login successful")
            } catch (_: FirebaseAuthInvalidCredentialsException) {
                callback.onResult(false, "Invalid email or password")
            } catch (e: Exception) {
                callback.onResult(false, "Login failed: ${e.message}")
            }
        }
    }
    
    fun logout() {
        auth.signOut()
        _userProfile.value = null
    }

    fun signup(
        email: String,
        password: String,
        fullName: String,
        birthDate: String,
        gender: String,
        contact: String,
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

                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val userId = result.user?.uid ?: ""

                val userData = hashMapOf(
                    "fullName" to fullName,
                    "email" to email,
                    "birthDate" to birthDate,
                    "gender" to gender,
                    "contact" to contact,
                    "createdAt" to System.currentTimeMillis(),
                    "photoUrl" to null // Initialize photoUrl as null
                )
                firestore.collection("users").document(userId).set(userData).await()
                fetchUserProfile() // Fetch profile after signup
                callback.onResult(true, "Signup successful! Please check your email for verification.")
            } catch (_: FirebaseAuthUserCollisionException) {
                callback.onResult(false, "Email already in use")
            } catch (_: FirebaseAuthWeakPasswordException) {
                callback.onResult(
                    false,
                    "Weak password. Use at least 8 characters, uppercase, lowercase, number, and special character"
                )
            } catch (e: Exception) {
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
                // Re-authenticate user with old password
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(
                    currentUser.email ?: "",
                    oldPassword
                )
                currentUser.reauthenticate(credential).await()
                
                // Update password
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

    private fun fetchUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid)
                .addSnapshotListener { snapshot, _ ->
                    // Automatically map the Firestore document to our UserProfile data class
                    _userProfile.value = snapshot?.toObject(UserProfile::class.java)
                }
        } else {
            // If there's no user, the profile state is null
            _userProfile.value = null
        }
    }
    
    /**
     * Fetch a user's profile by userId (for viewing other users' profiles)
     */
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
                Log.d("AuthViewModel", "Starting profile image upload for user: $userId")
                
                // Use Cloudinary (FREE) instead of Firebase Storage
                val uploadService = context?.let { CloudinaryUploadService(it) } ?: run {
                    callback(false, "Context not available for image upload")
                    return@launch
                }
                
                // Upload to Cloudinary
                val imageUrl = uploadService.uploadImage(uri)
                
                if (imageUrl == null) {
                    callback(false, "Failed to upload image to Cloudinary. Please check your credentials.")
                    return@launch
                }
                
                Log.d("AuthViewModel", "Profile image upload completed. URL: $imageUrl")
                
                // Update the 'photoUrl' field in the user's Firestore document
                firestore.collection("users").document(userId).update("photoUrl", imageUrl).await()
                Log.d("AuthViewModel", "Profile image URL updated in Firestore")
                
                callback(true, "Profile picture updated successfully.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Profile image upload failed", e)
                Log.e("AuthViewModel", "Error message: ${e.message}")
                Log.e("AuthViewModel", "Error class: ${e.javaClass.simpleName}")
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
                Log.d("AuthViewModel", "Starting cover photo upload for user: $userId")
                
                // Use Cloudinary (FREE) instead of Firebase Storage
                val uploadService = context?.let { CloudinaryUploadService(it) } ?: run {
                    callback(false, "Context not available for image upload")
                    return@launch
                }
                
                // Upload to Cloudinary
                val imageUrl = uploadService.uploadImage(uri)
                
                if (imageUrl == null) {
                    callback(false, "Failed to upload image to Cloudinary. Please check your credentials.")
                    return@launch
                }
                
                Log.d("AuthViewModel", "Cover photo upload completed. URL: $imageUrl")
                
                // Update the 'coverPhotoUrl' field in the user's Firestore document
                firestore.collection("users").document(userId).update("coverPhotoUrl", imageUrl).await()
                Log.d("AuthViewModel", "Cover photo URL updated in Firestore")
                
                callback(true, "Cover photo updated successfully.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Cover photo upload failed", e)
                Log.e("AuthViewModel", "Error message: ${e.message}")
                Log.e("AuthViewModel", "Error class: ${e.javaClass.simpleName}")
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
                // Try to delete from storage; ignore failures if file doesn't exist
                try {
                    storageRef.delete().await()
                } catch (_: Exception) {
                    // proceed even if not present
                }
                // Set coverPhotoUrl to null in Firestore
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
                // Try to delete from storage; ignore failures if file doesn't exist
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

    fun updateProfileField(fieldName: String, value: String, callback: (Boolean, String) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in.")
            return
        }

        viewModelScope.launch {
            try {
                firestore.collection("users").document(userId).update(fieldName, value).await()
                callback(true, "Profile updated successfully.")
            } catch (e: Exception) {
                callback(false, "Failed to update profile: ${e.message}")
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
                
                // Update the specific field's privacy
                val updatedPrivacy = currentPrivacy.toMutableMap()
                updatedPrivacy[fieldName] = privacy
                
                // Update Firestore with the new privacy map
                firestore.collection("users").document(userId).update("fieldPrivacy", updatedPrivacy).await()
                callback(true, "Privacy setting updated successfully.")
            } catch (e: Exception) {
                callback(false, "Failed to update privacy setting: ${e.message}")
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
                
                // Save token to Firestore
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
}
