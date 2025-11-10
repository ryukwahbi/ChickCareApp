package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.repository.FriendSuggestion
import com.bisu.chickcare.backend.viewmodels.FriendViewModel
import com.bisu.chickcare.frontend.components.ActiveStatusIndicator
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendSuggestionsScreen(navController: NavController) {
    val viewModel: FriendViewModel = viewModel()
    val suggestions by viewModel.suggestions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val requestStatusMap = remember { mutableStateMapOf<String, String?>() }
    
    LaunchedEffect(Unit) {
        viewModel.loadFriendSuggestions()
        viewModel.loadPendingFriendRequests()
        // Use callback version to ensure it's called
        viewModel.getPendingFriendRequests { success, requests, message ->
            if (success) {
                android.util.Log.d("FriendSuggestionsScreen", "Loaded ${requests.size} pending friend requests via callback")
            }
        }
    }
    
    // Reload suggestions when pending requests change
    LaunchedEffect(pendingRequests.size) {
        if (pendingRequests.isNotEmpty()) {
            android.util.Log.d("FriendSuggestionsScreen", "Pending requests updated: ${pendingRequests.size}")
            // Refresh suggestions when pending requests change
            viewModel.loadFriendSuggestions()
        }
    }
    LaunchedEffect(suggestions) {
        suggestions.forEach { suggestion ->
            if (!requestStatusMap.containsKey(suggestion.userId)) {
                viewModel.checkRequestStatus(suggestion.userId) { status ->
                    requestStatusMap[suggestion.userId] = status
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Friend Suggestion")
                        if (pendingRequests.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "(${pendingRequests.size} pending)",
                                fontSize = 12.sp,
                                color = ThemeColorUtils.lightGray(Color.Gray),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeColorUtils.white(),
                    titleContentColor = ThemeColorUtils.black()
                )
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (suggestions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No friend suggestions available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ThemeColorUtils.lightGray(Color.Gray)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(suggestions) { suggestion ->
                    FriendSuggestionItem(
                        suggestion = suggestion,
                        requestStatus = requestStatusMap[suggestion.userId],
                        onAddFriend = {
                            viewModel.sendFriendRequest(
                                suggestion.userId,
                                suggestion.fullName
                            ) { success, message ->
                                if (success) {
                                    requestStatusMap[suggestion.userId] = "pending"
                                    viewModel.loadFriendSuggestions()
                                }
                            }
                        },
                        onStatusRefresh = {
                            viewModel.checkRequestStatus(suggestion.userId) { status ->
                                requestStatusMap[suggestion.userId] = status
                                if (status == "declined" || status == "accepted") {
                                    viewModel.loadFriendSuggestions()
                                }
                            }
                        },
                        onViewProfile = {
                            navController.navigate("view_profile?userId=${suggestion.userId}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FriendSuggestionItem(
    suggestion: FriendSuggestion,
    requestStatus: String?,
    onAddFriend: () -> Unit,
    onStatusRefresh: () -> Unit,
    onViewProfile: () -> Unit
) {
    LaunchedEffect(suggestion.userId, requestStatus) {
        if (requestStatus == "pending") {
            // Poll every 3 seconds while pending
            repeat(Int.MAX_VALUE) {
                kotlinx.coroutines.delay(3000)
                onStatusRefresh()
            }
        }
    }
    
    val buttonText = when (requestStatus) {
        "pending" -> "Requested"
        "declined" -> "Add"
        "accepted" -> "Friends"
        else -> "Add"
    }
    
    val buttonColor = when (requestStatus) {
        "pending" -> ThemeColorUtils.lightGray(Color.Gray)
        "declined" -> Color(0xFF4CAF50)
        "accepted" -> Color.Blue
        else -> Color(0xFF4CAF50)
    }
    
    val isButtonEnabled = requestStatus != "pending" && requestStatus != "accepted"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
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
                modifier = Modifier.clickable(onClick = onViewProfile)
            ) {
                AsyncImage(
                    model = suggestion.photoUrl ?: R.drawable.default_avatar,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                ActiveStatusIndicator(
                    lastActiveTimestamp = suggestion.lastActive,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onViewProfile)
            ) {
                Text(
                    text = suggestion.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (suggestion.mutualFriendsCount > 0) {
                    Text(
                        text = "${suggestion.mutualFriendsCount} mutual friend${if (suggestion.mutualFriendsCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeColorUtils.lightGray(Color.Gray)
                    )
                }
            }
            
            Button(
                onClick = onAddFriend,
                enabled = isButtonEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    disabledContainerColor = buttonColor.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = buttonText, 
                    color = ThemeColorUtils.white(),
                    fontWeight = if (requestStatus == "pending") FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}
