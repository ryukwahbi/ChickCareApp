package com.bisu.chickcare.frontend.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalContext
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
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.service.AudioSpectrogramConverter
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import com.bisu.chickcare.frontend.utils.persistUriToAppStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioInputScreen(
    navController: NavController,
    imageUri: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val decodedImageUri = remember(imageUri) {
        imageUri?.takeIf { it.isNotBlank() }?.let { Uri.decode(it) }
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableLongStateOf(0L) }
    var recordingDisplayTime by remember { mutableStateOf("00:00") }
    var audioFile by remember { mutableStateOf<File?>(null) }
    val amplitudes = remember { androidx.compose.runtime.mutableStateListOf<Float>() }
    var recordingFinished by remember { mutableStateOf(false) }
    var uploadedAudioUri by remember { mutableStateOf<String?>(null) }
    var audioDurationWarning by remember { mutableStateOf<String?>(null) }
    val mediaRecorder = remember { mutableStateOf<MediaRecorder?>(null) }
    var currentRecordingFile by remember { mutableStateOf<File?>(null) }
    val mediaPlayer = remember { MediaPlayer() }
    var isPlayingAudio by remember { mutableStateOf(false) }
    var playbackPosition by remember { mutableLongStateOf(0L) }
    LaunchedEffect(audioDurationWarning) {
        if (audioDurationWarning != null) {
            delay(4000)
            audioDurationWarning = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
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
                        Log.d(
                            "AudioInputScreen",
                            "Took persistent URI permission for: $it"
                        )
                    } catch (e: Exception) {
                        Log.w(
                            "AudioInputScreen",
                            "Could not take persistent URI permission: ${e.message}"
                        )
                    }
                    persistUriToAppStorage(
                        context = context,
                        sourceUriString = it.toString(),
                        subdirectory = "detection_audio",
                        fallbackExtension = "m4a",
                        logTag = "AudioInputScreen"
                    )?.let { stored ->
                        finalUriString = stored
                    }
                }
                uploadedAudioUri = finalUriString
                val fileUri = finalUriString.toUri()
                fileUri.path?.let { path ->
                    val possibleFile = File(path)
                    if (possibleFile.exists()) {
                        audioFile = possibleFile
                    }
                }

                // Validate audio duration
                val audioConverter = AudioSpectrogramConverter(context)
                val durationMs = withContext(Dispatchers.IO) {
                    audioConverter.getAudioDuration(finalUriString)
                }

                if (durationMs != null) {
                    val durationSeconds = durationMs / 1000f
                    audioDurationWarning = when {
                        durationSeconds < AudioSpectrogramConverter.MIN_DURATION_SECONDS -> {
                            "⚠️ Audio is too short (${String.format("%.1f", durationSeconds)}s). " +
                                    "Please record at least ${AudioSpectrogramConverter.MIN_DURATION_SECONDS} seconds."
                        }
                        durationSeconds > AudioSpectrogramConverter.MAX_DURATION_SECONDS -> {
                            "ℹ️ Audio > ${AudioSpectrogramConverter.MAX_DURATION_SECONDS}s. Auto-trimming active segment."
                        }
                        else -> null
                    }
                } else {
                    audioDurationWarning = null
                }

                Log.d("AudioInputScreen", "Audio uploaded - URI: $finalUriString, Duration: ${durationMs}ms, Warning: $audioDurationWarning")
            }
        }
    }

    val stopRecordingFunction: () -> Unit = let@{
        if (!isRecording) {
            return@let
        }

        isRecording = false

        var recordingStoppedSuccessfully = false

        try {
            val recorder = mediaRecorder.value
            if (recorder != null) {
                try {
                    var stateChecked = false
                    var state = -1

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            val getStateMethod = recorder::class.java.getMethod("getState")
                            state = (getStateMethod.invoke(recorder) as? Int) ?: -1
                            stateChecked = true
                        } catch (_: Exception) {
                            Log.d(
                                "AudioInputScreen",
                                "State check unavailable, proceeding with stop attempt"
                            )
                        }
                    }

                    val shouldStop = if (stateChecked) {
                        if (state == 4) { // RECORDING state
                            true
                        } else {
                            Log.d(
                                "AudioInputScreen",
                                "Recorder not in RECORDING state (state=$state), skipping stop"
                            )
                            false
                        }
                    } else {
                        true
                    }

                    if (shouldStop) {
                        try {
                            recorder.stop()
                            recordingStoppedSuccessfully = true
                            Log.d(
                                "AudioInputScreen",
                                "Recording stopped successfully"
                            )
                        } catch (stopError: IllegalStateException) {
                            Log.w(
                                "AudioInputScreen",
                                "Recorder not in valid state to stop: ${stopError.message}"
                            )
                        } catch (e: Exception) {
                            Log.e(
                                "AudioInputScreen",
                                "Unexpected error stopping recorder: ${e.message}",
                                e
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AudioInputScreen", "Error stopping recorder", e)
                }
            }
        } catch (e: Exception) {
            Log.e("AudioInputScreen", "Error in stopRecording", e)
        } finally {
            if (recordingStoppedSuccessfully) {
                currentRecordingFile?.let { file ->
                    if (file.exists() && file.length() > 0) {
                        audioFile = file
                        recordingFinished = true
                        Log.d(
                            "AudioInputScreen",
                            "Recording saved to: ${file.absolutePath}, hasAudio should now be true"
                        )
                    } else {
                        Log.w(
                            "AudioInputScreen",
                            "Recording file is empty or does not exist: ${file.absolutePath}"
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingTime = 0L // Reset timer when starting
            amplitudes.clear()
            val startTime = System.currentTimeMillis()
            Log.d("AudioInputScreen", "Timer started")

            while (true) {
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - startTime

                // Update display time with tenths of a second
                val seconds = (elapsedTime / 1000) % 60
                val minutes = (elapsedTime / 60000)
                recordingDisplayTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

                // Update the main recordingTime state (seconds) for the final result screen
                recordingTime = elapsedTime / 1000

                // Poll amplitude
                val recorder = mediaRecorder.value
                if (recorder != null) {
                    try {
                        val maxAmp = recorder.maxAmplitude
                        // Logarithmic scaling for better visuals or just linear?
                        // Max amplitude is approx 32767.
                        var norm = maxAmp / 32767f
                        if (norm > 1f) norm = 1f

                        // Add to list
                        amplitudes.add(norm)
                        // Keep a fixed history size (e.g. 100 samples)
                        if (amplitudes.size > 100) {
                            amplitudes.removeAt(0)
                        }
                    } catch (_: Exception) {
                        // Ignore
                    }
                }

                delay(50) // Update every 50ms (20fps)

                // Check 10s limit
                if (elapsedTime >= 10000L) {
                    Log.d("AudioInputScreen", "Recording reached 10 seconds, auto-stopping...")
                    stopRecordingFunction()
                    break
                }

                if (!isRecording) break
            }
        } else {
            if (!recordingFinished) {
                recordingTime = 0L
            }
        }
    }

    fun cleanupRecorder() {
        try {
            val recorder = mediaRecorder.value
            if (recorder != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        try {
                            val getStateMethod = recorder::class.java.getMethod("getState")
                            val state = getStateMethod.invoke(recorder) as? Int
                            if (state == 4) {
                                recorder.stop()
                                Log.d(
                                    "AudioInputScreen",
                                    "Stopped recorder in RECORDING state"
                                )
                            } else {
                                Log.d(
                                    "AudioInputScreen",
                                    "Recorder not in RECORDING state (state=$state), skipping stop"
                                )
                            }
                        } catch (e: Exception) {
                            Log.d(
                                "AudioInputScreen",
                                "Could not check recorder state: ${e.message}"
                            )
                        }
                    } else {
                        if (isRecording) {
                            try {
                                recorder.stop()
                            } catch (e: IllegalStateException) {
                                Log.d(
                                    "AudioInputScreen",
                                    "Recorder not in valid state to stop: ${e.message}"
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("AudioInputScreen", "Error stopping recorder: ${e.message}")
                }

                try {
                    recorder.release()
                } catch (e: Exception) {
                    Log.w("AudioInputScreen", "Error releasing recorder: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w("AudioInputScreen", "Error in cleanupRecorder: ${e.message}")
        } finally {
            mediaRecorder.value = null
        }
    }

    fun startRecording() {
        try {
            cleanupRecorder()

            val dir = context.externalCacheDir ?: context.cacheDir
            val file = File(dir, "audio_${System.currentTimeMillis()}.3gp")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION") MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(file.absolutePath)
                // Limit recording to 10 seconds for better accuracy and faster processing
                setMaxDuration(10000) // 10 seconds in milliseconds
                // Auto-stop when max duration is reached
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        Log.d("AudioInputScreen", "Max duration (10 seconds) reached, auto-stopping recording")
                        coroutineScope.launch {
                            stopRecordingFunction()
                        }
                    }
                }
                prepare()
            }

            currentRecordingFile = file
            mediaRecorder.value = recorder

            audioFile = null
            uploadedAudioUri = null
            audioDurationWarning = null

            recorder.start()

            recordingFinished = false
            recordingTime = 0L
            isRecording = true

            Log.d("AudioInputScreen", "Recording started: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("AudioInputScreen", "Error starting recording", e)
            e.printStackTrace()
            // Cleanup on error
            cleanupRecorder()
            isRecording = false
            recordingFinished = false
            audioFile = null
            currentRecordingFile = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) {
                try {
                    mediaRecorder.value?.stop()
                } catch (_: Exception) {
                }
            }
            cleanupRecorder()

            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.reset()
                mediaPlayer.release()
            } catch (e: Exception) {
                Log.w("AudioInputScreen", "Error releasing MediaPlayer: ${e.message}")
            }
        }
    }

    DisposableEffect(audioFile, uploadedAudioUri) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                    mediaPlayer.reset()
                }
                isPlayingAudio = false
            } catch (e: Exception) {
                Log.w("AudioInputScreen", "Error stopping audio on change: ${e.message}")
            }
        }
    }

    fun stopRecording() {
        stopRecordingFunction()
    }

    val hasAudio = (audioFile != null || uploadedAudioUri != null) && !isRecording

    LaunchedEffect(isPlayingAudio) {
        if (isPlayingAudio) {
            while (isPlayingAudio) {
                try {
                    if (mediaPlayer.isPlaying) {
                        playbackPosition = (mediaPlayer.currentPosition / 1000).toLong()
                    }
                } catch (_: Exception) {
                }
                delay(100)
            }
        } else {

        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ThemeColorUtils.beige(Color(0xFFE3B386)))
                    .statusBarsPadding()
                    .padding(top = 3.dp, bottom = 12.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "AUDIO INPUT",
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
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 56.dp)
                )
            }

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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Select Chicken Audio",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColorUtils.black(),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Record audio or upload from your files",
                            fontSize = 16.sp,
                            color = Color(0xFF806F60),
                            textAlign = TextAlign.Center,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        if (!hasAudio) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(horizontal = 24.dp)
                            ) {
                                // Record Button
                                Button(
                                    onClick = {
                                        // Play Record Start Sound
                                        try {
                                            val mp = MediaPlayer.create(context, R.raw.record_start)
                                            mp?.start()
                                            mp?.setOnCompletionListener { it.release() }
                                        } catch (e: Exception) {
                                            Log.e("AudioInputScreen", "Error playing record sound: ${e.message}")
                                        }

                                        if (hasAudioPermission) {
                                            startRecording()
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = ThemeColorUtils.black(),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Record Audio",
                                            fontSize = 18.sp,
                                            color = ThemeColorUtils.black(),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Upload Button
                                Button(
                                    onClick = {
                                        audioPickerLauncher.launch(arrayOf("audio/*"))
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
                                            imageVector = Icons.Default.UploadFile,
                                            contentDescription = null,
                                            tint = ThemeColorUtils.black(),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Upload an Audio",
                                            fontSize = 18.sp,
                                            color = ThemeColorUtils.black(),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))
                        HorizontalDivider(thickness = 1.dp, color = Color(0xFFAD9983))
                        Spacer(modifier = Modifier.height(30.dp))

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
                                    text = "NOTE: Audio Recording Guidelines",
                                    fontSize = 18.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else ThemeColorUtils.darkGray(Color(0xFF575450)),
                                    modifier = Modifier.padding(horizontal = 18.dp)
                                )

                                HorizontalDivider(
                                    thickness = 1.5.dp,
                                    color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.darkGray(Color(0xFF7E7C7C)) else ThemeColorUtils.darkGray(Color(0xFF6C6242)).copy(alpha = 0.5f)
                                )

                                // Guidelines lines
                                val highlightStyle = SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3EA818)
                                )
                                
                                Text(
                                    text = buildAnnotatedString {
                                        append("—  Record audio of the chicken ")
                                        withStyle(highlightStyle) {
                                            append("MAKING SOUNDS")
                                        }
                                        append(" (coughing, sneezing, or breathing).")
                                    },
                                    fontSize = 15.5.sp,
                                    color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else ThemeColorUtils.black(),
                                    lineHeight = 22.sp,
                                    modifier = Modifier.padding(horizontal = 18.dp)
                                )
                                Text(
                                    text = buildAnnotatedString {
                                        append("—  ")
                                        withStyle(highlightStyle) {
                                            append("GET CLOSE")
                                        }
                                        append(" to the chicken (within 1-2 meters) for clear audio capture.")
                                    },
                                    fontSize = 15.5.sp,
                                    color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else ThemeColorUtils.black(),
                                    lineHeight = 22.sp,
                                    modifier = Modifier.padding(horizontal = 18.dp)
                                )
                                Text(
                                    text = buildAnnotatedString {
                                        append("—  Record ")
                                        withStyle(highlightStyle) {
                                            append("5-10 SECONDS")
                                        }
                                        append(" of chicken sounds for best accuracy.")
                                    },
                                    fontSize = 15.5.sp,
                                    color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else ThemeColorUtils.black(),
                                    lineHeight = 22.sp,
                                    modifier = Modifier.padding(horizontal = 18.dp)
                                )
                                Text(
                                    text = buildAnnotatedString {
                                        append("—  ")
                                        withStyle(highlightStyle) {
                                            append("MINIMIZE BACKGROUND NOISE")
                                        }
                                        append(" for better analysis accuracy.")
                                    },
                                    fontSize = 15.5.sp,
                                    color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else ThemeColorUtils.black(),
                                    lineHeight = 22.sp,
                                    modifier = Modifier.padding(horizontal = 18.dp)
                                )
                                Text(
                                    text = buildAnnotatedString {
                                        append("—  Focus on capturing ")
                                        withStyle(highlightStyle) {
                                            append("RESPIRATORY SOUNDS")
                                        }
                                        append(" or abnormal vocalizations.")
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
                                            append("ONLY ONE CHICKEN")
                                        }
                                        append(" is being recorded at a time for accurate analysis.")
                                    },
                                    fontSize = 15.5.sp,
                                    color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else ThemeColorUtils.black(),
                                    lineHeight = 22.sp,
                                    modifier = Modifier.padding(horizontal = 18.dp)
                                )
                                Text(
                                    text = buildAnnotatedString {
                                        append("—  ")
                                        withStyle(highlightStyle) {
                                            append("HOLD DEVICE STEADY")
                                        }
                                        append(" and keep the microphone unobstructed during recording.")
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
                                    text = "Poor quality or noisy audio may result in inaccurate or NO detection results!",
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
            }
        }
    }

    AnimatedVisibility(
        visible = isRecording,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColorUtils.black(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Optional: Tap outside to stop? User said "Tap to stop" but usually that means the button.
                    // For now, let's allow tapping the scrim to do nothing (modal),
                    // or maybe stop recording if they tap outside card?
                    // The design has "Tap to stop" text inside the card.
                    // I'll make the scrim consume clicks but do nothing.
                    // Actually, I'll add a clickable to the CARD to call stopRecording?
                    // "Tap to stop" usually implies the whole area or the button.
                    // I'll keep the Card clickable to stop, matching the previous behavior?
                    // Reviewing previous code: The Card onClick called stopRecording().
                    // I will make the Card clickable.
                },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = ThemeColorUtils.black(alpha = 0.5f)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ThemeColorUtils.white()
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Timer
                    Text(
                        text = recordingDisplayTime,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626), // Red text color
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 24.dp)
                    )

                    // Red dot for recording indicator next to timer?
                    // The reference image had a red dot to the left of the timer.
                    // I'll skip the dot for now or make it part of the text row if needed,
                    // but strictly following the "Timer" text first.
                    // Actually, let's add the dot above or near it if we want to be exact,
                    // but simplest is just the text first.

                    Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFAFAFA))
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(horizontal = 24.dp)
                        ) {
                            val centerY = size.height / 2
                            val barWidth = 4.dp.toPx()
                            val gap = 2.dp.toPx()
                            val maxBarHeight = size.height * 0.8f

                            // Draw Red Cursor Line
                            val cursorX = size.width / 2
                            drawLine(
                                color = Color(0xFFEF4444), // Red
                                start = androidx.compose.ui.geometry.Offset(cursorX, 0f),
                                end = androidx.compose.ui.geometry.Offset(cursorX, size.height),
                                strokeWidth = 2.dp.toPx()
                            )

                            // Draw Bars to the left of cursor
                            // We iterate backwards from the end of the amplitudes list
                            val iterator = amplitudes.listIterator(amplitudes.size)
                            var currentX = cursorX - gap

                            while (iterator.hasPrevious() && currentX > 0) {
                                val amp = iterator.previous()
                                // Min height so we always see a bar
                                val barHeight = maxOf(4f, amp * maxBarHeight)

                                drawRoundRect(
                                    color = Color(0xFF1E40AF), // Blue color
                                    topLeft = androidx.compose.ui.geometry.Offset(currentX - barWidth, centerY - barHeight / 2),
                                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                                )
                                currentX -= (barWidth + gap)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Stop Button
                    Box(
                        modifier = Modifier
                            .padding(bottom = 24.dp)
                            .size(60.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = CircleShape,
                                spotColor = Color(0xFFDC2626).copy(alpha = 0.4f)
                            )
                            .background(Color(0xFFDC2626), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = androidx.compose.material3.ripple(color = Color.White)
                            ) {
                                if (hasAudioPermission) {
                                    stopRecording()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Stop Icon (White Square)
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.White, RoundedCornerShape(4.dp))
                        )
                    }
                    }

                    // Close/Cancel Button
                    IconButton(
                        onClick = {
                            isRecording = false
                            cleanupRecorder()
                            recordingFinished = false
                            recordingTime = 0L
                            amplitudes.clear()
                            try { currentRecordingFile?.delete() } catch (_: Exception) {}
                            currentRecordingFile = null
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Recording",
                            tint = ThemeColorUtils.black()
                        )
                    }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = hasAudio,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColorUtils.black(alpha = 0.7f))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)

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
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Audio Ready Icon and Info
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Animated Check Mark
                            val checkProgress = remember { Animatable(0f) }
                            LaunchedEffect(Unit) {
                                // Play Custom Success Sound
                                try {
                                    val mp = MediaPlayer.create(context, R.raw.success_sound)
                                    mp?.start()
                                    mp?.setOnCompletionListener { it.release() }
                                } catch (e: Exception) {
                                    Log.e("AudioInputScreen", "Error playing custom sound: ${e.message}")
                                }

                                checkProgress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(600)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF3C8632),
                                                Color(0xFF53A448)
                                            )
                                        ),
                                        CircleShape
                                    )
                                    .shadow(
                                        elevation = 16.dp,
                                        shape = CircleShape,
                                        spotColor = Color(0xFF3C8632).copy(alpha = 0.5f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.size(60.dp)) {
                                    val path = Path().apply {
                                        moveTo(size.width * 0.1f, size.height * 0.5f)
                                        lineTo(size.width * 0.4f, size.height * 0.8f)
                                        lineTo(size.width * 0.9f, size.height * 0.2f)
                                    }

                                    val pathMeasure = PathMeasure()
                                    pathMeasure.setPath(path, false)
                                    val length = pathMeasure.length

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
                                            width = 8.dp.toPx(),
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(1.dp))

                            if (recordingFinished) {
                                val displayTime = if (isPlayingAudio) {
                                    playbackPosition
                                } else {
                                    recordingTime
                                }
                                val displayMinutes = displayTime / 60
                                val displaySeconds = displayTime % 60

                                // Duration Text aligned to start, flush with container (same as buttons)
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(start = 0.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "Duration: ${
                                            String.format(
                                                Locale.getDefault(),
                                                "%02d:%02d",
                                                displayMinutes,
                                                displaySeconds
                                            )
                                        }",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (ThemeViewModel.isDarkMode) Color.White else Color(0xFF64748B)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Audio file selected",
                                    fontSize = 18.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    fontWeight = FontWeight.Medium,
                                    color = if (ThemeViewModel.isDarkMode) Color.White else Color(0xFF64748B)
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 0.dp), // Removed horizontal padding to align with buttons
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (ThemeViewModel.isDarkMode) Color(0xFF2B2D30) else Color(0xFFF5F5F5)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            try {
                                                if (isPlayingAudio) {
                                                    mediaPlayer.stop()
                                                    mediaPlayer.reset()
                                                    isPlayingAudio = false
                                                    Log.d("AudioInputScreen", "Audio playback stopped")
                                                } else {
                                                    val audioUriString = uploadedAudioUri ?: audioFile?.let {
                                                        Uri.fromFile(it).toString()
                                                    }

                                                    if (audioUriString != null) {
                                                        val decodedUri = Uri.decode(audioUriString).toUri()
                                                        Log.d("AudioInputScreen", "Starting audio playback: $decodedUri")

                                                        if (decodedUri.scheme == "content") {
                                                            try {
                                                                context.contentResolver.takePersistableUriPermission(
                                                                    decodedUri,
                                                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                                )
                                                            } catch (e: SecurityException) {
                                                                Log.w("AudioInputScreen", "Could not take persistent URI permission: ${e.message}")
                                                            }
                                                        }

                                                        mediaPlayer.apply {
                                                            reset()
                                                            setOnErrorListener { _, what, extra ->
                                                                Log.e("AudioInputScreen", "MediaPlayer error: what=$what, extra=$extra")
                                                                isPlayingAudio = false
                                                                android.widget.Toast.makeText(
                                                                    context,
                                                                    "Error playing audio",
                                                                    android.widget.Toast.LENGTH_SHORT
                                                                ).show()
                                                                false
                                                            }

                                                            when (decodedUri.scheme) {
                                                                "content" -> setDataSource(context, decodedUri)
                                                                "file" -> setDataSource(decodedUri.path ?: return@IconButton)
                                                                else -> {
                                                                    android.widget.Toast.makeText(
                                                                        context,
                                                                        "Unsupported audio format",
                                                                        android.widget.Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    return@IconButton
                                                                }
                                                            }

                                                            prepareAsync()
                                                            setOnPreparedListener {
                                                                start()
                                                                isPlayingAudio = true
                                                                Log.d("AudioInputScreen", "Audio playback started")
                                                            }
                                                            setOnCompletionListener {
                                                                isPlayingAudio = false
                                                                Log.d("AudioInputScreen", "Audio playback completed")
                                                            }
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Log.e("AudioInputScreen", "Error toggling audio playback: ${e.message}", e)
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Error: ${e.message}",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                                isPlayingAudio = false
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .shadow(
                                                elevation = 4.dp,
                                                shape = CircleShape
                                            )
                                            .background(
                                                color = Color(0xFF4CAF50),
                                                shape = CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlayingAudio) "Pause" else "Play",
                                            tint = ThemeColorUtils.white(),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = if (isPlayingAudio) "Playing..." else "Preview Audio",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (ThemeViewModel.isDarkMode) Color.White else Color(0xFF1E293B)
                                        )
                                        if (isPlayingAudio) {
                                            Text(
                                                text = "Tap to pause",
                                                fontSize = 11.sp,
                                                color = if (ThemeViewModel.isDarkMode) Color.White.copy(alpha = 0.7f) else Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            }
                        }




                        Spacer(modifier = Modifier.height(16.dp))

                        // Buttons Row - Always visible at bottom
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    audioFile = null
                                    uploadedAudioUri = null
                                    recordingFinished = false
                                    recordingTime = 0L
                                    currentRecordingFile = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
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

                            Button(
                                onClick = {
                                    val audioUri = uploadedAudioUri ?: audioFile?.let {
                                        Uri.fromFile(it).toString()
                                    }
                                    val imgUri = decodedImageUri ?: ""

                                    Log.d("AudioInputScreen", "Process & Analyze clicked - audioUri: $audioUri, imageUri: $imgUri")

                                    if (audioUri != null) {
                                        val encodedAudio = Uri.encode(audioUri)
                                        val routeBuilder = StringBuilder("processing?audioUri=$encodedAudio")

                                        if (imgUri.isNotEmpty()) {
                                            val encodedImage = Uri.encode(imgUri)
                                            routeBuilder.append("&imageUri=").append(encodedImage)
                                            Log.d("AudioInputScreen", "Navigating to processing with imageUri=$encodedImage&audioUri=$encodedAudio")
                                        } else {
                                            Log.d("AudioInputScreen", "Navigating to processing without imageUri, audioUri=$encodedAudio")
                                        }

                                        navController.navigate(routeBuilder.toString())
                                    } else {
                                        Log.e("AudioInputScreen", "Cannot navigate: audioUri is null")
                                    }
                                },
                                enabled = true,
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(50.dp)
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
                                    text = "Analyze",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ThemeColorUtils.white()
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            audioFile = null
                            uploadedAudioUri = null
                            recordingFinished = false
                            recordingTime = 0L
                            currentRecordingFile = null
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

            // Warning Overlay at Bottom
            AnimatedVisibility(
                visible = audioDurationWarning != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
            ) {
                audioDurationWarning?.let { warning ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (warning.contains("⚠️")) {
                                Color(0xFFFFF3CD)
                            } else {
                                Color(0xFFE3F2FD)
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = warning,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (warning.contains("⚠️")) {
                                Color(0xFF856404)
                            } else {
                                Color(0xFF0D47A1)
                            },
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
