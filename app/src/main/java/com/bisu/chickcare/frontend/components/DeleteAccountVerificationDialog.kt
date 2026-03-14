package com.bisu.chickcare.frontend.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

@Composable
fun DeleteAccountVerificationDialog(
    userEmail: String,
    userPhone: String,
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit,
    onResendCode: () -> Unit,
    isDeleting: Boolean
) {
    var verificationCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val hasEmail = userEmail.isNotEmpty()
    val hasPhone = userPhone.isNotEmpty()

    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = { 
            Text(
                "Verify Account Deletion", 
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFCB4444)
            ) 
        },
        text = { 
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Please enter the verification code sent to...",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (hasEmail) {
                    Text(
                        "📧 : $userEmail",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                    )
                }
                
                if (hasPhone) {
                    Text(
                        "📱 : $userPhone",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                    )
                }
                
                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = { 
                        verificationCode = it
                        errorMessage = ""
                    },
                    label = { Text("Verification Code") },
                    placeholder = { Text("Enter 6-digit code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    enabled = !isDeleting,
                    isError = errorMessage.isNotEmpty(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFCB4444),
                        unfocusedBorderColor = ThemeColorUtils.lightGray(Color(0xFF7E7C7C)),
                        focusedLabelColor = Color.Gray,
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (errorMessage.isNotEmpty()) {
                    Text(
                        errorMessage,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                if (isDeleting) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFFCB4444),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                        Text("Deleting account...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                TextButton(
                    onClick = onResendCode,
                    enabled = !isDeleting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                ) {
                    Text("Resend Code")
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isDeleting,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (verificationCode.length == 6) {
                                onVerify(verificationCode)
                            } else {
                                errorMessage = "Please enter a valid 6-digit code"
                            }
                        },
                        enabled = !isDeleting && verificationCode.length == 6,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFCB4444)
                        )
                    ) {
                        Text("Delete Account", color = Color.White)
                    }
                }
            }
        },
        confirmButton = { },
        dismissButton = { },
        containerColor = ThemeColorUtils.white(),
        shape = RoundedCornerShape(10.dp)
    )
}

