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
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bisu.chickcare.R
import com.bisu.chickcare.frontend.utils.ShareUtils
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        androidx.compose.ui.res.stringResource(R.string.about_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ThemeColorUtils.black()
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.navigate("settings") {
                                popUpTo("settings") { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    ) {
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
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = ThemeColorUtils.white()
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 28.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.chicken_icon),
                                contentDescription = androidx.compose.ui.res.stringResource(R.string.app_logo_desc),
                                modifier = Modifier.size(60.dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(R.string.about_app_name),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeColorUtils.black()
                                )
                                Text(
                                    text = androidx.compose.ui.res.stringResource(R.string.about_version),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ThemeColorUtils.darkGray(Color(0xFF666666))
                                )
                            }
                        }
                    }
                }

                item {
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.about_desc_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemeColorUtils.black()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.about_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThemeColorUtils.darkGray(Color(0xFF666666)),
                                lineHeight = 22.sp,
                                textAlign = TextAlign.Justify
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.about_features_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black(),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FeatureTextItem(
                            title = androidx.compose.ui.res.stringResource(R.string.about_feat_ai),
                            description = androidx.compose.ui.res.stringResource(R.string.about_feat_ai_desc)
                        )
                        FeatureTextItem(
                            title = androidx.compose.ui.res.stringResource(R.string.about_feat_realtime),
                            description = androidx.compose.ui.res.stringResource(R.string.about_feat_realtime_desc)
                        )
                        FeatureTextItem(
                            title = androidx.compose.ui.res.stringResource(R.string.about_feat_records),
                            description = androidx.compose.ui.res.stringResource(R.string.about_feat_records_desc)
                        )
                        FeatureTextItem(
                            title = androidx.compose.ui.res.stringResource(R.string.about_feat_vaccine),
                            description = androidx.compose.ui.res.stringResource(R.string.about_feat_vaccine_desc)
                        )
                        FeatureTextItem(
                            title = androidx.compose.ui.res.stringResource(R.string.about_feat_community),
                            description = androidx.compose.ui.res.stringResource(R.string.about_feat_community_desc)
                        )
                        FeatureTextItem(
                            title = androidx.compose.ui.res.stringResource(R.string.about_feat_offline),
                            description = androidx.compose.ui.res.stringResource(R.string.about_feat_offline_desc)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    // Share App Button
                    Button(
                        onClick = {
                            ShareUtils.shareAppDownloadLink(context)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF26C0C4)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = androidx.compose.ui.res.stringResource(R.string.about_share_btn),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.about_share_btn),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }


            }
        }
    }
}

@Composable
fun FeatureTextItem(
    title: String,
    description: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = "- ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ThemeColorUtils.black()
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ThemeColorUtils.black()
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = ThemeColorUtils.darkGray(Color(0xFF666666)),
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}