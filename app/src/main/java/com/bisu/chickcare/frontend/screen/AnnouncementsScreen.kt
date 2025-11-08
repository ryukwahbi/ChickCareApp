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
                content = "Thank you for using ChickCare! We're here to help you monitor and care for your chickens. Stay tuned for updates and new features.",
                date = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000),
                priority = "high"
            ),
            Announcement(
                id = "2",
                title = "New Feature: Health Trend Analysis",
                content = "Check out the new Health Trend Analysis feature in your dashboard. Track your chickens' health over the last 5 days with detailed confidence metrics.",
                date = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000), // 3 days ago
                priority = "normal"
            ),
            Announcement(
                id = "3",
                title = "Tips for Better Detection",
                content = "For best detection results, ensure good lighting and clear audio when using the image and audio detection features. Keep your chickens calm during the detection process.",
                date = System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000), // 14 days ago
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
                        color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ThemeColorUtils.darkGray(Color(0xFF231C16))
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFFFFF),
                    titleContentColor = ThemeColorUtils.darkGray(Color(0xFF231C16))
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFFFF0DB))
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
            Text(
                text = announcement.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF464644)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(announcement.date)),
                style = MaterialTheme.typography.bodySmall,
                color = ThemeColorUtils.lightGray(Color.Gray)
            )
        }
    }
}
