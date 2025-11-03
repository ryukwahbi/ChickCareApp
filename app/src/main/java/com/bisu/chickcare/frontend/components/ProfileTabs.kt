package com.bisu.chickcare.frontend.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

@Composable
fun TimelineTabContent(
    userProfile: UserProfile,
    suggestionCount: Int,
    onFriendSuggestions: () -> Unit,
    onNavigateToPost: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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
    onAddInfo: () -> Unit,
    onEditField: (String, String) -> Unit = { _, _ -> },
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
                    onEditField = onEditField,
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
    onEditField: (String, String) -> Unit,
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
            onEditField = null, // Read-only
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["email"] ?: "public"
        )
        InfoRow(
            label = "Contact Number",
            value = userProfile.contact,
            fieldName = "contact",
            onEditField = null, // Read-only
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["contact"] ?: "public"
        )
        InfoRow(
            label = "Birth Date",
            value = userProfile.birthDate,
            fieldName = "birthDate",
            onEditField = null, // Read-only
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["birthDate"] ?: "public"
        )
        InfoRow(
            label = "Gender",
            value = userProfile.gender ?: "Not set",
            fieldName = "gender",
            onEditField = onEditField,
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["gender"] ?: "public",
            isEmpty = userProfile.gender.isNullOrEmpty()
        )
        InfoRow(
            label = "Address",
            value = userProfile.address.ifEmpty { "Not set" },
            fieldName = "address",
            onEditField = onEditField,
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
            onEditField = onEditField,
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["farmName"] ?: "public",
            isEmpty = userProfile.farmName.isEmpty()
        )
        InfoRow(
            label = "Farm Location",
            value = userProfile.farmLocation.ifEmpty { "Not set" },
            fieldName = "farmLocation",
            onEditField = onEditField,
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["farmLocation"] ?: "public",
            isEmpty = userProfile.farmLocation.isEmpty()
        )
        InfoRow(
            label = "Farm Type",
            value = userProfile.farmType.ifEmpty { "Not set" },
            fieldName = "farmType",
            onEditField = onEditField,
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["farmType"] ?: "public",
            isEmpty = userProfile.farmType.isEmpty()
        )
        InfoRow(
            label = "Specialization",
            value = userProfile.specialization.ifEmpty { "Not set" },
            fieldName = "specialization",
            onEditField = onEditField,
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
            onEditField = onEditField,
            onPrivacyChange = onPrivacyChange,
            currentPrivacy = userProfile.fieldPrivacy["numberOfBirds"] ?: "public",
            isEmpty = userProfile.numberOfBirds.isEmpty()
        )
        InfoRow(
            label = "Years of Experience",
            value = userProfile.yearsExperience.ifEmpty { "Not set" },
            fieldName = "yearsExperience",
            onEditField = onEditField,
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
            onEditField = null,
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

