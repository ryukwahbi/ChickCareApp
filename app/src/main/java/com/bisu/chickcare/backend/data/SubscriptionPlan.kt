package com.bisu.chickcare.backend.data

/**
 * Subscription plan tiers for ChickCare premium features.
 */
enum class SubscriptionTier {
    FREE,
    PREMIUM
}

/**
 * Holds subscription information for a user.
 */
data class SubscriptionInfo(
    val plan: SubscriptionTier = SubscriptionTier.FREE,
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val isActive: Boolean = false
) {
    companion object {
        fun fromMap(map: Map<String, Any?>): SubscriptionInfo {
            val planStr = map["subscriptionPlan"] as? String ?: "FREE"
            val startDate = map["subscriptionStartDate"] as? Long ?: 0L
            val endDate = map["subscriptionEndDate"] as? Long ?: 0L
            val plan = try {
                SubscriptionTier.valueOf(planStr)
            } catch (_: Exception) {
                SubscriptionTier.FREE
            }
            val isActive = plan == SubscriptionTier.PREMIUM && 
                (endDate == 0L || endDate > System.currentTimeMillis())
            return SubscriptionInfo(plan, startDate, endDate, isActive)
        }
    }
}

/**
 * Features that require premium subscription.
 */
enum class PremiumFeature(val displayName: String, val description: String) {
    REPORTS_ANALYTICS("Reports & Analytics", "Detailed health and farm reports with visual charts"),
    FARM_INSIGHTS("Farm Insights", "Advanced farm performance insights and recommendations"),
    EXPENSE_TRACKER("Expense Tracker", "Track farm expenses and financial management"),
    EGG_PRODUCTION("Egg Production Tracker", "Monitor and analyze egg production data"),
    MEDICATIONS_LOG("Medications Log", "Complete medication tracking and scheduling")
}
