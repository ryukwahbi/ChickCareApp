package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Terms of Service",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ThemeColorUtils.black()
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
            HorizontalDivider(
                color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)),
                thickness = 1.dp
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Terms of Service",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColorUtils.black()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Last Updated: November 2025",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeColorUtils.darkGray(Color(0xFF666666))
                        )
                    }
                }
                item {
                    HorizontalDivider(color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)), thickness = 1.dp)
                }
                item {
                    TermsSectionBlock(
                        title = "1. Agreement to Terms",
                        body = "By accessing or using ChickCare, you agree to be bound by these Terms of Service and all applicable laws and regulations. If you do not agree with any of these terms, you are prohibited from using or accessing this app."
                    )
                }
                item {
                    HorizontalDivider(color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)), thickness = 1.dp)
                }
                item {
                    TermsSectionBlock(
                        title = "2. Use License",
                        body = "Permission is granted to temporarily download one copy of ChickCare for personal, non-commercial transitory viewing only. This is the grant of a license, not a transfer of title, and under this license you may not:\n\n" +
                                "• Modify or copy the materials\n" +
                                "• Use the materials for any commercial purpose\n" +
                                "• Attempt to decompile or reverse engineer any software\n" +
                                "• Remove any copyright or other proprietary notations"
                    )
                }
                item {
                    HorizontalDivider(color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)), thickness = 1.dp)
                }
                item {
                    TermsSectionBlock(
                        title = "3. User Account",
                        body = "You are responsible for maintaining the confidentiality of your account and password. You agree to accept responsibility for all activities that occur under your account or password."
                    )
                }
                item {
                    HorizontalDivider(color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)), thickness = 1.dp)
                }
                item {
                    TermsSectionBlock(
                        title = "4. Service Availability",
                        body = "We reserve the right to modify or discontinue the service at any time without prior notice. We shall not be liable to you or any third party for any modification, suspension, or discontinuance of the service."
                    )
                }
                item {
                    HorizontalDivider(color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)), thickness = 1.dp)
                }
                item {
                    TermsSectionBlock(
                        title = "5. Limitation of Liability",
                        body = "In no event shall ChickCare or its suppliers be liable for any damages (including, without limitation, damages for loss of data or profit, or due to business interruption) arising out of the use or inability to use the app."
                    )
                }
                item {
                    HorizontalDivider(color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)), thickness = 1.dp)
                }
                item {
                    TermsSectionBlock(
                        title = "6. Accuracy of Information",
                        body = "The materials appearing in ChickCare could include technical, typographical, or photographic errors. ChickCare does not warrant that any of the materials on its app are accurate, complete, or current."
                    )
                }
                item {
                    HorizontalDivider(color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)), thickness = 1.dp)
                }
                item {
                    TermsSectionBlock(
                        title = "7. Contact Information",
                        body = "If you have any questions about these Terms of Service, please contact us at:\n\n" +
                                "Email: chickcaresupp0rt@gmail.com\n" +
                                "Facebook: ChickCare Support Team"
                    )
                }
            }
        }
    }
}

@Composable
private fun TermsSectionBlock(
    title: String,
    body: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ThemeColorUtils.black()
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = ThemeColorUtils.darkGray(Color(0xFF666666)),
            lineHeight = 22.sp
        )
    }
}
