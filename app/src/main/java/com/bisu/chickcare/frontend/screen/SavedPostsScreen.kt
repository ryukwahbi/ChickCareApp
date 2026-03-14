package com.bisu.chickcare.frontend.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.backend.repository.PostRepository
import com.bisu.chickcare.backend.viewmodels.AuthViewModel
import com.bisu.chickcare.frontend.components.TimelinePostItem
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPostsScreen(navController: NavController) {
    val authViewModel: AuthViewModel = viewModel()
    val context = LocalContext.current
    val userId = authViewModel.getCurrentUserId(context) ?: ""
    val postRepository = remember { PostRepository() }
    val savedPostsFlow = remember(userId) {
        postRepository.getSavedPosts(userId)
    }
    val savedPosts by savedPostsFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Saved Posts",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFF141617) else Color.White,
                    titleContentColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
        ) {
            if (savedPosts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "No saved posts",
                            tint = ThemeColorUtils.lightGray(Color.Gray),
                            modifier = Modifier.padding(16.dp)
                        )
                        Text(
                            text = "No saved posts yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = ThemeColorUtils.lightGray(Color.Gray)
                        )
                        Text(
                            text = "Save posts from your timeline or from other users.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThemeColorUtils.lightGray(Color.Gray),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(savedPosts, key = { "${it.userId}_${it.id}" }) { post ->
                        // Determine original user ID (if saved from other user, check savedPosts collection)
                        val originalUserId = post.userId
                        val isOwnPost = originalUserId == userId
                        
                        // For saved posts, create a version with isSaved = true so the menu shows correctly
                        val savedPost = post.copy(isSaved = true)
                        
                        TimelinePostItem(
                            post = savedPost,
                            onDelete = { postId ->
                                // Only allow deletion if it's own post
                                if (isOwnPost) {
                                    scope.launch {
                                        try {
                                            postRepository.deletePost(userId, postId)
                                        } catch (e: Exception) {
                                            Log.e("SavedPostsScreen", "Error deleting post: ${e.message}")
                                        }
                                    }
                                }
                            },
                            onChangeAudience = { postId, newVisibility ->
                                // Only allow audience change if it's own post
                                if (isOwnPost) {
                                    scope.launch {
                                        try {
                                            postRepository.updatePostVisibility(userId, postId, newVisibility)
                                        } catch (e: Exception) {
                                            Log.e("SavedPostsScreen", "Error changing audience: ${e.message}")
                                        }
                                    }
                                }
                            },
                            onSavePost = { postId ->
                                scope.launch {
                                    try {
                                        val currentUserId = authViewModel.getCurrentUserId(context) ?: return@launch
                                        // All posts in this screen are saved, so always unsave
                                        postRepository.unsavePost(currentUserId, originalUserId, postId)
                                    } catch (e: Exception) {
                                        Log.e("SavedPostsScreen", "Error unsaving post: ${e.message}")
                                    }
                                }
                            },
                            onReaction = { postId, postOwnerId, reactionType ->
                                scope.launch {
                                    try {
                                        val currentUserId = authViewModel.getCurrentUserId(context) ?: return@launch
                                        postRepository.toggleReaction(postOwnerId, postId, currentUserId, reactionType)
                                    } catch (e: Exception) {
                                        Log.e("SavedPostsScreen", "Error toggling reaction: ${e.message}")
                                    }
                                }
                            },
                            onCommentClick = { postId, postOwnerId ->
                                navController.navigate("comments/$postId/$postOwnerId")
                            },
                            userId = userId
                        )
                    }
                }
            }
        }
    }
}
