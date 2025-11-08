package com.bisu.chickcare.frontend.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

@Composable
fun EditFullNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var fullName by remember { mutableStateOf(currentName) }
    var error by remember { mutableStateOf("") }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = ThemeColorUtils.darkGray(Color(0xFF212121)),
        unfocusedTextColor = ThemeColorUtils.darkGray(Color(0xFF212121)),
        cursorColor = Color(0xFF1A1818),
        focusedContainerColor = ThemeColorUtils.white(),
        unfocusedContainerColor = ThemeColorUtils.white(),
        focusedBorderColor = ThemeColorUtils.black(),
        unfocusedBorderColor = ThemeColorUtils.black(),
        focusedLabelColor = ThemeColorUtils.darkGray(Color(0xFF424242)),
        unfocusedLabelColor = ThemeColorUtils.darkGray(Color(0xFF424242)),
        errorBorderColor = Color.Red,
        errorLabelColor = Color.Red
    )

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
                // Header with title and X button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Full Name",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ThemeColorUtils.black()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Text Field
                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        error = ""
                    },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    isError = error.isNotEmpty(),
                    supportingText = if (error.isNotEmpty()) {
                        { Text(error, color = Color.Red) }
                    } else null
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Save Button
                Button(
                    onClick = {
                        if (fullName.trim().isEmpty()) {
                            error = "Full name cannot be empty"
                        } else if (fullName.trim().length < 2) {
                            error = "Full name must be at least 2 characters"
                        } else {
                            onSave(fullName.trim())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = ThemeColorUtils.white()
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EditEmailDialog(
    currentEmail: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var email by remember { mutableStateOf(currentEmail) }
    var error by remember { mutableStateOf("") }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = ThemeColorUtils.darkGray(Color(0xFF212121)),
        unfocusedTextColor = ThemeColorUtils.darkGray(Color(0xFF212121)),
        cursorColor = Color(0xFF1A1818),
        focusedContainerColor = ThemeColorUtils.white(),
        unfocusedContainerColor = ThemeColorUtils.white(),
        focusedBorderColor = ThemeColorUtils.black(),
        unfocusedBorderColor = ThemeColorUtils.black(),
        focusedLabelColor = ThemeColorUtils.darkGray(Color(0xFF424242)),
        unfocusedLabelColor = ThemeColorUtils.darkGray(Color(0xFF424242)),
        errorBorderColor = Color.Red,
        errorLabelColor = Color.Red
    )

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
                // Header with title and X button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Email",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ThemeColorUtils.black()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Info Text
                Text(
                    text = "Changing your email will require verification. Make sure you have access to the new email address.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeColorUtils.lightGray(Color(0xFF666666)),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Text Field
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        error = ""
                    },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    isError = error.isNotEmpty(),
                    supportingText = if (error.isNotEmpty()) {
                        { Text(error, color = Color.Red) }
                    } else null
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Save Button
                Button(
                    onClick = {
                        if (email.trim().isEmpty()) {
                            error = "Email cannot be empty"
                        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                            error = "Please enter a valid email address"
                        } else {
                            onSave(email.trim())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = ThemeColorUtils.white()
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    var error by remember { mutableStateOf("") }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = ThemeColorUtils.darkGray(Color(0xFF212121)),
        unfocusedTextColor = ThemeColorUtils.darkGray(Color(0xFF212121)),
        cursorColor = Color(0xFF1A1818),
        focusedContainerColor = ThemeColorUtils.white(),
        unfocusedContainerColor = ThemeColorUtils.white(),
        focusedBorderColor = ThemeColorUtils.black(),
        unfocusedBorderColor = ThemeColorUtils.black(),
        focusedLabelColor = ThemeColorUtils.darkGray(Color(0xFF424242)),
        unfocusedLabelColor = ThemeColorUtils.darkGray(Color(0xFF424242)),
        errorBorderColor = Color.Red,
        errorLabelColor = Color.Red
    )

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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Change Password",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ThemeColorUtils.black()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Old Password
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = {
                        oldPassword = it
                        error = ""
                    },
                    label = { Text("Current Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (oldPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { oldPasswordVisible = !oldPasswordVisible }) {
                            Icon(
                                if (oldPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (oldPasswordVisible) "Hide password" else "Show password",
                                tint = ThemeColorUtils.lightGray(Color.Gray)
                            )
                        }
                    },
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    isError = error.isNotEmpty() && oldPassword.isEmpty()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // New Password
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        error = ""
                    },
                    label = { Text("New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (newPasswordVisible) "Hide password" else "Show password",
                                tint = ThemeColorUtils.lightGray(Color.Gray)
                            )
                        }
                    },
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    isError = error.isNotEmpty() && newPassword.isEmpty(),
                    supportingText = {
                        Text(
                            "At least 8 characters with uppercase, lowercase, number, and special character",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeColorUtils.lightGray(Color(0xFF666666))
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Confirm Password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        error = ""
                    },
                    label = { Text("Confirm New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                tint = ThemeColorUtils.lightGray(Color.Gray)
                            )
                        }
                    },
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    isError = error.isNotEmpty(),
                    supportingText = if (error.isNotEmpty()) {
                        { Text(error, color = Color.Red) }
                    } else null
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Save Button
                Button(
                    onClick = {
                        when {
                            oldPassword.isEmpty() -> error = "Please enter your current password"
                            newPassword.isEmpty() -> error = "Please enter a new password"
                            newPassword.length < 8 -> error = "Password must be at least 8 characters"
                            !newPassword.any { it.isUpperCase() } -> error = "Password must contain an uppercase letter"
                            !newPassword.any { it.isLowerCase() } -> error = "Password must contain a lowercase letter"
                            !newPassword.any { it.isDigit() } -> error = "Password must contain a number"
                            !newPassword.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) } -> error = "Password must contain a special character"
                            newPassword != confirmPassword -> error = "Passwords do not match"
                            else -> {
                                error = ""
                                onSave(oldPassword, newPassword, confirmPassword)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = ThemeColorUtils.white()
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProfilePicturePickerDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit,
    onRemovePhoto: () -> Unit,
    hasPhoto: Boolean
) {
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
                // Header with title and X button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Change Profile Picture",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ThemeColorUtils.black()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action Buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            onTakePhoto()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = ThemeColorUtils.white()
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Take Photo")
                    }
                    Button(
                        onClick = {
                            onChooseFromGallery()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3),
                            contentColor = ThemeColorUtils.white()
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Choose from Gallery")
                    }
                    if (hasPhoto) {
                        TextButton(
                            onClick = {
                                onRemovePhoto()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remove Photo", color = Color.Red)
                        }
                    }
                }
            }
        }
    }
}
