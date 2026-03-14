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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.repository.AccountManager
import com.bisu.chickcare.backend.repository.SavedAccount
import com.bisu.chickcare.backend.viewmodels.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageProfilesScreen(navController: NavController) {
    val context = LocalContext.current
    val accountManager = remember { AccountManager(context) }
    val authViewModel: AuthViewModel = viewModel()
    val currentUserId = authViewModel.getCurrentUserId(context)
    
    var accounts by remember { mutableStateOf<List<SavedAccount>>(emptyList()) }
    var selectedAccountForRemoval by remember { mutableStateOf<SavedAccount?>(null) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    
    // Sync account info from Firestore when screen loads to get latest profile pictures
    LaunchedEffect(Unit) {
        // First load from SharedPreferences
        accounts = accountManager.getSavedAccounts()
        
        // Capture accounts list before entering coroutine
        val accountsToSync = accounts
        
        // Then sync all accounts with Firestore to get updated profile pictures
        withContext(Dispatchers.IO) {
            accountsToSync.forEach { account ->
                try {
                    accountManager.updateAccountInfo(account.userId)
                } catch (_: Exception) {
                    // Silent fail - continue with next account
                }
            }
        }
        // Reload accounts after syncing (back on main dispatcher)
        accounts = accountManager.getSavedAccounts()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage profiles") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeColorUtils.white(),
                    titleContentColor = ThemeColorUtils.black()
                )
            )
        },
        containerColor = ThemeColorUtils.beige(Color(0xFFFFF7E6))
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(accounts) { account ->
                ProfileItem(
                    account = account,
                    isCurrentAccount = account.userId == currentUserId,
                    onClick = {
                        // Set this account as active for data loading
                        accountManager.setActiveUser(account.userId)
                        
                        // Navigate to login with pre-filled email
                        // We must re-authenticate to get a valid Firebase token
                         navController.navigate("login?email=${account.email}") {
                            popUpTo("login") { inclusive = true } // Clear login from backstack if present
                            launchSingleTop = true
                        }
                    },
                    onRemoveClick = {
                        selectedAccountForRemoval = account
                        showRemoveDialog = true
                    }
                )
            }
            
            // Clear All Accounts Button
            if (accounts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.padding(vertical = 8.dp))
                    Button(
                        onClick = { showClearAllDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Clear All Accounts",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        
        if (showRemoveDialog && selectedAccountForRemoval != null) {
            RemoveProfileDialog(
                account = selectedAccountForRemoval!!,
                onConfirm = {
                    val removedUserId = selectedAccountForRemoval!!.userId
                    val isCurrentAccount = removedUserId == currentUserId
                    
                    accountManager.removeAccount(removedUserId)
                    accounts = accountManager.getSavedAccounts()
                    showRemoveDialog = false
                    selectedAccountForRemoval = null
                    
                    // If removed account is current account, sign out
                    if (isCurrentAccount) {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo("manage_profiles") { inclusive = true }
                        }
                    }
                },
                onDismiss = {
                    showRemoveDialog = false
                    selectedAccountForRemoval = null
                }
            )
        }
        
        if (showClearAllDialog) {
            ClearAllAccountsDialog(
                accountCount = accounts.size,
                onConfirm = {
                    val wasCurrentAccountInList = accounts.any { it.userId == currentUserId }
                    
                    accountManager.clearAllAccounts()
                    accounts = accountManager.getSavedAccounts()
                    showClearAllDialog = false
                    
                    // If current account was cleared, sign out
                    if (wasCurrentAccountInList) {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo("manage_profiles") { inclusive = true }
                        }
                    }
                },
                onDismiss = {
                    showClearAllDialog = false
                }
            )
        }
    }
}

@Composable
fun ProfileItem(
    account: SavedAccount,
    isCurrentAccount: Boolean,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
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
            // Profile Picture
            Box {
                AsyncImage(
                    model = if (account.photoUrl.isNullOrEmpty()) R.drawable.default_avatar else account.photoUrl,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    error = androidx.compose.ui.res.painterResource(R.drawable.default_avatar),
                    placeholder = androidx.compose.ui.res.painterResource(R.drawable.default_avatar)
                )
                if (isCurrentAccount) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color(0xFF4CAF50), CircleShape)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Name
            Text(
                text = account.fullName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrentAccount) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            
            // Remove button
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove account",
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun RemoveProfileDialog(
    account: SavedAccount,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = ThemeColorUtils.white()
            )
        ) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)) {
                // Close button at top right
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .offset(x = 8.dp, y = (-8).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = ThemeColorUtils.black(),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Profile name
                    Text(
                        text = account.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black(),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                
                    // Confirmation message
                    Text(
                        text = "Are you sure you want to remove this profile? This action cannot be undone. You will be signed out if this is your current account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeColorUtils.black(),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                
                    // Remove profile button
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Remove profile",
                            color = ThemeColorUtils.white(),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClearAllAccountsDialog(
    accountCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = ThemeColorUtils.white()
            )
        ) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)) {
                // Close button at top right
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .offset(x = 8.dp, y = (-8).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = ThemeColorUtils.black(),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Title
                    Text(
                        text = "Clear All Accounts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black(),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                
                    // Confirmation message
                    Text(
                        text = "Are you sure you want to clear all $accountCount saved account(s)? This action cannot be undone. You will be signed out if your current account is in the list.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeColorUtils.black(),
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                
                    // Clear All button
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Clear All",
                            color = ThemeColorUtils.white(),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
