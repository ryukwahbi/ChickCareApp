package com.bisu.chickcare.frontend.screen

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.repository.ChatMessage
import com.bisu.chickcare.backend.service.NetworkConnectivityHelper
import com.bisu.chickcare.backend.viewmodels.ChatViewModel
import com.bisu.chickcare.frontend.components.OfflineIndicator
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    userId: String? = null,
    userName: String? = null
) {
    val viewModel: ChatViewModel = viewModel()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val context = LocalContext.current
    val isOnline by NetworkConnectivityHelper.connectivityFlow(context)
        .collectAsState(initial = NetworkConnectivityHelper.isOnline(context))
    val isOffline = !isOnline

    // Load messages when userId is available
    LaunchedEffect(userId) {
        userId?.let { viewModel.loadMessages(it) }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            userName ?: "Chat",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        // Active status could go here if needed
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
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
            if (isLoading && messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(color = Color(0xFFD27D2D))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .imePadding()
                ) {
                    OfflineIndicator(isOffline = isOffline)
                    // Messages list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        reverseLayout = false
                    ) {
                        items(messages, key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                isCurrentUser = viewModel.isCurrentUser(message.senderId),
                                formatTime = { viewModel.formatMessageTime(it) },

                                onDeleteMessage = { viewModel.deleteMessage(it) },
                                onToggleReaction = { msg, reaction -> 
                                    viewModel.toggleReaction(msg, reaction) 
                                },
                                otherUserId = userId,
                                onAvatarClick = { senderId ->
                                    navController.navigate("view_profile?userId=$senderId")
                                }
                            )
                        }
                    }

                    // Input bar
                    ChatInputBar(
                        messageText = messageText,
                        onMessageTextChange = { messageText = it },
                        onSendClick = {
                            if (messageText.isNotBlank() && userId != null) {
                                viewModel.sendMessage(messageText, userId)
                                messageText = ""
                            }
                        },
                        enabled = isOnline,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }

}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isCurrentUser: Boolean,
    formatTime: (Long) -> String,
    onDeleteMessage: (ChatMessage) -> Unit,
    onToggleReaction: (ChatMessage, String) -> Unit,
    otherUserId: String? = null,
    onAvatarClick: (String) -> Unit
) {
    val mediaPlayer = remember { MediaPlayer() }
    var isPlaying by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }

    // Load other user's profile photo for incoming messages
    var otherUserPhotoUrl by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(message.senderId, otherUserId) {
        if (!isCurrentUser && message.senderId.isNotEmpty()) {
            try {
                val userDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(message.senderId)
                    .get()
                    .await()
                otherUserPhotoUrl = userDoc.getString("photoUrl")
            } catch (e: Exception) {
                android.util.Log.w("MessageBubble", "Error loading user photo: ${e.message}")
                otherUserPhotoUrl = null
            }
        }
    }

    // Cleanup MediaPlayer when composable is disposed
    DisposableEffect(message.id) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            } catch (_: Exception) {
                // Ignore errors during cleanup
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isCurrentUser) {
            // Avatar for incoming messages with default fallback
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.3f))
                    .clickable { onAvatarClick(message.senderId) },
                contentAlignment = Alignment.Center
            ) {
                if (!otherUserPhotoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = otherUserPhotoUrl,
                        contentDescription = "User avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.default_avatar),
                        placeholder = painterResource(id = R.drawable.default_avatar)
                    )
                } else {
                    AsyncImage(
                        model = R.drawable.default_avatar,
                        contentDescription = "Default avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            // Message bubble
            Box(
                modifier = Modifier
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showMenu = true }
                    )
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                            bottomEnd = if (isCurrentUser) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isCurrentUser) Color(0xFF2196F3) else Color(0xFFE0E0E0)
                    )
                    .padding(12.dp)
            ) {
                Column {
                    // Image message
                    if (!message.imageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = message.imageUrl,
                            contentDescription = "Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Audio message
                    if (!message.audioUrl.isNullOrEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        if (isPlaying) {
                                            mediaPlayer.pause()
                                            isPlaying = false
                                        } else {
                                            if (mediaPlayer.isPlaying) {
                                                mediaPlayer.stop()
                                            }
                                            mediaPlayer.reset()
                                            mediaPlayer.setDataSource(message.audioUrl)
                                            mediaPlayer.prepare()
                                            mediaPlayer.start()
                                            isPlaying = true

                                            coroutineScope.launch {
                                                while (mediaPlayer.isPlaying) {
                                                    delay(100)
                                                }
                                                isPlaying = false
                                            }
                                        }
                                    } catch (_: Exception) {
                                        // Handle error silently
                                        isPlaying = false
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = if (isCurrentUser) Color.White else Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "🎵 Audio message",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCurrentUser) Color.White else Color.Black,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Text message
                    if (message.message.isNotBlank()) {
                        Text(
                            text = message.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCurrentUser) Color.White else Color.Black,
                            fontSize = 15.sp
                        )
                    }
                }
            }
            
            // Reaction Menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                shape = RoundedCornerShape(16.dp),
                containerColor = Color.White
            ) {
                // Reaction Picker Row
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val reactions = listOf("❤️", "😂", "😮", "😢", "😠", "👍")
                    reactions.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .clickable {
                                    onToggleReaction(message, emoji)
                                    showMenu = false
                                }
                                .padding(4.dp)
                        )
                    }
                }
                
                // Divider
                androidx.compose.material3.HorizontalDivider()

                if (message.message.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text("Copy text") },
                        onClick = {
                            clipboardManager.setText(AnnotatedString(message.message))
                            showMenu = false
                        }
                    )
                }
                if (isCurrentUser) {
                    DropdownMenuItem(
                        text = { Text("Delete message") },
                        onClick = {
                            onDeleteMessage(message)
                            showMenu = false
                        }
                    )
                }
            }
            
            // Display Reactions
            if (message.reactions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    message.reactions.forEach { (emoji, userIds) ->
                        if (userIds.isNotEmpty()) {
                            Text(
                                text = "$emoji ${userIds.size}",
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }

            // Timestamp
            Text(
                text = formatTime(message.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )

            // Seen Indicator
            if (isCurrentUser && message.isRead) {
                Text(
                    text = "Seen ✓",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        if (isCurrentUser) {
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
fun ChatInputBar(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Initialize sound player for sending messages
    val sendSoundPlayer = remember { 
        try {
            MediaPlayer.create(context, R.raw.send_message)
        } catch (_: Exception) {
            null
        }
    }
    
    // Cleanup media player
    DisposableEffect(Unit) {
        onDispose {
            sendSoundPlayer?.release()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                ThemeColorUtils.white(),
                RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Text input
        OutlinedTextField(
            value = messageText,
            onValueChange = onMessageTextChange,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            placeholder = {
                Text(
                    "Send a message",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            enabled = enabled
        )

        // Send button
        IconButton(
            onClick = {
                onSendClick()
                // Play sound effect
                try {
                    if (sendSoundPlayer != null) {
                        if (sendSoundPlayer.isPlaying) {
                            sendSoundPlayer.seekTo(0)
                        } else {
                            sendSoundPlayer.start()
                        }
                    }
                } catch (_: Exception) {
                    // Ignore sound errors
                }
            },
            enabled = messageText.isNotBlank() && enabled,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (messageText.isNotBlank() && enabled) {
                    Color(0xFFD27D2D)
                } else {
                    Color.Gray
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

