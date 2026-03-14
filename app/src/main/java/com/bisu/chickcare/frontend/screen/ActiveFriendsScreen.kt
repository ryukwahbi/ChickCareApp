package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.repository.FriendSuggestion
import com.bisu.chickcare.backend.viewmodels.FriendViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

data class ActiveFriendGroup(
    val title: String,
    val friends: List<FriendSuggestion>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveFriendsScreen(navController: NavController) {
    val viewModel: FriendViewModel = viewModel()
    val authViewModel: com.bisu.chickcare.backend.viewmodels.AuthViewModel = viewModel()
    val userProfile by authViewModel.userProfile.collectAsState()
    val isChecked = userProfile?.showActiveStatus ?: true
    val friends by viewModel.friends.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadFriends()
    }
    
    // Filter and sort active friends
    val activeFriendsList = friends.filter { friend ->
        // Only show friends active within last 3 days for "Active Friends" list
        val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000)
        friend.lastActive > threeDaysAgo
    }.sortedByDescending { it.lastActive }
    
    // Grouping for headers using string resources
    val groupedFriends = activeFriendsList.groupBy { friend ->
        val diff = System.currentTimeMillis() - friend.lastActive
        when {
            diff < 60 * 60 * 1000 -> "now"
            diff < 24 * 60 * 60 * 1000 -> "min"
            diff < 24 * 60 * 60 * 1000 -> "hour"
            else -> "day"
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.active_friends_title),
                        fontWeight = FontWeight.Bold 
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
            HorizontalDivider(
                color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)),
                thickness = 1.dp
            )

            // Active Status Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Show when you're active",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeColorUtils.black(),
                    fontWeight = FontWeight.Medium
                )
                androidx.compose.material3.Switch(
                    checked = isChecked,
                    onCheckedChange = { 
                        authViewModel.toggleActiveStatus(it) { _, _ -> }
                    },
                    modifier = Modifier.scale(0.9f),
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = ThemeColorUtils.white(),
                        checkedTrackColor = Color(0xFF87BF69),
                        uncheckedThumbColor = ThemeColorUtils.white(),
                        uncheckedTrackColor = ThemeColorUtils.lightGray(Color.Gray)
                    )
                )
            }
            
            HorizontalDivider(
                color = ThemeColorUtils.lightGray(Color(0xFF939393)),
                thickness = 1.dp
            )
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFD27D2D))
                }
            } else if (activeFriendsList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.active_friends_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = ThemeColorUtils.lightGray(Color.Gray)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Logic to display grouped items with headers
                    // Note: Simplified logic for grouping display
                    // For better grouping, we iterate through defined groups order
                    
                    // Group 1: Active Now (< 1 hour)
                    val nowFriends = activeFriendsList.filter { (System.currentTimeMillis() - it.lastActive) < 60 * 60 * 1000 }
                    if (nowFriends.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.active_group_now),
                                style = MaterialTheme.typography.titleSmall,
                                color = ThemeColorUtils.primary(),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(nowFriends) { friend ->
                            ActiveFriendItem(
                                friend = friend,
                                onClick = { 
                                    navController.navigate("chat?userId=${friend.userId}&userName=${friend.fullName}")
                                }
                            )
                        }
                    }
                    
                    // Group 2: Active Today (< 24 hours, but > 1 hour)
                    val todayFriends = activeFriendsList.filter { 
                        val diff = System.currentTimeMillis() - it.lastActive
                        diff >= 60 * 60 * 1000 && diff < 24 * 60 * 60 * 1000
                    }
                    if (todayFriends.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.active_group_hour),
                                style = MaterialTheme.typography.titleSmall,
                                color = ThemeColorUtils.primary(),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(todayFriends) { friend ->
                            ActiveFriendItem(
                                friend = friend,
                                onClick = { 
                                    navController.navigate("chat?userId=${friend.userId}&userName=${friend.fullName}")
                                }
                            )
                        }
                    }
                    
                    // Group 3: Active Recently (> 24 hours)
                    val recentFriends = activeFriendsList.filter { 
                        (System.currentTimeMillis() - it.lastActive) >= 24 * 60 * 60 * 1000
                    }
                    if (recentFriends.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.active_group_day),
                                style = MaterialTheme.typography.titleSmall,
                                color = ThemeColorUtils.primary(),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(recentFriends) { friend ->
                            ActiveFriendItem(
                                friend = friend,
                                onClick = { 
                                    navController.navigate("chat?userId=${friend.userId}&userName=${friend.fullName}")
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
fun ActiveFriendItem(
    friend: FriendSuggestion,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.white()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AsyncImage(
                    model = friend.photoUrl ?: R.drawable.default_avatar,
                    contentDescription = stringResource(R.string.account_photo_desc),
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                // Online indicator dot for very recent activity (< 10 mins)
                if ((System.currentTimeMillis() - friend.lastActive) < 10 * 60 * 1000) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                            .border(2.dp, ThemeColorUtils.white(), CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = friend.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Format relative time
                val diff = System.currentTimeMillis() - friend.lastActive
                val timeStr = when {
                    diff < 60 * 1000 -> stringResource(R.string.active_status_now)
                    diff < 60 * 60 * 1000 -> {
                        val mins = diff / (60 * 1000)
                        if (mins <= 1) stringResource(R.string.active_status_min_fmt, mins)
                        else stringResource(R.string.active_status_mins_fmt, mins)
                    }
                    diff < 24 * 60 * 60 * 1000 -> {
                        val hours = diff / (60 * 60 * 1000)
                        if (hours <= 1) stringResource(R.string.active_status_hour_fmt, hours)
                        else stringResource(R.string.active_status_hours_fmt, hours)
                    }
                    else -> {
                        val days = diff / (24 * 60 * 60 * 1000)
                        if (days <= 1) stringResource(R.string.active_status_day_fmt, days)
                        else stringResource(R.string.active_status_days_fmt, days)
                    }
                }
                
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeColorUtils.black().copy(alpha = 0.6f)
                )
            }
        }
    }
}

