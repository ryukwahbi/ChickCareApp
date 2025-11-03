package com.bisu.chickcare.backend.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

data class FriendSuggestion(
    val userId: String,
    val fullName: String,
    val email: String,
    val photoUrl: String? = null,
    val lastActive: Long = 0L,
    val mutualFriendsCount: Int = 0
)

data class FriendRequest(
    val id: String = "",
    val fromUserId: String,
    val fromUserName: String,
    val toUserId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "pending"
)

class FriendRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    
    /**
     * Get suggested friends - users that are not yet friends and not the current user
     * Only fetches from server when online. Returns empty list when offline.
     */
    suspend fun getFriendSuggestions(currentUserId: String, limit: Int = 50): List<FriendSuggestion> {
        return try {
            // Use Source.SERVER: only fetches from server, fails when offline
            // This ensures suggestions only show when online, empty when offline
            val source = Source.SERVER
            
            Log.d("FriendRepository", "Fetching friend suggestions for user: $currentUserId")
            
            // Get all users (we'll filter out current user and friends)
            // Increase limit to ensure we get enough after filtering
            val allUsers = usersCollection
                .limit((limit * 2).toLong()) // Get more to account for filtering
                .get(source)
                .await()
            
            Log.d("FriendRepository", "Total users fetched from server: ${allUsers.size()}")
            
            // Get current user's friends
            val friendsSnapshot = usersCollection.document(currentUserId)
                .collection("friends")
                .get(source)
                .await()
            
            val friendIds = friendsSnapshot.documents.map { it.id }.toSet()
            Log.d("FriendRepository", "Current user has ${friendIds.size} friends")
            
            // Get pending requests (both sent and received)
            val pendingRequestsSnapshot = usersCollection.document(currentUserId)
                .collection("friendRequests")
                .whereEqualTo("status", "pending")
                .get(source)
                .await()
            
            val pendingRequestIds = pendingRequestsSnapshot.documents
                .mapNotNull { it.getString("fromUserId") }
                .toSet()
            Log.d("FriendRepository", "Current user has ${pendingRequestIds.size} pending requests")
            
            // Filter out current user, existing friends, and pending requests
            val filteredDocuments = allUsers.documents
                .filter { it.id != currentUserId }
                .filter { it.id !in friendIds }
                .filter { it.id !in pendingRequestIds }
            
            Log.d("FriendRepository", "After filtering: ${filteredDocuments.size} potential suggestions")
            
            // Map to FriendSuggestion objects
            val suggestions = filteredDocuments
                .take(limit) // Take only the limit after filtering
                .map { doc ->
                    val data = doc.data
                    FriendSuggestion(
                        userId = doc.id,
                        fullName = data?.get("fullName") as? String ?: "Unknown",
                        email = data?.get("email") as? String ?: "",
                        photoUrl = data?.get("photoUrl") as? String,
                        lastActive = (data?.get("lastActive") as? Long) ?: 0L,
                        mutualFriendsCount = try {
                            calculateMutualFriendsCount(currentUserId, doc.id)
                        } catch (e: Exception) {
                            Log.w("FriendRepository", "Error calculating mutual friends: ${e.message}")
                            0
                        }
                    )
                }
                .sortedByDescending { it.mutualFriendsCount }
            
            Log.d("FriendRepository", "Final suggestions count: ${suggestions.size} from server")
            if (suggestions.isNotEmpty()) {
                Log.d("FriendRepository", "Suggestions: ${suggestions.map { it.fullName }}")
            }
            
            suggestions
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error loading friend suggestions (likely offline): ${e.message}", e)
            // Return empty list when offline or on error - don't show cached suggestions
            emptyList()
        }
    }
    
    private suspend fun calculateMutualFriendsCount(user1Id: String, user2Id: String): Int {
        return try {
            val source = Source.SERVER // Use server source to match main query
            val user1Friends = usersCollection.document(user1Id)
                .collection("friends")
                .get(source)
                .await()
                .documents.map { it.id }
                .toSet()
            
            val user2Friends = usersCollection.document(user2Id)
                .collection("friends")
                .get(source)
                .await()
                .documents.map { it.id }
                .toSet()
            
            user1Friends.intersect(user2Friends).size
        } catch (e: Exception) {
            Log.w("FriendRepository", "Error calculating mutual friends for $user1Id and $user2Id: ${e.message}")
            0
        }
    }
    
    suspend fun sendFriendRequest(fromUserId: String, fromUserName: String, toUserId: String) {
        val requestData = hashMapOf(
            "fromUserId" to fromUserId,
            "fromUserName" to fromUserName,
            "toUserId" to toUserId,
            "timestamp" to System.currentTimeMillis(),
            "status" to "pending"
        )
        
        // Save request in receiver's friendRequests collection
        usersCollection.document(toUserId)
            .collection("friendRequests")
            .add(requestData)
            .await()
    }
    
    suspend fun acceptFriendRequest(
        requestId: String, 
        currentUserId: String, 
        friendUserId: String, 
        friendName: String,
        notificationRepository: NotificationRepository? = null
    ) {
        val currentUser = usersCollection.document(currentUserId)
        val friendUser = usersCollection.document(friendUserId)
        
        // Get the request to find the original sender (fromUserId)
        val requestDoc = currentUser.collection("friendRequests")
            .document(requestId)
            .get()
            .await()
        
        val requestData = requestDoc.data
        val fromUserId = requestData?.get("fromUserId") as? String // The person who sent the request
        
        // Update request status
        currentUser.collection("friendRequests")
            .document(requestId)
            .update("status", "accepted")
            .await()
        
        // Get current user's name for notification
        val currentUserDoc = currentUser.get().await()
        val currentUserName = currentUserDoc.data?.get("fullName") as? String ?: "User"
        
        // Add to friends collection for both users
        currentUser.collection("friends")
            .document(friendUserId)
            .set(hashMapOf(
                "friendId" to friendUserId,
                "friendName" to friendName,
                "addedAt" to System.currentTimeMillis()
            ))
            .await()
        
        friendUser.collection("friends")
            .document(currentUserId)
            .set(hashMapOf(
                "friendId" to currentUserId,
                "friendName" to currentUserName,
                "addedAt" to System.currentTimeMillis()
            ))
            .await()
        
        // Send notification to the person who originally sent the request (fromUserId)
        if (notificationRepository != null && fromUserId != null) {
            notificationRepository.addNotification(
                userId = fromUserId,
                type = NotificationType.FRIEND_ACCEPT,
                title = "Friend Request Accepted",
                message = "$currentUserName accepted your friend request",
                senderId = currentUserId,
                senderName = currentUserName,
                relatedEntityId = requestId
            )
        }
    }
    
    suspend fun declineFriendRequest(requestId: String, currentUserId: String) {
        usersCollection.document(currentUserId)
            .collection("friendRequests")
            .document(requestId)
            .update("status", "declined")
            .await()
    }
    
    suspend fun getPendingFriendRequests(userId: String): List<FriendRequest> {
        return try {
            val requestsSnapshot = usersCollection.document(userId)
                .collection("friendRequests")
                .whereEqualTo("status", "pending")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            
            requestsSnapshot.documents.map { doc ->
                val data = doc.data
                FriendRequest(
                    id = doc.id,
                    fromUserId = data?.get("fromUserId") as? String ?: "",
                    fromUserName = data?.get("fromUserName") as? String ?: "",
                    toUserId = data?.get("toUserId") as? String ?: userId,
                    timestamp = (data?.get("timestamp") as? Long) ?: System.currentTimeMillis(),
                    status = data?.get("status") as? String ?: "pending"
                )
            }
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error loading pending friend requests: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Check if current user has sent a friend request to the target user
     * Returns: "pending", "accepted", "declined", or null if no request exists
     */
    suspend fun getRequestStatus(currentUserId: String, targetUserId: String): String? {
        return try {
            val source = Source.SERVER
            // Check in target user's friendRequests collection for requests from current user
            val requestsSnapshot = usersCollection.document(targetUserId)
                .collection("friendRequests")
                .whereEqualTo("fromUserId", currentUserId)
                .limit(1)
                .get(source)
                .await()
            
            if (requestsSnapshot.isEmpty) {
                null // No request found
            } else {
                requestsSnapshot.documents.firstOrNull()?.getString("status")
            }
        } catch (e: Exception) {
            Log.w("FriendRepository", "Error checking request status: ${e.message}")
            null
        }
    }
    
    suspend fun getFriends(userId: String): List<FriendSuggestion> {
        return try {
            val friendsSnapshot = usersCollection.document(userId)
                .collection("friends")
                .get()
                .await()
            
            friendsSnapshot.documents.mapNotNull { friendDoc ->
                val friendId = friendDoc.getString("friendId") ?: return@mapNotNull null
                val friendData = usersCollection.document(friendId).get().await().data
                
                FriendSuggestion(
                    userId = friendId,
                    fullName = friendData?.get("fullName") as? String ?: "Unknown",
                    email = friendData?.get("email") as? String ?: "",
                    photoUrl = friendData?.get("photoUrl") as? String,
                    lastActive = (friendData?.get("lastActive") as? Long) ?: 0L
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

