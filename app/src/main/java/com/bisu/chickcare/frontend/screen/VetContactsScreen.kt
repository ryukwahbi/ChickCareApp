package com.bisu.chickcare.frontend.screen

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.bisu.chickcare.backend.data.EmergencyContact
import com.bisu.chickcare.backend.repository.EmergencyContactRepository
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.bisu.chickcare.frontend.components.AddVetDialog
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

@Composable
fun VetContactsScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { EmergencyContactRepository(context) }
    var contacts by remember { mutableStateOf(emptyList<EmergencyContact>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var contactToDelete by remember { mutableStateOf<EmergencyContact?>(null) }

    fun loadContacts() {
        contacts = repository.getContactsByType(EmergencyContact.ContactType.VETERINARIAN)
    }

    LaunchedEffect(Unit) {
        loadContacts()
    }

    if (showAddDialog) {
        AddVetDialog(
            onDismiss = { },
            onContactSaved = {
                loadContacts()
            }
        )
    }

    if (showDeleteConfirmation && contactToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirmation = false 
                contactToDelete = null
            },
            title = { Text("Delete Contact") },
            text = { Text("Are you sure you want to delete ${contactToDelete?.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        contactToDelete?.let { contact ->
                            repository.deleteContact(contact.id)
                            loadContacts()
                        }
                        showDeleteConfirmation = false
                        contactToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { 
                        showDeleteConfirmation = false
                        contactToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ThemeColorUtils.white())
                        .statusBarsPadding()
                        .padding(top = 11.dp, bottom = 12.dp)
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ThemeColorUtils.black()
                        )
                    }

                    Text(
                        text = "VETERINARIAN CONTACTS",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = ThemeColorUtils.black(),

                        ),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 56.dp)
                    )
                }

                // Content
                if (contacts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No veterinarian contacts yet.",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(contacts) { contact ->
                            VetContactCard(
                                contact = contact,
                                onCall = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = "tel:${contact.phoneNumber}".toUri()
                                    }
                                    context.startActivity(intent)
                                },
                                onDelete = {
                                    contactToDelete = contact
                                    showDeleteConfirmation = true
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
fun VetContactCard(
    contact: EmergencyContact,
    onCall: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFF2D2D2D)) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint =  Color.Gray,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ThemeColorUtils.black()
                        )
                    )
                    Text(
                        text = contact.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray
                        )
                    )
                    if (!contact.notes.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = contact.notes,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.Gray,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        )
                    }
                }
            }
            
            Row {
               IconButton(onClick = onCall) {
                   Icon(
                       imageVector = Icons.Default.Call,
                       contentDescription = "Call",
                       tint = Color(0xFF4CAF50)
                   )
               }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}
