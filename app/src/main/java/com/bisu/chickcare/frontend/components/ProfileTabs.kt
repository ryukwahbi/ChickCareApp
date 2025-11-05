package com.bisu.chickcare.frontend.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bisu.chickcare.backend.data.UserProfile
import com.bisu.chickcare.backend.repository.FriendSuggestion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TimelineTabContent(
    userProfile: UserProfile,
    suggestionCount: Int,
    onFriendSuggestions: () -> Unit,
    onNavigateToPost: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .imePadding()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // Dismiss keyboard when clicking outside input fields
                focusManager.clearFocus()
                keyboardController?.hide()
            },
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FriendSuggestionsSection(
            suggestionCount = suggestionCount,
            onClick = onFriendSuggestions
        )
        
        PostsSection(
            userProfile = userProfile,
            onNavigateToPost = onNavigateToPost  // This will navigate to post_detection_history
        )
    }
}

@Composable
fun AboutTabContent(
    userProfile: UserProfile,
    mutualFriends: List<FriendSuggestion>,
    onEditInfo: () -> Unit,
    onPrivacyChange: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        text = "Personal Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onEditInfo) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Info",
                            tint = Color(0xFF2F2F2F)
                        )
                    }
                }
                
                ProfileInfo(
                    userProfile = userProfile,
                    onPrivacyChange = onPrivacyChange
                )
            }
        }
        
        MutualFriendsSection(mutualFriends = mutualFriends)
    }
}

@Composable
fun ProfileInfo(
    userProfile: UserProfile,
    onPrivacyChange: (String, String) -> Unit
) {
    val memberSince = if (userProfile.createdAt > 0L) {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        dateFormat.format(Date(userProfile.createdAt))
    } else {
        "Not available"
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ========== PERSONAL INFORMATION SECTION ==========
        InfoRow(
            label = "Email",
            value = userProfile.email,
            fieldName = "email",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["email"] ?: "public"
        )
        InfoRow(
            label = "Contact Number",
            value = userProfile.contact,
            fieldName = "contact",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["contact"] ?: "public"
        )
        InfoRow(
            label = "Birth Date",
            value = userProfile.birthDate,
            fieldName = "birthDate",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["birthDate"] ?: "public"
        )
        InfoRow(
            label = "Gender",
            value = userProfile.gender ?: "Not set",
            fieldName = "gender",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["gender"] ?: "public",
            isEmpty = userProfile.gender.isNullOrEmpty()
        )
        InfoRow(
            label = "Address",
            value = userProfile.address.ifEmpty { "Not set" },
            fieldName = "address",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["address"] ?: "public",
            isEmpty = userProfile.address.isEmpty()
        )
        
        // Line divider between Personal Info and Farm Details
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = Color(0xFFE5E7EB)
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        // ========== FARM DETAILS SECTION ==========
        InfoRow(
            label = "Farm Name",
            value = userProfile.farmName.ifEmpty { "Not set" },
            fieldName = "farmName",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["farmName"] ?: "public",
            isEmpty = userProfile.farmName.isEmpty()
        )
        InfoRow(
            label = "Farm Location",
            value = userProfile.farmLocation.ifEmpty { "Not set" },
            fieldName = "farmLocation",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["farmLocation"] ?: "public",
            isEmpty = userProfile.farmLocation.isEmpty()
        )
        InfoRow(
            label = "Farm Type",
            value = userProfile.farmType.ifEmpty { "Not set" },
            fieldName = "farmType",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["farmType"] ?: "public",
            isEmpty = userProfile.farmType.isEmpty()
        )
        InfoRow(
            label = "Specialization",
            value = userProfile.specialization.ifEmpty { "Not set" },
            fieldName = "specialization",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["specialization"] ?: "public",
            isEmpty = userProfile.specialization.isEmpty()
        )
        
        // Line divider between Farm Details and Statistics
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = Color(0xFFE5E7EB)
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        // ========== STATISTICS SECTION ==========
        InfoRow(
            label = "Number of Chickens",
            value = userProfile.numberOfBirds.ifEmpty { "Not set" },
            fieldName = "numberOfBirds",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["numberOfBirds"] ?: "public",
            isEmpty = userProfile.numberOfBirds.isEmpty()
        )
        InfoRow(
            label = "Years of Experience",
            value = userProfile.yearsExperience.ifEmpty { "Not set" },
            fieldName = "yearsExperience",
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["yearsExperience"] ?: "public",
            isEmpty = userProfile.yearsExperience.isEmpty()
        )
        
        // Line divider between Statistics and Account Info
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = Color(0xFFE5E7EB)
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        // ========== ACCOUNT INFORMATION SECTION ==========
        InfoRow(
            label = "Member Since",
            value = memberSince,
            fieldName = "memberSince",
            onPrivacyChange = null,
            currentPrivacy = "public"
        )
    }
}

@Composable
fun PhotosTabContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Photos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("No photos yet.", color = Color.Gray)
    }
}

@Composable
fun AudiosTabContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Audios",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("No audio recordings yet.", color = Color.Gray)
    }
}

@Composable
fun MoreTabContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "More",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("More options coming soon.", color = Color.Gray)
    }
}

