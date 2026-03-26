package com.bisu.chickcare.backend.repository

import com.bisu.chickcare.backend.data.PremiumFeature
import com.bisu.chickcare.backend.data.SubscriptionInfo
import com.bisu.chickcare.backend.data.SubscriptionTier
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing user subscription data in Firestore.
 */
class SubscriptionRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Get the current user's subscription info from Firestore.
     */
    suspend fun getSubscriptionInfo(): SubscriptionInfo {
        val userId = currentUserId ?: return SubscriptionInfo()
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            val data = doc.data ?: return SubscriptionInfo()
            SubscriptionInfo.fromMap(data)
        } catch (_: Exception) {
            SubscriptionInfo()
        }
    }

    /**
     * Check if the current user has an active premium subscription.
     */
    suspend fun isPremiumActive(): Boolean {
        return getSubscriptionInfo().isActive
    }

    /**
     * Upgrade the current user to premium (30-day subscription).
     * In a production app, this would be called after payment verification.
     */
    suspend fun upgradeToPremium(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val now = System.currentTimeMillis()
            val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
            val endDate = now + thirtyDaysMs

            firestore.collection("users").document(userId).update(
                mapOf(
                    "subscriptionPlan" to SubscriptionTier.PREMIUM.name,
                    "subscriptionStartDate" to now,
                    "subscriptionEndDate" to endDate
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downgrade the current user to the free plan.
     */
    suspend fun downgradeToFree(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("Not authenticated"))
        return try {
            firestore.collection("users").document(userId).update(
                mapOf(
                    "subscriptionPlan" to SubscriptionTier.FREE.name,
                    "subscriptionStartDate" to 0L,
                    "subscriptionEndDate" to 0L
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if a specific feature requires premium and if the user has access.
     */
    suspend fun hasAccessToFeature(feature: PremiumFeature): Boolean {
        return isPremiumActive()
    }
}
