package com.bisu.chickcare

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bisu.chickcare.backend.repository.DetectionEntry
import com.bisu.chickcare.backend.utils.LocaleHelper
import com.bisu.chickcare.backend.viewmodels.AuthViewModel
import com.bisu.chickcare.frontend.screen.AboutScreen
import com.bisu.chickcare.frontend.screen.AccountSettingsScreen
import com.bisu.chickcare.frontend.screen.ActionScreen
import com.bisu.chickcare.frontend.screen.ActiveFriendsScreen
import com.bisu.chickcare.frontend.screen.AnnouncementsScreen
import com.bisu.chickcare.frontend.screen.AppInfoScreen
import com.bisu.chickcare.frontend.screen.ArchivesScreen
import com.bisu.chickcare.frontend.screen.AudioInputScreen
import com.bisu.chickcare.frontend.screen.BlockedUsersScreen
import com.bisu.chickcare.frontend.screen.CameraScreen
import com.bisu.chickcare.frontend.screen.ChatScreen
import com.bisu.chickcare.frontend.screen.CommentsScreen
import com.bisu.chickcare.frontend.screen.DashboardScreen
import com.bisu.chickcare.frontend.screen.DetectionHistoryScreen
import com.bisu.chickcare.frontend.screen.DiseaseDatabaseScreen
import com.bisu.chickcare.frontend.screen.EggProductionTrackerScreen
import com.bisu.chickcare.frontend.screen.EmergencyContactsScreen
import com.bisu.chickcare.frontend.screen.ExpenseTrackerScreen
import com.bisu.chickcare.frontend.screen.FarmInsightsScreen
import com.bisu.chickcare.frontend.screen.FarmTipsScreen
import com.bisu.chickcare.frontend.screen.FavoritesScreen
import com.bisu.chickcare.frontend.screen.FeedingScheduleScreen
import com.bisu.chickcare.frontend.screen.FriendSuggestionsScreen
import com.bisu.chickcare.frontend.screen.HealthRecordsScreen
import com.bisu.chickcare.frontend.screen.HelpCenterScreen
import com.bisu.chickcare.frontend.screen.HistoryResultScreen
import com.bisu.chickcare.frontend.screen.LanguageSettingsScreen
import com.bisu.chickcare.frontend.screen.LastDetectionDetailScreen
import com.bisu.chickcare.frontend.screen.LoginScreen
import com.bisu.chickcare.frontend.screen.MainScreen
import com.bisu.chickcare.frontend.screen.ManageProfilesScreen
import com.bisu.chickcare.frontend.screen.MedicationsLogScreen
import com.bisu.chickcare.frontend.screen.MessagesListScreen
import com.bisu.chickcare.frontend.screen.NotificationSettingsScreen
import com.bisu.chickcare.frontend.screen.NotificationsScreen
import com.bisu.chickcare.frontend.screen.OnboardingScreen
import com.bisu.chickcare.frontend.screen.PostDetectionHistoryScreen
import com.bisu.chickcare.frontend.screen.PrivacyPolicyScreen
import com.bisu.chickcare.frontend.screen.ProcessingScreen
import com.bisu.chickcare.frontend.screen.ProfileScreen
import com.bisu.chickcare.frontend.screen.RecentActivityScreen
import com.bisu.chickcare.frontend.screen.RecentlyDeletedScreen
import com.bisu.chickcare.frontend.screen.ReportsAnalyticsScreen
import com.bisu.chickcare.frontend.screen.ResetPasswordScreen
import com.bisu.chickcare.frontend.screen.ResultScreen
import com.bisu.chickcare.frontend.screen.SavedPostsScreen
import com.bisu.chickcare.frontend.screen.SecurityPrivacyScreen
import com.bisu.chickcare.frontend.screen.SettingsScreen
import com.bisu.chickcare.frontend.screen.SignupScreen
import com.bisu.chickcare.frontend.screen.TermsOfServiceScreen
import com.bisu.chickcare.frontend.screen.TrashScreen
import com.bisu.chickcare.frontend.screen.VaccinationScheduleScreen
import com.bisu.chickcare.frontend.screen.VetContactsScreen
import com.bisu.chickcare.frontend.screen.WelcomeScreen
import com.bisu.chickcare.frontend.screen.YourFriendsScreen
import com.bisu.chickcare.frontend.screen.SubscriptionScreen
import com.bisu.chickcare.frontend.components.PremiumFeatureGate
import com.bisu.chickcare.backend.viewmodels.SubscriptionViewModel
import com.bisu.chickcare.ui.theme.ChickCareAppTheme

class MainActivity : ComponentActivity() {
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.attachBaseContext(newBase))
    }
    
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "Notification permission granted")
        } else {
            android.util.Log.d("MainActivity", "Notification permission denied")
        }
    }
    
    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            android.util.Log.d("MainActivity", "Location permission granted")
        } else {
            android.util.Log.d("MainActivity", "Location permission denied")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        requestLocationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        
        setContent {
            ChickCareAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = viewModel()
                    val dashboardViewModel: com.bisu.chickcare.backend.viewmodels.DashboardViewModel = viewModel()
                    val subscriptionViewModel: SubscriptionViewModel = viewModel()

                    val context = LocalContext.current
                    
                    // Real-time Notification Manager
                    val realtimeNotificationManager = remember { com.bisu.chickcare.backend.service.RealtimeNotificationManager(context) }
                    val currentUserProfile by authViewModel.userProfile.collectAsState()

                    // Handle real-time listeners based on auth state
                    LaunchedEffect(currentUserProfile) {
                        if (currentUserProfile != null) {
                            realtimeNotificationManager.startListening()
                            com.bisu.chickcare.backend.service.NotificationForegroundService.start(context)
                            authViewModel.observeCurrentSession(context)
                        } else {
                            realtimeNotificationManager.stopListening()
                            com.bisu.chickcare.backend.service.NotificationForegroundService.stop(context)
                            authViewModel.stopObservingCurrentSession()
                        }
                    }

                    // Handle deep links and initial navigation
                    LaunchedEffect(Unit) {
                        val isAuthenticated = authViewModel.isAuthenticated(context)
                        
                        val hasDeepLink = intent.data != null
                        val notificationType = intent.getStringExtra("type")
                        val notificationUserId = intent.getStringExtra("userId")
                        val notificationUserName = intent.getStringExtra("userName")
                        
                        // Handle chat notification
                        if (notificationType == "CHAT_MESSAGE" && notificationUserId != null) {
                            if (isAuthenticated) {
                                navController.navigate("chat?userId=$notificationUserId&userName=${notificationUserName ?: ""}") {
                                    popUpTo("dashboard") { inclusive = false }
                                }
                            } else {
                                navController.navigate("login") {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            }
                        } else if (hasDeepLink) {
                            handleDeepLink(intent, navController, authViewModel)
                        }
                    }
                    
                    // Clean up listeners when MainActivity is destroyed
                    androidx.compose.runtime.DisposableEffect(Unit) {
                        onDispose {
                            realtimeNotificationManager.stopListening()
                        }
                    }
                    
                    // Handle new intents (when app is already running and receives a new deep link)
                    LaunchedEffect(intent) {
                        if (intent.data != null) {
                            handleDeepLink(intent, navController, authViewModel)
                        }
                    }

                    NavHost(navController = navController, startDestination = "welcome") {
                        composable("welcome") { WelcomeScreen(navController) }
                        composable("onboarding") { OnboardingScreen(navController) }
                        composable(
                            route = "login?email={email}",
                            arguments = listOf(
                                navArgument("email") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val email = backStackEntry.arguments?.getString("email")
                            LoginScreen(navController, email)
                        }
                        composable("signup") { SignupScreen(navController) }
                        composable("reset_password") { ResetPasswordScreen(navController) }
                        composable("action_tools") { ActionScreen(navController) }
                        composable(
                            route = "settings",
                            enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(500)
                                )
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(500)
                                )
                            }
                        ) { SettingsScreen(navController) }
                        composable("account_settings") { AccountSettingsScreen(navController) }
                        composable("security_privacy_settings") { SecurityPrivacyScreen(navController) }
                        composable("notification_settings") { NotificationSettingsScreen(navController) }
                        composable("about_settings") { AboutScreen(navController) }
                        composable("subscription") { SubscriptionScreen(navController, subscriptionViewModel) }
                        composable("app_info") { AppInfoScreen(navController) }
                        composable("language_settings") { LanguageSettingsScreen(navController) }
                        composable("privacy_policy") { PrivacyPolicyScreen(navController) }
                        composable("terms_of_service") { TermsOfServiceScreen(navController) }
                        composable("dashboard") {
                            MainScreen(navController = navController) { _ ->
                                DashboardScreen(navController = navController, dashboardViewModel = dashboardViewModel)
                            }
                        }
                        composable(
                            route = "profile?initialTab={initialTab}",
                            arguments = listOf(
                                navArgument("initialTab") {
                                    type = NavType.IntType
                                    defaultValue = 0
                                }
                            )
                        ) { backStackEntry ->
                            val initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 0
                            ProfileScreen(navController = navController, initialTab = initialTab)
                        }
                        composable(
                            route = "view_profile?userId={userId}",
                            arguments = listOf(
                                navArgument("userId") {
                                    type = NavType.StringType
                                }
                            )
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId")
                            if (userId != null) {
                                ProfileScreen(navController = navController, viewUserId = userId)
                            } else {
                                ProfileScreen(navController = navController)
                            }
                        }
                        composable("camera") { CameraScreen(navController) }
                        composable(
                            route = "audio_input?imageUri={imageUri}",
                            arguments = listOf(
                                navArgument("imageUri") {
                                    type = NavType.StringType
                                    nullable = true
                                }
                            )
                        ) { backStackEntry ->
                            val imageUri = backStackEntry.arguments?.getString("imageUri")
                            AudioInputScreen(
                                navController = navController,
                                imageUri = imageUri
                            )
                        }
                        composable(
                            route = "processing?imageUri={imageUri}&audioUri={audioUri}",
                            arguments = listOf(
                                navArgument("imageUri") {
                                    type = NavType.StringType
                                    nullable = true
                                },
                                navArgument("audioUri") {
                                    type = NavType.StringType
                                    nullable = true
                                }
                            ),
                            enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(500)
                                )
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(500)
                                )
                            }
                        ) { backStackEntry ->
                            val imgUri = backStackEntry.arguments?.getString("imageUri") ?: ""
                            val audUri = backStackEntry.arguments?.getString("audioUri") ?: ""
                            ProcessingScreen(
                                navController = navController,
                                imageUri = imgUri,
                                audioUri = audUri
                            )
                        }
                        composable(
                            route = "notifications",
                            enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(500)
                                )
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(500)
                                )
                            }
                        ) { NotificationsScreen(navController, viewModel = dashboardViewModel) }
                        composable("manage_profiles") { ManageProfilesScreen(navController) }
                        composable("friend_suggestions") { FriendSuggestionsScreen(navController) }
                        composable(
                            route = "friends?userId={userId}",
                            arguments = listOf(
                                navArgument("userId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId")
                            YourFriendsScreen(navController, userId)
                        }
                        composable(
                            route = "active_friends",
                            enterTransition = {
                                slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = tween(500)
                                )
                            },
                            popExitTransition = {
                                slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = tween(500)
                                )
                            }
                        ) { ActiveFriendsScreen(navController) }
                        composable("blocked_users") { BlockedUsersScreen(navController) }
                        composable("messages") { MessagesListScreen(navController) }
                        composable(
                            route = "chat?userId={userId}&userName={userName}",
                            arguments = listOf(
                                navArgument("userId") {
                                    type = NavType.StringType
                                    nullable = true
                                },
                                navArgument("userName") {
                                    type = NavType.StringType
                                    nullable = true
                                }
                            )
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId")
                            val userName = backStackEntry.arguments?.getString("userName")
                            ChatScreen(navController = navController, userId = userId, userName = userName)
                        }
                        composable("farm_tips") { FarmTipsScreen(navController) }
                        composable("post_detection_history") { PostDetectionHistoryScreen(navController) }
                        composable("recently_deleted") { RecentlyDeletedScreen(navController) }
                        composable("favorites") { FavoritesScreen(navController) }
                        composable("saved_posts") { SavedPostsScreen(navController) }
                        composable("archives") { ArchivesScreen(navController) }
                        composable("trash") { TrashScreen(navController) }
                        composable("health_records") { HealthRecordsScreen(navController) }
                        composable("vaccination_schedule") { VaccinationScheduleScreen(navController) }
                        composable("feeding_schedule") { FeedingScheduleScreen(navController) }
                        composable("egg_production") {
                            PremiumFeatureGate(
                                featureName = "Egg Production Tracker",
                                navController = navController,
                                subscriptionViewModel = subscriptionViewModel
                            ) {
                                EggProductionTrackerScreen(navController)
                            }
                        }
                        composable("expense_tracker") {
                            PremiumFeatureGate(
                                featureName = "Expense Tracker",
                                navController = navController,
                                subscriptionViewModel = subscriptionViewModel
                            ) {
                                ExpenseTrackerScreen(navController)
                            }
                        }
                        composable("disease_database") { DiseaseDatabaseScreen(navController) }
                        composable("medications_log") {
                            PremiumFeatureGate(
                                featureName = "Medications Log",
                                navController = navController,
                                subscriptionViewModel = subscriptionViewModel
                            ) {
                                MedicationsLogScreen(navController)
                            }
                        }
                        composable("reports_analytics") {
                            PremiumFeatureGate(
                                featureName = "Reports & Analytics",
                                navController = navController,
                                subscriptionViewModel = subscriptionViewModel
                            ) {
                                ReportsAnalyticsScreen(navController)
                            }
                        }
                        composable("emergency_contacts") { EmergencyContactsScreen(navController) }
                        composable("vet_contacts") { VetContactsScreen(navController) }

                        composable("detection_history") {
                            MainScreen(navController = navController) { paddingValues ->
                                DetectionHistoryScreen(navController = navController, paddingValues = paddingValues, dashboardViewModel = dashboardViewModel)
                            }
                        }
                        composable("help_center") {
                            MainScreen(navController = navController) { paddingValues ->
                                HelpCenterScreen(paddingValues = paddingValues, authViewModel = authViewModel)
                            }
                        }
                        composable("recent_activity") { RecentActivityScreen(navController) }
                        composable(
                            route = "comments/{postId}/{postOwnerId}",
                            arguments = listOf(
                                navArgument("postId") { type = NavType.StringType },
                                navArgument("postOwnerId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val postId = backStackEntry.arguments?.getString("postId") ?: ""
                            val postOwnerId = backStackEntry.arguments?.getString("postOwnerId") ?: ""
                            CommentsScreen(navController, postId, postOwnerId)
                        }
                        composable("farm_insights") {
                            PremiumFeatureGate(
                                featureName = "Farm Insights",
                                navController = navController,
                                subscriptionViewModel = subscriptionViewModel
                            ) {
                                FarmInsightsScreen(navController)
                            }
                        }
                        composable("announcements") { AnnouncementsScreen(navController) }

                        composable(
                            route = "last_detection_detail/{detectionId}",
                            arguments = listOf(
                                navArgument("detectionId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val detectionId = backStackEntry.arguments?.getString("detectionId") ?: ""
                            LastDetectionDetailScreen(
                                navController = navController,
                                detectionId = detectionId,
                                dashboardViewModel = dashboardViewModel
                            )
                        }

                        composable(
                            route = "history_result_detail" +
                                    "?entryId={entryId}" +
                                    "&timestamp={timestamp}" +
                                    "&location={location}" +
                                    "&imageUri={imageUri}" +
                                    "&audioUri={audioUri}" +
                                    "&result={result}" +
                                    "&isHealthy={isHealthy}" +
                                    "&confidence={confidence}" +
                                    "&recommendations={recommendations}",
                            arguments = listOf(
                                navArgument("entryId") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                },
                                navArgument("timestamp") {
                                    type = NavType.LongType
                                    defaultValue = 0L
                                },
                                navArgument("location") {
                                    type = NavType.StringType
                                    nullable = true
                                },
                                navArgument("imageUri") {
                                    type = NavType.StringType
                                    nullable = true
                                },
                                navArgument("audioUri") {
                                    type = NavType.StringType
                                    nullable = true
                                },
                                navArgument("result") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                },
                                navArgument("isHealthy") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                },
                                navArgument("confidence") {
                                    type = NavType.FloatType
                                    defaultValue = 0f
                                },
                                navArgument("recommendations") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                }
                            )
                        ) { backStackEntry ->
                            val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
                            val timestamp = backStackEntry.arguments?.getLong("timestamp") ?: 0L
                            val location = backStackEntry.arguments?.getString("location")
                            val imageUri = backStackEntry.arguments?.getString("imageUri")
                            val audioUri = backStackEntry.arguments?.getString("audioUri")
                            val result = backStackEntry.arguments?.getString("result") ?: ""
                            val isHealthy = backStackEntry.arguments?.getBoolean("isHealthy") ?: false
                            val confidence = backStackEntry.arguments?.getFloat("confidence") ?: 0f
                            val recommendationsStr = backStackEntry.arguments?.getString("recommendations") ?: ""
                            val recommendations = recommendationsStr
                                .split("|")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                            
                            val entry = DetectionEntry(
                                id = entryId,
                                result = result,
                                isHealthy = isHealthy,
                                confidence = confidence,
                                imageUri = imageUri,
                                audioUri = audioUri,
                                timestamp = timestamp,
                                location = location,
                                recommendations = recommendations
                            )
                            
                            HistoryResultScreen(
                                navController = navController,
                                entry = entry
                            )
                        }
                        
                        composable(
                            route = "detection_result" +
                                    "?status={status}" +
                                    "&suggestions={suggestions}" +
                                    "&imageUri={imageUri}" +
                                    "&audioUri={audioUri}",
                            arguments = listOf(
                                // Required arguments
                                navArgument("status") {
                                    type = NavType.StringType
                                    defaultValue = "Unknown"
                                },
                                navArgument("suggestions") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                },
                                navArgument("imageUri") {
                                    type = NavType.StringType
                                    nullable = true
                                },
                                navArgument("audioUri") {
                                    type = NavType.StringType
                                    nullable = true
                                }
                            )
                        ) { backStackEntry ->
                            ResultScreen(
                                navController = navController,
                                status = backStackEntry.arguments?.getString("status") ?: "Unknown",
                                suggestions = backStackEntry.arguments
                                    ?.getString("suggestions")
                                    ?.split('|')
                                    ?.map { it.trim() }
                                    ?.filter { it.isNotEmpty() }
                                    ?: emptyList(),
                                imageUri = backStackEntry.arguments?.getString("imageUri"),
                                audioUri = backStackEntry.arguments?.getString("audioUri")
                            )
                        }
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
    
    /**
     * Handle deep link intents
     */
    private fun handleDeepLink(
        intent: Intent?,
        navController: androidx.navigation.NavController,
        authViewModel: AuthViewModel
    ) {
        val data: Uri? = intent?.data
        if (data != null) {
            android.util.Log.d("MainActivity", "Deep link received: $data")
            
            // Check if user is authenticated
            val isAuthenticated = authViewModel.isAuthenticated(this)
            
            // Parse the deep link path
            val path = data.path ?: ""
            val host = data.host ?: ""
            
            when {
                // Handle download links
                path.contains("download", ignoreCase = true) || host.contains("download", ignoreCase = true) -> {
                    // Navigate to a download screen or show download dialog
                    // For now, we\'ll just log it - you can add a download screen later
                    android.util.Log.d("MainActivity", "Download link clicked")
                }
                
                // Handle dashboard links
                path.contains("dashboard", ignoreCase = true) || host.contains("dashboard", ignoreCase = true) -> {
                    if (isAuthenticated) {
                        navController.navigate("dashboard") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    } else {
                        navController.navigate("login") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                }
                
                // Handle profile links
                path.contains("profile", ignoreCase = true) || host.contains("profile", ignoreCase = true) -> {
                    val userId = data.getQueryParameter("userId")
                    if (isAuthenticated) {
                        if (userId != null) {
                            navController.navigate("view_profile?userId=$userId") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        } else {
                            navController.navigate("profile") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        }
                    } else {
                        navController.navigate("login") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                }
                
                path.contains("history", ignoreCase = true) || host.contains("history", ignoreCase = true) -> {
                    if (isAuthenticated) {
                        navController.navigate("detection_history") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    } else {
                        navController.navigate("login") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                }
                
                else -> {
                    if (isAuthenticated) {
                        navController.navigate("dashboard") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}
