package com.bisu.chickcare.frontend.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bisu.chickcare.backend.data.UserProfile
import com.bisu.chickcare.backend.repository.FriendSuggestion

@Composable
fun CoverPhotoSection(
    coverPhotoUrl: String?,
    onCoverPhotoClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onCoverPhotoClick)
    ) {
        if (coverPhotoUrl != null) {
            AsyncImage(
                model = coverPhotoUrl,
                contentDescription = "Cover Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE1E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.offset(y = (-40).dp)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Add cover photo",
                        tint = Color(0xFF3A3939).copy(alpha = 0.65f),
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add cover photo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF464343).copy(alpha = 0.65f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        IconButton(
            onClick = onCoverPhotoClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = if (coverPhotoUrl != null) "Change cover photo" else "Add cover photo",
                tint = Color.White,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun ProfilePictureAndNameSection(
    userProfile: UserProfile,
    isUploading: Boolean,
    onProfilePhotoClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (userProfile.photoUrl != null) {
                AsyncImage(
                    model = userProfile.photoUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color(0xFF3A3939), CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE1E0E0))
                        .border(3.dp, Color(0xFF3A3939), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Default profile picture",
                        tint = Color(0xFF464343),
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
            
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(140.dp),
                    color = Color(0xFF3A3939),
                    strokeWidth = 4.dp
                )
            }
            
            IconButton(
                onClick = onProfilePhotoClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(44.dp)
                    .background(Color.White, CircleShape)
                    .border(2.dp, Color(0xFF3A3939), CircleShape)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = if (userProfile.photoUrl != null) "Change photo" else "Add photo",
                    tint = Color(0xFF2F2E2E),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = userProfile.fullName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF26201C)
        )
    }
}

@Composable
fun FriendSuggestionsSection(
    suggestionCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Suggestions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF26201C)
                )
                if (suggestionCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge {
                                Text(
                                    text = suggestionCount.toString(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    ) {
                        Text("") // Empty content for badge
                    }
                }
            }
            
            if (suggestionCount == 0) {
                Text(
                    text = "No friend suggestions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            } else {
                Text(
                    text = "$suggestionCount new friend suggestions available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8B4513)
                )
            }
        }
    }
}

@Composable
fun PostsSection(
    userProfile: UserProfile,
    onNavigateToPost: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Posts",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF26201C)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    ) {
                        if (userProfile.photoUrl != null && userProfile.photoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = userProfile.photoUrl,
                                contentDescription = userProfile.fullName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color(0xFFE1E0E0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = userProfile.fullName,
                                    tint = Color(0xFF464343),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    
                    IconButton(
                        onClick = onNavigateToPost,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Color(0xFFF5F5F5),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.PostAdd,
                            contentDescription = "Create post from detection history",
                            tint = Color(0xFF8B4513),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToPost() }
                            .height(40.dp)
                            .background(
                                Color(0xFFF9F9F9),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Write a post...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                    
                    Button(
                        onClick = onNavigateToPost,
                        modifier = Modifier.height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B4513)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "Post",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        
        Text(
            text = "No posts yet. Create your first post!",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
fun MutualFriendsSection(
    mutualFriends: List<FriendSuggestion>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Friends",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${mutualFriends.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B4513)
                )
            }
            
            if (mutualFriends.isEmpty()) {
                Text(
                    text = "No friends yet. Start adding friends!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontStyle = FontStyle.Italic
                )
            } else {
                val displayFriends = mutualFriends.take(6)
                val rows = displayFriends.chunked(3)
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { rowFriends ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowFriends.forEach { friend ->
                                FriendAvatarItem(
                                    friend = friend,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowFriends.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                
                if (mutualFriends.size > 6) {
                    Text(
                        text = "and ${mutualFriends.size - 6} more...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8B4513),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FriendAvatarItem(
    friend: FriendSuggestion,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFFF5F5F5))
                .border(2.dp, Color(0xFFE0E0E0), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (friend.photoUrl != null && friend.photoUrl.isNotEmpty()) {
                AsyncImage(
                    model = friend.photoUrl,
                    contentDescription = friend.fullName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = friend.fullName,
                    tint = Color(0xFF8B4513),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Text(
            text = friend.fullName.split(" ").first(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    fieldName: String = "",
    onEditField: ((String, String) -> Unit)? = null,
    isEmpty: Boolean = false,
    onPrivacyChange: ((String, String) -> Unit)? = null,
    currentPrivacy: String = "public"
) {
    var showPrivacyMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(140.dp)
        )
        // Value text - NOT clickable (editing only through pencil icon)
        Row(
            modifier = Modifier
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                color = if (isEmpty) Color.Gray else Color.Unspecified,
                fontStyle = if (isEmpty) FontStyle.Italic else FontStyle.Normal,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Privacy icon (three dots)
        if (onPrivacyChange != null) {
            Box {
                IconButton(
                    onClick = { showPrivacyMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Privacy settings",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = showPrivacyMenu,
                    onDismissRequest = { showPrivacyMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Public") },
                        onClick = {
                            onPrivacyChange(fieldName, "public")
                            showPrivacyMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                if (currentPrivacy == "public") Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (currentPrivacy == "public") Color(0xFF6366F1) else Color.Gray
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Friends") },
                        onClick = {
                            onPrivacyChange(fieldName, "friends")
                            showPrivacyMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                if (currentPrivacy == "friends") Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (currentPrivacy == "friends") Color(0xFF6366F1) else Color.Gray
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Private") },
                        onClick = {
                            onPrivacyChange(fieldName, "private")
                            showPrivacyMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                if (currentPrivacy == "private") Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (currentPrivacy == "private") Color(0xFF6366F1) else Color.Gray
                            )
                        }
                    )
                }
            }
        }
    }
}

