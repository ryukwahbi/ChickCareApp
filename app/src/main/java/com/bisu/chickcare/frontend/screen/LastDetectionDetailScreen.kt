package com.bisu.chickcare.frontend.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bisu.chickcare.backend.repository.DetectionRepository
import com.bisu.chickcare.backend.utils.OfflineAuthHelper
import com.bisu.chickcare.backend.viewmodels.DashboardViewModel
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.bisu.chickcare.frontend.components.FullscreenImageViewer
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import com.bisu.chickcare.frontend.utils.sanitizeToUri
import com.bisu.chickcare.frontend.utils.sanitizeUriString
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatLastDetectionDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val date = Date(timestamp)
    return "${dateFormat.format(date)}\n${timeFormat.format(date)}"
}

fun Float.formatOneDecimal(): String = String.format(Locale.getDefault(), "%.1f", this)

fun openLocation(context: Context, rawLocation: String?) {
    if (rawLocation.isNullOrBlank()) {
        android.widget.Toast.makeText(
            context,
            "Location not available",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        return
    }

    val trimmedLocation = rawLocation.trim()
    val parts = trimmedLocation.split(",")
    val hasCoordinates = parts.size >= 2 &&
        parts[0].trim().toDoubleOrNull() != null &&
        parts[1].trim().toDoubleOrNull() != null

    val geoUri = if (hasCoordinates) {
        val lat = parts[0].trim()
        val lon = parts[1].trim()
        "geo:$lat,$lon?q=$lat,$lon(${Uri.encode(trimmedLocation)})"
    } else {
        "geo:0,0?q=${Uri.encode(trimmedLocation)}"
    }

    try {
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri.toUri())
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                "https://www.google.com/maps/search/?api=1&query=${Uri.encode(trimmedLocation)}".toUri()
            )
            context.startActivity(webIntent)
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(
            context,
            "Unable to open location",
            android.widget.Toast.LENGTH_SHORT
        ).show()
        Log.e("LastDetectionDetail", "Error opening maps: ${e.message}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastDetectionDetailScreen(
    navController: NavController,
    detectionId: String,
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val detectionRepository = remember { DetectionRepository() }
    val coroutineScope = rememberCoroutineScope()
    
    // Use DashboardViewModel to get the detection from already-loaded history
    val history by dashboardViewModel.detectionHistory.collectAsState()
    
    // Find the detection from the loaded history instead of fetching from Firestore
    var detectionEntry = remember(history, detectionId) {
        history.find { it.id == detectionId }
    }
    var isLoading = history.isEmpty() && detectionEntry == null

    val backgroundColor = if (ThemeViewModel.isDarkMode) ThemeColorUtils.beige(Color(0xFF020E1A)) else ThemeColorUtils.beige(Color(
        0xFFEAD1AC
    )
    )
    val accentOrange = Color(0xFFD27D2D)
    val accentGreen = Color(0xFF4CAF50)

    var showTreatmentDialog by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    var treatmentState by remember { mutableStateOf(detectionEntry?.treatment) }
    var treatmentDateState by remember { mutableStateOf(detectionEntry?.treatmentDate) }
    var nextDoseDateState by remember { mutableStateOf(detectionEntry?.nextDoseDate) }
    var treatmentNotesState by remember { mutableStateOf(detectionEntry?.treatmentNotes ?: "") }
    var treatmentTextInput by remember { mutableStateOf(detectionEntry?.treatment ?: "") }
    var nextDoseDaysInput by remember { mutableStateOf("") }
    var treatmentNotesInput by remember { mutableStateOf(detectionEntry?.treatmentNotes ?: "") }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid ?: OfflineAuthHelper.getCurrentLocalUserId(context)
    }

    // Log for debugging
    LaunchedEffect(detectionId, history.size) {
        Log.d("LastDetectionDetail", "Looking for detectionId=$detectionId in history of ${history.size} items")
        Log.d("LastDetectionDetail", "Found detection: ${detectionEntry?.id ?: "NULL"}")
    }

    detectionEntry?.let { entry ->
        treatmentState = entry.treatment
        treatmentDateState = entry.treatmentDate
        nextDoseDateState = entry.nextDoseDate
        treatmentNotesState = entry.treatmentNotes ?: ""
        treatmentTextInput = entry.treatment ?: ""
        treatmentNotesInput = entry.treatmentNotes ?: ""
    }

    fun saveTreatment() {
        val userId = getCurrentUserId() ?: return
        if (detectionId.isEmpty()) return

        coroutineScope.launch {
            try {
                val computedNextDoseDate = if (nextDoseDaysInput.isNotBlank()) {
                    val days = nextDoseDaysInput.toIntOrNull() ?: 0
                    System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000L)
                } else null

                detectionRepository.updateTreatment(
                    userId = userId,
                    detectionId = detectionId,
                    treatment = treatmentTextInput.takeIf { it.isNotBlank() },
                    treatmentDate = System.currentTimeMillis(),
                    nextDoseDate = computedNextDoseDate,
                    treatmentNotes = treatmentNotesInput.takeIf { it.isNotBlank() }
                )
                treatmentState = treatmentTextInput.takeIf { it.isNotBlank() }
                treatmentDateState = System.currentTimeMillis()
                nextDoseDateState = computedNextDoseDate
                treatmentNotesState = treatmentNotesInput
                showTreatmentDialog = false
                android.widget.Toast.makeText(
                    context,
                    "Treatment saved successfully",
                    android.widget.Toast.LENGTH_SHORT
                ).show()

                // Refresh the detection entry
                isLoading = true
                detectionEntry = detectionRepository.getDetection(userId, detectionId)
                isLoading = false

            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    context,
                    "Error saving treatment: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Grant URI permissions for content URIs before loading images
    LaunchedEffect(detectionEntry?.imageUri) {
        detectionEntry?.imageUri?.let {
            val sanitizedString = sanitizeUriString(it, "LastDetectionDetail")
            val uri = sanitizeToUri(it, "LastDetectionDetail")
            if (sanitizedString != null && uri?.scheme == "content") {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    Log.d("LastDetectionDetail", "Taken persistable URI permission for: $sanitizedString")
                } catch (e: SecurityException) {
                    Log.w("LastDetectionDetail", "Cannot take persistable permission (may not support it): $sanitizedString - ${e.message}")
                } catch (e: Exception) {
                    Log.w("LastDetectionDetail", "Error taking persistable permission: $sanitizedString - ${e.message}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detection Details",
                        fontWeight = FontWeight.ExtraBold,
                        color = ThemeColorUtils.black()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    backgroundColor
                )
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (detectionEntry != null) {
                val entry = detectionEntry!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Date and Time Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (ThemeViewModel.isDarkMode) {
                                    Modifier.shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        spotColor = Color.White,
                                        ambientColor = Color.White.copy(alpha = 0.5f)
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (ThemeViewModel.isDarkMode) ThemeColorUtils.surface(
                                Color(0xFFE5E2DE)
                            ) else ThemeColorUtils.surface(Color.White)
                        ),
                        elevation = if (ThemeViewModel.isDarkMode) {
                            CardDefaults.cardElevation(defaultElevation = 0.dp)
                        } else {
                            CardDefaults.cardElevation(8.dp)
                        },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = accentOrange
                            )
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Date and Time",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else Color(
                                            0xFF333333
                                        )
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = formatLastDetectionDate(entry.timestamp),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else Color(
                                            0xFFD27D2D
                                        )
                                    ),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }

                    // Location Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (ThemeViewModel.isDarkMode) {
                                    Modifier.shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        spotColor = Color.White,
                                        ambientColor = Color.White.copy(alpha = 0.5f)
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (ThemeViewModel.isDarkMode) ThemeColorUtils.surface(
                                Color(0xFFE5E2DE)
                            ) else ThemeColorUtils.surface(Color.White)
                        ),
                        elevation = if (ThemeViewModel.isDarkMode) {
                            CardDefaults.cardElevation(defaultElevation = 0.dp)
                        } else {
                            CardDefaults.cardElevation(8.dp)
                        },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = accentGreen
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Location",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else Color(
                                                    0xFF333333
                                                )
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val formattedLocation = entry.location
                                            ?.replace("+", " ")
                                            ?.replace(",", ", ")
                                            ?.replace("\\s+".toRegex(), " ")
                                            ?.trim()

                                        if (entry.location.isNullOrEmpty()) {
                                            Text(
                                                text = "Location not available",
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                    color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.darkGray(
                                                        Color(0xFF999999)
                                                    ) else ThemeColorUtils.lightGray(Color(0xFF999999))
                                                )
                                            )
                                        } else {
                                            Text(
                                                text = formattedLocation
                                                    ?: "Location not available",
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else Color(
                                                        0xFF333333
                                                    )
                                                ),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.clickable(enabled = entry.location.isNotEmpty()) {
                                                    openLocation(context, entry.location)
                                                }
                                            )
                                        }
                                    }
                                }

                                if (!entry.location.isNullOrEmpty()) {
                                    Button(
                                        onClick = { openLocation(context, entry.location) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = accentGreen
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Open in Maps",
                                            tint = ThemeColorUtils.white(),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Open in Maps",
                                            color = ThemeColorUtils.white(),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Detection Image Card (only show if image URI exists)
                    if (!entry.imageUri.isNullOrEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (ThemeViewModel.isDarkMode) {
                                        Modifier.shadow(
                                            elevation = 8.dp,
                                            shape = RoundedCornerShape(20.dp),
                                            spotColor = Color.White,
                                            ambientColor = Color.White.copy(alpha = 0.5f)
                                        )
                                    } else {
                                        Modifier
                                    }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (ThemeViewModel.isDarkMode) ThemeColorUtils.surface(
                                    Color(0xFFE5E2DE)
                                ) else ThemeColorUtils.surface(Color.White)
                            ),
                            elevation = if (ThemeViewModel.isDarkMode) {
                                CardDefaults.cardElevation(defaultElevation = 0.dp)
                            } else {
                                CardDefaults.cardElevation(8.dp)
                            },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Detection Image",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else Color(
                                            0xFF333333
                                        )
                                    ),
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                // Decode and parse the image URI - handle URL encoding from navigation
                                // entry.imageUri is already checked for null/empty in the outer if condition
                                val imageUriString = entry.imageUri
                                val sanitizedUriString =
                                    sanitizeUriString(imageUriString, "LastDetectionDetail")
                                val imageUri =
                                    sanitizeToUri(imageUriString, "LastDetectionDetail")
                                        ?: imageUriString.let { localPath ->
                                            // Fallback for plain file paths to ensure the photo is loaded locally
                                            Uri.fromFile(File(localPath))
                                        }

                                if (imageUri != null) {
                                    val imageRequest = ImageRequest.Builder(context)
                                        .data(imageUri)
                                        .crossfade(true)
                                        .build()

                                    AsyncImage(
                                        model = imageRequest,
                                        contentDescription = "Detection Image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(300.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { showImageViewer = true },
                                        contentScale = ContentScale.Crop,
                                        onError = {
                                            Log.e(
                                                "LastDetectionDetail",
                                                "Failed to load image: $sanitizedUriString - ${it.result.throwable.message}"
                                            )
                                        },
                                        onSuccess = {
                                            Log.d(
                                                "LastDetectionDetail",
                                                "Successfully loaded detection image: $sanitizedUriString"
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Additional Info Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (ThemeViewModel.isDarkMode) {
                                    Modifier.shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        spotColor = Color.White,
                                        ambientColor = Color.White.copy(alpha = 0.5f)
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (ThemeViewModel.isDarkMode) ThemeColorUtils.surface(
                                Color(0xFFE5E2DE)
                            ) else ThemeColorUtils.surface(Color.White)
                        ),
                        elevation = if (ThemeViewModel.isDarkMode) {
                            CardDefaults.cardElevation(defaultElevation = 0.dp)
                        } else {
                            CardDefaults.cardElevation(8.dp)
                        },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Detection Information",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else Color(
                                        0xFF333333
                                    )
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Status:",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.darkGray(
                                            Color(0xFF666666)
                                        ) else ThemeColorUtils.lightGray(Color(0xFF666666))
                                    )
                                )
                                val statusLabel =
                                    com.bisu.chickcare.frontend.utils.DetectionDisplayUtils.statusText(
                                        entry.isHealthy,
                                        entry.confidence
                                    )
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = com.bisu.chickcare.frontend.utils.DetectionDisplayUtils.statusColor(
                                            entry.isHealthy,
                                            entry.confidence
                                        )
                                    )
                                )
                            }
                            if (entry.confidence > 0f) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Confidence:",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.darkGray(
                                                Color(0xFF666666)
                                            ) else ThemeColorUtils.lightGray(Color(0xFF666666))
                                        )
                                    )
                                    val confidenceValue =
                                        (entry.confidence * 100).coerceAtLeast(0f)
                                    Text(
                                        text = "${confidenceValue.formatOneDecimal()}%",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else Color(
                                                0xFFD27D2D
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Treatment Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (ThemeViewModel.isDarkMode) {
                                    Modifier.shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        spotColor = Color.White,
                                        ambientColor = Color.White.copy(alpha = 0.5f)
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (ThemeViewModel.isDarkMode) ThemeColorUtils.surface(
                                Color(0xFFE5E2DE)
                            ) else ThemeColorUtils.surface(Color.White)
                        ),
                        elevation = if (ThemeViewModel.isDarkMode) {
                            CardDefaults.cardElevation(defaultElevation = 0.dp)
                        } else {
                            CardDefaults.cardElevation(8.dp)
                        },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Medication,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color(0xFF2196F3)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Treatment",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else Color(
                                                0xFF333333
                                            )
                                        )
                                    )
                                }
                                Button(
                                    onClick = { showTreatmentDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (treatmentState != null) Color(0xFF2196F3) else Color(
                                            0xFF4CAF50
                                        )
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (treatmentState != null) Icons.Default.Edit else Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = ThemeColorUtils.white()
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (treatmentState != null) "Edit" else "Add",
                                        color = ThemeColorUtils.white(),
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            if (treatmentState != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Treatment:",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.darkGray(
                                                Color(0xFF666666)
                                            ) else ThemeColorUtils.lightGray(Color(0xFF666666))
                                        )
                                    )
                                    Text(
                                        text = treatmentState ?: "",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else Color(
                                                0xFF2196F3
                                            )
                                        )
                                    )
                                }

                                if (treatmentDateState != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Date Given:",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.darkGray(
                                                    Color(0xFF666666)
                                                ) else ThemeColorUtils.lightGray(Color(0xFF666666))
                                            )
                                        )
                                        Text(
                                            text = formatLastDetectionDate(
                                                treatmentDateState ?: 0L
                                            ),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else Color(
                                                    0xFF333333
                                                )
                                            )
                                        )
                                    }
                                }

                                if (nextDoseDateState != null) {
                                    val isOverdue =
                                        nextDoseDateState!! < System.currentTimeMillis()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = if (isOverdue) Color(0xFFF44336) else Color(
                                                    0xFF4CAF50
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isOverdue) "Next Dose (Overdue):" else "Next Dose:",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.darkGray(
                                                        Color(0xFF666666)
                                                    ) else ThemeColorUtils.lightGray(
                                                        Color(
                                                            0xFF666666
                                                        )
                                                    )
                                                )
                                            )
                                        }
                                        Text(
                                            text = formatLastDetectionDate(
                                                nextDoseDateState ?: 0L
                                            ),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOverdue) Color(0xFFF44336) else Color(
                                                    0xFF4CAF50
                                                )
                                            )
                                        )
                                    }
                                }

                                if (treatmentNotesState.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Notes:",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.darkGray(
                                                Color(0xFF666666)
                                            ) else ThemeColorUtils.lightGray(Color(0xFF666666))
                                        )
                                    )
                                    Text(
                                        text = treatmentNotesState,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else Color(
                                                0xFF333333
                                            )
                                        )
                                    )
                                }
                            } else {
                                Text(
                                    text = "No treatment recorded yet. Add treatment information to track recovery progress.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.darkGray(
                                            Color(0xFF999999)
                                        ) else ThemeColorUtils.lightGray(Color(0xFF999999))
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                Text("Detection not found.", modifier = Modifier.align(Alignment.Center))
            }
        }

        // Treatment Dialog
        if (showTreatmentDialog) {
            AlertDialog(
                onDismissRequest = {
                    showTreatmentDialog = false
                    treatmentTextInput = treatmentState ?: ""
                    treatmentNotesInput = treatmentNotesState
                    nextDoseDaysInput = ""
                },

                shape = RoundedCornerShape(12.dp),
                containerColor = if (ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFF2C2C2C)) else Color.White,
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (treatmentState != null) "Edit Treatment" else "Add Treatment",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else Color.Black
                            )
                        )
                        IconButton(
                            onClick = {
                                showTreatmentDialog = false
                                treatmentTextInput = treatmentState ?: ""
                                treatmentNotesInput = treatmentNotesState
                                nextDoseDaysInput = ""
                            },
                             modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = if (ThemeViewModel.isDarkMode) ThemeColorUtils.black() else Color.Gray
                            )
                        }
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                         Text(
                            text = "Only treatments prescribed by a licensed veterinarian. DO NOT self-medicate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF44336),
                            fontWeight = FontWeight.Medium
                        )
                        
                        OutlinedTextField(
                            value = treatmentTextInput,
                            onValueChange = { treatmentTextInput = it },
                            label = { Text("Treatment (as prescribed by veterinarian)") },
                            placeholder = { Text("e.g., Antibiotic prescribed by Dr. [Veterinarian Name]") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ThemeColorUtils.black(),
                                unfocusedTextColor = ThemeColorUtils.black(),
                                focusedBorderColor = ThemeColorUtils.black(),
                                unfocusedBorderColor = ThemeColorUtils.black(),
                                cursorColor = ThemeColorUtils.black()
                            )
                        )

                        OutlinedTextField(
                            value = nextDoseDaysInput,
                            onValueChange = { if (it.all { char -> char.isDigit() }) nextDoseDaysInput = it },
                            label = { Text("Next Dose (Days from now)") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g., 3") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ThemeColorUtils.black(),
                                unfocusedTextColor = ThemeColorUtils.black(),
                                focusedBorderColor = ThemeColorUtils.black(),
                                unfocusedBorderColor = ThemeColorUtils.black(),
                                cursorColor = ThemeColorUtils.black()
                            )
                        )

                        OutlinedTextField(
                            value = treatmentNotesInput,
                            onValueChange = { treatmentNotesInput = it },
                            label = { Text("Notes (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ThemeColorUtils.black(),
                                unfocusedTextColor = ThemeColorUtils.black(),
                                focusedBorderColor = ThemeColorUtils.black(),
                                unfocusedBorderColor = ThemeColorUtils.black(),
                                cursorColor = ThemeColorUtils.black()
                            )
                        )
                    }
                },
                properties = DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(8.dp),
                confirmButton = {
                    Button(
                        onClick = { saveTreatment() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(8.dp), // Reducing corner radius
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save", color = ThemeColorUtils.white())
                    }
                }
            )
        }
    }

    if (showImageViewer && detectionEntry?.imageUri != null) {
        val imageUriString = detectionEntry?.imageUri!!
        sanitizeUriString(imageUriString, "LastDetectionDetail")
        val imageUri = sanitizeToUri(imageUriString, "LastDetectionDetail")
             ?: imageUriString.let { Uri.fromFile(File(it)) }

        if (imageUri != null) {
             FullscreenImageViewer(
                 images = listOf(imageUri.toString()),
                 initialIndex = 0,
                 onDismiss = { showImageViewer = false }
             )
        }
    }
}
