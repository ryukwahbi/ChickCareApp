package com.bisu.chickcare.frontend.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bisu.chickcare.backend.viewmodels.AuthViewModel

@Composable
fun HelpCenterScreen(paddingValues: PaddingValues) {
    val authViewModel: AuthViewModel = viewModel()
    // FIX: Observe the userProfile StateFlow from the ViewModel
    val userProfile by authViewModel.userProfile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showPrioritySupport by remember { mutableStateOf(true) }
    var expandedSections by remember { mutableStateOf(setOf<String>()) }

    // Use the collected userProfile state directly.
    // If the profile is not null, get the first name. Otherwise, default to "User".
    val userName = userProfile?.fullName?.split(" ")?.firstOrNull() ?: "User"

    // ERROR FIX: The problematic LaunchedEffect has been removed.

    val topics = listOf(
        "Disease Information" to "Learn about Infectious Bronchitis (IB), a highly contagious respiratory disease caused by the avian gammacoronavirus. It affects chickens of all ages, leading to reduced egg production, growth issues, and potential mortality. Variants can also impact kidneys (nephropathogenic strains).",
        "Symptoms" to "Common signs include coughing, sneezing, nasal discharge, tracheal rales, gasping, and dyspnea. In chicks: conjunctivitis and facial swelling. In layers: drop in egg production (up to 70%), misshapen/soft/wrinkled eggs with watery albumen. Nephropathogenic strains cause lethargy, wet droppings, excessive thirst, and higher mortality (up to 60% with complications). Early infection in chicks may lead to false layer syndrome due to oviduct damage.",
        "Causes" to "Caused by Infectious Bronchitis Virus (IBV), with many antigenic types. Severity depends on virus strain, bird age/breed/immune status/diet, environmental factors (ventilation, ammonia, temperature), and concurrent infections (e.g., E. coli, Mycoplasma). Virus mutates rapidly via genetic drift or recombination.",
        "Transmission" to "Spreads via aerosols from respiratory discharges, feces ingestion, or contact with contaminated equipment/clothing/personnel/feed/water. Incubation: 24-48 hours. Peak shedding: 3-5 days post-infection. Birds shed virus up to 20 weeks. Prevent with strict biosecurity: isolate new birds, disinfect regularly.",
        "Preventive Measures" to "Vaccination is key: Use live attenuated (e.g., Massachusetts strains like H120) for chicks (1-14 days via spray/water/eye drops), revaccinate layers. Killed vaccines for breeders. Match vaccines to local strains via surveillance. Biosecurity: Clean environments, good ventilation, balanced diets, avoid overcrowding. Monitor and quarantine.",
        "Treatment" to "No specific antiviral; focus on supportive care. Antimicrobials for secondary bacterial infections. Increase temperature in cold weather, reduce protein for kidney strains, add electrolytes to water. Isolate sick birds, improve ventilation, reduce stress. Consult vet for prescriptions; early action minimizes mortality to ~5%.",
        "Vaccines" to "Live attenuated for initial protection; killed/adjuvanted for layers/breeders to pass maternal antibodies. Common: M41, H120, H52. Use different serotypes for boosters. Autogenous for local variants. Store refrigerated, vaccinate uniformly. Efficacy depends on strain match; revaccinate as needed.",
        "Farm Tips" to "Monitor daily for signs; separate sick birds. Ensure proper ventilation/ammonia control. Provide clean water/feed. Vaccinate based on local threats. Reduce stress (e.g., avoid overcrowding). Use app for early detection to act fast and prevent outbreaks."
    )

    // FIX: Corrected indentation
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Hey, $userName \n" +
                        "\uD83D\uDC4B\uD83C\uDFFB",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium, fontSize = 24.sp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("How can we help you?") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            AnimatedVisibility(visible = showPrioritySupport) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Priority Support",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Get your questions answered faster in the support chat",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        IconButton(onClick = { showPrioritySupport = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HelpQuestionCard(
                    question = "How does the app detect bronchitis?",
                    onClick = { /* TODO: Expand or navigate to detailed explanation */ },
                    modifier = Modifier.weight(1f)
                )
                HelpQuestionCard(
                    question = "What are the symptoms of bronchitis?",
                    onClick = { expandedSections = if ("Symptoms" in expandedSections) expandedSections - "Symptoms" else expandedSections + "Symptoms" },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                text = "Other topics",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(topics.filter { it.first.contains(searchQuery, ignoreCase = true) || it.second.contains(searchQuery, ignoreCase = true) }) { (title, content) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expandedSections =
                            if (title in expandedSections) expandedSections - title else expandedSections + title
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Help,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        AnimatedVisibility(visible = title in expandedSections, enter = fadeIn(), exit = fadeOut()) {
                            Text(
                                text = content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Support & Contact Us",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "For further assistance, contact our support team at support@chickcare.com or call +63-123-456-7890.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun HelpQuestionCard(question: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = question,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
            maxLines = 2,
            textAlign = TextAlign.Center
        )
    }
}
