package com.bisu.chickcare.frontend.screen

import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

data class DiseaseResource(
    val id: String,
    val title: String,
    val description: String,
    val source: String,
    val url: String,
    val category: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiseaseDatabaseScreen(navController: NavController) {
    val context = LocalContext.current
    
    val resources = remember {
        listOf(
            DiseaseResource(
                id = "1",
                title = "USDA Animal Disease Information",
                description = "Comprehensive information about poultry diseases from the United States Department of Agriculture. Includes symptoms, prevention, and treatment guidelines.",
                source = "USDA",
                url = "https://www.aphis.usda.gov/aphis/ourfocus/animalhealth/animal-disease-information",
                category = "Government"
            ),
            DiseaseResource(
                id = "2",
                title = "Merck Veterinary Manual - Poultry Diseases",
                description = "Authoritative reference on poultry diseases, including detailed descriptions, diagnostic procedures, and treatment protocols.",
                source = "Merck Manual",
                url = "https://www.merckvetmanual.com/poultry",
                category = "Reference"
            ),
            DiseaseResource(
                id = "3",
                title = "The Poultry Site - Disease Guide",
                description = "Extensive database of poultry diseases with articles, case studies, and management advice from industry experts.",
                source = "The Poultry Site",
                url = "https://www.thepoultrysite.com/disease-guide",
                category = "Industry"
            ),
            DiseaseResource(
                id = "4",
                title = "World Organisation for Animal Health (OIE)",
                description = "International standards and guidelines for poultry health and disease prevention. Includes disease reporting and control measures.",
                source = "OIE",
                url = "https://www.woah.org/en/disease/",
                category = "International"
            ),
            DiseaseResource(
                id = "5",
                title = "Extension Poultry - Disease Management",
                description = "Educational resources on poultry disease management from university extension services. Practical guides for farmers.",
                source = "University Extension",
                url = "https://extension.umn.edu/poultry/poultry-diseases",
                category = "Education"
            ),
            DiseaseResource(
                id = "6",
                title = "FAO Animal Health - Poultry",
                description = "Food and Agriculture Organization resources on poultry health, disease prevention, and sustainable farming practices.",
                source = "FAO",
                url = "http://www.fao.org/animal-health/en/",
                category = "International"
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Disease Database",
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
                .background(ThemeColorUtils.beige(Color(0xFFF5F5DC)))
        ) {
            // Info Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "External Resources",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                        Text(
                            text = "These links open in your browser",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeColorUtils.lightGray(Color.Gray)
                        )
                    }
                }
            }

            // Resources List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(resources, key = { it.id }) { resource ->
                    DiseaseResourceCard(
                        resource = resource,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, resource.url.toUri())
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DiseaseResourceCard(
    resource: DiseaseResource,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFDA8041).copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        tint = Color(0xFFDA8041),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resource.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = Color(0xFF2196F3).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = resource.source,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open link",
                    tint = Color(0xFFDA8041),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = resource.description,
                style = MaterialTheme.typography.bodyMedium,
                color = ThemeColorUtils.lightGray(Color.Gray)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = resource.category,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFDA8041),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
