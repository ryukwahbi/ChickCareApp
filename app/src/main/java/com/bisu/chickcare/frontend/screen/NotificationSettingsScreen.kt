package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bisu.chickcare.R
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsRepository = remember { com.bisu.chickcare.backend.repository.SettingsRepository(context) }
    
    var pushNotificationsEnabled by remember { mutableStateOf(settingsRepository.getPushNotificationsEnabled()) }
    var detectionAlertsEnabled by remember { mutableStateOf(settingsRepository.getDetectionAlertsEnabled()) }
    var reminderNotificationsEnabled by remember { mutableStateOf(settingsRepository.getReminderNotificationsEnabled()) }
    var friendRequestsEnabled by remember { mutableStateOf(settingsRepository.getFriendRequestsEnabled()) }
    var vibrationEnabled by remember { mutableStateOf(settingsRepository.getVibrationEnabled()) }
    var soundEnabled by remember { mutableStateOf(settingsRepository.getSoundEnabled()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        androidx.compose.ui.res.stringResource(R.string.notif_settings_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ThemeColorUtils.black()
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
            // Divider below top bar
            HorizontalDivider(
                color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)),
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
                        text = androidx.compose.ui.res.stringResource(R.string.notif_section_general),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black(),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
            }

            item {
                NotificationSettingCard(
                    icon = R.drawable.ic_push_notifications_flaticon,
                    title = androidx.compose.ui.res.stringResource(R.string.notif_push_title),
                    description = androidx.compose.ui.res.stringResource(R.string.notif_push_desc),
                    isEnabled = pushNotificationsEnabled,
                    onToggle = { 
                        pushNotificationsEnabled = it
                        settingsRepository.setPushNotificationsEnabled(it)
                    }
                )
            }

            // Detection & Health Alerts
            item {
                Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.notif_section_detection),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black(),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
            }

            item {
                NotificationSettingCard(
                    icon = R.drawable.ic_detection_alerts_flaticon,
                    title = androidx.compose.ui.res.stringResource(R.string.notif_detection_title),
                    description = androidx.compose.ui.res.stringResource(R.string.notif_detection_desc),
                    isEnabled = detectionAlertsEnabled,
                    onToggle = { 
                        detectionAlertsEnabled = it
                        settingsRepository.setDetectionAlertsEnabled(it)
                    }
                )
            }

            item {
                NotificationSettingCard(
                    icon = R.drawable.ic_reminder_flaticon,
                    title = androidx.compose.ui.res.stringResource(R.string.notif_reminder_title),
                    description = androidx.compose.ui.res.stringResource(R.string.notif_reminder_desc),
                    isEnabled = reminderNotificationsEnabled,
                    onToggle = { 
                        reminderNotificationsEnabled = it
                        settingsRepository.setReminderNotificationsEnabled(it)
                    }
                )
            }

            // Social Notifications
            item {
                Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.notif_section_social),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black(),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
            }

            item {
                NotificationSettingCard(
                    icon = R.drawable.ic_friend_requests_flaticon,
                    title = androidx.compose.ui.res.stringResource(R.string.notif_friend_title),
                    description = androidx.compose.ui.res.stringResource(R.string.notif_friend_desc),
                    isEnabled = friendRequestsEnabled,
                    onToggle = { 
                        friendRequestsEnabled = it
                        settingsRepository.setFriendRequestsEnabled(it)
                    }
                )
            }

            // Notification Preferences
            item {
                Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.notif_section_pref),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black(),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
            }

            item {
                NotificationSettingCard(
                    icon = R.drawable.ic_vibration_flaticon,
                    title = androidx.compose.ui.res.stringResource(R.string.notif_vibration_title),
                    description = androidx.compose.ui.res.stringResource(R.string.notif_vibration_desc),
                    isEnabled = vibrationEnabled,
                    onToggle = { 
                        vibrationEnabled = it
                        settingsRepository.setVibrationEnabled(it)
                    }
                )
            }

            item {
                NotificationSettingCard(
                    icon = R.drawable.ic_notifications_off_flaticon,
                    title = androidx.compose.ui.res.stringResource(R.string.notif_sound_title),
                    description = androidx.compose.ui.res.stringResource(R.string.notif_sound_desc),
                    isEnabled = soundEnabled,
                    onToggle = { 
                        soundEnabled = it
                        settingsRepository.setSoundEnabled(it)
                    }
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
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.notif_cust_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeColorUtils.black()
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
        border = BorderStroke(
            width = 1.dp,
            color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C))
        ),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.surface(Color(0xFFE5E2DE)),
            contentColor = ThemeColorUtils.black()
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
                modifier = Modifier.size(38.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ThemeColorUtils.black()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeColorUtils.darkGray(Color(0xFF666666))
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ThemeColorUtils.white(),
                    checkedTrackColor = ThemeColorUtils.black(),
                    uncheckedThumbColor = ThemeColorUtils.white(),
                    uncheckedTrackColor = ThemeColorUtils.lightGray(Color(0xFF9C9FA1))
                )
            )
        }
    }
}