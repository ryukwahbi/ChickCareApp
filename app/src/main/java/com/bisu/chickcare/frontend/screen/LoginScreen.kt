package com.bisu.chickcare.frontend.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.repository.AccountManager
import com.bisu.chickcare.backend.viewmodels.AuthViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import com.bisu.chickcare.frontend.utils.Validators
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController, initialEmail: String? = null) {
    val viewModel: AuthViewModel = viewModel()
    val context = LocalContext.current
    var email by remember { mutableStateOf(initialEmail ?: "") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccessToast by remember { mutableStateOf(false) }
    val accountManager = remember { AccountManager(context) }
    var hasSavedAccounts by remember { mutableStateOf(false) }

    // Auto-hide toast and navigate
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        hasSavedAccounts = accountManager.getSavedAccounts().isNotEmpty()
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Optimized infinite transition to reduce recompositions
    val infiniteTransition = rememberInfiniteTransition(label = "background_zoom")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f, // Reduced from 1.3f to minimize recomposition impact
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing), // Slower animation
            repeatMode = RepeatMode.Reverse
        ),
        label = "zoom_scale"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // --- LAYER 1: BACKGROUND IMAGE ---
        // Optimized to reduce recomposition overhead
        Image(
            painter = painterResource(id = R.drawable.farm_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    Modifier.scale(scale)
                )
                .alpha(0.4f)
        )

        // --- LAYER 2: GRADIENT OVERLAY ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFDAAE73).copy(alpha = 0.6f),
                            Color(0xFF946644).copy(alpha = 0.75f),
                            Color(0xFF5C4033).copy(alpha = 0.99f)
                        )
                    )
                )
        )

        Surface(
            color = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.chicken_icon),
                    contentDescription = stringResource(R.string.app_logo_desc),
                    modifier = Modifier
                        .size(130.dp)
                        .alpha(0.8f)
                )
                Spacer(modifier = Modifier.height(70.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.login_email_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    isError = email.isNotEmpty() && !(Validators.isValidEmail(email) || Validators.isValidPhoneNumber(email)),
                    supportingText = {
                        if (email.isNotEmpty() && !(Validators.isValidEmail(email) || Validators.isValidPhoneNumber(email)))
                            Text(stringResource(R.string.login_email_error), color = MaterialTheme.colorScheme.error)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ThemeColorUtils.black(),
                        unfocusedTextColor = ThemeColorUtils.black(),
                        cursorColor = Color(0xFF2F1801),
                        focusedContainerColor = ThemeColorUtils.white(),
                        unfocusedContainerColor = ThemeColorUtils.white(),
                        focusedBorderColor = Color(0xFF2F1801),
                        unfocusedBorderColor = ThemeColorUtils.darkGray(Color.DarkGray),
                        focusedLabelColor = Color(0xFF2F1801),
                        unfocusedLabelColor = ThemeColorUtils.darkGray(Color.DarkGray),
                        errorBorderColor = Color.Red,
                        errorLabelColor = Color.Red,
                        errorContainerColor = ThemeColorUtils.white()
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.login_password_label)) },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = stringResource(R.string.password_visibility_toggle),
                                tint = ThemeColorUtils.darkGray(Color.DarkGray)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            isLoading = true
                            viewModel.login(email, password, context = context) { success, msg ->
                                isLoading = false
                                if (success) {
                                    showSuccessToast = true
                                    message = "" // Clear error message if any
                                    scope.launch {
                                        delay(600)
                                        showSuccessToast = false
                                        navController.navigate("dashboard") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                } else {
                                    message = msg
                                }
                            }
                        }
                    ),
                    isError = password.isNotEmpty() && password.length < 6,
                    supportingText = {
                        if (password.isNotEmpty() && password.length < 6)
                            Text(stringResource(R.string.login_password_error), color = MaterialTheme.colorScheme.error)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ThemeColorUtils.black(),
                        unfocusedTextColor = ThemeColorUtils.black(),
                        cursorColor = Color(0xFF2F1801),
                        focusedContainerColor = ThemeColorUtils.white(),
                        unfocusedContainerColor = ThemeColorUtils.white(),
                        focusedBorderColor = Color(0xFF2F1801),
                        unfocusedBorderColor = ThemeColorUtils.darkGray(Color.DarkGray),
                        focusedLabelColor = Color(0xFF2F1801),
                        unfocusedLabelColor = ThemeColorUtils.darkGray(Color.DarkGray),
                        errorBorderColor = Color.Red,
                        errorLabelColor = Color.Red,
                        errorContainerColor = ThemeColorUtils.white()
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = 200)),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = stringResource(R.string.login_forgot_password),
                            color = ThemeColorUtils.white(),
                            modifier = Modifier
                                .clickable { navController.navigate("reset_password") }
                                .padding(4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                ElevatedButton(
                    onClick = {
                        isLoading = true
                        viewModel.login(email, password, context = context) { success, msg ->
                            isLoading = false
                            if (success) {
                                showSuccessToast = true
                                message = ""
                                scope.launch {
                                    delay(600)
                                    showSuccessToast = false
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            } else {
                                message = msg
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFFD27D2D),
                        contentColor = ThemeColorUtils.white(),
                        disabledContainerColor = Color(0xFFD27D2D).copy(alpha = 0.10f),
                        disabledContentColor = ThemeColorUtils.white(alpha = 0.12f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ThemeColorUtils.white())
                    } else {
                        Text(stringResource(R.string.login_button))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (message.isNotEmpty()) {
                    Text(
                        text = message,
                        color = Color.Red,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.login_no_account),
                        color = ThemeColorUtils.white(),
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = stringResource(R.string.login_signup_link),
                        color = Color(0xFFD27D2D),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { navController.navigate("signup") }
                    )
                }
            }
            }

        // --- SUCCESS TOAST ---
        AnimatedVisibility(
            visible = showSuccessToast,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Login successful!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // --- SETTINGS ICON (rendered last so it's on top and clickable) ---
        // Only show if there are saved accounts
        if (hasSavedAccounts) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 42.dp, end = 20.dp)
                    .clickable { navController.navigate("manage_profiles") }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_icon_desc),
                    tint = Color(0xFF363230),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}