package com.bisu.chickcare.frontend.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
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
    var capturedAspectRatio by remember { mutableFloatStateOf(3f / 4f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(navBackStackEntry?.savedStateHandle) {
        val stateHandle = navBackStackEntry?.savedStateHandle
        stateHandle?.get<String>("capturedImageUri")?.let { uriString ->
            selectedImageUri = uriString.toUri()
            onImageSelected(uriString.toUri())
            // Get aspect ratio at the same time as the image
            stateHandle.get<String>("capturedAspectRatio")?.let { ratio ->
                capturedAspectRatio = when (ratio) {
                    "1:1" -> 1f
                    "3:4" -> 3f / 4f
                    else -> 3f / 4f
                }
                stateHandle.remove<String>("capturedAspectRatio")
            }
            stateHandle.remove<String>("capturedImageUri")
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

                // Calculate aspect ratio for gallery images
                try {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(finalUri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, options)
                    }
                    if (options.outWidth > 0 && options.outHeight > 0) {
                        capturedAspectRatio = options.outWidth.toFloat() / options.outHeight.toFloat()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ImageInputScreen", "Error calculating aspect ratio", e)
                }
            }
        }
    }



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
                    .padding(vertical = 24.dp),
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
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Take a photo or choose from your gallery",
                        fontSize = 16.sp,
                        color = Color(0xFF806F60),
                        textAlign = TextAlign.Center,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons Section (Now before guidelines)
                    if (selectedImageUri == null) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                        ) {
                            // Take a photo Button
                            Button(
                                onClick = {
                                    if (hasCameraPermission) {
                                        navController.navigate("camera")
                                    } else {
                                        cameraLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ThemeColorUtils.white(alpha = 0.5f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ThemeColorUtils.black()),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 0.dp,
                                    pressedElevation = 0.dp
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Camera",
                                        tint = ThemeColorUtils.black(),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "Take a photo",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ThemeColorUtils.black()
                                    )
                                }
                            }

                            // Upload an image Button
                            Button(
                                onClick = {
                                    galleryLauncher.launch(arrayOf("image/*"))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ThemeColorUtils.white(alpha = 0.5f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ThemeColorUtils.black()),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 0.dp,
                                    pressedElevation = 0.dp
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.UploadFile,
                                        contentDescription = "Upload",
                                        tint = ThemeColorUtils.black(),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "Upload an image",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ThemeColorUtils.black()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFAD9983))
                    Spacer(modifier = Modifier.height(30.dp))

                    // Important Instructions Card (Moved to bottom)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .then(
                                if (ThemeViewModel.isDarkMode) {
                                    Modifier.shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        spotColor = Color.White,
                                        ambientColor = Color.White.copy(alpha = 0.5f)
                                    )
                                } else {
                                    Modifier.shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        spotColor = ThemeColorUtils.black(alpha = 0.15f)
                                    )
                                }
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFFE5E2DE)) else ThemeColorUtils.beige(Color(0xFFFFF3CD))
                        ),
                        elevation = if (ThemeViewModel.isDarkMode) {
                            CardDefaults.cardElevation(defaultElevation = 0.dp)
                        } else {
                            CardDefaults.cardElevation(defaultElevation = 6.dp)
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Note: Image Capture Guidelines",
                                fontSize = 18.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else ThemeColorUtils.darkGray(Color(0xFF575450)),
                                modifier = Modifier.padding(horizontal = 18.dp)
                            )

                            HorizontalDivider(
                                thickness = 1.5.dp,
                                color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.darkGray(Color(0xFF7E7C7C)) else ThemeColorUtils.darkGray(Color(0xFF6C6242)).copy(alpha = 0.5f)
                            )
                            
                            // Guidelines lines (removed dashes as per typical UI but kept text as requested if needed, adjusting based on lines in file)
                            // Assuming keeping text content same just moving position
                            // Guidelines lines
                            val highlightStyle = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3EA818)
                            )
                            
                            Text(
                                text = buildAnnotatedString {
                                    append("—  Capture a ")
                                    withStyle(highlightStyle) {
                                        append("CLEAR")
                                    }
                                    append(" focused photo of the chicken's face and head area.")
                                },
                                fontSize = 15.5.sp,
                                color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else ThemeColorUtils.black(),
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(horizontal = 18.dp)
                            )
                            Text(
                                text = buildAnnotatedString {
                                    append("—  Ensure ")
                                    withStyle(highlightStyle) {
                                        append("GOOD LIGHTING")
                                    }
                                    append(" avoid shadows or dark areas that hide details.")
                                },
                                fontSize = 15.5.sp,
                                color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else ThemeColorUtils.black(),
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(horizontal = 18.dp)
                            )
                            Text(
                                text = buildAnnotatedString {
                                    append("—  Focus on the ")
                                    withStyle(highlightStyle) {
                                        append("HEAD REGION")
                                    }
                                    append(" capture symptoms like nasal discharge, facial swelling, or eye issues.")
                                },
                                fontSize = 15.5.sp,
                                color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else ThemeColorUtils.black(),
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(horizontal = 18.dp)
                            )
                            Text(
                                text = buildAnnotatedString {
                                    append("—  Get ")
                                    withStyle(highlightStyle) {
                                        append("CLOSE ENOUGH")
                                    }
                                    append(" the chicken should fill most of the frame for accurate analysis.")
                                },
                                fontSize = 15.5.sp,
                                color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else ThemeColorUtils.black(),
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(horizontal = 18.dp)
                            )
                            Text(
                                text = buildAnnotatedString { // Updated text "CHICKEN MUST STILL"
                                    append("—  ")
                                    withStyle(highlightStyle) {
                                        append("CHICKEN MUST STILL")
                                    }
                                    append(", wait for it to be calm before capturing.")
                                },
                                fontSize = 15.5.sp,
                                color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else ThemeColorUtils.black(),
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(horizontal = 18.dp)
                            )

                            HorizontalDivider(
                                thickness = 1.5.dp,
                                color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.darkGray(Color(0xFF7E7C7C)) else ThemeColorUtils.darkGray(Color(0xFF6C6242)).copy(alpha = 0.5f)
                            )

                            Text(
                                text = "Blurry, dark, or distant photos may result in inaccurate or NO detection results!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD74D4D),
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(horizontal = 18.dp)
                            )
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

                    Card(
                        modifier = Modifier
                            .fillMaxWidth(maxWidthFraction)
                            .wrapContentHeight()
                            .align(Alignment.Center)
                            .offset(y = (-40).dp)
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
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 56.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Image Preview with Check Badge
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(capturedAspectRatio)
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
                                                contentScale = ContentScale.Crop // Changed to Crop to fill whole area and avoid grey bars
                                            )
                                            // Check Badge Animation
                                            val checkProgress = remember { Animatable(0f) }
                                            LaunchedEffect(Unit) {
                                                // Play Custom Success Sound
                                                try {
                                                    val mp = MediaPlayer.create(context, R.raw.success_sound)
                                                    mp?.start()
                                                    mp?.setOnCompletionListener { it.release() }
                                                } catch (e: Exception) {
                                                    // Ignore play errors
                                                }

                                                checkProgress.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = tween(1200)
                                                )
                                            }

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
                                                androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp)) {
                                                    val path = Path().apply {
                                                        moveTo(2.dp.toPx(), 10.dp.toPx())
                                                        lineTo(8.dp.toPx(), 16.dp.toPx())
                                                        lineTo(18.dp.toPx(), 4.dp.toPx())
                                                    }
                                                    
                                                    // Measure the path
                                                    val pathMeasure = PathMeasure()
                                                    pathMeasure.setPath(path, false)
                                                    val length = pathMeasure.length
                                                    
                                                    // Create partial path
                                                    val partialPath = Path()
                                                    pathMeasure.getSegment(
                                                        startDistance = 0f,
                                                        stopDistance = length * checkProgress.value,
                                                        destination = partialPath,
                                                        startWithMoveTo = true
                                                    )

                                                    drawPath(
                                                        path = partialPath,
                                                        color = Color.White,
                                                        style = Stroke(
                                                            width = 3.dp.toPx(),
                                                            cap = StrokeCap.Round,
                                                            join = StrokeJoin.Round
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

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
                                            text = "Proceed",
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
                                    .padding(8.dp)
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