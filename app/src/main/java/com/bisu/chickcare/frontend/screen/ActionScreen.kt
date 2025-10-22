package com.bisu.chickcare.frontend.screen

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Picture", "Audio")
    val viewModel: DashboardViewModel = viewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Action Tools") },
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                indicator = { TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(selectedTab)) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if(selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                        selectedContentColor = Color(0xFF8B4513),
                        unselectedContentColor = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> CameraTabContent(navController, viewModel)
                1 -> AudioTabContent(navController, viewModel)
            }
        }
    }
}

@Composable
fun CameraTabContent(navController: NavController, viewModel: DashboardViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(uiState.detectionResult) {
        // FIX: Removed unused 'isInfected' variable
        uiState.detectionResult?.let { (_, status) ->
            val suggestionsString = uiState.remedySuggestions.joinToString("|")
            val encodedSuggestions = URLEncoder.encode(suggestionsString, StandardCharsets.UTF_8.toString())
            val encodedImage = capturedImageUri?.let { URLEncoder.encode(it.toString(), StandardCharsets.UTF_8.toString()) } ?: ""

            // FIX: Added 'suggestions' to the navigation route
            navController.navigate("detection_result?imageUri=$encodedImage&status=$status&suggestions=$encodedSuggestions") {
                popUpTo("action_tools")
            }
            viewModel.clearDetectionResult()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                capturedImageUri = it
                viewModel.onScanNowClicked(it.toString(), null)
            }
        }
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f).height(150.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF3B82F6)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "INSTRUCTIONS",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 18.sp),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val instructions = listOf(
                        "For best results, use a clear and well-lit photo.",
                        "Ensure the chicken's face, comb, and wattle are visible.",
                        "Avoid blurry images. Keep your hand steady during capture.",
                        "Capture images in a natural environment if possible.",
                        "Only upload pictures of a single chicken at a time.",
                        "The AI model is trained for specific breeds; results may vary for others.",
                        "Use the back camera for higher resolution images.",
                        "Do not use flash if it creates harsh reflections or shadows.",
                        "Ensure the background is not too cluttered.",
                        "After uploading, please wait for the analysis to complete."
                    )
                    items(instructions.size) { index ->
                        Text(
                            text = "• ${instructions[index]}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = 15.sp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton(
                icon = Icons.Default.CameraAlt,
                label = "Capture",
                onClick = {
                    viewModel.clearDetectionResult()
                    navController.navigate("camera")
                }
            )
            ActionButton(
                icon = Icons.Default.UploadFile,
                label = "Upload",
                onClick = {
                    viewModel.clearDetectionResult()
                    galleryLauncher.launch("image/*")
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.LightGray.copy(alpha = 0.5f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF26201C),
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
    }
}

@Composable
fun AudioTabContent(navController: NavController, viewModel: DashboardViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var hasAudioPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableLongStateOf(0L) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var recordingFinished by remember { mutableStateOf(false) }
    val mediaRecorder = remember { mutableStateOf<MediaRecorder?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasAudioPermission = isGranted }
    )

    LaunchedEffect(uiState.detectionResult) {
        // FIX: Removed unused 'isInfected' variable
        uiState.detectionResult?.let { (_, status) ->
            val suggestionsString = uiState.remedySuggestions.joinToString("|")
            val encodedSuggestions = URLEncoder.encode(suggestionsString, StandardCharsets.UTF_8.toString())
            val encodedAudio = audioFile?.let { URLEncoder.encode(Uri.fromFile(it).toString(), StandardCharsets.UTF_8.toString()) } ?: ""

            // FIX: Added 'suggestions' to the navigation route
            navController.navigate("detection_result?audioUri=$encodedAudio&status=$status&suggestions=$encodedSuggestions") {
                popUpTo("action_tools")
            }
            viewModel.clearDetectionResult()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                recordingTime++
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder.value?.release()
            mediaRecorder.value = null
        }
    }

    fun startRecording() {
        val dir = context.externalCacheDir ?: context.cacheDir
        val file = File(dir, "audio_${System.currentTimeMillis()}.3gp")
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(file.absolutePath)
            try {
                prepare()
                start()
                audioFile = file
                mediaRecorder.value = this
                isRecording = true
                recordingTime = 0L
                recordingFinished = false
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun stopRecording() {
        if (isRecording) {
            mediaRecorder.value?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            isRecording = false
            recordingFinished = true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (hasAudioPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f).height(150.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B82F6)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AUDIO RECORDING GUIDE",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 18.sp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val instructions = listOf(
                            "Record in a quiet environment to minimize background noise.",
                            "Place the microphone close to the chicken (6-12 inches) without causing stress.",
                            "Record for at least 10-15 seconds to capture clear respiratory sounds.",
                            "Listen for distinct sounds like coughing, sneezing, or rattling.",
                            "Do not talk or make other noises during the recording session.",
                            "Record audio from different chickens separately if you suspect an issue.",
                            "Ensure the microphone is not covered by your hand or other objects.",
                            "If the recording is unclear, use the 'Retake' option to try again."
                        )
                        items(instructions.size) { index ->
                            Text(
                                text = "• ${instructions[index]}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = 15.sp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(visible = isRecording || recordingFinished) {
                    val minutes = recordingTime / 60
                    val seconds = recordingTime % 60
                    Text(
                        text = when {
                            isRecording -> "Recording... ${String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)}"
                            recordingFinished -> "Finished: ${String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)}"
                            else -> ""
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                if (recordingFinished) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Button(
                            onClick = {
                                audioFile?.let {
                                    viewModel.onScanNowClicked(null, it.toString())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Analyze")
                            Spacer(Modifier.width(8.dp))
                            Text("Analyze")
                        }
                        Button(
                            onClick = {
                                recordingFinished = false
                                recordingTime = 0L
                                audioFile = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retake")
                            Spacer(Modifier.width(8.dp))
                            Text("Retake")
                        }
                    }
                } else {
                    IconButton(
                        onClick = { if (isRecording) stopRecording() else startRecording() },
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) Color.Red.copy(alpha = 0.8f) else Color.LightGray.copy(alpha = 0.5f))
                            .border(2.dp, Color.Gray, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                            tint = if (isRecording) Color.White else Color(0xFF26201C),
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Audio recording permission is required to use this feature.")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text("Grant Permission")
                }
            }
        }
    }
}
