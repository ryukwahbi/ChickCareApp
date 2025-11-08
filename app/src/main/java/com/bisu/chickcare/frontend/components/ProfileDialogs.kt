package com.bisu.chickcare.frontend.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.bisu.chickcare.backend.data.UserProfile
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePhotoBottomSheet(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit,
    canRemove: Boolean,
    onRemove: () -> Unit,
    onViewCurrent: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            SheetActionRow(icon = Icons.Default.CameraAlt, label = "Take Photo", onClick = onTakePhoto)
            Spacer(modifier = Modifier.height(8.dp))
            SheetActionRow(icon = Icons.Default.PhotoLibrary, label = "Choose from Gallery", onClick = onChooseFromGallery)
            Spacer(modifier = Modifier.height(8.dp))
            SheetActionRow(icon = Icons.Default.RemoveRedEye, label = "View current photo", onClick = onViewCurrent)
            if (canRemove) {
                Spacer(modifier = Modifier.height(8.dp))
                SheetActionRow(icon = Icons.Default.Delete, label = "Remove photo", onClick = onRemove)
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    }
}

@Composable
fun SheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = ThemeColorUtils.black())
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun PhotoPreviewDialog(
    imageModel: Any,
    onDismiss: () -> Unit,
    aspectRatio: Float? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ThemeColorUtils.surface(Color.White)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                val imageShape = RoundedCornerShape(20.dp)
                var imageModifier = Modifier
                    .fillMaxWidth()
                    .clip(imageShape)

                imageModifier = if (aspectRatio != null) {
                    imageModifier.aspectRatio(aspectRatio)
                } else {
                    imageModifier.wrapContentHeight()
                }

                Box(
                    modifier = imageModifier,
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = "Photo preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                TextButton(onClick = onDismiss) {
                    Text("Close", color = Color(0xFF9C4A0C))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("DEPRECATION")
fun EditProfileDialog(
    userProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    // Initialize all field values
    var gender by remember { mutableStateOf(userProfile.gender?.takeIf { it.isNotEmpty() } ?: "") }
    var address by remember { mutableStateOf(userProfile.address) }
    var farmName by remember { mutableStateOf(userProfile.farmName) }
    var farmLocation by remember { mutableStateOf(userProfile.farmLocation) }
    var farmType by remember { mutableStateOf(userProfile.farmType) }
    var specialization by remember { mutableStateOf(userProfile.specialization) }
    var numberOfBirds by remember { mutableStateOf(userProfile.numberOfBirds) }
    var yearsExperience by remember { mutableStateOf(userProfile.yearsExperience) }
    
    // Dropdown states
    var showGenderDropdown by remember { mutableStateOf(false) }
    var showFarmTypeDropdown by remember { mutableStateOf(false) }
    var showSpecializationDropdown by remember { mutableStateOf(false) }
    
    val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")
    val farmTypeOptions = listOf(
        "Free Range",
        "Cage System",
        "Barn System",
        "Pasture Raised",
        "Organic",
        "Conventional",
        "Backyard"
    )
    val specializationOptions = listOf(
        "Egg Production",
        "Meat Production",
        "Breeding",
        "Mixed Operations",
        "Commercial",
        "Backyard/Hobby"
    )
    
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = ThemeColorUtils.black(),
        unfocusedTextColor = ThemeColorUtils.black(),
        cursorColor = Color(0xFF1A1818),
        focusedContainerColor = ThemeColorUtils.white(),
        unfocusedContainerColor = ThemeColorUtils.white(),
        focusedBorderColor = Color(0xFF3F3E3D),
        unfocusedBorderColor = Color(0xFF000000),
        focusedLabelColor = Color(0xFF3F3E3D),
        unfocusedLabelColor = Color(0xFF000000),
        errorBorderColor = Color.Red,
        errorLabelColor = Color.Red,
        disabledTextColor = ThemeColorUtils.lightGray(Color.Gray),
        disabledBorderColor = ThemeColorUtils.lightGray(Color.LightGray),
        disabledContainerColor = Color(0xFFF5F5F5)
    )
    
    val scrollState = rememberScrollState()
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = ThemeColorUtils.white()
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2F1801)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ThemeColorUtils.black()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "READ ONLY:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ThemeColorUtils.lightGray(Color.Gray)
                            )
                        }
                        
                        OutlinedTextField(
                            value = userProfile.email,
                            onValueChange = {},
                            label = { Text("Email") },
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                        
                        OutlinedTextField(
                            value = userProfile.contact,
                            onValueChange = {},
                            label = { Text("Contact Number") },
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                        
                        OutlinedTextField(
                            value = userProfile.birthDate,
                            onValueChange = {},
                            label = { Text("Birth Date") },
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFFE5E7EB),
                        thickness = 1.dp
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = showGenderDropdown,
                                onExpandedChange = { showGenderDropdown = !showGenderDropdown },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = gender.ifEmpty { "Select gender" },
                                    onValueChange = {},
                                    label = { Text("Gender") },
                                    readOnly = true,
                                    modifier = Modifier.menuAnchor(),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showGenderDropdown)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = if (gender.isEmpty()) ThemeColorUtils.lightGray(Color.Gray) else ThemeColorUtils.black(),
                                        unfocusedTextColor = if (gender.isEmpty()) ThemeColorUtils.lightGray(Color.Gray) else ThemeColorUtils.black(),
                                        focusedBorderColor = Color(0xFF3F3E3D),
                                        unfocusedBorderColor = Color(0xFF000000),
                                        focusedLabelColor = Color(0xFF3F3E3D),
                                        unfocusedLabelColor = Color(0xFF000000)
                                    )
                                )
                                
                                ExposedDropdownMenu(
                                    expanded = showGenderDropdown,
                                    onDismissRequest = { showGenderDropdown = false },
                                    modifier = Modifier
                                        .heightIn(max = 200.dp)
                                ) {
                                    genderOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { 
                                                Text(
                                                    option,
                                                    fontWeight = if (gender == option) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (gender == option) Color(0xFF000000) else Color.Unspecified
                                                )
                                            },
                                            onClick = {
                                                gender = option
                                                showGenderDropdown = false
                                            },
                                            leadingIcon = if (gender == option) {
                                                {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = "Selected",
                                                        tint = Color(0xFF94D048)
                                                    )
                                                }
                                            } else null
                                        )
                                    }
                                }
                            }
                            
                            // Address - regular text input (NOT dropdown)
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Address") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors
                            )
                        }
                        
                        OutlinedTextField(
                            value = farmName,
                            onValueChange = { farmName = it },
                            label = { Text("Farm Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                        
                        // Farm Location - regular text input (NOT dropdown)
                        OutlinedTextField(
                            value = farmLocation,
                            onValueChange = { farmLocation = it },
                            label = { Text("Farm Location") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                        
                        // Farm Type dropdown using ExposedDropdownMenuBox
                        ExposedDropdownMenuBox(
                            expanded = showFarmTypeDropdown,
                            onExpandedChange = { showFarmTypeDropdown = !showFarmTypeDropdown },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = farmType,
                                onValueChange = {},
                                label = { Text("Farm Type") },
                                readOnly = true,
                                modifier = Modifier.menuAnchor(),
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFarmTypeDropdown)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors
                            )
                            
                            ExposedDropdownMenu(
                                expanded = showFarmTypeDropdown,
                                onDismissRequest = { showFarmTypeDropdown = false },
                                modifier = Modifier
                                    .heightIn(max = 200.dp)
                            ) {
                                farmTypeOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                option,
                                                color = if (farmType == option) Color(0xFF000000) else Color.Unspecified,
                                                fontWeight = if (farmType == option) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            farmType = option
                                            showFarmTypeDropdown = false
                                        },
                                        leadingIcon = if (farmType == option) {
                                            {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = Color(0xFF94D048)
                                                )
                                            }
                                        } else null
                                    )
                                }
                            }
                        }
                        
                        ExposedDropdownMenuBox(
                            expanded = showSpecializationDropdown,
                            onExpandedChange = { showSpecializationDropdown = !showSpecializationDropdown },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = specialization,
                                onValueChange = {},
                                label = { Text("Specialization") },
                                readOnly = true,
                                modifier = Modifier.menuAnchor(),
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSpecializationDropdown)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors
                            )
                            
                            ExposedDropdownMenu(
                                expanded = showSpecializationDropdown,
                                onDismissRequest = { showSpecializationDropdown = false },
                                modifier = Modifier
                                    .heightIn(max = 200.dp)
                            ) {
                                specializationOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                option,
                                                color = if (specialization == option) Color(
                                                    0xFF000000
                                                ) else Color.Unspecified,
                                                fontWeight = if (specialization == option) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            specialization = option
                                            showSpecializationDropdown = false
                                        },
                                        leadingIcon = if (specialization == option) {
                                            {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = Color(0xFF94D048)
                                                )
                                            }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFFE5E7EB),
                        thickness = 1.dp
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = numberOfBirds,
                            onValueChange = { numberOfBirds = it },
                            label = { Text("Number of Chickens") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                        
                        OutlinedTextField(
                            value = yearsExperience,
                            onValueChange = { yearsExperience = it },
                            label = { Text("Years of Experience") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Save Button
                Button(
                    onClick = {
                        val updatedFields = mapOf(
                            "gender" to gender,
                            "address" to address,
                            "farmName" to farmName,
                            "farmLocation" to farmLocation,
                            "farmType" to farmType,
                            "specialization" to specialization,
                            "numberOfBirds" to numberOfBirds,
                            "yearsExperience" to yearsExperience
                        )
                        onSave(updatedFields)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = ThemeColorUtils.white()
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "SAVE",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInfoDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }
    var selectedFieldLabel by remember { mutableStateOf("") }
    var selectedFieldKey by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    
    val poultryFields = listOf(
        "Farm Name" to "farmName",
        "Farm Location" to "farmLocation",
        "Farm Type" to "farmType",
        "Specialization" to "specialization",
        "Number of Birds" to "numberOfBirds",
        "Years of Experience" to "yearsExperience"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Personal Information") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box {
                    OutlinedTextField(
                        value = selectedFieldLabel,
                        onValueChange = {},
                        label = { Text("Select Field") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDropdown = true },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select field")
                        }
                    )
                    
                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        poultryFields.forEach { (label, fieldKey) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedFieldLabel = label
                                    selectedFieldKey = fieldKey
                                    showDropdown = false
                                }
                            )
                        }
                    }
                }
                
                if (selectedFieldKey.isNotEmpty()) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text(selectedFieldLabel) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = when (selectedFieldKey.lowercase()) {
                            "numberofbirds", "yearsexperience" -> KeyboardOptions(keyboardType = KeyboardType.Number)
                            else -> KeyboardOptions(keyboardType = KeyboardType.Text)
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedFieldKey.isNotEmpty() && value.isNotEmpty()) {
                        onSave(selectedFieldKey, value)
                    }
                },
                enabled = selectedFieldKey.isNotEmpty() && value.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}