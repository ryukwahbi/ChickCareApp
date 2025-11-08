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
import androidx.compose.material.icons.filled.Grass
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

data class BreedingResource(
    val id: String,
    val title: String,
    val description: String,
    val source: String,
    val url: String,
    val category: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedingRecordsScreen(navController: NavController) {
    val context = LocalContext.current
    
    val resources = remember {
        listOf(
            BreedingResource(
                id = "1",
                title = "American Poultry Association - Breeding Standards",
                description = "Official breeding standards and guidelines for various chicken breeds. Comprehensive breed information and breeding practices.",
                source = "APA",
                url = "https://www.amerpoultryassn.com/",
                category = "Association"
            ),
            BreedingResource(
                id = "2",
                title = "Poultry Breeding & Genetics Resources",
                description = "Scientific resources on poultry genetics, breeding methods, and genetic improvement from agricultural universities.",
                source = "Agricultural Extension",
                url = "https://extension.psu.edu/poultry-breeding-and-genetics",
                category = "Education"
            ),
            BreedingResource(
                id = "3",
                title = "Livestock Conservancy - Chicken Breeds",
                description = "Information on heritage chicken breeds, breeding programs, and conservation efforts for rare poultry breeds.",
                source = "Livestock Conservancy",
                url = "https://livestockconservancy.org/heritage-chicken-breeds/",
                category = "Conservation"
            ),
            BreedingResource(
                id = "4",
                title = "Poultry Hub - Breeding & Reproduction",
                description = "Technical information on poultry breeding, reproduction, genetics, and management practices for breeders.",
                source = "Poultry Hub",
                url = "https://www.poultryhub.org/",
                category = "Industry"
            ),
            BreedingResource(
                id = "5",
                title = "FAO - Poultry Breeding Programs",
                description = "International guidelines and best practices for poultry breeding programs from the Food and Agriculture Organization.",
                source = "FAO",
                url = "http://www.fao.org/poultry-production-products/production/poultry-breeding/en/",
                category = "International"
            ),
            BreedingResource(
                id = "6",
                title = "Backyard Chickens - Breeding Guide",
                description = "Practical breeding guides and community resources for backyard chicken breeders. Includes breed selection and breeding tips.",
                source = "Backyard Chickens",
                url = "https://www.backyardchickens.com/articles/categories/breeding.43/",
                category = "Community"
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Breeding Records",
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Grass,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "External Breeding Resources",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "Tap any resource to open in your browser",
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
                    BreedingResourceCard(
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
fun BreedingResourceCard(
    resource: BreedingResource,
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
                        .background(Color(0xFF4CAF50).copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Grass,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
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
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = resource.source,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
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
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

}