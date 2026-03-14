package com.bisu.chickcare.frontend.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Search and filter states
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    
    val resources = remember {
        listOf(
            DiseaseResource(
                id = "1",
                title = "USDA APHIS - Animal Disease Information",
                description = "Official animal disease information from the United States Department of Agriculture Animal and Plant Health Inspection Service. Authoritative source for disease reporting, prevention, and control measures.",
                source = "USDA APHIS",
                url = "https://www.aphis.usda.gov/aphis/ourfocus/animalhealth",
                category = "Government Authority"
            ),
            DiseaseResource(
                id = "2",
                title = "Merck Veterinary Manual - Poultry",
                description = "World-renowned veterinary reference manual providing comprehensive, peer-reviewed information on poultry diseases, diagnostics, and treatment protocols. Trusted by veterinarians worldwide.",
                source = "Merck & Co., Inc.",
                url = "https://www.merckvetmanual.com/poultry",
                category = "Medical Reference"
            ),
            DiseaseResource(
                id = "3",
                title = "WOAH - World Organisation for Animal Health",
                description = "International intergovernmental organization setting standards for animal health, including poultry diseases. Official source for global disease reporting and control measures.",
                source = "WOAH",
                url = "https://www.woah.org/en/what-we-do/animal-health-and-welfare/animal-diseases/",
                category = "International Organization"
            ),
            DiseaseResource(
                id = "4",
                title = "FAO - Animal Production and Health",
                description = "United Nations Food and Agriculture Organization providing authoritative resources on poultry health, disease prevention, and sustainable animal production practices.",
                source = "FAO - United Nations",
                url = "https://www.fao.org/animal-production/en/",
                category = "International Organization"
            ),
            DiseaseResource(
                id = "5",
                title = "CDC - Avian Influenza Information",
                description = "Centers for Disease Control and Prevention official information on avian influenza and other zoonotic poultry diseases. Critical for public health awareness and disease prevention.",
                source = "CDC",
                url = "https://www.cdc.gov/flu/avianflu/index.htm",
                category = "Government Authority"
            ),
            DiseaseResource(
                id = "6",
                title = "USDA Defend the Flock Program",
                description = "USDA's official program for protecting poultry flocks from disease. Provides biosecurity guidelines, disease prevention strategies, and educational resources for poultry owners.",
                source = "USDA",
                url = "https://www.aphis.usda.gov/aphis/ourfocus/animalhealth/animal-disease-information/avian/defend-the-flock-program",
                category = "Government Authority"
            ),
            DiseaseResource(
                id = "7",
                title = "AVMA - Animal Health Resources",
                description = "Leading professional organization representing veterinarians. Provides authoritative resources on animal health, including poultry diseases, treatment protocols, and veterinary best practices.",
                source = "AVMA",
                url = "https://www.avma.org/resources-tools/animal-health-and-welfare",
                category = "Professional Association"
            ),
            DiseaseResource(
                id = "8",
                title = "WHO - Zoonotic Diseases",
                description = "World Health Organization information on zoonotic diseases including avian influenza. Provides global health guidelines and disease surveillance information.",
                source = "WHO",
                url = "https://www.who.int/health-topics/zoonoses",
                category = "International Organization"
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
                        color = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.navigate("dashboard") {
                                popUpTo("dashboard") { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color(0xFF141617) else Color.White,
                    titleContentColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) Color.White else Color(0xFF231C16)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
        ) {
            // Info Banner - Enhanced
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0C5C0F).copy(alpha = 0.08f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color(0xFF0C5C0F).copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFF0C5C0F),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Verified Professional Resources",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0C5C0F),
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "All resources are from authoritative sources: government agencies, international organizations, academic institutions, and professional associations. Links open in your browser.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeColorUtils.darkGray(Color(0xFF666666)),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search resources...", color = ThemeColorUtils.lightGray(Color(0xFF999999))) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = ThemeColorUtils.black()
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = ThemeColorUtils.lightGray(Color(0xFF999999))
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThemeColorUtils.black(),
                    unfocusedBorderColor = ThemeColorUtils.black(),
                    focusedTextColor = ThemeColorUtils.darkGray(Color(0xFF231C16)),
                    unfocusedTextColor = ThemeColorUtils.darkGray(Color(0xFF231C16)),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Category Filter Chips
            val categories = remember {
                resources.map { it.category }.distinct().sorted()
            }
            
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0C5C0F),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF0C5C0F).copy(alpha = 0.1f),
                            labelColor = Color(0xFF0C5C0F)
                        )
                    )
                }
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { 
                            selectedCategory = if (selectedCategory == category) null else category
                        },
                        label = { Text(category, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0C5C0F),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF0C5C0F).copy(alpha = 0.1f),
                            labelColor = Color(0xFF0C5C0F)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            // Filtered Resources
            val filteredResources = remember(searchQuery, selectedCategory) {
                resources.filter { resource ->
                    val matchesSearch = searchQuery.isEmpty() || 
                        resource.title.contains(searchQuery, ignoreCase = true) ||
                        resource.description.contains(searchQuery, ignoreCase = true) ||
                        resource.source.contains(searchQuery, ignoreCase = true) ||
                        resource.category.contains(searchQuery, ignoreCase = true)
                    
                    val matchesCategory = selectedCategory == null || resource.category == selectedCategory
                    
                    matchesSearch && matchesCategory
                }
            }

            // Resources List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (filteredResources.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = ThemeColorUtils.lightGray(Color(0xFFBDBDBD)),
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = "No resources found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ThemeColorUtils.darkGray(Color(0xFF666666))
                                )
                                Text(
                                    text = "Try adjusting your search or filter",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ThemeColorUtils.lightGray(Color(0xFF999999))
                                )
                            }
                        }
                    }
                } else {
                    items(filteredResources, key = { it.id }) { resource ->
                        DiseaseResourceCard(
                            resource = resource,
                            onOpenLink = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, resource.url.toUri())
                                    context.startActivity(intent)
                                    android.widget.Toast.makeText(
                                        context,
                                        "Opening ${resource.source}...",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } catch (_: Exception) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Unable to open link. Please check your internet connection.",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            onCopyLink = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Resource URL", resource.url)
                                clipboard.setPrimaryClip(clip)
                                android.widget.Toast.makeText(
                                    context,
                                    "URL copied to clipboard",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiseaseResourceCard(
    resource: DiseaseResource,
    onOpenLink: () -> Unit,
    onCopyLink: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ThemeColorUtils.surface(Color.White)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Title and Source
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resource.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.darkGray(Color(0xFF231C16)),
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Source badge - enhanced
                    Surface(
                        color = Color(0xFF0C5C0F).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = resource.source,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0C5C0F),
                            fontSize = 12.sp
                        )
                    }
                }
                
                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy link button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Color(0xFF0C5C0F).copy(alpha = 0.08f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(onClick = onCopyLink),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy URL",
                            tint = Color(0xFF0C5C0F),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    // Open link button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Color(0xFF0C5C0F).copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(onClick = onOpenLink),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in browser",
                            tint = Color(0xFF0C5C0F),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Divider
            HorizontalDivider(
                color = ThemeColorUtils.lightGray(Color(0xFFE0E0E0)),
                thickness = 1.dp
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = resource.description,
                style = MaterialTheme.typography.bodyMedium,
                color = ThemeColorUtils.darkGray(Color(0xFF424242)),
                lineHeight = 20.sp,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            
            // Category badge
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF0C5C0F).copy(alpha = 0.08f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = resource.category,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF0C5C0F),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
