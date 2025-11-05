package com.bisu.chickcare

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bisu.chickcare.backend.repository.DetectionEntry
import com.bisu.chickcare.backend.viewmodels.AuthViewModel
import com.bisu.chickcare.frontend.screen.ActionScreen
import com.bisu.chickcare.frontend.screen.ArchivesScreen
import com.bisu.chickcare.frontend.screen.AudioInputScreen
import com.bisu.chickcare.frontend.screen.CameraScreen
import com.bisu.chickcare.frontend.screen.DashboardScreen
import com.bisu.chickcare.frontend.screen.DetectionHistoryScreen
import com.bisu.chickcare.frontend.screen.FarmTipsScreen
import com.bisu.chickcare.frontend.screen.FavoritesScreen
import com.bisu.chickcare.frontend.screen.FriendSuggestionsScreen
import com.bisu.chickcare.frontend.screen.HelpCenterScreen
import com.bisu.chickcare.frontend.screen.HistoryResultScreen
import com.bisu.chickcare.frontend.screen.LastDetectionDetailScreen
import com.bisu.chickcare.frontend.screen.LoginScreen
import com.bisu.chickcare.frontend.screen.MainScreen
import com.bisu.chickcare.frontend.screen.ManageProfilesScreen
import com.bisu.chickcare.frontend.screen.NotificationsScreen
import com.bisu.chickcare.frontend.screen.PostDetectionHistoryScreen
import com.bisu.chickcare.frontend.screen.ProcessingScreen
import com.bisu.chickcare.frontend.screen.ProfileScreen
import com.bisu.chickcare.frontend.screen.RecentlyDeletedScreen
import com.bisu.chickcare.frontend.screen.ResetPasswordScreen
import com.bisu.chickcare.frontend.screen.ResultScreen
import com.bisu.chickcare.frontend.screen.SettingsScreen
import com.bisu.chickcare.frontend.screen.SignupScreen
import com.bisu.chickcare.frontend.screen.TrashScreen
import com.bisu.chickcare.frontend.screen.WelcomeScreen
import com.bisu.chickcare.frontend.screen.HealthRecordsScreen
import com.bisu.chickcare.frontend.screen.VaccinationScheduleScreen
import com.bisu.chickcare.frontend.screen.FeedingScheduleScreen
import com.bisu.chickcare.frontend.screen.EggProductionTrackerScreen
import com.bisu.chickcare.frontend.screen.GrowthMonitoringScreen
import com.bisu.chickcare.frontend.screen.ExpenseTrackerScreen
import com.bisu.chickcare.frontend.screen.DiseaseDatabaseScreen
import com.bisu.chickcare.frontend.screen.MedicationsLogScreen
import com.bisu.chickcare.frontend.screen.CoopManagementScreen
import com.bisu.chickcare.frontend.screen.BreedingRecordsScreen
import com.bisu.chickcare.frontend.screen.ReportsAnalyticsScreen
import com.bisu.chickcare.frontend.screen.RecentActivityScreen
import com.bisu.chickcare.frontend.screen.FarmInsightsScreen
import com.bisu.chickcare.ui.theme.ChickCareAppTheme

class MainActivity : ComponentActivity() {
    
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

                    LaunchedEffect(Unit) {
                        if (authViewModel.auth.currentUser != null) {
                            dashboardViewModel.updateActiveStatus()
                            navController.navigate("dashboard") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        }
                    }

                    NavHost(navController = navController, startDestination = "welcome") {
                        composable("welcome") { WelcomeScreen(navController) }
                        composable("login") { LoginScreen(navController) }
                        composable("signup") { SignupScreen(navController) }
                        composable("reset_password") { ResetPasswordScreen(navController) }
                        composable("action_tools") { ActionScreen(navController) }
                        composable("settings") { SettingsScreen(navController) }
                        composable("dashboard") {
                            MainScreen(navController = navController) { paddingValues ->
                                DashboardScreen(navController = navController)
                            }
                        }
                        composable("profile") { ProfileScreen(navController) }
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
                            )
                        ) { backStackEntry ->
                            val imgUri = backStackEntry.arguments?.getString("imageUri") ?: ""
                            val audUri = backStackEntry.arguments?.getString("audioUri") ?: ""
                            ProcessingScreen(
                                navController = navController,
                                imageUri = imgUri,
                                audioUri = audUri
                            )
                        }
                        composable("notifications") { NotificationsScreen(navController) }
                        composable("manage_profiles") { ManageProfilesScreen(navController) }
                        composable("friend_suggestions") { FriendSuggestionsScreen(navController) }
                        composable("farm_tips") { FarmTipsScreen(navController) }
                        composable("post_detection_history") { PostDetectionHistoryScreen(navController) }
                        composable("recently_deleted") { RecentlyDeletedScreen(navController) }
                        composable("favorites") { FavoritesScreen(navController) }
                        composable("archives") { ArchivesScreen(navController) }
                        composable("trash") { TrashScreen(navController) }

                        // New feature screens
                        composable("health_records") { HealthRecordsScreen(navController) }
                        composable("vaccination_schedule") { VaccinationScheduleScreen(navController) }
                        composable("feeding_schedule") { FeedingScheduleScreen(navController) }
                        composable("egg_production") { EggProductionTrackerScreen(navController) }
                        composable("growth_monitoring") { GrowthMonitoringScreen(navController) }
                        composable("expense_tracker") { ExpenseTrackerScreen(navController) }
                        composable("disease_database") { DiseaseDatabaseScreen(navController) }
                        composable("medications_log") { MedicationsLogScreen(navController) }
                        composable("coop_management") { CoopManagementScreen(navController) }
                        composable("breeding_records") { BreedingRecordsScreen(navController) }
                        composable("reports_analytics") { ReportsAnalyticsScreen(navController) }

                        composable("detection_history") {
                            MainScreen(navController = navController) { paddingValues ->
                                DetectionHistoryScreen(navController = navController, paddingValues = paddingValues)
                            }
                        }
                        composable("help_center") {
                            MainScreen(navController = navController) { paddingValues ->
                                HelpCenterScreen(paddingValues = paddingValues)
                            }
                        }
                        composable("recent_activity") { RecentActivityScreen(navController) }
                        composable("farm_insights") { FarmInsightsScreen(navController) }

                        composable(
                            route = "last_detection_detail" +
                                    "?entryId={entryId}" +
                                    "&timestamp={timestamp}" +
                                    "&location={location}" +
                                    "&imageUri={imageUri}" +
                                    "&result={result}" +
                                    "&isHealthy={isHealthy}" +
                                    "&confidence={confidence}",
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
                                }
                            )
                        ) { backStackEntry ->
                            val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
                            val timestamp = backStackEntry.arguments?.getLong("timestamp") ?: 0L
                            val location = backStackEntry.arguments?.getString("location")
                            val imageUri = backStackEntry.arguments?.getString("imageUri")
                            val result = backStackEntry.arguments?.getString("result") ?: ""
                            val isHealthy = backStackEntry.arguments?.getBoolean("isHealthy") ?: false
                            val confidence = backStackEntry.arguments?.getFloat("confidence") ?: 0f
                            
                            val entry = DetectionEntry(
                                id = entryId,
                                result = result,
                                isHealthy = isHealthy,
                                confidence = confidence,
                                imageUri = imageUri,
                                timestamp = timestamp,
                                location = location
                            )
                            
                            LastDetectionDetailScreen(
                                navController = navController,
                                entry = entry
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
                            val recommendations = if (recommendationsStr.isNotEmpty()) {
                                recommendationsStr.split("|")
                            } else {
                                emptyList()
                            }
                            
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
                                suggestions = backStackEntry.arguments?.getString("suggestions")?.split('|') ?: emptyList(),
                                imageUri = backStackEntry.arguments?.getString("imageUri"),
                                audioUri = backStackEntry.arguments?.getString("audioUri")
                            )
                        }
                    }
                }
            }
        }
    }
}
