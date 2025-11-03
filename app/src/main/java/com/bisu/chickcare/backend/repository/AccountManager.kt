package com.bisu.chickcare.backend.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

data class SavedAccount(
    val userId: String,
    val email: String,
    val fullName: String,
    val photoUrl: String? = null,
    val lastLoginTime: Long = System.currentTimeMillis(),
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("userId", userId)
        json.put("email", email)
        json.put("fullName", fullName)
        json.put("photoUrl", photoUrl ?: JSONObject.NULL)
        json.put("lastLoginTime", lastLoginTime)
        return json
    }
    
    companion object {
        fun fromJson(json: JSONObject): SavedAccount {
            return SavedAccount(
                userId = json.getString("userId"),
                email = json.getString("email"),
                fullName = json.getString("fullName"),
                photoUrl = if (json.isNull("photoUrl")) null else json.getString("photoUrl"),
                lastLoginTime = json.getLong("lastLoginTime")
            )
        }
    }
}

class AccountManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("account_manager", Context.MODE_PRIVATE)
    private val firestore = FirebaseFirestore.getInstance()
    private val maxAccounts = 10
    
    fun getSavedAccounts(): List<SavedAccount> {
        val accountsJson = prefs.getString("saved_accounts", "[]") ?: "[]"
        return try {
            val jsonArray = JSONArray(accountsJson)
            (0 until jsonArray.length()).map { index ->
                SavedAccount.fromJson(jsonArray.getJSONObject(index))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
    
    fun saveAccount(account: SavedAccount) {
        val accounts = getSavedAccounts().toMutableList()
        
        // Remove if already exists
        accounts.removeAll { it.userId == account.userId }
        
        // Add to the beginning (most recent first)
        accounts.add(0, account)
        
        if (accounts.size > maxAccounts) {
            accounts.removeAt(accounts.size - 1)
        }
        
        val jsonArray = JSONArray()
        accounts.forEach { jsonArray.put(it.toJson()) }
        
        prefs.edit { putString("saved_accounts", jsonArray.toString()) }
    }
    
    fun removeAccount(userId: String) {
        val accounts = getSavedAccounts().toMutableList()
        accounts.removeAll { it.userId == userId }
        
        val jsonArray = JSONArray()
        accounts.forEach { jsonArray.put(it.toJson()) }
        
        prefs.edit { putString("saved_accounts", jsonArray.toString()) }
    }
    
    fun clearAllAccounts() {
        prefs.edit { remove("saved_accounts") }
    }
    
    suspend fun updateAccountInfo(userId: String) {
        try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data
            
            val account = SavedAccount(
                userId = userId,
                email = userData?.get("email") as? String ?: "",
                fullName = userData?.get("fullName") as? String ?: "User",
                photoUrl = userData?.get("photoUrl") as? String,
                lastLoginTime = System.currentTimeMillis()
            )
            saveAccount(account)
        } catch (_: Exception) {
            // Silent fail
        }
    }
}

