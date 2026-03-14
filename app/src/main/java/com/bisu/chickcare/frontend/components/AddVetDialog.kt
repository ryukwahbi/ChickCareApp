package com.bisu.chickcare.frontend.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bisu.chickcare.backend.data.EmergencyContact
import com.bisu.chickcare.backend.repository.EmergencyContactRepository
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import java.util.UUID

@Composable
fun AddVetDialog(
    onDismiss: () -> Unit,
    onContactSaved: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { EmergencyContactRepository(context) }
    
    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (ThemeViewModel.isDarkMode) ThemeColorUtils.surface(Color(0xFFE5E2DE)) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Contacts,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Add Veterinarian",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = ThemeColorUtils.black()
                            )
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ThemeColorUtils.black()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // Inputs
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Veterinarian Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = customOutlinedTextFieldColors(ThemeViewModel.isDarkMode)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    colors = customOutlinedTextFieldColors(ThemeViewModel.isDarkMode)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Clinic Address / Notes (Optional)") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = customOutlinedTextFieldColors(ThemeViewModel.isDarkMode)
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Name and Phone Number are required",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Button(
                        onClick = {
                            if (name.isBlank() || phoneNumber.isBlank()) {
                                isError = true
                            } else {
                                val newContact = EmergencyContact(
                                    id = UUID.randomUUID().toString(),
                                    name = name,
                                    phoneNumber = phoneNumber,
                                    type = EmergencyContact.ContactType.VETERINARIAN,
                                    notes = notes.takeIf { it.isNotBlank() }
                                )
                                repository.addContact(newContact)
                                onContactSaved()
                            }
                        },
                        modifier = Modifier.height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text(
                            text = "SAVE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThemeColorUtils.white()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun customOutlinedTextFieldColors(isDarkMode: Boolean) = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ThemeColorUtils.primary(),
    unfocusedBorderColor = if (isDarkMode) Color.Gray else Color.LightGray,
    focusedLabelColor = ThemeColorUtils.primary(),
    unfocusedLabelColor = if (isDarkMode) Color.Gray else Color.Gray,
    cursorColor = ThemeColorUtils.primary(),
    focusedTextColor = ThemeColorUtils.black(),
    unfocusedTextColor = ThemeColorUtils.black()
)
