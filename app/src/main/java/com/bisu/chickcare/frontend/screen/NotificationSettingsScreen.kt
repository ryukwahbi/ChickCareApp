package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(navController: NavController) {
    var pushNotificationsEnabled by remember { mutableStateOf(true) }
    var detectionAlertsEnabled by remember { mutableStateOf(true) }
    var reminderNotificationsEnabled by remember { mutableStateOf(true) }
    var friendRequestsEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Notifications",
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
                .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
        ) {
            // Divider below top bar
            HorizontalDivider(
                color = ThemeColorUtils.lightGray(Color(0xFF7E7C7C)),
                thickness = 1.dp
            )
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Notifications
            item {
                Text(
                    text = "General",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColorUtils.darkGray(Color(0xFF231C16)),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                NotificationSettingCard(
                    icon = R.drawable.ic_push_notifications_flaticon,
                    title = "Push Notifications",
                    description = "Receive notifications on your device",
                    isEnabled = pushNotificationsEnabled,
                    onToggle = { pushNotificationsEnabled = it }
                )
            }

            // Detection & Health Alerts
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Detection & Health",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColorUtils.darkGray(Color(0xFF231C16)),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                NotificationSettingCard(
                    icon = R.drawable.ic_detection_alerts_flaticon,
                    title = "Detection Alerts",
                    description = "Get notified when health issues are detected",
                    isEnabled = detectionAlertsEnabled,
                    onToggle = { detectionAlertsEnabled = it }
                )
            }

            item {
                NotificationSettingCard(
                    icon = R.drawable.ic_reminder_flaticon,
                    title = "Reminder Notifications",
                    description = "Receive reminders for vaccinations and checkups",
                    isEnabled = reminderNotificationsEnabled,
                    onToggle = { reminderNotificationsEnabled = it }
                )
            }

            // Social Notifications
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Social",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColorUtils.darkGray(Color(0xFF231C16)),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                NotificationSettingCard(
                    icon = R.drawable.ic_friend_requests_flaticon,
                    title = "Friend Requests",
                    description = "Get notified about friend requests",
                    isEnabled = friendRequestsEnabled,
                    onToggle = { friendRequestsEnabled = it }
                )
            }

            // Notification Preferences
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColorUtils.darkGray(Color(0xFF231C16)),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                NotificationSettingCard(
                    icon = R.drawable.ic_vibration_flaticon,
                    title = "Vibration",
                    description = "Enable vibration for notifications",
                    isEnabled = vibrationEnabled,
                    onToggle = { vibrationEnabled = it }
                )
            }

            item {
                NotificationSettingCard(
                    icon = R.drawable.ic_notifications_off_flaticon,
                    title = "Sound",
                    description = "Play sound for notifications",
                    isEnabled = soundEnabled,
                    onToggle = { soundEnabled = it }
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ThemeColorUtils.white()
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_notifications_flaticon),
                            contentDescription = null,
                            modifier = Modifier.size(38.dp),
                            colorFilter = if (ThemeViewModel.isDarkMode) {
                                ColorFilter.tint(
                                    color = ThemeColorUtils.lightGray(Color(0xFFA1AAB2)),
                                    blendMode = BlendMode.SrcAtop
                                )
                            } else {
                                null
                            }
                        )
                        Text(
                            text = "You can customize notification preferences for each category",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
fun NotificationSettingCard(
    @androidx.annotation.DrawableRes icon: Int,
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.beige(Color(0xFFE5E2DE))
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 5.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 6.dp,
            focusedElevation = 6.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = title,
                modifier = Modifier.size(38.dp),
                colorFilter = if (ThemeViewModel.isDarkMode) {
                    ColorFilter.tint(
                        color = ThemeColorUtils.lightGray(Color(0xFFA1AAB2)),
                        blendMode = BlendMode.SrcAtop
                    )
                } else {
                    null
                }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeColorUtils.lightGray(Color(0xFF666666))
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ThemeColorUtils.white(),
                    checkedTrackColor = Color(0xFF131211),
                    uncheckedThumbColor = ThemeColorUtils.white(),
                    uncheckedTrackColor = Color(0xFFA9A9A9)
                )
            )
        }
    }
}