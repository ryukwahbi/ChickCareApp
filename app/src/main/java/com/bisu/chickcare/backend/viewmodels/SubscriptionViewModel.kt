package com.bisu.chickcare.backend.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.data.PremiumFeature
import com.bisu.chickcare.backend.data.SubscriptionInfo
import com.bisu.chickcare.backend.data.SubscriptionTier
import com.bisu.chickcare.backend.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing subscription state throughout the app.
 */
class SubscriptionViewModel : ViewModel() {
    private val repository = SubscriptionRepository()

    private val _subscriptionInfo = MutableStateFlow(SubscriptionInfo())
    val subscriptionInfo: StateFlow<SubscriptionInfo> = _subscriptionInfo.asStateFlow()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _upgradeResult = MutableStateFlow<UpgradeResult?>(null)
    val upgradeResult: StateFlow<UpgradeResult?> = _upgradeResult.asStateFlow()

    init {
        refreshSubscription()
    }

    /**
     * Refresh subscription status from Firestore.
     */
    fun refreshSubscription() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val info = repository.getSubscriptionInfo()
                _subscriptionInfo.value = info
                _isPremium.value = info.isActive
            } catch (_: Exception) {
                // Keep current state on error
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Upgrade to premium subscription.
     */
    fun upgradeToPremium() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.upgradeToPremium()
            if (result.isSuccess) {
                refreshSubscription()
                _upgradeResult.value = UpgradeResult.SUCCESS
            } else {
                _upgradeResult.value = UpgradeResult.FAILURE
                _isLoading.value = false
            }
        }
    }

    /**
     * Downgrade to free plan.
     */
    fun downgradeToFree() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.downgradeToFree()
            if (result.isSuccess) {
                refreshSubscription()
            }
            _isLoading.value = false
        }
    }

    /**
     * Check if a specific premium feature is accessible.
     */
    fun hasFeatureAccess(feature: PremiumFeature): Boolean {
        return _isPremium.value
    }

    /**
     * Clear the upgrade result after it's been handled.
     */
    fun clearUpgradeResult() {
        _upgradeResult.value = null
    }
}

enum class UpgradeResult {
    SUCCESS,
    FAILURE
}
