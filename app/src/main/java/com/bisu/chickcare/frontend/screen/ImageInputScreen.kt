package com.bisu.chickcare.frontend.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import com.bisu.chickcare.frontend.utils.persistUriToAppStorage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageInputScreen(
    navController: NavController,
    onImageSelected: (Uri) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(navBackStackEntry?.savedStateHandle) {
        navBackStackEntry?.savedStateHandle?.get<String>("capturedImageUri")?.let { uriString ->
            selectedImageUri = uriString.toUri()
            onImageSelected(uriString.toUri())
            navBackStackEntry?.savedStateHandle?.remove<String>("capturedImageUri")
        }
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            navController.navigate("camera")
        }
    }

    // Use OpenDocument instead of GetContent to support persistable URI permissions
    // OpenDocument allows us to take persistable permissions for content URIs
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                var finalUriString = it.toString()
                if (it.scheme == "content") {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        android.util.Log.d(
                            "ImageInputScreen",
                            "Taken persistable URI permission for image: $it"
                        )
                    } catch (e: Exception) {
                        android.util.Log.w(
                            "ImageInputScreen",
                            "Persistable permission not granted for image: ${e.message}"
                        )
                    }
                    persistUriToAppStorage(
                        context = context,
                        sourceUriString = it.toString(),
                        subdirectory = "detection_images",
                        fallbackExtension = "jpg",
                        logTag = "ImageInputScreen"
                    )?.let { stored ->
                        finalUriString = stored
                    }
                }

                val finalUri = finalUriString.toUri()
                selectedImageUri = finalUri
                onImageSelected(finalUri)
            }
        }
    }

    // Function to load image dimensions and calculate aspect ratio
    val scaleCamera by animateFloatAsState(
        targetValue = if (selectedImageUri == null) 1f else 0.95f,
        animationSpec = tween(300),
        label = "cameraScale"
    )

    val scaleUpload by animateFloatAsState(
        targetValue = if (selectedImageUri == null) 1f else 0.95f,
        animationSpec = tween(300),
        label = "uploadScale"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Custom top bar that extends to top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ThemeColorUtils.beige(Color(0xFFE3B386)))
                .statusBarsPadding()
                .padding(top = 1.dp, bottom = 21.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "IMAGE INPUT",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp,
                    color = ThemeColorUtils.black(),
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = ThemeColorUtils.black(alpha = 0.3f),
                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                        blurRadius = 4f
                    )
                ),
                modifier = Modifier.padding(start = 24.dp)
            )
        }

        // Main content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ThemeColorUtils.beige(Color(0xFFF1E0C9)),
                            ThemeColorUtils.beige(Color(0xFFF1E0C9))
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top section with instructions
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Chicken Image",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Take a photo or choose from your gallery",
                        fontSize = 16.sp,
                        color = Color(0xFFFD8F4C),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Important Instructions Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(12.dp),
                                spotColor = ThemeColorUtils.black(alpha = 0.15f)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ThemeColorUtils.beige(Color(0xFFFFF3CD))
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "NOTE: Image Capture Guidelines",
                                fontSize = 18.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ThemeColorUtils.darkGray(Color(0xFF575450))
                            )

                            HorizontalDivider(
                                thickness = 1.5.dp,
                                color = ThemeColorUtils.darkGray(Color(0xFF6C6242)).copy(alpha = 0.5f)
                            )

                            Text(
                                text = "—  Capture a CLEAR focused photo of the chicken's face and head area.",
                                fontSize = 15.5.sp,
                                color = ThemeColorUtils.black(),
                                lineHeight = 22.sp
                            )
                            Text(
                                text = "—  Ensure GOOD LIGHTING avoid shadows or dark areas that hide details.",
                                fontSize = 15.5.sp,
                                color = ThemeColorUtils.black(),
                                lineHeight = 22.sp
                            )
                            Text(
                                text = "—  Focus on the HEAD REGION capture symptoms like nasal discharge, facial swelling, or eye issues.",
                                fontSize = 15.5.sp,
                                color = ThemeColorUtils.black(),
                                lineHeight = 22.sp
                            )
                            Text(
                                text = "—  Get CLOSE ENOUGH the chicken should fill most of the frame for accurate analysis.",
                                fontSize = 15.5.sp,
                                color = ThemeColorUtils.black(),
                                lineHeight = 22.sp
                            )
                            Text(
                                text = "—  Keep the chicken STILL, wait for it to be calm before capturing.",
                                fontSize = 15.5.sp,
                                color = ThemeColorUtils.black(),
                                lineHeight = 22.sp
                            )

                            HorizontalDivider(
                                thickness = 1.5.dp,
                                color = ThemeColorUtils.darkGray(Color(0xFF6C6242)).copy(alpha = 0.5f)
                            )

                            Text(
                                text = "Blurry, dark, or distant photos may result in inaccurate detection results!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFCC6565),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Selection Cards (always show when no image selected)
                if (selectedImageUri == null) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                onClick = {
                                    if (hasCameraPermission) {
                                        navController.navigate("camera") {
                                        }
                                    } else {
                                        cameraLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .scale(scaleCamera)
                                    .shadow(
                                        elevation = 6.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        spotColor = ThemeColorUtils.black(alpha = 0.15f)
                                    ),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = ThemeColorUtils.white()
                            )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .shadow(
                                                elevation = 4.dp,
                                                shape = RoundedCornerShape(40.dp),
                                                spotColor = ThemeColorUtils.black(alpha = 0.2f)
                                            )
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFF606060),
                                                        Color(0xFF404040)
                                                    )
                                                ),
                                                RoundedCornerShape(40.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.CameraAlt,
                                            contentDescription = "Camera",
                                            tint = ThemeColorUtils.white(),
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Camera",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF000000)
                                    )
                                }
                            }

                            Card(
                                onClick = {
                                    galleryLauncher.launch(arrayOf("image/*"))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .scale(scaleUpload)
                                    .shadow(
                                        elevation = 6.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        spotColor = ThemeColorUtils.black(alpha = 0.15f)
                                    ),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = ThemeColorUtils.white()
                            )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .shadow(
                                                elevation = 4.dp,
                                                shape = RoundedCornerShape(40.dp),
                                                spotColor = ThemeColorUtils.black(alpha = 0.2f)
                                            )
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFFFFD54F),
                                                        Color(0xFFF9A825)
                                                    )
                                                ),
                                                RoundedCornerShape(40.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.UploadFile,
                                            contentDescription = "Upload",
                                            tint = ThemeColorUtils.white(),
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Upload",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }
                        }
                    }

                }
            }

            if (selectedImageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ThemeColorUtils.black(alpha = 0.7f))
                ) {
                    val maxWidthFraction = 0.82f
                    val maxHeightFraction = 0.70f

                    Card(
                        modifier = Modifier
                            .fillMaxWidth(maxWidthFraction)
                            .fillMaxHeight(maxHeightFraction)
                            .align(Alignment.Center)
                            .shadow(
                                elevation = 24.dp,
                                shape = RoundedCornerShape(24.dp),
                                spotColor = ThemeColorUtils.black(alpha = 0.5f)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ThemeColorUtils.white()
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Image Preview with Check Badge
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth(0.91f)
                                            .aspectRatio(1f)
                                            .shadow(
                                                elevation = 8.dp,
                                                shape = RoundedCornerShape(16.dp)
                                            ),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = ThemeColorUtils.lightGray(Color.Gray)
                                        )
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            AsyncImage(
                                                model = selectedImageUri,
                                                contentDescription = "Selected image",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit // Changed from Crop to Fit to show whole image
                                            )
                                            // Check Badge
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(12.dp)
                                                    .size(48.dp)
                                                    .shadow(
                                                        elevation = 6.dp,
                                                        shape = CircleShape
                                                    )
                                                    .background(
                                                        Color(0xFF4CAF50),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = ThemeColorUtils.white(),
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Retake Button
                                    Button(
                                        onClick = {
                                            selectedImageUri = null
                                            navController.navigate("camera")
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF9E9E9E)
                                        )
                                    ) {
                                        Text(
                                            text = "Retake",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemeColorUtils.white()
                                        )
                                    }

                                    // Proceed Button
                                    Button(
                                        onClick = {
                                            selectedImageUri?.let { uri ->
                                                val encodedUri = Uri.encode(uri.toString())
                                                navController.navigate("audio_input?imageUri=$encodedUri")
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .height(56.dp)
                                            .shadow(
                                                elevation = 8.dp,
                                                shape = RoundedCornerShape(16.dp),
                                                spotColor = ThemeColorUtils.black(alpha = 0.3f)
                                            ),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4CAF50)
                                        )
                                    ) {
                                        Text(
                                            text = "Proceed Audio Input",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ThemeColorUtils.white()
                                        )
                                    }
                                }
                            }
                            
                            // Close button (X) in top right corner - positioned above Column
                            IconButton(
                                onClick = {
                                    selectedImageUri = null
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .background(
                                        ThemeColorUtils.white(alpha = 0.9f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = ThemeColorUtils.black(),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}