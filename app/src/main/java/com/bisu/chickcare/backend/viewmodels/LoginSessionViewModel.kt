package com.bisu.chickcare.backend.viewmodels

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.data.LoginSessionData
import com.bisu.chickcare.backend.repository.LoginSessionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginSessionViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val repository = LoginSessionRepository()

    private val _sessions = MutableStateFlow<List<LoginSessionData>>(emptyList())
    val sessions = _sessions.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    init {
        startListeningToSessions()
    }

    private fun startListeningToSessions() {
        val userId = auth.currentUser?.uid ?: return

        listenerRegistration = firestore.collection("users")
            .document(userId)
            .collection("sessions")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("LoginSessionViewModel", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val sessionList = snapshot.documents.mapNotNull { it.toObject(LoginSessionData::class.java) }
                    // Sort descending by when they were last active/created
                    _sessions.value = sessionList.sortedByDescending { it.lastActive }
                }
            }
    }

    fun revokeSession(sessionId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repository.revokeSession(userId, sessionId)
        }
    }

    fun revokeAllOtherSessions(context: Context) {
        val userId = auth.currentUser?.uid ?: return
        val currentDeviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: return
        viewModelScope.launch {
            repository.revokeAllOtherSessions(userId, currentDeviceId)
        }
    }

    fun getCurrentDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device_id"
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
