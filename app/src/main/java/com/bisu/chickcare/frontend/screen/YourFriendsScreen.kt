package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.repository.FriendSuggestion
import com.bisu.chickcare.backend.service.NetworkConnectivityHelper
import com.bisu.chickcare.backend.viewmodels.FriendViewModel
import com.bisu.chickcare.frontend.components.ActiveStatusIndicator
import com.bisu.chickcare.frontend.components.OfflineIndicator
import com.bisu.chickcare.frontend.utils.ShareUtils
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourFriendsScreen(navController: NavController, viewUserId: String? = null) {
    val viewModel: FriendViewModel = viewModel()
    val friends by viewModel.friends.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val successMessage = stringResource(R.string.common_success)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Network connectivity monitoring
    val isOnline by NetworkConnectivityHelper.connectivityFlow(context)
        .collectAsState(initial = NetworkConnectivityHelper.isOnline(context))
    val isOffline = !isOnline
    
    // Fetch user name if viewing someone else's friends to customize title
    var targetUserName by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(viewUserId) {
        if (viewUserId != null) {
            // Fetch name for title
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val doc = db.collection("users").document(viewUserId).get().await()
                targetUserName = doc.getString("fullName")
            } catch (_: Exception) {}
        }
    }

    // Load friends
    LaunchedEffect(viewUserId) {
        viewModel.loadFriends(viewUserId)
    }

    // Reload friends when screen comes into focus
    LaunchedEffect(navController, viewUserId) {
        viewModel.loadFriends(viewUserId)
    }

    // Calculate active friends (active within last 5 minutes)
    val activeFriends = friends.filter { friend ->
        val now = System.currentTimeMillis()
        val lastActive = friend.lastActive
        (now - lastActive) < 5 * 60 * 1000 // 5 minutes
    }

    var expandedMenuIndex by remember { mutableStateOf<Int?>(null) }
    var showUnfriendDialog by remember { mutableStateOf<FriendSuggestion?>(null) }
    var showBlockDialog by remember { mutableStateOf<FriendSuggestion?>(null) }
    var showReportDialog by remember { mutableStateOf<FriendSuggestion?>(null) }
    var showMutualFriendsDialog by remember { mutableStateOf<FriendSuggestion?>(null) }
    var mutualFriends by remember { mutableStateOf<List<FriendSuggestion>>(emptyList()) }
    var isLoadingMutualFriends by remember { mutableStateOf(false) }

    // Show snackbar helper
    fun showMessage(message: String, isError: Boolean = false) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = if (isError) SnackbarDuration.Long else SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = if (data.visuals.message.contains(
                            "Failed",
                            ignoreCase = true
                        ) ||
                        data.visuals.message.contains("Error", ignoreCase = true)
                    ) {
                        Color(0xFFD32F2F)
                    } else {
                        Color(0xFF4CAF50)
                    }
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        targetUserName?.let { "$it's Friends" } ?: stringResource(R.string.friends_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
                .padding(innerPadding)
        ) {
                // Offline indicator
                OfflineIndicator(
                    isOffline = isOffline,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFD27D2D))
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // Status Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Friends Count Chip (not clickable)
                                FilterChip(
                                    selected = false,
                                    onClick = { /* Not clickable */ },
                                    label = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(stringResource(R.string.friends_chip_fmt, friends.size))
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = ThemeColorUtils.white(),
                                        labelColor = ThemeColorUtils.black()
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )

                                // Active Friends Chip (clickable)
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        navController.navigate("active_friends")
                                    },
                                    label = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(
                                                        Color(0xFF4CAF50),
                                                        CircleShape
                                                    )
                                            )
                                            Text(stringResource(R.string.friends_active_chip_fmt, activeFriends.size))
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = ThemeColorUtils.white(),
                                        labelColor = ThemeColorUtils.black()
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Friends List
                            if (friends.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.friends_empty),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = ThemeColorUtils.lightGray(Color.Gray)
                                    )
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(friends, key = { it.userId }) { friend ->
                                        FriendCard(
                                            friend = friend,
                                            onMoreClick = { index ->
                                                expandedMenuIndex =
                                                    if (expandedMenuIndex == index) null else index
                                            },
                                            expandedMenuIndex = expandedMenuIndex,
                                            menuIndex = friends.indexOf(friend),
                                            onViewProfile = {
                                                navController.navigate("view_profile?userId=${friend.userId}")
                                            },
                                            onUnfriend = {
                                                expandedMenuIndex = null
                                                showUnfriendDialog = friend
                                            },
                                            onBlock = {
                                                expandedMenuIndex = null
                                                showBlockDialog = friend
                                            },
                                            onShare = {
                                                expandedMenuIndex = null
                                                ShareUtils.shareProfile(
                                                    context,
                                                    friend.userId,
                                                    friend.fullName
                                                )
                                                showMessage(successMessage)
                                            },
                                            onMessage = {
                                                expandedMenuIndex = null
                                                // Navigate to messaging screen (create if doesn't exist)
                                                navController.navigate("chat?userId=${friend.userId}&userName=${friend.fullName}")
                                            },
                                            onViewMutualFriends = {
                                                expandedMenuIndex = null
                                                showMutualFriendsDialog = friend
                                                isLoadingMutualFriends = true
                                                viewModel.getMutualFriends(friend.userId) { success, mutuals, message ->
                                                    isLoadingMutualFriends = false
                                                    if (success) {
                                                        mutualFriends = mutuals
                                                    } else {
                                                        showMessage(
                                                            "Failed to load mutual friends: $message",
                                                            true
                                                        )
                                                    }
                                                }
                                            },
                                            onPin = {
                                                expandedMenuIndex = null
                                                viewModel.pinFriend(friend.userId) { success, message ->
                                                    if (success) {
                                                        showMessage(message)
                                                    } else {
                                                        showMessage(message, true)
                                                    }
                                                }
                                            },
                                            onUnpin = {
                                                expandedMenuIndex = null
                                                viewModel.unpinFriend(friend.userId) { success, message ->
                                                    if (success) {
                                                        showMessage(message)
                                                    } else {
                                                        showMessage(message, true)
                                                    }
                                                }
                                            },
                                            onMute = {
                                                expandedMenuIndex = null
                                                viewModel.muteFriendNotifications(friend.userId) { success, message ->
                                                    if (success) {
                                                        showMessage(message)
                                                    } else {
                                                        showMessage(message, true)
                                                    }
                                                }
                                            },
                                            onUnmute = {
                                                expandedMenuIndex = null
                                                viewModel.unmuteFriendNotifications(friend.userId) { success, message ->
                                                    if (success) {
                                                        showMessage(message)
                                                    } else {
                                                        showMessage(message, true)
                                                    }
                                                }
                                            },
                                            onReport = {
                                                expandedMenuIndex = null
                                                showReportDialog = friend
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Unfriend Confirmation Dialog
                showUnfriendDialog?.let { friend ->
                    AlertDialog(
                        onDismissRequest = { showUnfriendDialog = null },
                        containerColor = ThemeColorUtils.white(),
                        shape = RoundedCornerShape(4.dp),
                        title = { Text(stringResource(R.string.friends_dialog_unfriend_title, friend.fullName)) },
                        text = {
                            Text(stringResource(R.string.friends_dialog_unfriend_msg, friend.fullName))
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.unfriend(friend.userId) { success, message ->
                                        showUnfriendDialog = null
                                        if (success) {
                                            showMessage(message)
                                        } else {
                                            showMessage(message, true)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD32F2F)
                                ),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(stringResource(R.string.friends_menu_unfriend), color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showUnfriendDialog = null }) {
                                Text(stringResource(R.string.common_cancel), color = Color(0xFF191919))
                            }
                        }
                    )
                }

                // Block Confirmation Dialog
                showBlockDialog?.let { friend ->
                    AlertDialog(
                        onDismissRequest = { showBlockDialog = null },
                        containerColor = ThemeColorUtils.white(),
                        shape = RoundedCornerShape(4.dp),
                        title = { Text(stringResource(R.string.friends_dialog_block_title, friend.fullName)) },
                        text = {
                            Text(stringResource(R.string.friends_dialog_block_msg, friend.fullName))
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.blockUser(
                                        friend.userId,
                                        friend.fullName
                                    ) { success, message ->
                                        showBlockDialog = null
                                        if (success) {
                                            showMessage(message)
                                        } else {
                                            showMessage(message, true)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD32F2F)
                                ),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(stringResource(R.string.friends_menu_block), color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBlockDialog = null }) {
                                Text(stringResource(R.string.common_cancel), color = Color(0xFF191919))
                            }
                        }
                    )
                }

                // Report Dialog
                showReportDialog?.let { friend ->
                    ReportUserDialog(
                        friend = friend,
                        onDismiss = { showReportDialog = null },
                        onReport = { reason, description ->
                            viewModel.reportUser(
                                friend.userId,
                                friend.fullName,
                                reason,
                                description
                            ) { success, message ->
                                showReportDialog = null
                                if (success) {
                                    showMessage(message)
                                } else {
                                    showMessage(message, true)
                                }
                            }
                        }
                    )
                }

                // Mutual Friends Dialog
                showMutualFriendsDialog?.let { friend ->
                    MutualFriendsDialog(
                        friend = friend,
                        mutualFriends = mutualFriends,
                        isLoading = isLoadingMutualFriends,
                        onDismiss = { showMutualFriendsDialog = null },
                        onViewProfile = { userId ->
                            showMutualFriendsDialog = null
                            navController.navigate("view_profile?userId=$userId")
                        }
                    )
                }
            }
        }
    }


@Composable
private fun FriendCard(
    friend: FriendSuggestion,
    onMoreClick: (Int) -> Unit,
    expandedMenuIndex: Int?,
    menuIndex: Int,
    onViewProfile: () -> Unit,
    onUnfriend: () -> Unit,
    onBlock: () -> Unit,
    onShare: () -> Unit,
    onMessage: () -> Unit,
    onViewMutualFriends: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onMute: () -> Unit,
    onUnmute: () -> Unit,
    onReport: () -> Unit
) {
    val isPinned = friend.isPinned == true
    val isMuted = friend.notificationsMuted == true

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
                    model = friend.photoUrl ?: R.drawable.default_avatar,
                    contentDescription = stringResource(R.string.account_photo_desc),
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                ActiveStatusIndicator(
                    lastActiveTimestamp = friend.lastActive,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = friend.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = stringResource(R.string.friends_pinned_desc),
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFD27D2D)
                        )
                    }
                }
                Text(
                    text = friend.address?.takeIf { it.isNotBlank() } ?: stringResource(R.string.friends_place_placeholder),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFB0B0B0) else Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                if (friend.mutualFriendsCount > 0) {
                    val count = friend.mutualFriendsCount
                    Text(
                        text = if (count > 1) stringResource(R.string.friends_mutual_plural_fmt, count) else stringResource(R.string.friends_mutual_fmt, count),
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeColorUtils.lightGray(Color.Gray),
                        fontSize = 12.sp
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { onMoreClick(menuIndex) }
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.friends_more_desc),
                        tint = ThemeColorUtils.black()
                    )
                }

                DropdownMenu(
                    expanded = expandedMenuIndex == menuIndex,
                    onDismissRequest = { onMoreClick(menuIndex) },
                    containerColor = ThemeColorUtils.white()
                ) {
                    var showMoreOptions by remember { mutableStateOf(false) }

                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.friends_menu_view_profile)) },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        onClick = {
                            onViewProfile()
                            onMoreClick(menuIndex)
                            showMoreOptions = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.friends_menu_message)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Message, null) },
                        onClick = {
                            onMessage()
                            onMoreClick(menuIndex)
                            showMoreOptions = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.friends_menu_share)) },
                        leadingIcon = { Icon(Icons.Default.Share, null) },
                        onClick = {
                            onShare()
                            onMoreClick(menuIndex)
                            showMoreOptions = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.friends_menu_mutual)) },
                        leadingIcon = { Icon(Icons.Default.Group, null) },
                        onClick = {
                            onViewMutualFriends()
                            onMoreClick(menuIndex)
                            showMoreOptions = false
                        }
                    )

                    HorizontalDivider()
                    
                    // See more / See less toggle
                    DropdownMenuItem(
                        text = { 
                            Text(
                                if (showMoreOptions) "See less" else "See more...", // Or keep "See more..." if user prefers, but "See less" is better UI
                                fontWeight = FontWeight.SemiBold 
                            ) 
                        },
                        leadingIcon = { 
                            Icon(
                                if (showMoreOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore, // Use arrows to indicate direction
                                null 
                            ) 
                        },
                        onClick = { showMoreOptions = !showMoreOptions }
                    )

                    if (showMoreOptions) {
                        DropdownMenuItem(
                            text = { Text(if (isPinned) stringResource(R.string.friends_menu_unpin) else stringResource(R.string.friends_menu_pin)) },
                            leadingIcon = { Icon(Icons.Default.PushPin, null) },
                            onClick = {
                                if (isPinned) onUnpin() else onPin()
                                onMoreClick(menuIndex)
                                showMoreOptions = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isMuted) stringResource(R.string.friends_menu_unmute) else stringResource(R.string.friends_menu_unmute)) },
                            leadingIcon = {
                                Icon(
                                    if (isMuted) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                    null
                                )
                            },
                            onClick = {
                                if (isMuted) onUnmute() else onMute()
                                onMoreClick(menuIndex)
                                showMoreOptions = false
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.friends_menu_unfriend), color = Color(0xFFFF6B6B)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    tint = Color(0xFFFF6B6B)
                                )
                            },
                            onClick = {
                                onUnfriend()
                                onMoreClick(menuIndex)
                                showMoreOptions = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.friends_menu_block), color = Color(0xFFFF6B6B)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    tint = Color(0xFFFF6B6B)
                                )
                            },
                            onClick = {
                                onBlock()
                                onMoreClick(menuIndex)
                                showMoreOptions = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.friends_menu_report), color = Color(0xFFFF6B6B)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Report,
                                    null,
                                    tint = Color(0xFFFF6B6B)
                                )
                            },
                            onClick = {
                                onReport()
                                onMoreClick(menuIndex)
                                showMoreOptions = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportUserDialog(
    friend: FriendSuggestion,
    onDismiss: () -> Unit,
    onReport: (String, String?) -> Unit
) {
    var selectedReason by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val reasons = listOf(
        stringResource(R.string.friends_report_reason_spam),
        stringResource(R.string.friends_report_reason_harass),
        stringResource(R.string.friends_report_reason_inapp),
        stringResource(R.string.friends_report_reason_fake),
        stringResource(R.string.friends_report_reason_other)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ThemeColorUtils.white(),
        shape = RoundedCornerShape(4.dp),
        title = { Text(stringResource(R.string.friends_dialog_report_title, friend.fullName)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.friends_dialog_report_reason))

                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFA12727))
                        )
                        Text(
                            text = reason,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                if (selectedReason.isNotEmpty()) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.friends_dialog_report_details)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onReport(selectedReason, description.takeIf { it.isNotEmpty() })
                },
                enabled = selectedReason.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F)
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Report", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF191919))
            }
        }
    )
}

@Composable
private fun MutualFriendsDialog(
    friend: FriendSuggestion,
    mutualFriends: List<FriendSuggestion>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onViewProfile: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = ThemeColorUtils.white()
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mutual Friends with ${friend.fullName}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFD27D2D))
                    }
                } else if (mutualFriends.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No mutual friends",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ThemeColorUtils.lightGray(Color.Gray)
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(mutualFriends, key = { it.userId }) { mutualFriend ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onViewProfile(mutualFriend.userId) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = mutualFriend.photoUrl
                                        ?: R.drawable.default_avatar,
                                    contentDescription = "Profile",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = mutualFriend.fullName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
