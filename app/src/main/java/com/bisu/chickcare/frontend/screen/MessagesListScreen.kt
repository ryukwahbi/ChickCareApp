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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MessagesListScreen(navController: NavController) {
    val viewModel: FriendViewModel = viewModel()
    val authViewModel: com.bisu.chickcare.backend.viewmodels.AuthViewModel = viewModel()
    val context = LocalContext.current
    val currentUserId = authViewModel.getCurrentUserId(context)
    val friends by viewModel.friends.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadFriends()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Messages",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    androidx.compose.material3.IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFD27D2D))
                }
            } else if (friends.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = ThemeColorUtils.lightGray(Color.Gray)
                        )
                        Text(
                            text = "No messages yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = ThemeColorUtils.lightGray(Color.Gray)
                        )
                        Text(
                            text = "Start a conversation with your friends!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThemeColorUtils.lightGray(Color.Gray)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(friends, key = { it.userId }) { friend ->
                        MessageConversationCard(
                            friend = friend,
                            onClick = {
                                navController.navigate("chat?userId=${friend.userId}&userName=${friend.fullName}")
                            },
                            currentUserId = currentUserId,
                            onDelete = {
                                viewModel.deleteConversation(friend.userId) { success ->
                                    if (success) {
                                        android.widget.Toast.makeText(context, "Conversation deleted", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onMarkRead = {
                                viewModel.markConversationAsRead(friend.userId)
                                android.widget.Toast.makeText(context, "Marked as read", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageConversationCard(
    friend: FriendSuggestion,
    onClick: () -> Unit,
    currentUserId: String?,
    onDelete: () -> Unit,
    onMarkRead: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    
    // Get last message preview
    var lastMessagePreview by remember { mutableStateOf("Tap to start conversation") }
    var lastMessageTime by remember { mutableStateOf<Long?>(null) }
    
    LaunchedEffect(friend.userId) {
        if (currentUserId != null) {
            // Load chat metadata to get last message
            try {
                val chatDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserId)
                    .collection("chats")
                    .document(friend.userId)
                    .get()
                    .await()
                
                if (chatDoc.exists()) {
                    val lastMessage = chatDoc.getString("lastMessage") ?: ""
                    val lastMessageType = chatDoc.getString("lastMessageType") ?: "text"
                    val timestamp = chatDoc.getLong("lastMessageTimestamp")
                    
                    lastMessagePreview = when (lastMessageType) {
                        "image" -> "📷 Image"
                        "audio" -> "🎵 Audio"
                        else -> lastMessage.ifBlank { "Tap to start conversation" }
                    }
                    lastMessageTime = timestamp
                }
            } catch (e: Exception) {
                // Chat doesn't exist yet or error loading
                lastMessagePreview = "Tap to start conversation"
            }
        }
    }
    
    // Format timestamp helper
    fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            else -> {
                val dateFormat = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                dateFormat.format(java.util.Date(timestamp))
            }
        }
    }
    
    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                ),
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
                Box {
                    AsyncImage(
                        model = friend.photoUrl ?: R.drawable.default_avatar,
                        contentDescription = "Profile",
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
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = friend.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lastMessagePreview,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFB0B0B0) else Color.Gray,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
                
                // Show timestamp if available
                lastMessageTime?.let { time ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatTimestamp(time),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFB0B0B0) else Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        // Context Menu
        androidx.compose.material3.DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = ThemeColorUtils.white()
        ) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Mark as read") },
                onClick = {
                    onMarkRead()
                    showMenu = false
                },
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        Icons.Default.PriorityHigh, // Or checkmark icon
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Gray
                    )
                }
            )
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Delete conversation", color = Color.Red) },
                onClick = {
                    onDelete()
                    showMenu = false
                },
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Red
                    )
                }
            )
        }
    }
}

