package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.viewmodels.FriendViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(navController: NavController) {
    val viewModel: FriendViewModel = viewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var blockedUsers by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showUnblockDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadBlockedUsers { success, users, message ->
            isLoading = false
            if (success) {
                blockedUsers = users
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }

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
                        androidx.compose.ui.res.stringResource(R.string.blocked_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ThemeColorUtils.black()
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = androidx.compose.ui.res.stringResource(R.string.back),
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
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFD27D2D))
                    }
                } else if (blockedUsers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = ThemeColorUtils.darkGray(Color.Gray)
                            )
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.blocked_empty_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = ThemeColorUtils.black()
                            )
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.blocked_empty_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThemeColorUtils.darkGray(Color(0xFF666666))
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(blockedUsers, key = { it.first }) { (userId, userName) ->
                            BlockedUserCard(
                                userName = userName,
                                onUnblock = {
                                    showUnblockDialog = userId to userName
                                },
                                onViewProfile = {
                                    navController.navigate("view_profile?userId=$userId")
                                }
                            )
                        }
                    }
                }
            }

            // Unblock Confirmation Dialog
            showUnblockDialog?.let { (userId, userName) ->
                AlertDialog(
                    onDismissRequest = { showUnblockDialog = null },
                    title = {
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.blocked_dialog_title, userName),
                            color = ThemeColorUtils.black()
                        )
                    },
                    text = {
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.blocked_dialog_msg, userName),
                            color = ThemeColorUtils.black()
                        )
                    },
                    containerColor = ThemeColorUtils.white(),
                    shape = RoundedCornerShape(16.dp),
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.unblockUser(userId) { success, message ->
                                    showUnblockDialog = null
                                    if (success) {
                                        // Remove from list
                                        blockedUsers = blockedUsers.filter { it.first != userId }
                                        showMessage(message)
                                    } else {
                                        showMessage(message, true)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text(androidx.compose.ui.res.stringResource(R.string.blocked_unblock_btn), color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUnblockDialog = null }) {
                            Text(androidx.compose.ui.res.stringResource(R.string.common_cancel), color = ThemeColorUtils.black())
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BlockedUserCard(
    userName: String,
    onUnblock: () -> Unit,
    onViewProfile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            width = 1.dp,
            color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C))
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.surface(Color(0xFFE5E2DE)),
            contentColor = ThemeColorUtils.black()
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
                        .size(64.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onViewProfile)
            ) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColorUtils.black()
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.blocked_status),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF6B6B),
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onUnblock,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4DB0D3)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(androidx.compose.ui.res.stringResource(R.string.blocked_unblock_btn), color = Color.White)
            }
        }
    }
}