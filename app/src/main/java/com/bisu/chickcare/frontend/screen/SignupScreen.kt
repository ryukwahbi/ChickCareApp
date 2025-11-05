package com.bisu.chickcare.frontend.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.data.COUNTRIES
import com.bisu.chickcare.backend.viewmodels.AuthViewModel
import com.bisu.chickcare.frontend.utils.Validators
import com.phucynwa.profanity.filter.AndroidProfanityFilter
import com.phucynwa.profanity.filter.dictionary.PlainDictionary
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(navController: NavController) {
    val viewModel: AuthViewModel = viewModel()
    var step by remember { mutableIntStateOf(1) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var pronoun by remember { mutableStateOf("") }
    var customGender by remember { mutableStateOf("") }
    var contactInput by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(COUNTRIES.find { it.name == "Philippines" }!!) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    var isFirstNameError by remember { mutableStateOf(false) }
    var isLastNameError by remember { mutableStateOf(false) }
    var isBirthDateError by remember { mutableStateOf(false) }
    var isGenderError by remember { mutableStateOf(false) }
    var isContactError by remember { mutableStateOf(false) }
    var isEmailError by remember { mutableStateOf(false) }
    var isPasswordError by remember { mutableStateOf(false) }

    // Date picker state
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    // Country selection dialog state
    var showCountryDialog by remember { mutableStateOf(false) }

    // Gender selection states
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showPronounSheet by remember { mutableStateOf(false) }

    // Initialize the new profanity filter
    val context = LocalContext.current
    val dictionary = remember { PlainDictionary(context, "bad_words.txt") }
    val filter = remember { AndroidProfanityFilter(dictionary) }

    val customInvalidWords = remember {
        setOf(
            // Keep common non-name words to prevent them from being used as names
            "pangalan", "apelyido", "telepono", "bahay", "kalye", "lungsod", "bansa", "halimbawa",
            "ako", "ikaw", "siya", "tayo", "kami", "kayo", "sila", "ito", "iyan", "iyon", "pagkain", "pagkainan"
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "background_zoom")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zoom_scale"
    )

    fun isNameInvalid(name: String): Boolean {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return true

        // Check if the first letter is uppercase
        if (trimmedName.first().isLowerCase()) return true

        // Check if the name exceeds 15 characters
        if (trimmedName.length > 15) return true

        // Check for allowed characters (letters, spaces, and hyphens)
        if (!trimmedName.matches(Regex("^[a-zA-Z- ]+$"))) return true

        val censored = filter.censor(trimmedName)
        if (censored != trimmedName) return true

        val words = trimmedName.split(Regex("\\s+"))
        return words.any { word -> customInvalidWords.any { it.equals(word, ignoreCase = true) } }
    }

    fun calculateAge(birthDateStr: String): Int {
        if (birthDateStr.isEmpty()) return 0
        return try {
            val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
            val birth = LocalDate.parse(birthDateStr, formatter)
            val now = LocalDate.now()
            Period.between(birth, now).years
        } catch (_: Exception) {
            0
        }
    }

    fun isValidProvider(email: String): Boolean {
        val domain = email.substringAfter('@', "")
        val providers = listOf(
            "gmail.com", "yahoo.com", "outlook.com", "hotmail.com",
            "icloud.com", "aol.com", "protonmail.com", "zoho.com",
        )
        return providers.any { domain.equals(it, ignoreCase = true) || domain.endsWith(".$it", ignoreCase = true) }
    }

    val fullContact = (if (selectedCountry.name == "Philippines") "+63" else selectedCountry.code) + contactInput

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        disabledTextColor = Color.Black,
        cursorColor = Color(0xFF2F1801),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Color.White,
        focusedBorderColor = Color.Black,
        unfocusedBorderColor = Color.Black,
        disabledBorderColor = Color.Black,
        focusedLabelColor = Color(0xFF2F1801),
        unfocusedLabelColor = Color.DarkGray,
        disabledLabelColor = Color.DarkGray,
        errorBorderColor = Color.Red,
        errorLabelColor = Color.Red,
        errorCursorColor = Color.Red,
        errorContainerColor = Color.White
    )

    val title = when (step) {
        1 -> "What's your name?"
        2 -> "What's your birth date?"
        3 -> "What's your gender?"
        4 -> "What's your contact number?"
        5 -> "What's your email?"
        6 -> "Create a password"
        else -> ""
    }
    val subtitle = when (step) {
        1 -> "Enter the name you use in real life. It cannot contain numbers, symbols, or offensive words."
        2 -> "Select your date of birth. You must be at least 18 years old above."
        3 -> "You can change who sees your gender on your profile later."
        4 -> "Enter your mobile number. We'll use this for verification."
        5 -> "Enter your email address. We recommend using Gmail, Outlook, Yahoo or any other provider."
        6 -> "Create a strong password. Include uppercase, lowercase, numbers, and symbols."
        else -> ""
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // --- LAYER 1: BACKGROUND IMAGE ---
        Image(
            painter = painterResource(id = R.drawable.farm_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
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
                            Color(0xFF946644).copy(alpha = 0.7f),
                            Color(0xFF5C4033).copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // --- LAYER 3: SIGNUP FORM ---
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (step > 1) {
                                step--
                            } else {
                                navController.popBackStack()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))

                when (step) {
                    1 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = { firstName = it; isFirstNameError = false },
                                label = { Text("First Name") },
                                modifier = Modifier.weight(1f),
                                isError = isFirstNameError,
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors
                            )
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = { lastName = it; isLastNameError = false },
                                label = { Text("Last Name") },
                                modifier = Modifier.weight(1f),
                                isError = isLastNameError,
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors
                            )
                        }
                        if (isFirstNameError || isLastNameError) {
                            Text(
                                text = "Please use your real name. Names must start with a capital letter, be 15 characters or less, and not contain numbers, symbols (except '-'), or offensive words.",
                                color = Color.Red,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = {
                                isFirstNameError = isNameInvalid(firstName)
                                isLastNameError = isNameInvalid(lastName)
                                if (!isFirstNameError && !isLastNameError) {
                                    step = 2
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = firstName.isNotEmpty() && lastName.isNotEmpty(),
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
                            Text("Next")
                        }
                    }
                    2 -> {
                        OutlinedTextField(
                            value = birthDate.ifEmpty { "Select date" },
                            onValueChange = { /* Read-only */ },
                            label = { Text("Birth Date (MM/DD/YYYY)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true },
                            enabled = false,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Select date",
                                    tint = Color.Black
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                        if (isBirthDateError) {
                            Text(
                                text = "You must be at least 18 years old.",
                                color = Color.Red,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val age = calculateAge(birthDate)
                                isBirthDateError = age < 18
                                if (birthDate.isNotEmpty() && !isBirthDateError) {
                                    step = 3
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = birthDate.isNotEmpty(),
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
                            Text("Next")
                        }

                        if (showDatePicker) {
                            DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            val selectedDate = datePickerState.selectedDateMillis?.let {
                                                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                                            }
                                            if (selectedDate != null) {
                                                val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
                                                birthDate = selectedDate.format(formatter)
                                                val age = calculateAge(birthDate)
                                                isBirthDateError = age < 18
                                            }
                                            showDatePicker = false
                                        }
                                    ) {
                                        Text("OK")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDatePicker = false }) {
                                        Text("Cancel")
                                    }
                                }
                            ) {
                                DatePicker(state = datePickerState, showModeToggle = false, title = null, headline = null)
                            }
                        }
                    }
                    3 -> {
                        val genderOptions = listOf("Female", "Male", "More options")
                        val (selectedOption, onOptionSelected) = remember { mutableStateOf(gender) }

                        LaunchedEffect(selectedOption) {
                            if (selectedOption == "More options") {
                                showPronounSheet = true
                            } else if (selectedOption.isNotEmpty()) {
                                pronoun = ""
                                customGender = ""
                                gender = selectedOption
                            }
                        }

                        Column(Modifier.fillMaxWidth()) {
                            genderOptions.forEach { text ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = (text == selectedOption),
                                            onClick = { onOptionSelected(text) },
                                            role = Role.RadioButton
                                        )
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (text == selectedOption),
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Color(0xFFD27D2D),
                                            unselectedColor = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (text != "More options") {
                                        Text(text, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                    } else {
                                        Column {
                                            Text(text, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                            Text(
                                                "Select More options to choose another gender or if you’d rather not say.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (selectedOption == "More options" && pronoun.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = customGender,
                                onValueChange = { customGender = it },
                                label = { Text("Gender (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors
                            )
                        }

                        if (showPronounSheet) {
                            ModalBottomSheet(
                                onDismissRequest = {
                                    showPronounSheet = false
                                    if (pronoun.isEmpty()) onOptionSelected("")
                                },
                                sheetState = sheetState
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Select your pronoun", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "Your pronoun is visible to everyone.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )

                                    val pronouns = mapOf(
                                        "She" to "\"Wish her a happy birthday!\"",
                                        "He" to "\"Wish him a happy birthday!\"",
                                        "They" to "\"Wish them a happy birthday!\""
                                    )

                                    pronouns.forEach { (p, ex) ->
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    pronoun = p
                                                    gender = customGender.ifBlank { p }
                                                    scope.launch { sheetState.hide() }
                                                        .invokeOnCompletion {
                                                            if (!sheetState.isVisible) {
                                                                showPronounSheet = false
                                                            }
                                                        }
                                                }
                                                .padding(vertical = 12.dp)
                                        ) {
                                            Column {
                                                Text(p, style = MaterialTheme.typography.bodyLarge)
                                                Text(ex, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (isGenderError) {
                            Text(
                                text = "Please select your gender.",
                                color = Color.Red,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val finalGender = when (selectedOption) {
                                    "More options" -> customGender.ifBlank { pronoun }
                                    else -> selectedOption
                                }
                                gender = finalGender
                                isGenderError = gender.isBlank()
                                if (!isGenderError) {
                                    step = 4
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = (selectedOption.isNotEmpty() && selectedOption != "More options") || (selectedOption == "More options" && pronoun.isNotEmpty()),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD27D2D),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Next")
                        }
                    }
                    4 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = selectedCountry.name,
                                    onValueChange = { },
                                    label = { Text("Country") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showCountryDialog = true },
                                    enabled = false,
                                    trailingIcon = {
                                        IconButton(onClick = { showCountryDialog = true }) {
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = "Select Country",
                                                tint = Color.Black
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = textFieldColors
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = contactInput,
                                onValueChange = { text ->
                                    val newText = text.filter { it.isDigit() }
                                    if (selectedCountry.name == "Philippines") {
                                        if (newText.length <= 10) {
                                            contactInput = newText
                                        }
                                    } else {
                                        contactInput = newText
                                    }
                                    isContactError = false
                                },
                                label = { Text("Mobile Number") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.weight(2f),
                                isError = isContactError,
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors,
                                leadingIcon = {
                                    Text(
                                        text = if (selectedCountry.name == "Philippines") "+63" else selectedCountry.code,
                                        modifier = Modifier.padding(start = 16.dp),
                                        color = Color.Black
                                    )
                                }
                            )
                        }
                        if (isContactError) {
                            Text(
                                text = "Invalid mobile number format for the Philippines (should be 10 digits starting with 9).",
                                color = Color.Red,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val isValidPhilippineNumber = selectedCountry.name == "Philippines" && contactInput.length == 10 && contactInput.startsWith("9")
                                val isValidOtherNumber = selectedCountry.name != "Philippines" && contactInput.isNotEmpty()
                                isContactError = !(isValidPhilippineNumber || isValidOtherNumber)

                                if (!isContactError) {
                                    step = 5
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = contactInput.isNotEmpty(),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD27D2D),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Next")
                        }
                    }
                    5 -> {
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                isEmailError = false
                            },
                            label = { Text("Email") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            isError = isEmailError,
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            supportingText = {
                                if (isEmailError) {
                                    Text("Please use a valid provider like Gmail or Yahoo.", color = Color.Red)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                isEmailError = !(Validators.isValidEmail(email) && isValidProvider(email))
                                if (!isEmailError) {
                                    step = 6
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = email.isNotEmpty(),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD27D2D),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Next")
                        }
                    }
                    6 -> {
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                isPasswordError = false
                            },
                            label = { Text("Password") },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        "Toggle password visibility"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            isError = isPasswordError && password.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            supportingText = {
                                if (password.isNotEmpty() && !viewModel.isValidSignupPassword(password)) {
                                    Text("Use 8+ characters with a mix of letters, numbers & symbols.", color = Color.Red)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                isPasswordError = password != it
                            },
                            label = { Text("Confirm Password") },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            isError = isPasswordError && confirmPassword.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            supportingText = {
                                if (isPasswordError && confirmPassword.isNotEmpty()) {
                                    Text("Passwords do not match.", color = Color.Red)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val isPasswordValid = viewModel.isValidSignupPassword(password)
                                val doPasswordsMatch = password == confirmPassword
                                isPasswordError = !isPasswordValid || !doPasswordsMatch

                                if (!isPasswordError) {
                                    isLoading = true
                                    val finalGender = if (gender == "Custom") customGender else gender
                                    viewModel.signup(
                                        email = email,
                                        password = password,
                                        fullName = "$firstName $lastName",
                                        birthDate = birthDate,
                                        gender = finalGender,
                                        contact = fullContact
                                    ) { success, msg ->
                                        isLoading = false
                                        message = msg
                                        if (success) {
                                            navController.navigate("login") {
                                                popUpTo("welcome") { inclusive = true }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = password.isNotEmpty() && confirmPassword.isNotEmpty() && !isLoading,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD27D2D),
                                contentColor = Color.White
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Text("Sign Up")
                            }
                        }
                    }
                }

                if (message.isNotEmpty() && !message.contains("successful")) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(top = 16.dp),
                        color = Color.Red
                    )
                }
            }
        }

        // Country Selection Dialog
        if (showCountryDialog) {
            AlertDialog(
                modifier = Modifier
                    .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                    .sizeIn(maxHeight = 400.dp),
                onDismissRequest = { showCountryDialog = false },
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Country",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        IconButton(
                            onClick = { showCountryDialog = false },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Black
                            )
                        }
                    }
                },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 8.dp)
                    ) {
                        items(COUNTRIES) { country ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCountry = country
                                        showCountryDialog = false
                                        contactInput =
                                            "" // Reset contact input when country changes
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = country.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {},
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}
