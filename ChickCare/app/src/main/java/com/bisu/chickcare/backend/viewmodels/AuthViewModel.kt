package com.bisu.chickcare.backend.viewmodels

import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.data.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
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

    fun login(email: String, password: String, callback: AuthCallback) {
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
                fetchUserProfile() // Fetch profile after successful login
                callback.onResult(true, "Login successful")
            } catch (_: FirebaseAuthInvalidCredentialsException) {
                callback.onResult(false, "Invalid email or password")
            } catch (e: Exception) {
                callback.onResult(false, "Login failed: ${e.message}")
            }
        }
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

    fun uploadProfileImage(uri: Uri, callback: (Boolean, String) -> Unit) {
        val userId = auth.currentUser?.uid ?: run {
            callback(false, "User not logged in.")
            return
        }
        val storageRef = storage.reference.child("profile_pictures/$userId.jpg")

        viewModelScope.launch {
            try {
                // Upload the file to Firebase Storage
                storageRef.putFile(uri).await()
                // Get the public download URL for the uploaded file
                val downloadUrl = storageRef.downloadUrl.await().toString()
                // Update the 'photoUrl' field in the user's Firestore document
                firestore.collection("users").document(userId).update("photoUrl", downloadUrl).await()
                callback(true, "Profile picture updated successfully.")
            } catch (e: Exception) {
                callback(false, "Upload failed: ${e.message}")
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
}
