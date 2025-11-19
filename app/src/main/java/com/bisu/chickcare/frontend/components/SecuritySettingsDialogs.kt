package com.bisu.chickcare.frontend.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

@Composable
fun TwoFactorAuthSetupDialog(
    onDismiss: () -> Unit,
    onEnable: (String) -> Unit,
    secretKey: String? = null
) {
    var verificationCode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var showSecret by remember { mutableStateOf(false) }

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
                        text = "Setup Two-Factor Authentication",
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
                
                if (secretKey != null) {
                    Text(
                        text = "1. Add this secret key to your authenticator app (Google Authenticator, Authy, etc.):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeColorUtils.lightGray(Color(0xFF666666))
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Secret key display
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = ThemeColorUtils.beige(Color(0xFFF5F5F5))
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showSecret) secretKey else "••••••••••••••••",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = ThemeColorUtils.darkGray(Color(0xFF231C16)),
                                modifier = Modifier.weight(1f)
                            )
                            androidx.compose.material3.TextButton(
                                onClick = { showSecret = !showSecret }
                            ) {
                                Text(if (showSecret) "Hide" else "Show")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "2. Enter the 6-digit verification code from your authenticator app:",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeColorUtils.darkGray(Color(0xFF231C16)),
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "To enable two-factor authentication, you'll need to set up an authenticator app (like Google Authenticator) and enter the verification code.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeColorUtils.lightGray(Color(0xFF666666))
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Enter the verification code from your authenticator app:",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeColorUtils.darkGray(Color(0xFF231C16)),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = {
                        verificationCode = it.take(6)
                        error = ""
                    },
                    label = { Text("Verification Code") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    isError = error.isNotEmpty(),
                    supportingText = if (error.isNotEmpty()) {
                        { Text(error, color = Color.Red) }
                    } else null
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Enable Button
                Button(
                    onClick = {
                        if (verificationCode.length != 6) {
                            error = "Verification code must be 6 digits"
                        } else {
                            error = ""
                            onEnable(verificationCode)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = ThemeColorUtils.white()
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Enable", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BiometricAuthSetupDialog(
    onDismiss: () -> Unit,
    onEnable: () -> Unit
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
                        text = "Enable Biometric Authentication",
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
                Column {
                    Text(
                        text = "Biometric authentication allows you to use your fingerprint or face recognition to unlock the app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeColorUtils.lightGray(Color(0xFF666666))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "You'll be prompted to set up biometric authentication on your device. Make sure your device supports this feature.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeColorUtils.lightGray(Color(0xFF666666))
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Enable Button
                Button(
                    onClick = {
                        onEnable()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = ThemeColorUtils.white()
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Enable", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
