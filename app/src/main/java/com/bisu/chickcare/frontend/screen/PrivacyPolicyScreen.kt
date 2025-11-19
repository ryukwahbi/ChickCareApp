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
fun PrivacyPolicyScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Privacy Policy",
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
                            text = "Privacy Policy",
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
                    SectionBlock(
                        title = "1. Introduction",
                        body = "ChickCare respects your privacy and is committed to protecting your personal data. This privacy policy will inform you about how we look after your personal data when you visit our app and tell you about your privacy rights and how the law protects you."
                    )
                }
                item {
                    HorizontalDivider(color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)), thickness = 1.dp)
                }
                item {
                    SectionBlock(
                        title = "2. Data We Collect",
                        body = "We collect the following types of personal data:\n\n" +
                                "• Account Information: Name, email address, contact number\n" +
                                "• Profile Information: Farm details, location, experience\n" +
                                "• Health Data: Chicken health records, detection history\n" +
                                "• Device Information: Device type, operating system, unique device identifiers\n" +
                                "• Usage Data: How you use our app, features accessed, timestamps"
                    )
                }
                item {
                    HorizontalDivider(color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)), thickness = 1.dp)
                }
                item {
                    SectionBlock(
                        title = "3. How We Use Your Data",
                        body = "We use your personal data for the following purposes:\n\n" +
                                "• To provide and maintain our service\n" +
                                "• To notify you about changes to our service\n" +
                                "• To provide customer support\n" +
                                "• To gather analysis or valuable information to improve our service\n" +
                                "• To monitor the usage of our service\n" +
                                "• To detect, prevent and address technical issues"
                    )
                }
                item {
                    HorizontalDivider(color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)), thickness = 1.dp)
                }
                item {
                    SectionBlock(
                        title = "4. Data Security",
                        body = "We implement appropriate technical and organizational security measures to protect your personal data. However, no method of transmission over the Internet or electronic storage is 100% secure."
                    )
                }
                item {
                    HorizontalDivider(color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)), thickness = 1.dp)
                }
                item {
                    SectionBlock(
                        title = "5. Your Rights",
                        body = "You have the right to:\n\n" +
                                "• Access your personal data\n" +
                                "• Rectify inaccurate data\n" +
                                "• Request deletion of your data\n" +
                                "• Object to processing of your data\n" +
                                "• Request restriction of processing\n" +
                                "• Request transfer of your data\n" +
                                "• Withdraw consent at any time"
                    )
                }
                item {
                    HorizontalDivider(color = ThemeColorUtils.darkGray(Color(0xFF7E7C7C)), thickness = 1.dp)
                }
                item {
                    SectionBlock(
                        title = "6. Contact Us",
                        body = "If you have any questions about this Privacy Policy, please contact us at:\n\n" +
                                "Email: chickcaresupp0rt@gmail.com\n" +
                                "Facebook: ChickCare Support Team"
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionBlock(
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
