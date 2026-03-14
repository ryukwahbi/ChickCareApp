package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextAlign

data class Announcement(
    val id: String,
    val title: String,
    val content: String,
    val date: Long,
    val priority: String = "normal"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsScreen(navController: NavController) {
    val announcements = remember {
        listOf(
            Announcement(
                id = "1",
                title = "Welcome to ChickCare!",
                content = "We are thrilled to welcome you to ChickCare! Our goal is to assist you in monitoring and caring for your chickens with the best tools available. Stay tuned for continuous updates and exciting new features coming your way.",
                date = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000),
                priority = "high"
            ),
            Announcement(
                id = "2",
                title = "New Feature: Health Trend Analysis",
                content = "Explore the newly added Health Trend Analysis on your dashboard. This feature allows you to track your chickens' health trends over the last 5 days, providing detailed confidence metrics for more informed decision-making.",
                date = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000), // 3 days ago
                priority = "normal"
            ),
            Announcement(
                id = "3",
                title = "Tips for Better Detection",
                content = "To ensure the best possible detection results, please use the image and audio features in well-lit and quiet environments. Keeping your chickens calm during the process significantly improves accuracy.",
                date = System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000), // 14 days ago
                priority = "normal"
            ),
            Announcement(
                id = "4",
                title = "Stay Connected",
                content = "Did you know you can connect with other poultry enthusiasts? Visit the Community section to share insights, compare health trends, and learn from fellow breeders in the ChickCare network.",
                date = System.currentTimeMillis() - (20 * 24 * 60 * 60 * 1000), // 20 days ago
                priority = "normal"
            )
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Announcements",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFF141617) else Color.White,
                    titleContentColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (announcements.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Campaign,
                                    contentDescription = null,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    tint = ThemeColorUtils.lightGray(Color.Gray)
                                )
                                Text(
                                    text = "No announcements available.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ThemeColorUtils.lightGray(Color.Gray)
                                )
                            }
                        }
                    }
                } else {
                    items(announcements.size) { index ->
                        AnnouncementItem(announcement = announcements[index])
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncementItem(announcement: Announcement) {
    val priorityColor = when (announcement.priority) {
        "high" -> Color(0xFFE1615A)
        "normal" -> Color(0xFF589CDA)
        "low" -> Color(0xFF689169)
        else -> ThemeColorUtils.lightGray(Color.Gray)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.white()
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                )
                if (announcement.priority == "high") {
                    Box(
                        modifier = Modifier
                            .background(priorityColor, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Important",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColorUtils.white(),
                            fontSize = 10.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            val indentedContent = buildAnnotatedString {
                withStyle(style = ParagraphStyle(textIndent = TextIndent(firstLine = 24.sp))) {
                    append(announcement.content)
                }
            }
            
            Text(
                text = indentedContent,
                style = MaterialTheme.typography.bodyMedium,
                color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFFB0B0B0) else Color(0xFF464644),
                textAlign = TextAlign.Justify
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(announcement.date)),
                style = MaterialTheme.typography.bodySmall,
                color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFF888888) else Color.Gray
            )
        }
    }
}
