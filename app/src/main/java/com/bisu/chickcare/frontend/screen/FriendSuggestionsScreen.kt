package com.bisu.chickcare.frontend.screen

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendSuggestionsScreen(navController: NavController) {
    val viewModel: FriendViewModel = viewModel()
    val suggestions by viewModel.suggestions.collectAsState()
    val friends by viewModel.friends.collectAsState() // Get friends list to filter them out
    val isLoading by viewModel.isLoading.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val hiddenUserIds by viewModel.hiddenUserIds.collectAsState()
    val requestStatusMap = remember { mutableStateMapOf<String, String?>() }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    
    // Filter out users who are already friends
    val friendIds = friends.map { it.userId }.toSet()
    val baseSuggestions = suggestions.filter { it.userId !in friendIds }
    
    // Filter logic:
    // If searching: Show matching name (even if hidden).
    // If NOT searching: Exclude hidden users.
    val filteredSuggestions = if (searchQuery.isNotEmpty()) {
        baseSuggestions.filter { 
            it.fullName.contains(searchQuery, ignoreCase = true) 
        }
    } else {
        baseSuggestions.filter { 
            it.userId !in hiddenUserIds 
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadFriends() // Load friends first to ensure they're excluded
        viewModel.loadFriendSuggestions()
        viewModel.loadPendingFriendRequests()
        // Use callback version to ensure it's called
        viewModel.getPendingFriendRequests { success, requests, _ ->
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
            viewModel.loadFriends() // Reload friends first
            viewModel.loadFriendSuggestions()
        }
    }
    LaunchedEffect(filteredSuggestions) {
        filteredSuggestions.forEach { suggestion ->
            if (!requestStatusMap.containsKey(suggestion.userId)) {
                viewModel.checkRequestStatus(suggestion.userId) { status ->
                    requestStatusMap[suggestion.userId] = status
                }
            }
        }
    }
    
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (data.visuals.message.contains("Failed", ignoreCase = true)) {
                        Color(0xFFD32F2F) // Red for errors
                    } else {
                        Color(0xFF4CAF50) // Green for success
                    }
                )
            }
        },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
                .padding(innerPadding)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                }
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { 
                    Text(
                        "Search users...",
                        color = Color(0xFF666666) // Using a neutral gray for placeholder to likely match default behavior or close to it
                    ) 
                },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Search, 
                        contentDescription = "Search"
                        // Tint removed to follow DetectionHistory style which often defaults or sets specific block in colors
                    ) 
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    cursorColor = Color.Black,
                    focusedBorderColor = Color.Black,
                    unfocusedBorderColor = Color.Black,
                    focusedLeadingIconColor = Color.Black,
                    unfocusedLeadingIconColor = Color.Black,
                    focusedPlaceholderColor = Color(0xFF666666),
                    unfocusedPlaceholderColor = Color(0xFF666666)
                )
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredSuggestions.isEmpty() && pendingRequests.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No users found" else "No friend suggestions available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ThemeColorUtils.lightGray(Color.Gray)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Pending Friend Requests Section
                    if (pendingRequests.isNotEmpty()) {
                        item {
                            Text(
                                text = "Friend Requests",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemeColorUtils.black(),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(pendingRequests) { request ->
                            PendingFriendRequestItem(
                                request = request,
                                onAccept = {
                                    viewModel.acceptFriendRequest(
                                        requestId = request.id,
                                        friendUserId = request.fromUserId,
                                        friendName = request.fromUserName,
                                        callback = { success, message ->
                                            if (success) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("You are now friends with ${request.fromUserName}")
                                                }
                                            } else {
                                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                },
                                onDecline = {
                                    viewModel.declineFriendRequest(request.id) { success, message ->
                                        if (success) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Friend request declined")
                                            }
                                        } else {
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onViewProfile = {
                                    navController.navigate("view_profile?userId=${request.fromUserId}")
                                }
                            )
                        }
                        // Separator between pending requests and suggestions
                        if (filteredSuggestions.isNotEmpty()) {
                            item {
                                androidx.compose.material3.HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = ThemeColorUtils.lightGray(Color.LightGray)
                                )
                                Text(
                                    text = "People You May Know",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeColorUtils.black(),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }
                    
                    items(filteredSuggestions) { suggestion ->
                        FriendSuggestionItem(
                            suggestion = suggestion,
                            requestStatus = requestStatusMap[suggestion.userId],
                            onAddFriend = {
                                val previousStatus = requestStatusMap[suggestion.userId]
                                requestStatusMap[suggestion.userId] = "pending"
                                
                                viewModel.sendFriendRequest(
                                    suggestion.userId,
                                    suggestion.fullName
                                ) { success, message ->
                                    if (success) {
                                        viewModel.loadFriendSuggestions()
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Sent a friend request to ${suggestion.fullName}",
                                                duration = androidx.compose.material3.SnackbarDuration.Short
                                            )
                                        }
                                    } else {
                                        requestStatusMap[suggestion.userId] = previousStatus
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Failed to send friend request: $message",
                                                duration = androidx.compose.material3.SnackbarDuration.Long
                                            )
                                        }
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
                            },
                            onCancelRequest = {
                                val previousStatus = requestStatusMap[suggestion.userId]
                                requestStatusMap[suggestion.userId] = null
                                
                                viewModel.cancelFriendRequest(suggestion.userId) { success, message ->
                                    if (success) {
                                        viewModel.loadFriendSuggestions()
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Friend request cancelled")
                                        }
                                    } else {
                                        requestStatusMap[suggestion.userId] = previousStatus
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onHide = {
                                viewModel.hideUser(suggestion.userId)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Suggestion hidden")
                                }
                            },
                            onBlock = {
                                viewModel.blockUser(suggestion.userId, suggestion.fullName) { success, message ->
                                    if (success) {
                                        viewModel.loadFriendSuggestions()
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Blocked ${suggestion.fullName}")
                                        }
                                    } else {
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
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
    onViewProfile: () -> Unit,
    onCancelRequest: () -> Unit,
    onHide: () -> Unit,
    onBlock: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(suggestion.userId, requestStatus) {
        if (requestStatus == "pending") {
            // Poll every 3 seconds while pending
            repeat(Int.MAX_VALUE) {
                kotlinx.coroutines.delay(3000)
                onStatusRefresh()
            }
        }
    }
    
    // If status is "accepted", this user should not appear in suggestions
    // Filter them out at the UI level as a safety measure
    if (requestStatus == "accepted") {
        return // Don't render this item if already friends
    }
    
    val buttonText = when (requestStatus) {
        "pending" -> "Cancel"
        "declined" -> "Add"
        else -> "Add"
    }
    
    val buttonColor = when (requestStatus) {
        "pending" -> ThemeColorUtils.lightGray(Color.Gray)
        "declined" -> Color(0xFF4CAF50)
        else -> Color(0xFF4CAF50)
    }
    
    val isButtonEnabled = true // Always enabled to allow cancellation or adding
    
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
            // More Options Menu (Left aligned)
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = ThemeColorUtils.darkGray(Color.Gray)
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = ThemeColorUtils.white()
                ) {
                    DropdownMenuItem(
                        text = { Text("Hide this Person", color = ThemeColorUtils.black()) },
                        onClick = {
                            showMenu = false
                            onHide()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Block this Person", color = Color(0xFFD32F2F)) }, // Red for block
                        onClick = {
                            showMenu = false
                            onBlock()
                        }
                    )
                }
            }

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
                onClick = {
                    if (requestStatus == "pending") {
                        onCancelRequest()
                    } else {
                        onAddFriend()
                    }
                },
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

@Composable
fun PendingFriendRequestItem(
    request: com.bisu.chickcare.backend.repository.FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onViewProfile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFD9DADB)
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
                    model = R.drawable.default_avatar,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onViewProfile)
            ) {
                Text(
                    text = request.fromUserName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Wants to be your friend",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeColorUtils.lightGray(Color.Gray)
                )
            }
            
            // Accept Button
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50) // Green
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.size(width = 70.dp, height = 36.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text(
                    text = "Accept",
                    color = ThemeColorUtils.white(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Decline Button
            Button(
                onClick = onDecline,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE57373) // Light Red
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.size(width = 70.dp, height = 36.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text(
                    text = "Decline",
                    color = ThemeColorUtils.white(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
