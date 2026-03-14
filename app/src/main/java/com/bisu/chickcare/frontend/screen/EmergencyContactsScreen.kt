package com.bisu.chickcare.frontend.screen

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.data.EmergencyContact
import com.bisu.chickcare.backend.repository.EmergencyContactRepository
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(navController: NavController) {
    val context = LocalContext.current
    val contactRepo = remember { EmergencyContactRepository(context) }
    var contacts by remember { mutableStateOf<List<EmergencyContact>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<EmergencyContact?>(null) }
    
    LaunchedEffect(Unit) {
        contacts = contactRepo.getAllContacts()
    }
    
    fun refreshContacts() {
        contacts = contactRepo.getAllContacts()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        androidx.compose.ui.res.stringResource(R.string.emergency_title),
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.black()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = androidx.compose.ui.res.stringResource(R.string.back),
                            tint = ThemeColorUtils.black()
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeColorUtils.white()
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFFF44336)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = androidx.compose.ui.res.stringResource(R.string.emergency_add_title),
                    tint = ThemeColorUtils.white()
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
        ) {
            if (contacts.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = ThemeColorUtils.black().copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.emergency_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = ThemeColorUtils.black().copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.emergency_empty_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeColorUtils.black().copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(contacts) { contact ->
                        EmergencyContactCard(
                            contact = contact,
                            onCall = {
                                val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                    data = "tel:${contact.phoneNumber}".toUri()
                                }
                                context.startActivity(callIntent)
                            },
                            onEdit = {
                                editingContact = contact
                                showAddDialog = true
                            },
                            onDelete = {
                                contactRepo.deleteContact(contact.id)
                                refreshContacts()
                            }
                        )
                    }
                }
            }
        }
        
        // Add/Edit Dialog
        if (showAddDialog) {
            AddEditContactDialog(
                contact = editingContact,
                onDismiss = {
                    showAddDialog = false
                    editingContact = null
                },
                onSave = { contact ->
                    if (editingContact != null) {
                        contactRepo.updateContact(contact)
                    } else {
                        contactRepo.addContact(contact)
                    }
                    refreshContacts()
                    showAddDialog = false
                    editingContact = null
                }
            )
        }
    }
}

@Composable
fun EmergencyContactCard(
    contact: EmergencyContact,
    onCall: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (ThemeViewModel.isDarkMode) Color(0xFF2C2C2C) else ThemeColorUtils.white()
        ),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on type
            Icon(
                imageVector = if (contact.type == EmergencyContact.ContactType.VETERINARIAN) 
                    Icons.Default.LocalHospital else Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFFF44336),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemeColorUtils.black()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeColorUtils.black().copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(contact.type.getTypeLabelRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFF44336),
                    fontWeight = FontWeight.Medium
                )
                if (!contact.notes.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = contact.notes!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeColorUtils.black().copy(alpha = 0.6f)
                    )
                }
            }
            
            // Action buttons
            IconButton(onClick = onCall) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = "Call",
                    tint = Color(0xFF4CAF50)
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color(0xFF2196F3)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
fun AddEditContactDialog(
    contact: EmergencyContact?,
    onDismiss: () -> Unit,
    onSave: (EmergencyContact) -> Unit
) {
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phoneNumber by remember { mutableStateOf(contact?.phoneNumber ?: "") }
    var selectedType by remember { mutableStateOf(contact?.type ?: EmergencyContact.ContactType.VETERINARIAN) }
    var notes by remember { mutableStateOf(contact?.notes ?: "") }
    
    val title = if (contact != null) 
        androidx.compose.ui.res.stringResource(R.string.emergency_edit_title) 
    else 
        androidx.compose.ui.res.stringResource(R.string.emergency_add_title)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ThemeColorUtils.white(),
        titleContentColor = ThemeColorUtils.black(),
        textContentColor = ThemeColorUtils.black(),
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(androidx.compose.ui.res.stringResource(R.string.emergency_field_name), color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ThemeColorUtils.white(),
                        unfocusedContainerColor = ThemeColorUtils.white(),
                        focusedTextColor = ThemeColorUtils.black(),
                        unfocusedTextColor = ThemeColorUtils.black(),
                        focusedBorderColor = ThemeColorUtils.black(),
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = ThemeColorUtils.black()
                    )
                )
                
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text(androidx.compose.ui.res.stringResource(R.string.emergency_field_phone), color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ThemeColorUtils.white(),
                        unfocusedContainerColor = ThemeColorUtils.white(),
                        focusedTextColor = ThemeColorUtils.black(),
                        unfocusedTextColor = ThemeColorUtils.black(),
                        focusedBorderColor = ThemeColorUtils.black(),
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = ThemeColorUtils.black()
                    )
                )
                
                // Type selector (simplified - can be enhanced with dropdown)
                Text(
                    text = "Type: ${androidx.compose.ui.res.stringResource(selectedType.getTypeLabelRes())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeColorUtils.black()
                )
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(androidx.compose.ui.res.stringResource(R.string.emergency_field_notes), color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ThemeColorUtils.white(),
                        unfocusedContainerColor = ThemeColorUtils.white(),
                        focusedTextColor = ThemeColorUtils.black(),
                        unfocusedTextColor = ThemeColorUtils.black(),
                        focusedBorderColor = ThemeColorUtils.black(),
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = ThemeColorUtils.black()
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phoneNumber.isNotBlank()) {
                        val newContact = EmergencyContact(
                            id = contact?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            phoneNumber = phoneNumber.trim(),
                            type = selectedType,
                            notes = notes.takeIf { it.isNotBlank() }
                        )
                        onSave(newContact)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(androidx.compose.ui.res.stringResource(R.string.emergency_save), color = ThemeColorUtils.white())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(R.string.common_cancel), color = Color(0xFF959597))
            }
        }
    )
}

// Extension function for ContactType to return resource ID
private fun EmergencyContact.ContactType.getTypeLabelRes(): Int {
    return when (this) {
        EmergencyContact.ContactType.VETERINARIAN -> R.string.emergency_type_vet
        EmergencyContact.ContactType.FARM_HELP -> R.string.emergency_type_help
        EmergencyContact.ContactType.EMERGENCY -> R.string.emergency_type_emergency
        EmergencyContact.ContactType.OTHER -> R.string.emergency_type_other
    }
}

