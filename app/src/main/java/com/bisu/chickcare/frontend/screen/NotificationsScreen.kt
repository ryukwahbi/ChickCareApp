package com.bisu.chickcare.frontend.screen

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
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(navController: NavController) {
    val viewModel: DashboardViewModel = viewModel()
    val friendViewModel: FriendViewModel = viewModel()
    val notifications by viewModel.notifications.collectAsState()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.markAllNotificationsAsRead()
    }
    
    val unreadCount = notifications.count { !it.isRead }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Notifications", fontWeight = FontWeight.Bold)
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color.Red, CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    if (unreadCount > 10) "10+" else "$unreadCount",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF000000))
                    }
                },
                actions = {
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = { viewModel.markAllNotificationsAsRead() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF8B4513))
                        ) {
                            Text("Mark all as read", fontSize = 12.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF000000)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5))
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
                                                viewModel.markNotificationAsRead(notification.id)
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
                                            viewModel.markNotificationAsRead(notification.id)
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
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRead() },
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread) Color.White else Color(0xFFFAFAFA)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isUnread) 4.dp else 2.dp
        ),
        shape = RoundedCornerShape(12.dp)
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
                            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = notification.message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp,
                            color = Color.Gray,
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
                    color = Color.Gray,
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
            tint = Color.Gray.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No notifications yet",
            style = MaterialTheme.typography.titleLarge,
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You'll see notifications here when you receive them",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray.copy(alpha = 0.7f)
        )
    }
}

private fun getNotificationStyle(type: NotificationType): Triple<androidx.compose.ui.graphics.vector.ImageVector, Color, Color> {
    return when (type) {
        NotificationType.ANNOUNCEMENT -> Triple(
            Icons.AutoMirrored.Filled.SpeakerNotes,
            Color(0xFF2196F3),
            Color(0xFF2196F3).copy(alpha = 0.1f)
        )
        NotificationType.FRIEND_REQUEST -> Triple(
            Icons.Default.PersonAdd,
            Color(0xFF9C27B0),
            Color(0xFF9C27B0).copy(alpha = 0.1f)
        )
        NotificationType.FRIEND_ACCEPT -> Triple(
            Icons.Default.CheckCircle,
            Color(0xFF4CAF50),
            Color(0xFF4CAF50).copy(alpha = 0.1f)
        )
        NotificationType.DETECTION_RESULT -> Triple(
            Icons.Default.Notifications,
            Color(0xFFFF9800),
            Color(0xFFFF9800).copy(alpha = 0.1f)
        )
        NotificationType.DATA_ADDED -> Triple(
            Icons.Default.CheckCircle,
            Color(0xFF4CAF50),
            Color(0xFF4CAF50).copy(alpha = 0.1f)
        )
        NotificationType.DATA_EDITED -> Triple(
            Icons.Default.Edit,
            Color(0xFF2196F3),
            Color(0xFF2196F3).copy(alpha = 0.1f)
        )
        NotificationType.DATA_DELETED -> Triple(
            Icons.Default.Delete,
            Color(0xFFF44336),
            Color(0xFFF44336).copy(alpha = 0.1f)
        )
        NotificationType.SYSTEM_UPDATE, NotificationType.PROFILE_UPDATE -> Triple(
            Icons.Default.SystemUpdate,
            Color(0xFF607D8B),
            Color(0xFF607D8B).copy(alpha = 0.1f)
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
