package com.bisu.chickcare.backend.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalUserDao {
    @Query("SELECT * FROM local_users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): LocalUser?
    
    @Query("SELECT * FROM local_users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): LocalUser?
    
    @Query("SELECT * FROM local_users WHERE email = :email AND passwordHash = :passwordHash LIMIT 1")
    suspend fun authenticateUser(email: String, passwordHash: String): LocalUser?
    
    @Query("SELECT * FROM local_users WHERE isSynced = 0")
    suspend fun getUnsyncedUsers(): List<LocalUser>
    
    @Query("SELECT * FROM local_users")
    fun getAllUsers(): Flow<List<LocalUser>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: LocalUser)
    
    @Update
    suspend fun updateUser(user: LocalUser)
    
    @Query("DELETE FROM local_users WHERE userId = :userId")
    suspend fun deleteUser(userId: String)
}

