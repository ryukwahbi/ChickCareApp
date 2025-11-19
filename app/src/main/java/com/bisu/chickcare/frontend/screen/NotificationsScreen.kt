package com.bisu.chickcare.frontend.screen

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
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.backend.repository.NotificationEntry
import com.bisu.chickcare.backend.repository.NotificationType
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel
import com.bisu.chickcare.backend.viewmodels.FriendViewModel
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavController) {
    val viewModel: DashboardViewModel = viewModel()
    val friendViewModel: FriendViewModel = viewModel()
    val notifications by viewModel.notifications.collectAsState()
    val pendingRequests by friendViewModel.pendingRequests.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.markAllNotificationsAsRead()
        friendViewModel.loadPendingFriendRequests()
        // Also use callback version to ensure it's called
        friendViewModel.getPendingFriendRequests { success, requests, message ->
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
                            "Notifications",
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
                                "($pendingRequestsCount pending)",
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
                            contentDescription = "Back",
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
                            Text("Mark all as read", fontSize = 12.sp)
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
                            onRead = { viewModel.markNotificationAsRead(notification.id) },
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
            .clickable { onRead() },
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
                    .background(backgroundColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = notification.title.ifEmpty { getDefaultTitle(notificationType) },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = ThemeColorUtils.black(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                                .size(8.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Timestamp
                Text(
                    text = formatTimestamp(notification.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = ThemeColorUtils.darkGray(Color(0xFF666666)),
                    fontSize = 11.sp
                )
                
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
                            Text("Accept", fontSize = 12.sp)
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
                            Text("Decline", fontSize = 12.sp)
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
            text = "No notifications yet",
            style = MaterialTheme.typography.titleLarge,
            color = ThemeColorUtils.black(),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You'll see notifications here when you receive them",
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
            Color(0xFF94C46C)
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
    }
}

private fun getDefaultTitle(type: NotificationType): String {
    return when (type) {
        NotificationType.ANNOUNCEMENT -> "New Announcement"
        NotificationType.FRIEND_REQUEST -> "Friend Request"
        NotificationType.FRIEND_ACCEPT -> "Friend Request Accepted"
        NotificationType.DETECTION_RESULT -> "Detection Result"
        NotificationType.DATA_ADDED -> "New Data Added"
        NotificationType.DATA_EDITED -> "Data Updated"
        NotificationType.DATA_DELETED -> "Data Deleted"
        NotificationType.SYSTEM_UPDATE -> "System Update"
        NotificationType.PROFILE_UPDATE -> "Profile Updated"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        diff < 604800000 -> "${diff / 86400000}d ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}