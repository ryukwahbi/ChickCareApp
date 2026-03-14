package com.bisu.chickcare.frontend.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.SpeakerNotes
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.repository.NotificationEntry
import com.bisu.chickcare.backend.repository.NotificationType
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel
import com.bisu.chickcare.backend.viewmodels.FriendViewModel
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.bisu.chickcare.frontend.components.FullscreenImageViewer
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    viewModel: DashboardViewModel = viewModel()
) {
    val friendViewModel: FriendViewModel = viewModel()
    val notifications by viewModel.notifications.collectAsState()
    val pendingRequests by friendViewModel.pendingRequests.collectAsState()
    val context = LocalContext.current

    // State for interactions
    var showDeleteMessage by remember { mutableStateOf(false) }
    var selectedDetection by remember { mutableStateOf<NotificationEntry?>(null) }

    // Auto-hide delete message
    LaunchedEffect(showDeleteMessage) {
        if (showDeleteMessage) {
            delay(3000)
            showDeleteMessage = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.markAllNotificationsAsRead()
        friendViewModel.loadPendingFriendRequests()
        // Also use callback version to ensure it's called
        friendViewModel.getPendingFriendRequests { success, requests, _ ->
            if (success) {
                android.util.Log.d("NotificationsScreen", "Loaded ${requests.size} pending friend requests via callback")
            }
        }
    }

    // Reload pending requests when they change
    LaunchedEffect(pendingRequests.size) {
        if (pendingRequests.isNotEmpty()) {
            android.util.Log.d("NotificationsScreen", "Pending requests updated: ${pendingRequests.size}")
        }
    }

    val unreadCount = notifications.count { !it.isRead }
    val pendingRequestsCount = pendingRequests.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            androidx.compose.ui.res.stringResource(R.string.notifications_title),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ThemeColorUtils.black()
                        )
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color.Red, CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    if (unreadCount > 10) "10+" else "$unreadCount",
                                    color = ThemeColorUtils.white(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        // Display pending friend requests count if any
                        if (pendingRequestsCount > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                androidx.compose.ui.res.stringResource(R.string.notifications_pending_fmt, pendingRequestsCount),
                                fontSize = 12.sp,
                                color = ThemeColorUtils.darkGray(Color(0xFF666666)),
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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = androidx.compose.ui.res.stringResource(R.string.back),
                            tint = ThemeColorUtils.black()
                        )
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = { viewModel.markAllNotificationsAsRead() },
                            colors = ButtonDefaults.textButtonColors(contentColor = ThemeColorUtils.black())
                        ) {
                            Text(androidx.compose.ui.res.stringResource(R.string.notifications_mark_read), fontSize = 12.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeColorUtils.white(),
                    titleContentColor = ThemeColorUtils.black()
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
            ) {
                HorizontalDivider(
                    color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)),
                    thickness = 1.dp
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (notifications.isEmpty()) {
                        EmptyNotificationsState()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(notifications, key = { it.id }) { notification ->
                                NotificationItem(
                                    notification = notification,
                                    onRead = {
                                        viewModel.markNotificationAsRead(notification.id)
                                        
                                        // Handle specific notification types
                                        when (notification.type) {
                                            NotificationType.DATA_DELETED.name -> {
                                                showDeleteMessage = true
                                            }
                                            NotificationType.DETECTION_RESULT.name -> {
                                                selectedDetection = notification
                                            }
                                            // Social notifications - navigate to post/comments
                                            NotificationType.REACTION.name, 
                                            NotificationType.COMMENT.name,
                                            NotificationType.NEW_POST.name -> {
                                                // Navigate to comments screen to view the post
                                                val postId = notification.postId
                                                val postOwnerId = notification.postOwnerId
                                                if (!postId.isNullOrEmpty() && !postOwnerId.isNullOrEmpty()) {
                                                    navController.navigate("comments/$postId/$postOwnerId")
                                                } else {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Post not found",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                            // Follow and Friend Accept - navigate to user profile
                                            NotificationType.FOLLOW.name,
                                            NotificationType.FRIEND_ACCEPT.name -> {
                                                val senderId = notification.senderId
                                                if (!senderId.isNullOrEmpty()) {
                                                    navController.navigate("view_profile?userId=$senderId")
                                                }
                                            }
                                            else -> {
                                                // Other notification types - just mark as read
                                            }
                                        }
                                    },
                                    onAccept = {
                                        // Accept friend request
                                        if (notification.type == NotificationType.FRIEND_REQUEST.name && notification.senderId != null) {
                                            val requestId = notification.relatedEntityId ?: ""
                                            val friendUserId = notification.senderId
                                            val friendName = notification.senderName ?: "User"
                                            
                                            val notificationRepo = com.bisu.chickcare.backend.repository.NotificationRepository()
                                            friendViewModel.acceptFriendRequest(
                                                requestId = requestId,
                                                friendUserId = friendUserId,
                                                friendName = friendName,
                                                callback = { success: Boolean, message: String ->
                                                    if (success) {
                                                        // Delete the notification instead of just marking as read
                                                        viewModel.deleteNotification(notification.id)
                                                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                notificationRepository = notificationRepo
                                            )
                                        }
                                    },
                                    onDecline = {
                                        // Decline friend request
                                        if (notification.type == NotificationType.FRIEND_REQUEST.name) {
                                            val requestId = notification.relatedEntityId ?: ""
                                            
                                            friendViewModel.declineFriendRequest(requestId) { success, message ->
                                                if (success) {
                                                    // Delete the notification instead of just marking as read
                                                    viewModel.deleteNotification(notification.id)
                                                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                                                } else {
                                                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Delete Message Overlay
            AnimatedVisibility(
                visible = showDeleteMessage,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
            ) {
                 Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF333333).copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.notifications_delete_msg),
                        color = Color.White,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Detection Details Dialog
            if (selectedDetection != null) {
                DetectionDetailsDialog(
                    notification = selectedDetection!!,
                    onDismiss = { selectedDetection = null }
                )
            }
        }
    }
}

@Composable
fun DetectionDetailsDialog(
    notification: NotificationEntry,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var showFullImage by remember { mutableStateOf(false) }

    // Image URI resolution (move up for accessibility)
    val imageUri = notification.imageUri
    val cloudImageUri = notification.cloudImageUri
    val imageModel = com.bisu.chickcare.frontend.utils.getAccessibleUri(context, imageUri, cloudImageUri)

    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        isPlaying = false
    }

    DisposableEffect(Unit) {
        onDispose { stopPlayback() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = ThemeColorUtils.white()
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.notif_detail_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black()
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = androidx.compose.ui.res.stringResource(R.string.common_cancel),
                            tint = ThemeColorUtils.darkGray(Color(0xFF666666))
                        )
                    }
                }
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = ThemeColorUtils.lightGray(Color.LightGray)
                )
                
                // Content
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Message / Result
                    Column {
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.notif_detail_result),
                            style = MaterialTheme.typography.labelMedium,
                            color = ThemeColorUtils.darkGray(Color(0xFF666666))
                        )
                        Text(
                            text = notification.message,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = ThemeColorUtils.black()
                        )
                    }

                    // Image preview (if available) - matches the mock card
                    if (imageModel != null) {
                        val imageRequest = remember(imageModel) {
                            ImageRequest.Builder(context)
                                .data(imageModel)
                                .crossfade(true)
                                .build()
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .clickable { showFullImage = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color(0xFFF3F4F6))),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            AsyncImage(
                                model = imageRequest,
                                contentDescription = "Detection image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Play Audio button (if available)
                    val audioUri = notification.audioUri
                    val cloudAudioUri = notification.cloudAudioUri
                    val audioModel = com.bisu.chickcare.frontend.utils.getAccessibleUri(context, audioUri, cloudAudioUri)

                    if (audioModel != null) {
                        Button(
                            onClick = {
                                if (isPlaying) {
                                    stopPlayback()
                                    return@Button
                                }
                                try {
                                    stopPlayback()
                                    
                                    val player = android.media.MediaPlayer()
                                    
                                    if (audioModel is android.net.Uri) {
                                         player.setDataSource(context, audioModel)
                                    } else if (audioModel is String) {
                                         player.setDataSource(audioModel)
                                    }
                                    
                                    player.prepareAsync()
                                    player.setOnPreparedListener { 
                                        it.start() 
                                        mediaPlayer = it
                                        isPlaying = true
                                    }
                                    player.setOnCompletionListener { 
                                        stopPlayback() 
                                    }
                                    player.setOnErrorListener { _, what, extra ->
                                         android.util.Log.e("DetectionDetailsDialog", "MediaPlayer error: $what, $extra")
                                         stopPlayback()
                                         false
                                    }

                                } catch (e: Exception) {
                                    stopPlayback()
                                    android.widget.Toast
                                        .makeText(context, "Error playing audio: ${e.message}", android.widget.Toast.LENGTH_SHORT)
                                        .show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (ThemeViewModel.isDarkMode) Color(0xFF333333) else ThemeColorUtils.surface(Color(0xFFE5E2DE)),
                                contentColor = ThemeColorUtils.black()
                            )
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Stop audio" else "Play audio",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPlaying) "Stop Audio" else "Play Audio",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    // Date
                    Column {
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.notif_detail_date),
                            style = MaterialTheme.typography.labelMedium,
                            color = ThemeColorUtils.darkGray(Color(0xFF666666))
                        )
                        Text(
                            text = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(notification.timestamp)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThemeColorUtils.black()
                        )
                    }
                }
            }
        }
    }

    if (showFullImage && imageModel != null) {
        FullscreenImageViewer(
            images = listOf(imageModel.toString()),
            onDismiss = { showFullImage = false }
        )
    }
}

@Composable
fun NotificationItem(
    notification: NotificationEntry,
    onRead: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val notificationType = try {
        NotificationType.valueOf(notification.type)
    } catch (_: Exception) {
        NotificationType.DETECTION_RESULT
    }
    
    val (icon, iconColor, backgroundColor) = getNotificationStyle(notificationType)
    val isUnread = !notification.isRead
    val shape = RoundedCornerShape(12.dp)
    val elevation = if (isUnread) 5.dp else 2.dp
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (ThemeViewModel.isDarkMode) {
                    Modifier.shadow(
                        elevation = elevation,
                        shape = shape,
                        spotColor = Color.White,
                        ambientColor = Color.White.copy(alpha = 0.5f)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(enabled = notificationType != NotificationType.ANNOUNCEMENT) { onRead() },
        border = BorderStroke(
            width = 1.dp,
            color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C))
        ),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.surface(Color(0xFFE5E2DE)),
            contentColor = ThemeColorUtils.black()
        ),
        shape = shape,
        elevation = if (ThemeViewModel.isDarkMode) {
            CardDefaults.cardElevation(defaultElevation = 0.dp)
        } else {
            CardDefaults.cardElevation(
                defaultElevation = elevation,
                pressedElevation = 8.dp,
                hoveredElevation = 6.dp,
                focusedElevation = 6.dp
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon with background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(backgroundColor, CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (notificationType == NotificationType.FRIEND_REQUEST) {
                    if (!notification.senderPhotoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = notification.senderPhotoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.default_avatar),
                            contentDescription = "Default Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else if (notificationType == NotificationType.ANNOUNCEMENT) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.notes_pic),
                        contentDescription = "Announcement",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp), // Add padding to fit nicely in circle
                        contentScale = ContentScale.Fit
                    )
                } else if (notificationType == NotificationType.DETECTION_RESULT) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.bell_pic),
                        contentDescription = "Detection Result",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp), // Adjust padding for bell icon
                        contentScale = ContentScale.Fit
                    )
                } else if (notificationType == NotificationType.PROFILE_UPDATE) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.profile_notify),
                        contentDescription = "Profile Update",
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                        contentScale = ContentScale.Fit
                    )
                } else if (notificationType == NotificationType.DATA_DELETED) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.deleted_notify),
                        contentDescription = "Data Deleted",
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                        contentScale = ContentScale.Fit
                    )
                } else if (notificationType == NotificationType.REACTION) {
                     androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.reaction_notify),
                        contentDescription = "Reaction",
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                        contentScale = ContentScale.Fit
                    )
                } else if (notificationType == NotificationType.FOLLOW) {
                     androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.follower_notify),
                        contentDescription = "New Follower",
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                        contentScale = ContentScale.Fit
                    )
                } else if (notificationType == NotificationType.FRIEND_ACCEPT) {
                     androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.request_accepted_notify),
                        contentDescription = "Friend Request Accepted",
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Timestamp Calculation (Moved up)
                val now = System.currentTimeMillis()
                val diff = now - notification.timestamp
                val timeText = when {
                    diff < 60000 -> androidx.compose.ui.res.stringResource(R.string.time_just_now)
                    diff < 3600000 -> androidx.compose.ui.res.stringResource(R.string.time_min_ago, diff / 60000)
                    diff < 86400000 -> androidx.compose.ui.res.stringResource(R.string.time_hour_ago, diff / 3600000)
                    diff < 604800000 -> androidx.compose.ui.res.stringResource(R.string.time_day_ago, diff / 86400000)
                    else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(notification.timestamp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // Title Row with Timestamp
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when {
                                    // For friend requests, show sender's name instead of "New Friend Request"
                                    notificationType == NotificationType.FRIEND_REQUEST && !notification.senderName.isNullOrEmpty() -> {
                                        notification.senderName
                                    }
                                    // Otherwise use the title or default title
                                    else -> notification.title.ifEmpty { androidx.compose.ui.res.stringResource(getNotificationTitleResId(notificationType)) }
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = ThemeColorUtils.black(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false) // Allow text to shrink, keep time visible if possible or wrap
                            )
                            
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemeColorUtils.darkGray(Color(0xFF666666))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = ThemeColorUtils.darkGray(Color(0xFF666666)),
                                fontSize = 12.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = notification.message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            color = ThemeColorUtils.darkGray(Color(0xFF666666)),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    if (isUnread) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp, top = 6.dp)
                                .size(8.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                    }
                }
                
                // Action buttons for friend requests
                if (notification.actionRequired && notificationType == NotificationType.FRIEND_REQUEST) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAccept,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(androidx.compose.ui.res.stringResource(R.string.notif_btn_accept), fontSize = 12.sp)
                        }
                        Button(
                            onClick = onDecline,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336)
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(androidx.compose.ui.res.stringResource(R.string.notif_btn_decline), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        
        if (isUnread) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.3f),
                thickness = 2.dp
            )
        }
    }
}

@Composable
fun EmptyNotificationsState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = ThemeColorUtils.lightGray(Color.Gray).copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(R.string.notifications_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = ThemeColorUtils.black(),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(R.string.notifications_empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = ThemeColorUtils.darkGray(Color(0xFF666666))
        )
    }
}

private fun getNotificationStyle(type: NotificationType): Triple<androidx.compose.ui.graphics.vector.ImageVector, Color, Color> {
    return when (type) {
        NotificationType.ANNOUNCEMENT -> Triple(
            Icons.AutoMirrored.Filled.SpeakerNotes,
            Color(0xFF316E15),
            Color(0xFF4CAF50)
        )
        NotificationType.FRIEND_REQUEST -> Triple(
            Icons.Default.PersonAdd,
            Color(0xFFCE681A),
            Color(0xFFE09A67)
        )
        NotificationType.FRIEND_ACCEPT -> Triple(
            Icons.Default.CheckCircle,
            Color(0xFFAF8109),
            Color(0xFFE3C878)
        )
        NotificationType.DETECTION_RESULT -> Triple(
            Icons.Default.Notifications,
            Color(0xFF1D393B),
            Color(0xFF5D8C9D)
        )
        NotificationType.DATA_ADDED -> Triple(
            Icons.Default.CheckCircle,
            Color(0xFF1245A9),
            Color(0xFF5D8AE1)
        )
        NotificationType.DATA_EDITED -> Triple(
            Icons.Default.Edit,
            Color(0xFFABBD0A),
            Color(0xFFE4EF84)
        )
        NotificationType.DATA_DELETED -> Triple(
            Icons.Default.Delete,
            Color(0xFF590B0B),
            Color(0xFFE87F7F)
        )
        NotificationType.SYSTEM_UPDATE, NotificationType.PROFILE_UPDATE -> Triple(
            Icons.Default.SystemUpdate,
            Color(0xFF041418),
            Color(0xFF666D70)
        )
        // Social notification types
        NotificationType.REACTION -> Triple(
            Icons.Default.Notifications,
            Color(0xFFE91E63),
            Color(0xFFF8BBD9)
        )
        NotificationType.COMMENT -> Triple(
            Icons.AutoMirrored.Filled.SpeakerNotes,
            Color(0xFF2196F3),
            Color(0xFFBBDEFB)
        )
        NotificationType.FOLLOW -> Triple(
            Icons.Default.PersonAdd,
            Color(0xFFDA8041),
            Color(0xFFFFF0DB)
        )
        NotificationType.NEW_POST -> Triple(
            Icons.Default.Notifications,
            Color(0xFF4CAF50),
            Color(0xFFC8E6C9)
        )
        NotificationType.DISEASE_ALERT -> Triple(
            Icons.Default.Warning, 
            Color(0xFFD32F2F),    
            Color(0xFFFFCDD2)      
        )
    }
}

private fun getNotificationTitleResId(type: NotificationType): Int {
    return when (type) {
        NotificationType.ANNOUNCEMENT -> R.string.notif_type_announcement
        NotificationType.FRIEND_REQUEST -> R.string.notif_type_friend_req
        NotificationType.FRIEND_ACCEPT -> R.string.notif_type_friend_accept
        NotificationType.DETECTION_RESULT -> R.string.notif_type_detection
        NotificationType.DATA_ADDED -> R.string.notif_type_data_add
        NotificationType.DATA_EDITED -> R.string.notif_type_data_edit
        NotificationType.DATA_DELETED -> R.string.notif_type_data_del
        NotificationType.SYSTEM_UPDATE -> R.string.notif_type_sys_update
        NotificationType.PROFILE_UPDATE -> R.string.notif_type_profile_update
        NotificationType.REACTION -> R.string.notif_type_reaction
        NotificationType.COMMENT -> R.string.notif_type_comment
        NotificationType.FOLLOW -> R.string.notif_type_follow
        NotificationType.NEW_POST -> R.string.notif_type_post
        NotificationType.DISEASE_ALERT -> R.string.notif_type_disease
    }
}