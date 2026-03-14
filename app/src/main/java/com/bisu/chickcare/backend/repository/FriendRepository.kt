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
    val mutualFriendsCount: Int = 0,
    val isPinned: Boolean? = null,
    val notificationsMuted: Boolean? = null,
    val address: String? = null
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
            // Try to fetch from server first to get latest users
            var source = Source.SERVER
            
            Log.d("FriendRepository", "Fetching friend suggestions for user: $currentUserId from SERVER")
            
            // Get all users (we'll filter out current user and friends)
            // Increase limit to ensure we get enough after filtering
            val allUsers = try {
                 usersCollection
                    .limit((limit * 2).toLong()) // Get more to account for filtering
                    .get(Source.SERVER)
                    .await()
            } catch (e: Exception) {
                // Fallback to cache if server fails (offline)
                Log.w("FriendRepository", "Server fetch failed, falling back to CACHE: ${e.message}")
                source = Source.CACHE // Switch to CACHE for subsequent calls
                usersCollection
                    .limit((limit * 2).toLong())
                    .get(Source.CACHE)
                    .await()
            }
            
            Log.d("FriendRepository", "Total users fetched: ${allUsers.size()} (Source: $source)")
            
            // Get current user's friends
            val friendsSnapshot = usersCollection.document(currentUserId)
                .collection("friends")
                .get(source) // Uses updated source (SERVER or CACHE)
                .await()
            
            val friendIds = friendsSnapshot.documents.map { it.id }.toSet()
            Log.d("FriendRepository", "Current user has ${friendIds.size} friends")
            
            // Get all friend requests (pending, accepted, declined) to filter them out
            val allRequestsSnapshot = usersCollection.document(currentUserId)
                .collection("friendRequests")
                .get(source)
                .await()
            
            // Get IDs of users with pending or accepted requests
            val pendingRequestIds = allRequestsSnapshot.documents
                .filter { it.getString("status") == "pending" }
                .mapNotNull { it.getString("fromUserId") }
                .toSet()
            
            val acceptedRequestIds = allRequestsSnapshot.documents
                .filter { it.getString("status") == "accepted" }
                .mapNotNull { it.getString("fromUserId") }
                .toSet()
            
            Log.d("FriendRepository", "Current user has ${pendingRequestIds.size} pending requests and ${acceptedRequestIds.size} accepted requests")
            
            // Get blocked users
            val blockedUsersSnapshot = usersCollection.document(currentUserId)
                .collection("blockedUsers")
                .get(source)
                .await()
            
            val blockedUserIds = blockedUsersSnapshot.documents.map { it.id }.toSet()
            Log.d("FriendRepository", "Current user has ${blockedUserIds.size} blocked users")
            
            // Filter out current user, existing friends, pending/accepted requests, and blocked users
            val filteredDocuments = allUsers.documents
                .filter { it.id != currentUserId }
                .filter { it.id !in friendIds } // Already friends - exclude
                .filter { it.id !in pendingRequestIds } // Pending requests - exclude
                .filter { it.id !in acceptedRequestIds } // Accepted requests (should be in friends, but double-check) - exclude
                .filter { it.id !in blockedUserIds } // Blocked users - exclude
            
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
            val source = Source.DEFAULT // Allow cache usage
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
    
    suspend fun sendFriendRequest(
        fromUserId: String, 
        fromUserName: String, 
        toUserId: String,
        notificationRepository: NotificationRepository? = null
    ) {
        val requestData = hashMapOf(
            "fromUserId" to fromUserId,
            "fromUserName" to fromUserName,
            "toUserId" to toUserId,
            "timestamp" to System.currentTimeMillis(),
            "status" to "pending"
        )
        
        // Save request in receiver's friendRequests collection
        val requestDocRef = usersCollection.document(toUserId)
            .collection("friendRequests")
            .add(requestData)
            .await()
        
        // Send notification to the receiver
        notificationRepository?.addNotification(
            userId = toUserId,
            type = NotificationType.FRIEND_REQUEST,
            title = fromUserName, // Use full name as title
            message = "sent you a friend request",
            senderId = fromUserId,
            senderName = fromUserName,
            relatedEntityId = requestDocRef.id,
            actionRequired = true
        )
    }
    
    /**
     * Cancel a sent friend request
     */
    suspend fun cancelFriendRequest(
        fromUserId: String,
        toUserId: String
    ) {
        try {
            // Find the pending request from this user in the target user's collection
            val requestsSnapshot = usersCollection.document(toUserId)
                .collection("friendRequests")
                .whereEqualTo("fromUserId", fromUserId)
                .whereEqualTo("status", "pending")
                .get()
                .await()
            
            for (doc in requestsSnapshot.documents) {
                // Delete the request
                usersCollection.document(toUserId)
                    .collection("friendRequests")
                    .document(doc.id)
                    .delete()
                    .await()
            }
            
            Log.d("FriendRepository", "Cancelled friend request from $fromUserId to $toUserId")
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error cancelling friend request: ${e.message}", e)
            throw e
        }
    }
    
    suspend fun acceptFriendRequest(
        requestId: String, 
        currentUserId: String, 
        friendUserId: String, 
        friendName: String,
        notificationRepository: NotificationRepository? = null
    ) {
        val currentUser = usersCollection.document(currentUserId)
        
        // Get the request to find the original sender (fromUserId)
        val requestDoc = currentUser.collection("friendRequests")
            .document(requestId)
            .get()
            .await()
        
        val requestData = requestDoc.data
        val fromUserId = requestData?.get("fromUserId") as? String // The person who sent the request
        
        // Use fromUserId from the request (more reliable than parameter)
        val actualFriendUserId = fromUserId ?: friendUserId
        val friendUser = usersCollection.document(actualFriendUserId)
        
        // Get friend's name from request or use provided name
        val actualFriendName = (requestData?.get("fromUserName") as? String) ?: friendName
        
        // Update request status
        currentUser.collection("friendRequests")
            .document(requestId)
            .update("status", "accepted")
            .await()
        
        // Get current user's name for notification and friend record
        val currentUserDoc = currentUser.get().await()
        val currentUserName = currentUserDoc.data?.get("fullName") as? String ?: "User"
        
        // Add to friends collection for both users
        addFriendToCollection(currentUserId, actualFriendUserId, actualFriendName)
        addFriendToCollection(actualFriendUserId, currentUserId, currentUserName)
        
        // Send notification to the person who originally sent the request (fromUserId)
        notificationRepository?.addNotification(
            userId = actualFriendUserId,
            type = NotificationType.FRIEND_ACCEPT,
            title = "Friend Request Accepted",
            message = "$currentUserName accepted your friend request",
            senderId = currentUserId,
            senderName = currentUserName,
            relatedEntityId = requestId
        )
    }
    
    /**
     * Add a friend to a user's friend collection
     */
    private suspend fun addFriendToCollection(userId: String, friendId: String, friendName: String) {
         usersCollection.document(userId)
            .collection("friends")
            .document(friendId)
            .set(hashMapOf(
                "friendId" to friendId,
                "friendName" to friendName,
                "addedAt" to System.currentTimeMillis(),
                "isPinned" to false,
                "notificationsMuted" to false
            ), com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    /**
     * Repair friendship (ensure mutual connection).
     * Call this when we know users SHOULD be friends (e.g. one side says so),
     * but we suspect the other side is missing the record.
     */
    suspend fun repairFriendship(currentUserId: String, otherUserId: String) {
        try {
            // Check if I am in OTHER's friend list
            val amIFriend = isFriend(otherUserId, currentUserId)
            
            if (!amIFriend) {
                Log.d("FriendRepository", "Repairing friendship: Adding $currentUserId to $otherUserId's friends")
                
                // Get my name
                val myName = try {
                    usersCollection.document(currentUserId).get().await().getString("fullName") ?: "Unknown"
                } catch (e: Exception) {
                    "Unknown"
                }
                
                // Add me to their list
                addFriendToCollection(otherUserId, currentUserId, myName)
            }
            
            // Check if THEY are in MY friend list (just in case)
            val areTheyFriend = isFriend(currentUserId, otherUserId)
            if (!areTheyFriend) {
                 Log.d("FriendRepository", "Repairing friendship: Adding $otherUserId to $currentUserId's friends")
                 
                 // Get their name
                val theirName = try {
                    usersCollection.document(otherUserId).get().await().getString("fullName") ?: "Unknown"
                } catch (e: Exception) {
                    "Unknown"
                }
                
                 addFriendToCollection(currentUserId, otherUserId, theirName)
            }
            
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error repairing friendship: ${e.message}")
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
            // Try to fetch from server first
            var source = Source.SERVER
            
            // Check in target user's friendRequests collection for requests from current user
            val requestsSnapshot = try {
                usersCollection.document(targetUserId)
                    .collection("friendRequests")
                    .whereEqualTo("fromUserId", currentUserId)
                    .limit(1)
                    .get(source)
                    .await()
            } catch (e: Exception) {
                // Fallback to cache if server fails
                Log.w("FriendRepository", "Server check failed for request status, falling back to CACHE: ${e.message}")
                usersCollection.document(targetUserId)
                    .collection("friendRequests")
                    .whereEqualTo("fromUserId", currentUserId)
                    .limit(1)
                    .get(Source.CACHE)
                    .await()
            }
            
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
            Log.d("FriendRepository", "Loading friends for user: $userId")
            // Fetch from SERVER first for accurate friends count, fallback to CACHE if offline
            val friendsSnapshot = try {
                usersCollection.document(userId)
                    .collection("friends")
                    .get(Source.SERVER)
                    .await()
            } catch (e: Exception) {
                Log.w("FriendRepository", "Server fetch failed for friends list, using cache: ${e.message}")
                usersCollection.document(userId)
                    .collection("friends")
                    .get(Source.CACHE)
                    .await()
            }
            
            Log.d("FriendRepository", "Found ${friendsSnapshot.size()} friend documents")
            
            friendsSnapshot.documents.mapNotNull { friendDoc ->
                // Use document ID as primary source (consistent with getFriendSuggestions)
                // Document ID is the friend's user ID when stored via acceptFriendRequest
                val friendId = friendDoc.id.ifEmpty { 
                    friendDoc.getString("friendId") ?: return@mapNotNull null
                }
                
                if (friendId.isEmpty()) {
                    Log.w("FriendRepository", "Friend document ${friendDoc.id} has no valid friendId")
                    return@mapNotNull null
                }
                
                Log.d("FriendRepository", "Processing friend: $friendId (docId: ${friendDoc.id})")
                
                try {
                    // Fetch friend profile from SERVER first for fresh lastActive timestamp
                    // Fall back to CACHE if offline
                    val friendData = try {
                        usersCollection.document(friendId).get(Source.SERVER).await().data
                    } catch (e: Exception) {
                        Log.w("FriendRepository", "Server fetch failed for $friendId, using cache: ${e.message}")
                        usersCollection.document(friendId).get(Source.CACHE).await().data
                    }
                    
                    if (friendData == null) {
                        Log.w("FriendRepository", "Friend $friendId profile not found")
                        return@mapNotNull null
                    }
                    
                    // Get isPinned and notificationsMuted from the friend document in friends collection
                    val isPinned = friendDoc.getBoolean("isPinned") ?: false
                    val notificationsMuted = friendDoc.getBoolean("notificationsMuted") ?: false
                    
                    // Respect privacy setting
                    val showActiveStatus = (friendData["showActiveStatus"] as? Boolean) ?: true
                    val effectiveLastActive = if (showActiveStatus) {
                        (friendData["lastActive"] as? Long) ?: 0L
                    } else {
                        0L // Hide active status
                    }
                    
                    FriendSuggestion(
                        userId = friendId,
                        fullName = friendData["fullName"] as? String ?: "Unknown",
                        email = friendData["email"] as? String ?: "",
                        photoUrl = friendData["photoUrl"] as? String,
                        lastActive = effectiveLastActive,
                        isPinned = isPinned,
                        notificationsMuted = notificationsMuted,
                        address = friendData["address"] as? String
                    )
                } catch (e: Exception) {
                    Log.e("FriendRepository", "Error loading friend $friendId: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error loading friends: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Unfriend a user - removes friend relationship from both users
     */
    suspend fun unfriend(currentUserId: String, friendUserId: String) {
        try {
            // Remove from current user's friends
            usersCollection.document(currentUserId)
                .collection("friends")
                .document(friendUserId)
                .delete()
                .await()
            
            // Remove from friend's friends
            usersCollection.document(friendUserId)
                .collection("friends")
                .document(currentUserId)
                .delete()
                .await()
            
            Log.d("FriendRepository", "Unfriended: $currentUserId and $friendUserId")
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error unfriending: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Block a user - adds to blocked list and removes from friends
     */
    suspend fun blockUser(currentUserId: String, blockedUserId: String, blockedUserName: String) {
        try {
            // Add to blocked users collection
            usersCollection.document(currentUserId)
                .collection("blockedUsers")
                .document(blockedUserId)
                .set(hashMapOf(
                    "blockedUserId" to blockedUserId,
                    "blockedUserName" to blockedUserName,
                    "blockedAt" to System.currentTimeMillis()
                ))
                .await()
            
            // Remove from friends if they were friends
            try {
                usersCollection.document(currentUserId)
                    .collection("friends")
                    .document(blockedUserId)
                    .delete()
                    .await()
                
                usersCollection.document(blockedUserId)
                    .collection("friends")
                    .document(currentUserId)
                    .delete()
                    .await()
            } catch (_: Exception) {
                // Not friends, ignore
                Log.d("FriendRepository", "User was not a friend, skipping friend removal")
            }
            
            Log.d("FriendRepository", "Blocked user: $blockedUserId by $currentUserId")
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error blocking user: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Unblock a user
     */
    suspend fun unblockUser(currentUserId: String, unblockedUserId: String) {
        try {
            usersCollection.document(currentUserId)
                .collection("blockedUsers")
                .document(unblockedUserId)
                .delete()
                .await()
            
            Log.d("FriendRepository", "Unblocked user: $unblockedUserId by $currentUserId")
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error unblocking user: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Check if a user is blocked
     */
    suspend fun isBlocked(currentUserId: String, targetUserId: String): Boolean {
        return try {
            val blockedDoc = try {
                usersCollection.document(currentUserId)
                    .collection("blockedUsers")
                    .document(targetUserId)
                    .get(Source.SERVER)
                    .await()
            } catch (e: Exception) {
                 usersCollection.document(currentUserId)
                    .collection("blockedUsers")
                    .document(targetUserId)
                    .get(Source.CACHE)
                    .await()
            }
            blockedDoc.exists()
        } catch (e: Exception) {
            Log.w("FriendRepository", "Error checking if blocked: ${e.message}")
            false
        }
    }

    /**
     * Check if a user is a friend
     */
    suspend fun isFriend(currentUserId: String, targetUserId: String): Boolean {
        return try {
            val friendDoc = try {
                usersCollection.document(currentUserId)
                    .collection("friends")
                    .document(targetUserId)
                    .get(Source.SERVER)
                    .await()
            } catch (e: Exception) {
                usersCollection.document(currentUserId)
                    .collection("friends")
                    .document(targetUserId)
                    .get(Source.CACHE)
                    .await()
            }
            friendDoc.exists()
        } catch (e: Exception) {
            Log.w("FriendRepository", "Error checking if friend: ${e.message}")
            false
        }
    }
    
    /**
     * Pin a friend (mark as favorite/important)
     */
    suspend fun pinFriend(currentUserId: String, friendUserId: String) {
        try {
            usersCollection.document(currentUserId)
                .collection("friends")
                .document(friendUserId)
                .update("isPinned", true, "pinnedAt", System.currentTimeMillis())
                .await()
            
            Log.d("FriendRepository", "Pinned friend: $friendUserId")
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error pinning friend: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Unpin a friend
     */
    suspend fun unpinFriend(currentUserId: String, friendUserId: String) {
        try {
            usersCollection.document(currentUserId)
                .collection("friends")
                .document(friendUserId)
                .update("isPinned", false)
                .await()
            
            Log.d("FriendRepository", "Unpinned friend: $friendUserId")
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error unpinning friend: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Mute notifications from a friend
     */
    suspend fun muteFriendNotifications(currentUserId: String, friendUserId: String) {
        try {
            usersCollection.document(currentUserId)
                .collection("friends")
                .document(friendUserId)
                .update("notificationsMuted", true, "mutedAt", System.currentTimeMillis())
                .await()
            
            Log.d("FriendRepository", "Muted notifications from friend: $friendUserId")
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error muting friend notifications: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Unmute notifications from a friend
     */
    suspend fun unmuteFriendNotifications(currentUserId: String, friendUserId: String) {
        try {
            usersCollection.document(currentUserId)
                .collection("friends")
                .document(friendUserId)
                .update("notificationsMuted", false)
                .await()
            
            Log.d("FriendRepository", "Unmuted notifications from friend: $friendUserId")
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error unmuting friend notifications: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Report a user
     */
    suspend fun reportUser(
        reporterUserId: String,
        reportedUserId: String,
        reportedUserName: String,
        reason: String,
        description: String? = null
    ) {
        try {
            val reportData = hashMapOf(
                "reporterUserId" to reporterUserId,
                "reportedUserId" to reportedUserId,
                "reportedUserName" to reportedUserName,
                "reason" to reason,
                "description" to (description ?: ""),
                "timestamp" to System.currentTimeMillis(),
                "status" to "pending"
            )
            
            firestore.collection("reports")
                .add(reportData)
                .await()
            
            Log.d("FriendRepository", "Reported user: $reportedUserId by $reporterUserId")
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error reporting user: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Get mutual friends between two users
     */
    suspend fun getMutualFriends(user1Id: String, user2Id: String): List<FriendSuggestion> {
        return try {
            // Force SERVER fetch to ensure we have permission and latest data
            // This is critical after permission rule updates
            val user1Friends = usersCollection.document(user1Id)
                .collection("friends")
                .get(Source.SERVER)
                .await()
                .documents.map { it.getString("friendId") ?: it.id }
                .toSet()
            
            val user2Friends = usersCollection.document(user2Id)
                .collection("friends")
                .get(Source.SERVER)
                .await()
                .documents.map { it.getString("friendId") ?: it.id }
                .toSet()
            
            val mutualFriendIds = user1Friends.intersect(user2Friends)
            Log.d("FriendRepository", "Mutual friends calculation: User1 has ${user1Friends.size}, User2 has ${user2Friends.size}, Intersection: ${mutualFriendIds.size}")
            
            mutualFriendIds.mapNotNull { friendId ->
                try {
                    val friendData = usersCollection.document(friendId).get().await().data
                    FriendSuggestion(
                        userId = friendId,
                        fullName = friendData?.get("fullName") as? String ?: "Unknown",
                        email = friendData?.get("email") as? String ?: "",
                        photoUrl = friendData?.get("photoUrl") as? String,
                        lastActive = (friendData?.get("lastActive") as? Long) ?: 0L
                    )
                } catch (e: Exception) {
                    Log.w("FriendRepository", "Error fetching mutual friend $friendId: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error getting mutual friends: ${e.message} (User1: $user1Id, User2: $user2Id)", e)
            emptyList()
        }
    }
    
    /**
     * Get list of blocked users
     */
    suspend fun getBlockedUsers(currentUserId: String): List<Pair<String, String>> {
        return try {
            // Try with orderBy first, fallback to simple get if it fails
            val blockedSnapshot = try {
                usersCollection.document(currentUserId)
                    .collection("blockedUsers")
                    .orderBy("blockedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
            } catch (e: Exception) {
                // If orderBy fails (e.g., missing index or field), try without ordering
                Log.w("FriendRepository", "OrderBy failed, using simple query: ${e.message}")
                usersCollection.document(currentUserId)
                    .collection("blockedUsers")
                    .get()
                    .await()
            }
            
            val blockedUsers = blockedSnapshot.documents.mapNotNull { doc ->
                val blockedUserId = doc.getString("blockedUserId") ?: doc.id
                val blockedUserName = doc.getString("blockedUserName") ?: "Unknown"
                val blockedAt = doc.getLong("blockedAt") ?: 0L
                Triple(blockedUserId, blockedUserName, blockedAt)
            }
            
            // Sort by blockedAt in memory if we got it without orderBy
            blockedUsers.sortedByDescending { it.third }
                .map { it.first to it.second }
        } catch (e: Exception) {
            Log.e("FriendRepository", "Error loading blocked users: ${e.message}", e)
            emptyList()
        }
    }
}

