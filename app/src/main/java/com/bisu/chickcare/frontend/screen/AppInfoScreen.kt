package com.bisu.chickcare.frontend.screen

// ... imports
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.bisu.chickcare.R
import com.bisu.chickcare.frontend.utils.ShareUtils
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoScreen(navController: NavController) {
    val context = LocalContext.current
    var showComingSoon by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "App Information",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.darkGray(Color(0xFF231C16))
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.darkGray(Color(0xFF231C16))
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (ThemeViewModel.isDarkMode) Color(0xFF141617) else Color(0xFFFFFFFF),
                    titleContentColor = if (ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.darkGray(Color(0xFF231C16))
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(ThemeColorUtils.beige(Color(0xFFFFF7E6))),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // App Header Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = ThemeColorUtils.white()
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(5.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.chicken_icon),
                                contentDescription = "App Icon",
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "ChickCare",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontFamily = com.bisu.chickcare.ui.theme.FiraSans,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else Color(0xFF664512)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Version 1.0.0",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (ThemeViewModel.isDarkMode) Color(0xFFB0B0B0) else ThemeColorUtils.darkGray(Color(0xFF666666))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = if (ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else Color(0xFF664512),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Official Release",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else Color(0xFF664512),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Quick Actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionButton(
                            icon = Icons.Default.Share,
                            label = "Share App",
                            color = Color(0xFF0C5C0F),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                ShareUtils.shareAppDownloadLink(context)
                            }
                        )
                        QuickActionButton(
                            icon = Icons.Default.Star,
                            label = "Rate App",
                            color = Color(0xFFFF9800),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                // Open Play Store rating
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = "market://details?id=${context.packageName}".toUri()
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data =
                                            "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
                }
                
                // Expandable Sections
                item {
                    ExpandableSection(
                        title = "About the App",
                        icon = Icons.Default.Info,
                        iconColor = Color(0xFF8D6E63)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "     ChickCare is designed to make poultry health management easier and more effective for farmers. By using smart AI technology, the app helps you detect potential diseases early and provides clear, actionable insights to keep your flock healthy.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (ThemeViewModel.isDarkMode) Color(0xFFB0B0B0) else ThemeColorUtils.darkGray(Color(0xFF424242)),
                                lineHeight = 22.sp,
                                textAlign = TextAlign.Justify
                            )
                            Text(
                                text = "     Our goal is to support your farming journey by helping you minimize losses and improve productivity. With tools for early detection and health monitoring, ChickCare is your partner in maintaining a thriving and sustainable poultry farm.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (ThemeViewModel.isDarkMode) Color(0xFFB0B0B0) else ThemeColorUtils.darkGray(Color(0xFF424242)),
                                lineHeight = 22.sp,
                                textAlign = TextAlign.Justify
                            )
                        }
                    }
                }
                
                item {
                    ExpandableSection(
                        title = "Technology & AI",
                        icon = Icons.Default.Science,
                        iconColor = Color(0xFF8D6E63),
                        isLocked = true,
                        onLockedClick = { showComingSoon = true }
                    ) {
                        // Content hidden
                    }
                }
                
                item {
                    ExpandableSection(
                        title = "Development Team",
                        icon = Icons.Default.People,
                        iconColor = Color(0xFF8D6E63),
                        isLocked = true,
                        onLockedClick = { showComingSoon = true }
                    ) {
                         // Content hidden
                    }
                }
                
                item {
                    ExpandableSection(
                        title = "Contact & Support",
                        icon = Icons.Default.Phone,
                        iconColor = Color(0xFF8D6E63),
                        isLocked = true,
                        onLockedClick = { showComingSoon = true }
                    ) {
                         // Content hidden
                    }
                }
                
                item {
                    ExpandableSection(
                        title = "Acknowledgments",
                        icon = Icons.Default.Star,
                        iconColor = Color(0xFF8D6E63),
                        isLocked = true,
                        onLockedClick = { showComingSoon = true }
                    ) {
                         // Content hidden
                    }
                }
                
                item {
                    ExpandableSection(
                        title = "Open Source & Licenses",
                        icon = Icons.Default.Code,
                        iconColor = Color(0xFF8D6E63),
                        isLocked = true,
                        onLockedClick = { showComingSoon = true }
                    ) {
                         // Content hidden
                    }
                }
                
                item {
                    ExpandableSection(
                        title = "App Statistics",
                        icon = Icons.Default.Language,
                        iconColor = Color(0xFF8D6E63),
                        isLocked = true,
                        onLockedClick = { showComingSoon = true }
                    ) {
                         // Content hidden
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            
            // Coming Soon Dialog
            if (showComingSoon) {
                ComingSoonDialog(onDismiss = { showComingSoon = false })
            }
        }
    }
}

@Composable
fun ComingSoonDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = ThemeColorUtils.white()
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFFFF3E0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Text(
                    text = "Coming Soon!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColorUtils.black()
                )
                
                Text(
                    text = "This feature is currently under development. Stay tuned for updates!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = ThemeColorUtils.darkGray(Color(0xFF666666)),
                    lineHeight = 22.sp
                )
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0C5C0F)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Got it", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = color
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun ExpandableSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    isLocked: Boolean = false,
    onLockedClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.white()
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(5.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        if (isLocked) {
                            onLockedClick()
                        } else {
                            isExpanded = !isExpanded 
                        }
                    }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                iconColor.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = title,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (ThemeViewModel.isDarkMode) Color(0xFFE3E5E8) else ThemeColorUtils.darkGray(Color(0xFF231C16))
                    )
                }
                
                if (isLocked) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = ThemeColorUtils.lightGray(Color.Gray).copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = if (ThemeViewModel.isDarkMode) Color(0xFFB0B0B0) else ThemeColorUtils.darkGray(Color(0xFF666666))
                    )
                }
            }
            
            AnimatedVisibility(
                visible = isExpanded && !isLocked,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = ThemeColorUtils.lightGray(Color(0xFFE0E0E0))
                )
                content()
            }
        }
    }
}
// ... existing TechnologyItem, TeamMemberCard, ContactItem, AcknowledgmentItem, LicenseItem, StatItem ...

/*
@Composable
fun TechnologyItem(name: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(Color(0xFF0C5C0F), CircleShape)
                .padding(top = 6.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = ThemeColorUtils.darkGray(Color(0xFF231C16))
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = ThemeColorUtils.darkGray(Color(0xFF666666))
            )
        }
    }
}

@Composable
fun TeamMemberCard(name: String, role: String, email: String) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = ThemeColorUtils.darkGray(Color(0xFF231C16))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = role,
                style = MaterialTheme.typography.bodyMedium,
                color = ThemeColorUtils.darkGray(Color(0xFF666666))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = "Email",
                    tint = Color(0xFF0C5C0F),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF0C5C0F),
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_SEND-TO).apply {
                            data = "mailto:$email".toUri()
                        }
                        context.startActivity(Intent.createChooser(intent, "Send Email"))
                    }
                )
            }
        }
    }
}

@Composable
fun ContactItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF5F5F5)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color(0xFF0C5C0F),
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeColorUtils.darkGray(Color(0xFF666666))
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Open",
                tint = Color(0xFF0C5C0F),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AcknowledgmentItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFFFF9800),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = ThemeColorUtils.darkGray(Color(0xFF424242))
        )
    }
}

@Composable
fun LicenseItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Code,
            contentDescription = null,
            tint = Color(0xFF607D8B),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = ThemeColorUtils.darkGray(Color(0xFF424242))
        )
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = ThemeColorUtils.darkGray(Color(0xFF424242))
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0C5C0F)
        )
    }
}
*/

