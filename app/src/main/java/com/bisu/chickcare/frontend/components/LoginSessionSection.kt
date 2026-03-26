package com.bisu.chickcare.frontend.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisu.chickcare.backend.data.LoginSessionData
import com.bisu.chickcare.backend.viewmodels.LoginSessionViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LoginSessionSection(
    viewModel: LoginSessionViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessions by viewModel.sessions.collectAsState()
    val currentDeviceId = remember { viewModel.getCurrentDeviceId(context) }

    var showLogoutDialog by remember { mutableStateOf<LoginSessionData?>(null) }
    var showLogoutAllDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current Device Section
        val currentSession = sessions.find { it.sessionId == currentDeviceId }
        if (currentSession != null) {
            Text(
                text = "Current Session",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ThemeColorUtils.black(),
                modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
            )

            SessionCard(
                session = currentSession,
                isCurrentDevice = true,
                onLogoutClick = { /* Cannot logout current session from here */ }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Other Devices Section
        val otherSessions = sessions.filter { it.sessionId != currentDeviceId }
        if (otherSessions.isNotEmpty()) {
            Text(
                text = "Other Devices",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ThemeColorUtils.black(),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            otherSessions.forEach { session ->
                SessionCard(
                    session = session,
                    isCurrentDevice = false,
                    onLogoutClick = { showLogoutDialog = session }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Logout All Button
            OutlinedButton(
                onClick = { showLogoutAllDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFD32F2F) // Soft red
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Log Out of All Other Devices",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        } else {
            // Information note if no other devices
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = ThemeColorUtils.beige(Color(0xFFF5F5F5)).copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ThemeColorUtils.lightGray(Color.Gray).copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = ThemeColorUtils.lightGray(Color.Gray),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "You are currently only logged into this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeColorUtils.lightGray(Color.Gray)
                    )
                }
            }
        }
    }

    // Individual Logout Dialog
    if (showLogoutDialog != null) {
        val session = showLogoutDialog!!
        AlertDialog(
            onDismissRequest = { showLogoutDialog = null },
            title = {
                Text("Log Out Device", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to log out from ${session.deviceName}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.revokeSession(session.sessionId)
                        showLogoutDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Log Out", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = null }) {
                    Text("Cancel", color = ThemeColorUtils.black())
                }
            },
            containerColor = ThemeColorUtils.white()
        )
    }

    // Logout All Dialog
    if (showLogoutAllDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutAllDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Log Out All Devices", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("You will be signed out of all devices except this one. You'll need to sign back in on other devices.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.revokeAllOtherSessions(context)
                        showLogoutAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Log Out All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutAllDialog = false }) {
                    Text("Cancel", color = ThemeColorUtils.black())
                }
            },
            containerColor = ThemeColorUtils.white()
        )
    }
}

@Composable
fun SessionCard(
    session: LoginSessionData,
    isCurrentDevice: Boolean,
    onLogoutClick: () -> Unit
) {
    val icon: ImageVector = if (session.deviceType == "computer") {
        Icons.Default.Computer
    } else {
        Icons.Default.PhoneAndroid
    }

    val displayDate = remember(session.timestamp) {
        if (session.timestamp != null) {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            sdf.format(session.timestamp)
        } else {
            "Just now"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.white()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon Background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCurrentDevice)
                                Color(0xFF4CAF50).copy(alpha = 0.1f)
                            else
                                ThemeColorUtils.primary().copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = session.deviceType,
                        tint = if (isCurrentDevice) Color(0xFF4CAF50) else ThemeColorUtils.primary(),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.deviceName.ifEmpty { "Unknown Device" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black(),
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = session.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeColorUtils.lightGray(Color.Gray),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "IP: ${session.ipAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeColorUtils.lightGray(Color.Gray),
                        fontSize = 12.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Status row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCurrentDevice) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Active now",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = displayDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = ThemeColorUtils.lightGray(Color.Gray)
                            )
                        }
                    }
                }

                if (!isCurrentDevice) {
                    Text(
                        text = "Log out",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable(onClick = onLogoutClick)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}
