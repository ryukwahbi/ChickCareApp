package com.bisu.chickcare.frontend.screen

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.bisu.chickcare.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    navController: NavController,
    imageUri: String?,
    audioUri: String?,
    // The screen now receives the final result directly
    status: String,
    suggestions: List<String>
) {
    // No more ViewModel, LaunchedEffect, or local state for loading/results needed.
    // The screen is now "dumb" and only displays what it's given.

    val isHealthy = status.equals("Healthy", ignoreCase = true)
    val resultColor = if (isHealthy) Color(0xFF4CAF50) else Color(0xFFF44336)
    val backgroundUrl = "https://media.istockphoto.com/id/1342480600/photo/free-range-healthy-brown-organic-chickens-and-a-white-rooster-on-a-green-meadow.jpg?s=612x612&w=0&k=20&c=HWwPGRkHpEnObkcsMzopcmXorwHD0PS7NQ1EiA8K53c="

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detection Result") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFD2B48C),
                    titleContentColor = Color(0xFF8B4513)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Brush.verticalGradient(colors = listOf(Color(0xFFF5F5DC), Color(0xFFD2B48C))))
        ) {
            AsyncImage(
                model = backgroundUrl,
                contentDescription = "Background",
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.3f),
                contentScale = ContentScale.Crop
            )

            // No need for the `if (isLoading)` block, we display the results directly.
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Result Display Card ---
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                        elevation = CardDefaults.cardElevation(8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Decode the URI before passing it to AsyncImage
                            val decodedImageUri = imageUri?.let { Uri.decode(it) }
                            AsyncImage(
                                model = decodedImageUri ?: R.drawable.chicken_icon,
                                contentDescription = "Detection Input Image",
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            AnimatedVisibility(visible = true, enter = scaleIn() + fadeIn()) {
                                Text(
                                    text = "Result: $status",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp,
                                        color = resultColor
                                    )
                                )
                            }
                        }
                    }
                }

                // --- Remedies Card (only shows if not healthy and suggestions exist) ---
                if (!isHealthy && suggestions.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                            elevation = CardDefaults.cardElevation(8.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text("Recommended Remedies", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(8.dp))
                                suggestions.forEach { suggestion ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Healing,
                                            contentDescription = null,
                                            tint = Color(0xFF8B4513),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = suggestion, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Action Buttons ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { navController.popBackStack() }, // Go back to retake
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD27D2D)),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Retake")
                        }

                        Button(
                            onClick = { navController.navigate("dashboard") { popUpTo(navController.graph.startDestinationId) } },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}
