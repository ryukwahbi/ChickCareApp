package com.bisu.chickcare.frontend.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.backend.viewmodels.AuthViewModel
import com.bisu.chickcare.frontend.utils.Validators
import kotlinx.coroutines.delay

private enum class MessageType {
    NONE, SUCCESS, ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(navController: NavController) {
    val viewModel: AuthViewModel = viewModel()
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var messageType by remember { mutableStateOf(MessageType.NONE) }
    var isLoading by remember { mutableStateOf(false) }
    var isEmailError by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        disabledTextColor = Color.Black,
        cursorColor = Color(0xFF2F1801),
        focusedContainerColor = Color.LightGray.copy(alpha = 0.9f),
        unfocusedContainerColor = Color.LightGray.copy(alpha = 0.7f),
        disabledContainerColor = Color.LightGray.copy(alpha = 0.7f),
        focusedBorderColor = Color.Black,
        unfocusedBorderColor = Color.Black,
        disabledBorderColor = Color.Black,
        focusedLabelColor = Color(0xFF2F1801),
        unfocusedLabelColor = Color.DarkGray,
        disabledLabelColor = Color.DarkGray,
        errorBorderColor = Color.Red,
        errorLabelColor = Color.Red,
        errorCursorColor = Color.Red,
        errorContainerColor = Color.LightGray.copy(alpha = 0.7f)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFDAAE73).copy(alpha = 0.75f),
                            Color(0xFF946644).copy(alpha = 0.7f),
                            Color(0xFF5C4033).copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(top = 64.dp), // Adjusted to move content upward
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Back button sa top
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp)) // Reduced spacing for tighter placement

            Text(
                text = "Reset Password",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, // Added boldness
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your email address and we'll send you a link to reset your password.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    isEmailError = false
                    message = ""
                    messageType = MessageType.NONE
                },
                label = { Text("Email Address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                isError = isEmailError,
                colors = textFieldColors,
                enabled = !isSuccess,
                supportingText = {
                    if (isEmailError) {
                        Text("Please enter a valid email address", color = Color.Red)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val trimmedEmail = email.trim()
                    if (!Validators.isValidEmail(trimmedEmail)) {
                        isEmailError = true
                        message = "Please enter a valid email address"
                        messageType = MessageType.ERROR
                        return@Button
                    }
                    
                    isLoading = true
                    message = ""
                    messageType = MessageType.NONE
                    isEmailError = false
                    
                    viewModel.resetPassword(trimmedEmail) { success, msg ->
                        isLoading = false
                        message = msg
                        messageType = if (success) MessageType.SUCCESS else MessageType.ERROR
                        if (success) {
                            isEmailError = false
                            isSuccess = true
                        } else {
                            isEmailError = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && !isSuccess && email.trim().isNotEmpty(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD27D2D),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFD27D2D).copy(alpha = 0.7f),
                    disabledContentColor = Color.White.copy(alpha = 0.8f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp,
                    disabledElevation = 4.dp
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isSuccess) "Reset Link Sent!" else "Send Reset Link",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (message.isNotEmpty() && messageType != MessageType.NONE) {
                Text(
                    text = message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    color = when (messageType) {
                        MessageType.SUCCESS -> Color(0xFF4CAF50)
                        MessageType.ERROR -> Color(0xFFEF5350)
                        MessageType.NONE -> Color.Transparent
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Auto-navigate back after successful reset (optional)
            if (isSuccess) {
                LaunchedEffect(Unit) {
                    delay(3000) // Wait 3 seconds
                    navController.popBackStack()
                }
            }
        }
    }
}