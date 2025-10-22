package com.bisu.chickcare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bisu.chickcare.frontend.screen.*
import com.bisu.chickcare.ui.theme.ChickCareAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChickCareAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "welcome") {
                        // ... (other composable routes remain the same)
                        composable("welcome") { WelcomeScreen(navController) }
                        composable("login") { LoginScreen(navController) }
                        composable("signup") { SignupScreen(navController) }
                        composable("reset_password") { ResetPasswordScreen(navController) }
                        composable("action_tools") { ActionScreen(navController) }
                        composable("settings") { SettingsScreen(navController) }
                        composable("dashboard") { DashboardScreen(navController) }
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
                        composable("profile") { ProfileScreen(navController) }
                        composable("camera") { CameraScreen(navController) }

                        composable(
                            route = "detection_result?imageUri={imageUri}&audioUri={audioUri}&status={status}&suggestions={suggestions}",
                            arguments = listOf(
                                navArgument("imageUri") { type = NavType.StringType; nullable = true },
                                navArgument("audioUri") { type = NavType.StringType; nullable = true },
                                navArgument("status") { type = NavType.StringType },
                                navArgument("suggestions") { type = NavType.StringType } // Suggestions as a single string
                            )
                        ) { backStackEntry ->
                            ResultScreen(
                                navController = navController,
                                imageUri = backStackEntry.arguments?.getString("imageUri"),
                                audioUri = backStackEntry.arguments?.getString("audioUri"),
                                status = backStackEntry.arguments?.getString("status") ?: "Unknown",
                                suggestions = backStackEntry.arguments?.getString("suggestions")?.split('|') ?: emptyList()
                            )
                        }
                    }
                }
            }
        }
    }
}
