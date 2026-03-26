package com.bisu.chickcare.frontend.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.backend.data.PremiumFeature
import com.bisu.chickcare.backend.data.SubscriptionTier
import com.bisu.chickcare.backend.viewmodels.SubscriptionViewModel
import com.bisu.chickcare.backend.viewmodels.UpgradeResult
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    navController: NavController,
    subscriptionViewModel: SubscriptionViewModel = viewModel()
) {
    val subscriptionInfo by subscriptionViewModel.subscriptionInfo.collectAsState()
    val isPremium by subscriptionViewModel.isPremium.collectAsState()
    val isLoading by subscriptionViewModel.isLoading.collectAsState()
    val upgradeResult by subscriptionViewModel.upgradeResult.collectAsState()

    var showUpgradeDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showDowngradeDialog by remember { mutableStateOf(false) }

    // Refresh on screen load
    LaunchedEffect(Unit) {
        subscriptionViewModel.refreshSubscription()
    }

    // Handle upgrade result
    LaunchedEffect(upgradeResult) {
        when (upgradeResult) {
            UpgradeResult.SUCCESS -> {
                showUpgradeDialog = false
                showSuccessDialog = true
                subscriptionViewModel.clearUpgradeResult()
            }
            UpgradeResult.FAILURE -> {
                subscriptionViewModel.clearUpgradeResult()
            }
            null -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Subscription",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ThemeColorUtils.black()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ThemeColorUtils.black()
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeColorUtils.white(),
                    titleContentColor = ThemeColorUtils.black()
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
                .padding(innerPadding)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFD27D2D))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Current Plan Status Card
                    item {
                        CurrentPlanCard(
                            isPremium = isPremium,
                            endDate = subscriptionInfo.endDate
                        )
                    }

                    // Plan Comparison
                    item {
                        Text(
                            text = "Choose Your Plan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColorUtils.black(),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Free Plan Card
                    item {
                        PlanCard(
                            planName = "Free",
                            price = "₱0",
                            period = "forever",
                            isCurrentPlan = !isPremium,
                            isPremiumPlan = false,
                            features = listOf(
                                PlanFeatureItem("Unlimited Disease Detection", true),
                                PlanFeatureItem("Last 10 Detection History", true),
                                PlanFeatureItem("Community & Chat", true),
                                PlanFeatureItem("Farm Tips & Disease Database", true),
                                PlanFeatureItem("Weather Updates", true),
                                PlanFeatureItem("Health Records (View Only)", true),
                                PlanFeatureItem("Reports & Analytics", false),
                                PlanFeatureItem("Farm Insights", false),
                                PlanFeatureItem("Expense Tracker", false),
                                PlanFeatureItem("Egg Production Tracker", false),
                                PlanFeatureItem("Medications Log", false)
                            ),
                            onSelectPlan = {
                                if (isPremium) {
                                    showDowngradeDialog = true
                                }
                            }
                        )
                    }

                    // Premium Plan Card
                    item {
                        PlanCard(
                            planName = "Premium",
                            price = "₱149",
                            period = "/month",
                            isCurrentPlan = isPremium,
                            isPremiumPlan = true,
                            features = listOf(
                                PlanFeatureItem("Unlimited Disease Detection", true),
                                PlanFeatureItem("Unlimited Detection History", true),
                                PlanFeatureItem("Community & Chat", true),
                                PlanFeatureItem("Farm Tips & Disease Database", true),
                                PlanFeatureItem("Weather Updates", true),
                                PlanFeatureItem("Health Records (Full Access)", true),
                                PlanFeatureItem("Reports & Analytics", true),
                                PlanFeatureItem("Farm Insights", true),
                                PlanFeatureItem("Expense Tracker", true),
                                PlanFeatureItem("Egg Production Tracker", true),
                                PlanFeatureItem("Medications Log", true)
                            ),
                            onSelectPlan = {
                                if (!isPremium) {
                                    showUpgradeDialog = true
                                }
                            }
                        )
                    }

                    // Premium Features Showcase
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Premium Features",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColorUtils.black()
                        )
                    }

                    item {
                        PremiumFeatureShowcaseItem(
                            icon = Icons.Default.Analytics,
                            feature = PremiumFeature.REPORTS_ANALYTICS
                        )
                    }
                    item {
                        PremiumFeatureShowcaseItem(
                            icon = Icons.Default.Insights,
                            feature = PremiumFeature.FARM_INSIGHTS
                        )
                    }
                    item {
                        PremiumFeatureShowcaseItem(
                            icon = Icons.Default.AttachMoney,
                            feature = PremiumFeature.EXPENSE_TRACKER
                        )
                    }
                    item {
                        PremiumFeatureShowcaseItem(
                            icon = Icons.Default.Egg,
                            feature = PremiumFeature.EGG_PRODUCTION
                        )
                    }
                    item {
                        PremiumFeatureShowcaseItem(
                            icon = Icons.Default.MedicalServices,
                            feature = PremiumFeature.MEDICATIONS_LOG
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    // Upgrade Confirmation Dialog
    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showUpgradeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color(0xFFD27D2D),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upgrade to Premium",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "You're about to upgrade to ChickCare Premium!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ThemeColorUtils.black()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• ₱149/month\n• 30-day subscription\n• Access to all premium features\n• Cancel anytime",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeColorUtils.darkGray(Color(0xFF666666))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { subscriptionViewModel.upgradeToPremium() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD27D2D)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Confirm Upgrade", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpgradeDialog = false }) {
                    Text("Cancel", color = ThemeColorUtils.black())
                }
            },
            containerColor = ThemeColorUtils.white(),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Welcome to Premium!",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = "You now have full access to all premium features. Enjoy your enhanced ChickCare experience!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ThemeColorUtils.black()
                )
            },
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD27D2D)
                    )
                ) {
                    Text("Let's Go!", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = ThemeColorUtils.white(),
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Downgrade Confirmation Dialog
    if (showDowngradeDialog) {
        AlertDialog(
            onDismissRequest = { showDowngradeDialog = false },
            title = {
                Text(
                    text = "Downgrade to Free?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "You will lose access to premium features including Reports & Analytics, Farm Insights, Expense Tracker, Egg Production Tracker, and Medications Log.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ThemeColorUtils.black()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        subscriptionViewModel.downgradeToFree()
                        showDowngradeDialog = false
                    }
                ) {
                    Text(
                        "Downgrade",
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDowngradeDialog = false }) {
                    Text("Keep Premium", color = ThemeColorUtils.black())
                }
            },
            containerColor = ThemeColorUtils.white(),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun CurrentPlanCard(isPremium: Boolean, endDate: Long) {
    val gradientColors = if (isPremium) {
        listOf(Color(0xFFD27D2D), Color(0xFFE8A04E), Color(0xFFFFB300))
    } else {
        listOf(Color(0xFF8D6E63), Color(0xFFA1887F))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .padding(24.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isPremium) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Current Plan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = if (isPremium) "Premium" else "Free",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    if (isPremium) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                if (isPremium && endDate > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    Text(
                        text = "Valid until ${dateFormatter.format(Date(endDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

data class PlanFeatureItem(
    val name: String,
    val included: Boolean
)

@Composable
private fun PlanCard(
    planName: String,
    price: String,
    period: String,
    isCurrentPlan: Boolean,
    isPremiumPlan: Boolean,
    features: List<PlanFeatureItem>,
    onSelectPlan: () -> Unit
) {
    val borderColor = if (isPremiumPlan) Color(0xFFD27D2D) else Color(0xFFBDBDBD)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isPremiumPlan) Modifier.border(
                    2.dp,
                    Color(0xFFD27D2D),
                    RoundedCornerShape(20.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.white()
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPremiumPlan) 8.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Popular badge for premium
            if (isPremiumPlan) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFD27D2D))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⭐ MOST POPULAR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Plan name & price
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = planName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColorUtils.black()
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = price,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isPremiumPlan) Color(0xFFD27D2D) else ThemeColorUtils.black()
                )
                Text(
                    text = period,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeColorUtils.darkGray(Color(0xFF888888)),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Features list
            features.forEach { feature ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (feature.included) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (feature.included) Color(0xFF4CAF50) else Color(0xFFBDBDBD)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = feature.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (feature.included)
                            ThemeColorUtils.black()
                        else
                            ThemeColorUtils.lightGray(Color(0xFFBDBDBD))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action button
            if (isCurrentPlan) {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Current Plan",
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onSelectPlan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPremiumPlan) Color(0xFFD27D2D) else Color(0xFF8D6E63)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isPremiumPlan) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isPremiumPlan) "Upgrade Now" else "Downgrade",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumFeatureShowcaseItem(
    icon: ImageVector,
    feature: PremiumFeature
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.white()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFD27D2D).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFD27D2D),
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feature.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ThemeColorUtils.black()
                )
                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeColorUtils.darkGray(Color(0xFF888888))
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFD27D2D).copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "PRO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD27D2D)
                )
            }
        }
    }
}
