package com.bisu.chickcare.frontend.screen

import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.viewmodels.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

enum class HelpCategory(val title: String, val icon: ImageVector) {
    ALL("All Topics", Icons.Default.Info),
    DISEASE("Disease Info", Icons.Default.LocalHospital),
    APP_GUIDE("App Guide", Icons.Default.Build),
    PREVENTION("Prevention", Icons.Default.Shield),
    TREATMENT("Treatment", Icons.Default.MedicalServices),
    FAQ("FAQs", Icons.AutoMirrored.Filled.Help)
}

data class HelpLink(
    val url: String,
    val text: String
)

data class HelpTopic(
    val title: String,
    val content: String,
    val category: HelpCategory,
    val icon: ImageVector,
    val isPopular: Boolean = false,
    val isEmergency: Boolean = false,
    val links: List<HelpLink> = emptyList()
)

data class SearchSuggestion(val text: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpCenterScreen(paddingValues: PaddingValues) {
    val authViewModel: AuthViewModel = viewModel()
    val userProfile by authViewModel.userProfile.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var searchQuery by remember { mutableStateOf("") }
    var showPrioritySupport by remember { mutableStateOf(true) }
    var expandedSection by remember { mutableStateOf<String?>(null) } // Only one card expanded at a time
    var selectedCategory by remember { mutableStateOf(HelpCategory.ALL) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val userName = userProfile?.fullName?.split(" ")?.firstOrNull() ?: "User"

    // Shared TTS instance - only create once
    var ttsReady by remember { mutableStateOf(false) }
    var currentSpeakingTopic by remember { mutableStateOf<String?>(null) }

    val sharedTts = remember {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS && ttsInstance != null) {
                val result = ttsInstance!!.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported")
                } else {
                    // Try to set a female voice if available
                    val voices = ttsInstance!!.voices
                    val femaleVoice = voices.find { voice ->
                        voice.locale == Locale.US &&
                                (voice.name.contains("female", ignoreCase = true) ||
                                        voice.name.contains("samantha", ignoreCase = true) ||
                                        voice.name.contains("susan", ignoreCase = true) ||
                                        voice.name.contains("karen", ignoreCase = true))
                    }
                    if (femaleVoice != null) {
                        ttsInstance!!.voice = femaleVoice
                    }
                    // Set up global utterance progress listener
                    ttsInstance!!.setOnUtteranceProgressListener(object :
                        UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            // Speech started
                        }

                        override fun onDone(utteranceId: String?) {
                            // Speech finished - update state on main thread
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                if (utteranceId != null && utteranceId.endsWith("_utterance")) {
                                    val topicTitle = utteranceId.removeSuffix("_utterance")
                                    if (currentSpeakingTopic == topicTitle) {
                                        currentSpeakingTopic = null
                                    }
                                }
                            }
                        }

                        @Suppress("Deprecation")
                        override fun onError(utteranceId: String?) {
                            // Speech error - update state on main thread
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                if (utteranceId != null && utteranceId.endsWith("_utterance")) {
                                    val topicTitle = utteranceId.removeSuffix("_utterance")
                                    if (currentSpeakingTopic == topicTitle) {
                                        currentSpeakingTopic = null
                                    }
                                }
                            }
                        }
                    })
                    ttsReady = true
                }
            } else {
                Log.e("TTS", "Initialization failed")
            }
        }
        ttsInstance
    }

    // Cleanup TTS when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            sharedTts.stop()
            sharedTts.shutdown()
        }
    }

    val topics = listOf(
        HelpTopic(
            "Disease Information",
            "Learn about Infectious Bronchitis (IB), a highly contagious respiratory disease caused by the avian gammacoronavirus. It affects chickens of all ages, leading to reduced egg production, growth issues, and potential mortality. Variants can also impact kidneys (nephropathogenic strains).",
            HelpCategory.DISEASE,
            Icons.Default.LocalHospital,
            isPopular = true,
            links = listOf(
                HelpLink(
                    url = "https://www.merckvetmanual.com/poultry/infectious-bronchitis/infectious-bronchitis-in-poultry",
                    text = "Learn more about Infectious Bronchitis"
                )
            )
        ),
        HelpTopic(
            "Symptoms",
            "Common signs include coughing, sneezing, nasal discharge, tracheal rales, gasping, and dyspnea. In chicks: conjunctivitis and facial swelling. In layers: drop in egg production (up to 70%), misshapen/soft/wrinkled eggs with watery albumen. Nephropathogenic strains cause lethargy, wet droppings, excessive thirst, and higher mortality (up to 60% with complications). Early infection in chicks may lead to false layer syndrome due to oviduct damage.",
            HelpCategory.DISEASE,
            Icons.Default.Warning,
            isPopular = true,
            isEmergency = true,
            links = listOf(
                HelpLink(
                    url = "https://www.merckvetmanual.com/poultry/infectious-bronchitis/infectious-bronchitis-in-poultry#v3209191",
                    text = "Recognize poultry respiratory disease symptoms"
                ),
                HelpLink(
                    url = "https://www.thepoultrysite.com/disease-guide/infectious-bronchitis-ib",
                    text = "Identify infectious bronchitis clinical signs"
                ),
                HelpLink(
                    url = "https://extension.umn.edu/poultry/poultry-diseases",
                    text = "Understand chicken disease symptom patterns"
                )
            )
        ),
        HelpTopic(
            "Causes",
            "Caused by Infectious Bronchitis Virus (IBV), with many antigenic types. Severity depends on virus strain, bird age/breed/immune status/diet, environmental factors (ventilation, ammonia, temperature), and concurrent infections (e.g., E. coli, Mycoplasma). Virus mutates rapidly via genetic drift or recombination.",
            HelpCategory.DISEASE,
            Icons.Default.Info
        ),
        HelpTopic(
            "Transmission",
            "Spreads via aerosols from respiratory discharges, feces ingestion, or contact with contaminated equipment/clothing/personnel/feed/water. Incubation: 24-48 hours. Peak shedding: 3-5 days post-infection. Birds shed virus up to 20 weeks. Prevent with strict biosecurity: isolate new birds, disinfect regularly.",
            HelpCategory.DISEASE,
            Icons.Default.Warning,
            isEmergency = true
        ),
        HelpTopic(
            "Preventive Measures",
            "Vaccination is key: Use live attenuated (e.g., Massachusetts strains like H120) for chicks (1-14 days via spray/water/eye drops), revaccinate layers. Killed vaccines for breeders. Match vaccines to local strains via surveillance. Biosecurity: Clean environments, good ventilation, balanced diets, avoid overcrowding. Monitor and quarantine.",
            HelpCategory.PREVENTION,
            Icons.Default.Shield,
            isPopular = true,
            links = listOf(
                HelpLink(
                    url = "https://www.aphis.usda.gov/aphis/ourfocus/animalhealth/animal-disease-information/avian/defend-the-flock",
                    text = "USDA approved poultry biosecurity guidelines"
                ),
                HelpLink(
                    url = "https://www.merckvetmanual.com/poultry/infectious-bronchitis/infectious-bronchitis-in-poultry#v3209193",
                    text = "Veterinary recommended prevention strategies"
                )
            )
        ),
        HelpTopic(
            "Treatment",
            "No specific antiviral; focus on supportive care. Antimicrobials for secondary bacterial infections. Increase temperature in cold weather, reduce protein for kidney strains, add electrolytes to water. Isolate sick birds, improve ventilation, reduce stress. Consult vet for prescriptions; early action minimizes mortality to ~5%.",
            HelpCategory.TREATMENT,
            Icons.Default.MedicalServices,
            isEmergency = true
        ),
        HelpTopic(
            "Vaccines",
            "Live attenuated for initial protection; killed/adjuvanted for layers/breeders to pass maternal antibodies. Common: M41, H120, H52. Use different serotypes for boosters. Autogenous for local variants. Store refrigerated, vaccinate uniformly. Efficacy depends on strain match; revaccinate as needed.",
            HelpCategory.PREVENTION,
            Icons.Default.Vaccines
        ),
        HelpTopic(
            "Farm Tips",
            "Monitor daily for signs; separate sick birds. Ensure proper ventilation/ammonia control. Provide clean water/feed. Vaccinate based on local threats. Reduce stress (e.g., avoid overcrowding). Use app for early detection to act fast and prevent outbreaks.",
            HelpCategory.PREVENTION,
            Icons.Default.CheckCircle,
            isPopular = true,
            links = listOf(
                HelpLink(
                    url = "https://extension.umn.edu/poultry/commercial-poultry-production",
                    text = "Effective farm management best practices"
                )
            )
        ),
        HelpTopic(
            "How to Use Detection Feature",
            "1. Open the Detection screen from the bottom navigation\n2. Take a photo of your chicken using the camera button\n3. Or record audio of chicken sounds (coughing, sneezing)\n4. Wait for AI analysis (usually 10-30 seconds)\n5. Review the results and recommendations\n6. Save important detections to your history",
            HelpCategory.APP_GUIDE,
            Icons.Default.CameraAlt,
            isPopular = false
        ),
        HelpTopic(
            "Understanding Results",
            "The app analyzes your chicken's image and audio to detect signs of Infectious Bronchitis. Results show: Confidence level, Detected symptoms, Risk assessment (Low/Medium/High), Recommended actions, Treatment suggestions. Green = Healthy, Yellow = Monitor, Red = Urgent care needed.",
            HelpCategory.APP_GUIDE,
            Icons.Default.Book
        ),
        HelpTopic(
            "Saving & Managing Records",
            "All detections are automatically saved to your history. You can: View past detections, Filter by date/results, Export records, Share with veterinarian, Set reminders for follow-ups, Mark as favorites for quick access.",
            HelpCategory.APP_GUIDE,
            Icons.Default.Book
        ),
        HelpTopic(
            "When to Contact a Veterinarian",
            "Immediately contact a vet if: Multiple birds show symptoms, High mortality rate (>5%), Severe respiratory distress, Drop in egg production >50%, Birds not eating/drinking, Symptoms worsen after 3 days. Early intervention saves lives and prevents spread.",
            HelpCategory.FAQ,
            Icons.Default.LocalHospital,
            isEmergency = true
        ),
        HelpTopic(
            "App Accuracy & Limitations",
            "Our AI has 85-92% accuracy in detecting IB symptoms. However: Results are not a substitute for veterinary diagnosis, Environmental factors may affect detection, Early-stage infections may be missed, Always consult a vet for confirmation, Regular health monitoring is essential.",
            HelpCategory.FAQ,
            Icons.Default.Info
        )
    )

    val searchSuggestions = remember(searchQuery) {
        if (searchQuery.length >= 2) {
            topics.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.content.contains(searchQuery, ignoreCase = true)
            }.take(5).map { SearchSuggestion(it.title) }
        } else {
            emptyList()
        }
    }

    val filteredTopics = remember(topics, searchQuery, selectedCategory) {
        topics.filter { topic ->
            val matchesSearch = searchQuery.isEmpty() ||
                    topic.title.contains(searchQuery, ignoreCase = true) ||
                    topic.content.contains(searchQuery, ignoreCase = true)
            val matchesCategory =
                selectedCategory == HelpCategory.ALL || topic.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    val popularTopics = topics.filter { it.isPopular }
    val emergencyTopics = topics.filter { it.isEmergency }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Help Center",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF8B4513)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFFFFF),
                    titleContentColor = Color(0xFF8B4513)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(innerPadding)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 0.dp)
            ) {
                // Personalized Greeting
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hey, $userName!",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = Color(0xFF231C16)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "How can we help you today?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Search Bar with Suggestions
                item {
                    Column {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            label = { Text("How can we help you?", color = Color.Black) },
                            placeholder = { Text("Search for help...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedBorderColor = Color.Black,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = Color.Black,
                                unfocusedLabelColor = Color.Gray
                            )
                        )

                        // Search Suggestions
                        AnimatedVisibility(
                            visible = searchSuggestions.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    searchSuggestions.forEach { suggestion ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    searchQuery = suggestion.text
                                                    focusManager.clearFocus()
                                                    keyboardController?.hide()
                                                }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Search,
                                                contentDescription = null,
                                                tint = Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                suggestion.text,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Priority Support Banner
                item(key = "priority_support") {
                    AnimatedVisibility(visible = showPrioritySupport) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    // Scroll to contact section at bottom
                                    coroutineScope.launch {
                                        delay(100)
                                        val lastIndex = listState.layoutInfo.totalItemsCount - 1
                                        if (lastIndex >= 0) {
                                            listState.animateScrollToItem(lastIndex)
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Priority Support",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "Get instant help from our support team",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }

                // Emergency Alert Section
                if (emergencyTopics.isNotEmpty() && searchQuery.isEmpty()) {
                    item(key = "emergency_info") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    expandedSection = emergencyTopics.firstOrNull()?.title
                                    selectedCategory = HelpCategory.ALL
                                    searchQuery = ""
                                    coroutineScope.launch {
                                        delay(100)
                                        val targetIndex = 5
                                        if (targetIndex < listState.layoutInfo.totalItemsCount) {
                                            listState.animateScrollToItem(
                                                minOf(
                                                    targetIndex,
                                                    listState.layoutInfo.totalItemsCount - 1
                                                )
                                            )
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Emergency Info",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "Critical information you need to know",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }

                // Popular Questions
                if (searchQuery.isEmpty() && popularTopics.isNotEmpty()) {
                    item {
                        Column {
                            HorizontalDivider(
                                color = Color.Gray.copy(alpha = 0.3f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Popular Questions",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    items(popularTopics) { topic ->
                        HelpTopicCard(
                            topic = topic,
                            isExpanded = expandedSection == topic.title,
                            onExpandToggle = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                if (expandedSection == topic.title) {
                                    // Collapse this card
                                    if (currentSpeakingTopic == topic.title) {
                                        sharedTts.stop()
                                        currentSpeakingTopic = null
                                    }
                                    expandedSection = null
                                } else {
                                    // Expand this card and collapse any other
                                    // Stop TTS from previous card if playing
                                    if (currentSpeakingTopic != null) {
                                        sharedTts.stop()
                                        currentSpeakingTopic = null
                                    }
                                    expandedSection = topic.title
                                }
                            },
                            tts = sharedTts,
                            ttsReady = ttsReady,
                            currentSpeakingTopic = currentSpeakingTopic,
                            onSpeakingTopicChanged = { currentSpeakingTopic = it }
                        )
                    }
                }

                // Category Filter
                if (searchQuery.isEmpty()) {
                    item {
                        Column {
                            HorizontalDivider(
                                color = Color.Gray.copy(alpha = 0.3f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Browse by Category",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(HelpCategory.entries) { category ->
                                    CategoryChip(
                                        category = category,
                                        isSelected = selectedCategory == category,
                                        onClick = {
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                            selectedCategory = category
                                        },
                                        topicCount = topics.count { it.category == category }
                                    )
                                }
                            }
                        }
                    }
                }

                // All Topics Section
                item {
                    if (searchQuery.isNotEmpty() || selectedCategory != HelpCategory.ALL) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Search Results" else "All Topics",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )
                    }
                }

                items(filteredTopics.filter { if (searchQuery.isEmpty()) !it.isPopular else true }) { topic ->
                    HelpTopicCard(
                        topic = topic,
                        isExpanded = expandedSection == topic.title,
                        onExpandToggle = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            if (expandedSection == topic.title) {
                                // Collapse this card
                                if (currentSpeakingTopic == topic.title) {
                                    sharedTts.stop()
                                    currentSpeakingTopic = null
                                }
                                expandedSection = null
                            } else {
                                // Expand this card and collapse any other
                                // Stop TTS from previous card if playing
                                if (currentSpeakingTopic != null) {
                                    sharedTts.stop()
                                    currentSpeakingTopic = null
                                }
                                expandedSection = topic.title
                            }
                        },
                        tts = sharedTts,
                        ttsReady = ttsReady,
                        currentSpeakingTopic = currentSpeakingTopic,
                        onSpeakingTopicChanged = { currentSpeakingTopic = it }
                    )
                }

                // Divider before Support & Contact
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(
                        color = Color(0xFF8B4513).copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Support & Contact
                item(key = "contact_section") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFC1E0EC)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Need more assistance?",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp
                                ),
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Our support team is ready to help.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            HorizontalDivider(color = Color.Black.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))
                            // Facebook Contact
                            ContactInfoRowWithLogo(
                                label = "ChickCare Support Team",
                                value = "Facebook",
                                labelColor = Color(0xFF064575),
                                valueColor = Color.Black,
                                logoResId = R.drawable.facebook_logo,
                                onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        "https://www.facebook.com/profile.php?id=61582950316747".toUri()
                                    )
                                    context.startActivity(intent)
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Gmail Contact
                            ContactInfoRowWithLogo(
                                label = "chickcaresupp0rt@gmail.com",
                                value = "Gmail",
                                labelColor = Color(0xFFAB2626),
                                valueColor = Color.Black,
                                logoResId = R.drawable.gmail_log,
                                onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        "https://mail.google.com/mail/u/3/#inbox?compose=new".toUri()
                                    )
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: HelpCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    topicCount: Int
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFC9A779)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                category.icon,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = Color.Black
            )
            if (topicCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "$topicCount",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun HelpTopicCard(
    topic: HelpTopic,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    tts: TextToSpeech?,
    ttsReady: Boolean,
    currentSpeakingTopic: String?,
    onSpeakingTopicChanged: (String?) -> Unit
) {
    val context = LocalContext.current

    // Local state for this card
    var isSpeaking by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    val isThisTopicSpeaking = currentSpeakingTopic == topic.title

    // Update local speaking state based on current speaking topic
    LaunchedEffect(isThisTopicSpeaking) {
        isSpeaking = isThisTopicSpeaking
        if (!isThisTopicSpeaking) {
            isPaused = false
        }
    }

    // Auto-start TTS when card is expanded
    LaunchedEffect(isExpanded, ttsReady, isThisTopicSpeaking) {
        if (isExpanded && ttsReady && tts != null && !isThisTopicSpeaking && !isPaused) {
            delay(300) // Small delay for smooth animation
            // Stop any currently playing TTS
            tts.stop()
            onSpeakingTopicChanged(topic.title)
            isSpeaking = true
            tts.speak(topic.content, TextToSpeech.QUEUE_FLUSH, null, "${topic.title}_utterance")
        }
    }

    // Cleanup when card is collapsed
    LaunchedEffect(isExpanded) {
        if (!isExpanded && isThisTopicSpeaking) {
            tts?.stop()
            onSpeakingTopicChanged(null)
            isSpeaking = false
            isPaused = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onExpandToggle),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = topic.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (topic.isPopular) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(containerColor = Color(0xFFFF5722)) {
                                Text("Popular", fontSize = 8.sp, color = Color.White)
                            }
                        }
                        if (topic.isEmergency) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Badge(containerColor = Color(0xFFD32F2F)) {
                                Text("Urgent", fontSize = 8.sp, color = Color.White)
                            }
                        }
                    }
                    Text(
                        text = topic.category.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    // Content with sound icon for all cards
                    if (ttsReady && tts != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = topic.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    when {
                                        isThisTopicSpeaking && isSpeaking -> {
                                            tts.stop()
                                            isSpeaking = false
                                            isPaused = true
                                            onSpeakingTopicChanged(null)
                                        }

                                        isPaused || (!isSpeaking && !isThisTopicSpeaking) -> {
                                            // Stop any other playing TTS
                                            if (currentSpeakingTopic != null && currentSpeakingTopic != topic.title) {
                                                tts.stop()
                                            }
                                            onSpeakingTopicChanged(topic.title)
                                            tts.speak(
                                                topic.content,
                                                TextToSpeech.QUEUE_FLUSH,
                                                null,
                                                "${topic.title}_utterance"
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isPaused || (!isSpeaking && !isThisTopicSpeaking)) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = if (isPaused || (!isSpeaking && !isThisTopicSpeaking)) "Play audio" else "Pause audio",
                                    tint = Color(0xFF2196F3)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = topic.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                    // Add clickable links if provided
                    if (topic.links.isNotEmpty()) {
                        topic.links.forEach { link ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = link.text,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color(0xFF2196F3),
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, link.url.toUri())
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactInfoRowWithLogo(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
    logoResId: Int?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo (rounded PNG)
        if (logoResId != null) {
            Image(
                painter = painterResource(id = logoResId),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = labelColor
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor
            )
        }
    }
}
