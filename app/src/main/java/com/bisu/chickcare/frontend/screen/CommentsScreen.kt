package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.res.stringResource
import com.bisu.chickcare.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bisu.chickcare.backend.repository.Comment
import com.bisu.chickcare.backend.repository.CommentRepository
import com.bisu.chickcare.backend.viewmodels.AuthViewModel
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.bisu.chickcare.frontend.utils.SoundManager
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
    navController: NavController,
    postId: String,
    postOwnerId: String
) {
    val authViewModel: AuthViewModel = viewModel()
    val userProfile by authViewModel.userProfile.collectAsState()
    val commentRepository = remember { CommentRepository() }
    val comments by commentRepository.getComments(postOwnerId, postId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    var commentText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    
    // State for comment actions
    var selectedComment by remember { mutableStateOf<Comment?>(null) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val currentUserId = authViewModel.getCurrentUserId(context) ?: ""
    
    // Scroll to bottom when new comments are added
    LaunchedEffect(comments.size) {
        if (comments.isNotEmpty()) {
            listState.animateScrollToItem(comments.size - 1)
        }
    }
    
    // Fetch post details
    var post by remember { mutableStateOf<com.bisu.chickcare.backend.repository.TimelinePost?>(null) }
    val postRepository = remember { com.bisu.chickcare.backend.repository.PostRepository() }
    
    LaunchedEffect(postId, postOwnerId) {
        post = postRepository.getPost(postOwnerId, postId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.comments_title),
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = ThemeColorUtils.black()
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeColorUtils.white()
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (ThemeViewModel.isDarkMode) Color(0xFF1A1A1A) else ThemeColorUtils.beige(Color(0xFFFFF7E6)))
                .padding(paddingValues)
                .imePadding()
        ) {
            HorizontalDivider(
                color = if (ThemeViewModel.isDarkMode) Color(0xFF333333) else Color(0xFFE0E0E0)
            )
            
            // Comments list with Post Header
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Display the post at the top
                if (post != null) {
                    item {
                        com.bisu.chickcare.frontend.components.TimelinePostItem(
                            post = post!!,
                            onDelete = { /* Disable delete from here or handle if needed */ },
                            onChangeAudience = { _, _ -> /* Disable audience from here */ },
                            onSavePost = { postId ->
                                scope.launch {
                                    if (post!!.isSaved) {
                                        postRepository.unsavePost(currentUserId, postOwnerId, postId)
                                        post = post?.copy(isSaved = false)
                                    } else {
                                        postRepository.savePost(currentUserId, postOwnerId, postId, post!!)
                                        post = post?.copy(isSaved = true)
                                    }
                                }
                            },
                            onReaction = { postId, ownerId, reactionType ->
                                scope.launch {
                                    postRepository.toggleReaction(ownerId, postId, currentUserId, reactionType)
                                    post = postRepository.getPost(ownerId, postId)
                                }
                            },
                            onCommentClick = { _, _ -> }, // Already on comments screen
                            userId = currentUserId
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(
                            color = if (ThemeViewModel.isDarkMode) Color(0xFF333333) else Color(0xFFE0E0E0),
                            thickness = 1.dp
                        )
                    }
                }

                if (comments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_comments_yet),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (ThemeViewModel.isDarkMode) Color(0xFFAAAAAA) else Color(0xFF666666)
                            )
                        }
                    }
                }
                
                items(comments, key = { it.id }) { comment ->
                    CommentItem(
                        comment = comment,
                        isOwnComment = comment.userId == currentUserId,
                        onLongClick = {
                            if (comment.userId == currentUserId) {
                                selectedComment = comment
                                showOptionsDialog = true
                            }
                        }
                    )
                }
            }
            
            // Comment input
            HorizontalDivider(
                color = if (ThemeViewModel.isDarkMode) Color(0xFF333333) else Color(0xFFE0E0E0)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (ThemeViewModel.isDarkMode) Color(0xFF2C2C2C) else Color.White)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // User avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE1E0E0))
                ) {
                    if (userProfile?.photoUrl != null) {
                        AsyncImage(
                            model = userProfile?.photoUrl,
                            contentDescription = stringResource(R.string.your_avatar),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF464343),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
                        )
                    }
                }
                
                // Comment text field
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = {
                        Text(
                            stringResource(R.string.write_a_comment),
                            color = if (ThemeViewModel.isDarkMode) Color(0xFFAAAAAA) else Color(0xFF666666)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (ThemeViewModel.isDarkMode) Color(0xFF3A3A3A) else Color(0xFFF5F5F5),
                        unfocusedContainerColor = if (ThemeViewModel.isDarkMode) Color(0xFF3A3A3A) else Color(0xFFF5F5F5),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = if (ThemeViewModel.isDarkMode) Color.White else Color.Black,
                        unfocusedTextColor = if (ThemeViewModel.isDarkMode) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = false,
                    maxLines = 3
                )
                
                // Send button
                IconButton(
                    onClick = {
                        if (commentText.trim().isNotEmpty() && !isSubmitting) {
                            isSubmitting = true
                            scope.launch {
                                try {
                                    val comment = Comment(
                                        userId = currentUserId,
                                        userName = userProfile?.fullName ?: "User",
                                        userPhotoUrl = userProfile?.photoUrl,
                                        text = commentText.trim()
                                    )
                                    commentRepository.addComment(postOwnerId, postId, comment)
                                    
                                    // Play posted sound
                                    SoundManager.playSound(context, com.bisu.chickcare.R.raw.comments_sound)
                                    
                                    commentText = ""
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        }
                    },
                    enabled = commentText.trim().isNotEmpty() && !isSubmitting
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.send_comment),
                        tint = if (commentText.trim().isNotEmpty() && !isSubmitting) {
                            Color(0xFFDA8041)
                        } else {
                            Color(0xFFAAAAAA)
                        }
                    )
                }
            }
        }
    }
    
    // Dialogs
    if (showOptionsDialog && selectedComment != null) {
        CommentOptionsDialog(
            onDismiss = { showOptionsDialog = false },
            onEdit = {
                showOptionsDialog = false
                showEditDialog = true
            },
            onDelete = {
                showOptionsDialog = false
                showDeleteDialog = true
            }
        )
    }
    
    if (showEditDialog && selectedComment != null) {
        EditCommentDialog(
            initialText = selectedComment!!.text,
            onDismiss = { showEditDialog = false },
            onConfirm = { newText ->
                scope.launch {
                    try {
                        commentRepository.updateComment(postOwnerId, postId, selectedComment!!.id, newText)
                        showEditDialog = false
                        selectedComment = null
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
    }
    
    if (showDeleteDialog && selectedComment != null) {
        DeleteCommentConfirmationDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                scope.launch {
                    try {
                        commentRepository.deleteComment(postOwnerId, postId, selectedComment!!.id)
                        showDeleteDialog = false
                        selectedComment = null
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    isOwnComment: Boolean,
    onLongClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd 'at' HH:mm", Locale.getDefault()) }
    val dateString = remember(comment.timestamp) {
        dateFormat.format(Date(comment.timestamp))
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {}, // No action on simple click
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ThemeViewModel.isDarkMode) Color(0xFF2C2C2C) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // User avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE1E0E0))
            ) {
                if (comment.userPhotoUrl != null) {
                    AsyncImage(
                        model = comment.userPhotoUrl,
                        contentDescription = comment.userName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = comment.userName,
                        tint = Color(0xFF464343),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = comment.userName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
                    )
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ThemeViewModel.isDarkMode) Color(0xFFAAAAAA) else Color(0xFF666666)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = comment.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
                )
            }
        }
    }
}

@Composable
fun CommentOptionsDialog(
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFF2C2C2C)) else ThemeColorUtils.white()
            )
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                androidx.compose.material3.TextButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.edit_comment),
                        color = if (ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black(),
                        modifier = Modifier.padding(8.dp)
                    )
                }
                androidx.compose.material3.TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.delete_comment),
                        color = Color.Red,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EditCommentDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFF2C2C2C)) else ThemeColorUtils.white()
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.edit_comment),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black(),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (ThemeViewModel.isDarkMode) Color.White else Color.Black,
                        unfocusedTextColor = if (ThemeViewModel.isDarkMode) Color.White else Color.Black
                    ),
                    minLines = 3
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.TextButton(onClick = onDismiss) {
                        Text(
                            stringResource(R.string.cancel),
                            color = if (ThemeViewModel.isDarkMode) Color(0xFFAAAAAA) else Color(0xFF666666)
                        )
                    }
                    androidx.compose.material3.Button(
                        onClick = { onConfirm(text) },
                        enabled = text.trim().isNotEmpty(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDA8041)
                        )
                    ) {
                        Text(stringResource(R.string.save), color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteCommentConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFF2C2C2C)) else ThemeColorUtils.white()
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.delete_comment),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.delete_comment_confirmation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (ThemeViewModel.isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    androidx.compose.material3.TextButton(onClick = onDismiss) {
                        Text(
                            stringResource(R.string.cancel), 
                            color = if (ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.black()
                        )
                    }
                    androidx.compose.material3.Button(
                        onClick = onConfirm,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF5350)
                        )
                    ) {
                        Text(stringResource(R.string.delete), color = Color.White)
                    }
                }
            }
        }
    }
}
