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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.bisu.chickcare.backend.data.COUNTRIES
import com.bisu.chickcare.backend.viewmodels.AuthViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils
import com.bisu.chickcare.frontend.utils.Validators
import kotlinx.coroutines.delay
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
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showCountryDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showPronounSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val customInvalidWords = remember {
        setOf(
            "pangalan",
            "apelyido",
            "telepono",
            "bahay",
            "kalye",
            "lungsod",
            "bansa",
            "halimbawa",
            "ako",
            "ikaw",
            "siya",
            "tayo",
            "kami",
            "kayo",
            "sila",
            "ito",
            "iyan",
            "iyon",
            "pagkain",
            "pagkainan"
        )
    }

    var showSuccessToast by remember { mutableStateOf(false) }

    // Optimized infinite transition to reduce recompositions
    val infiniteTransition = rememberInfiniteTransition(label = "background_zoom")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zoom_scale"
    )

    fun isNameInvalid(name: String): Boolean {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return true
        if (trimmedName.first().isLowerCase()) return true
        if (trimmedName.length > 15) return true
        if (!trimmedName.matches(Regex("^[a-zA-Z- ]+$"))) return true

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
        return providers.any {
            domain.equals(it, ignoreCase = true) || domain.endsWith(
                ".$it",
                ignoreCase = true
            )
        }
    }

    val fullContact =
        (if (selectedCountry.name == "Philippines") "+63" else selectedCountry.code) + contactInput
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = ThemeColorUtils.black(),
        unfocusedTextColor = ThemeColorUtils.black(),
        disabledTextColor = ThemeColorUtils.black(),
        cursorColor = Color(0xFF2F1801),
        focusedContainerColor = ThemeColorUtils.white(),
        unfocusedContainerColor = ThemeColorUtils.white(),
        disabledContainerColor = ThemeColorUtils.white(),
        focusedBorderColor = ThemeColorUtils.black(),
        unfocusedBorderColor = ThemeColorUtils.black(),
        disabledBorderColor = ThemeColorUtils.black(),
        focusedLabelColor = Color(0xFF2F1801),
        unfocusedLabelColor = ThemeColorUtils.darkGray(Color.DarkGray),
        disabledLabelColor = ThemeColorUtils.darkGray(Color.DarkGray),
        errorBorderColor = Color.Red,
        errorLabelColor = Color.Red,
        errorCursorColor = Color.Red,
        errorContainerColor = ThemeColorUtils.white()
    )

    val title = when (step) {
        1 -> stringResource(R.string.signup_step1_title)
        2 -> stringResource(R.string.signup_step2_title)
        3 -> stringResource(R.string.signup_step3_title)
        4 -> stringResource(R.string.signup_step4_title)
        5 -> stringResource(R.string.signup_step5_title)
        6 -> stringResource(R.string.signup_step6_title)
        else -> ""
    }
    val subtitle = when (step) {
        1 -> stringResource(R.string.signup_step1_subtitle)
        2 -> stringResource(R.string.signup_step2_subtitle)
        3 -> stringResource(R.string.signup_step3_subtitle)
        4 -> stringResource(R.string.signup_step4_subtitle)
        5 -> stringResource(R.string.signup_step5_subtitle)
        6 -> stringResource(R.string.signup_step6_subtitle)
        else -> ""
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // --- LAYER 1: BACKGROUND IMAGE ---
        // Using key to prevent unnecessary recompositions
        Image(
            painter = painterResource(id = R.drawable.farm_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    // Only apply scale transform, reducing recomposition overhead
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
                                contentDescription = stringResource(R.string.back),
                                tint = ThemeColorUtils.white()
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
                    color = ThemeColorUtils.white()
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeColorUtils.white(alpha = 0.8f),
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
                                label = { Text(stringResource(R.string.signup_first_name_label)) },
                                modifier = Modifier.weight(1f),
                                isError = isFirstNameError,
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = { lastName = it; isLastNameError = false },
                                label = { Text(stringResource(R.string.signup_last_name_label)) },
                                modifier = Modifier.weight(1f),
                                isError = isLastNameError,
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        defaultKeyboardAction(ImeAction.Done)
                                        isFirstNameError = isNameInvalid(firstName)
                                        isLastNameError = isNameInvalid(lastName)
                                        if (!isFirstNameError && !isLastNameError) {
                                            step = 2
                                        }
                                    }
                                )
                            )
                        }
                        if (isFirstNameError || isLastNameError) {
                            Text(
                                text = stringResource(R.string.signup_name_error),
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
                                contentColor = ThemeColorUtils.white(),
                                disabledContainerColor = Color(0xFFD27D2D).copy(alpha = 0.7f),
                                disabledContentColor = ThemeColorUtils.white(alpha = 0.8f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 12.dp,
                                disabledElevation = 4.dp
                            )
                        ) {
                            Text(stringResource(R.string.next))
                        }
                    }

                    2 -> {
                        OutlinedTextField(
                            value = birthDate.ifEmpty { stringResource(R.string.signup_select_date) },
                            onValueChange = { /* Read-only */ },
                            label = { Text(stringResource(R.string.signup_date_label)) },
                            modifier = Modifier
                                .fillMaxWidth(),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = stringResource(R.string.signup_select_date),
                                        tint = ThemeColorUtils.black()
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                        if (isBirthDateError) {
                            Text(
                                text = stringResource(R.string.signup_age_error),
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
                                contentColor = ThemeColorUtils.white(),
                                disabledContainerColor = Color(0xFFD27D2D).copy(alpha = 0.7f),
                                disabledContentColor = ThemeColorUtils.white(alpha = 0.8f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 12.dp,
                                disabledElevation = 4.dp
                            )
                        ) {
                            Text(stringResource(R.string.next))
                        }

                        if (showDatePicker) {
                            DatePickerDialog(
                                onDismissRequest = { showDatePicker = false },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            val selectedDate =
                                                datePickerState.selectedDateMillis?.let {
                                                    Instant.ofEpochMilli(it)
                                                        .atZone(ZoneId.systemDefault())
                                                        .toLocalDate()
                                                }
                                            if (selectedDate != null) {
                                                val formatter =
                                                    DateTimeFormatter.ofPattern("MM/dd/yyyy")
                                                birthDate = selectedDate.format(formatter)
                                                val age = calculateAge(birthDate)
                                                isBirthDateError = age < 18
                                            }
                                            showDatePicker = false
                                        }
                                    ) {
                                        Text(stringResource(R.string.ok))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDatePicker = false }) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                }
                            ) {
                                DatePicker(
                                    state = datePickerState,
                                    showModeToggle = false,
                                    title = null,
                                    headline = null
                                )
                            }
                        }
                    }

                    3 -> {
                        val moreOption = stringResource(R.string.signup_gender_more)
                        val genderOptions = listOf(
                            stringResource(R.string.signup_gender_female),
                            stringResource(R.string.signup_gender_male),
                            moreOption
                        )
                        val (selectedOption, onOptionSelected) = remember { mutableStateOf(gender) }

                        LaunchedEffect(selectedOption) {
                            if (selectedOption == moreOption) {
                                showPronounSheet = true
                            } else if (selectedOption.isNotEmpty()) {
                                pronoun = ""
                                customGender = ""
                                gender = selectedOption
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
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
                                                    unselectedColor = ThemeColorUtils.black()
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            if (text != moreOption) {
                                                Text(
                                                    text,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = ThemeColorUtils.black()
                                                )
                                            } else {
                                                Column {
                                                    Text(
                                                        text,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = ThemeColorUtils.black()
                                                    )
                                                    Text(
                                                        stringResource(R.string.signup_gender_more_desc),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = ThemeColorUtils.darkGray(Color.Gray)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (selectedOption == moreOption && pronoun.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedTextField(
                                        value = customGender,
                                        onValueChange = { customGender = it },
                                        label = { Text(stringResource(R.string.signup_gender_custom_label)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = textFieldColors,
                                        singleLine = true
                                    )
                                }

                                if (isGenderError) {
                                    Text(
                                        text = stringResource(R.string.signup_gender_error),
                                        color = Color.Red,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        val finalGender = when (selectedOption) {
                                            moreOption -> customGender.ifBlank { pronoun }
                                            else -> selectedOption
                                        }
                                        gender = finalGender
                                        isGenderError = gender.isBlank()
                                        if (!isGenderError) {
                                            step = 4
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = (selectedOption.isNotEmpty() && selectedOption != moreOption) || (selectedOption == moreOption && pronoun.isNotEmpty()),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFD27D2D),
                                        contentColor = ThemeColorUtils.white()
                                    )
                                ) {
                                    Text(stringResource(R.string.next))
                                }
                            }
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
                                    Text(
                                        stringResource(R.string.signup_pronoun_sheet_title),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        stringResource(R.string.signup_pronoun_sheet_subtitle),
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
                                    label = { Text(stringResource(R.string.signup_country_label)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showCountryDialog = true },
                                    enabled = false,
                                    trailingIcon = {
                                        IconButton(onClick = { showCountryDialog = true }) {
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = stringResource(R.string.signup_select_country),
                                                tint = ThemeColorUtils.black()
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
                                label = { Text(stringResource(R.string.signup_mobile_label)) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Done
                                ),
                                modifier = Modifier.weight(2f),
                                isError = isContactError,
                                shape = RoundedCornerShape(12.dp),
                                colors = textFieldColors,
                                singleLine = true,
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        defaultKeyboardAction(ImeAction.Done)
                                        val isValidPhilippineNumber =
                                            selectedCountry.name == "Philippines" && contactInput.length == 10 && contactInput.startsWith(
                                                "9"
                                            )
                                        val isValidOtherNumber =
                                            selectedCountry.name != "Philippines" && contactInput.isNotEmpty()
                                        isContactError =
                                            !(isValidPhilippineNumber || isValidOtherNumber)

                                        if (!isContactError) {
                                            step = 5
                                        }
                                    }
                                ),
                                leadingIcon = {
                                    Text(
                                        text = if (selectedCountry.name == "Philippines") "+63" else selectedCountry.code,
                                        modifier = Modifier.padding(start = 16.dp),
                                        color = ThemeColorUtils.black()
                                    )
                                }
                            )
                        }
                        if (isContactError) {
                            Text(
                                text = stringResource(R.string.signup_mobile_error_ph),
                                color = Color.Red,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val isValidPhilippineNumber =
                                    selectedCountry.name == "Philippines" && contactInput.length == 10 && contactInput.startsWith(
                                        "9"
                                    )
                                val isValidOtherNumber =
                                    selectedCountry.name != "Philippines" && contactInput.isNotEmpty()
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
                                contentColor = ThemeColorUtils.white()
                            )
                        ) {
                            Text(stringResource(R.string.next))
                        }
                    }

                    5 -> {
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                isEmailError = false
                            },
                            label = { Text(stringResource(R.string.signup_email_label)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    defaultKeyboardAction(ImeAction.Next)
                                    isEmailError =
                                        !(Validators.isValidEmail(email) && isValidProvider(email))
                                    if (!isEmailError) {
                                        step = 6
                                    }
                                }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            isError = isEmailError,
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            singleLine = true,
                            supportingText = {
                                if (isEmailError) {
                                    Text(
                                        stringResource(R.string.signup_email_error_provider),
                                        color = Color.Red
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                isEmailError =
                                    !(Validators.isValidEmail(email) && isValidProvider(email))
                                if (!isEmailError) {
                                    step = 6
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = email.isNotEmpty(),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD27D2D),
                                contentColor = ThemeColorUtils.white()
                            )
                        ) {
                            Text(stringResource(R.string.next))
                        }
                    }

                    6 -> {
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                isPasswordError = false
                            },
                            label = { Text(stringResource(R.string.signup_password_label)) },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = stringResource(R.string.password_visibility_toggle),
                                        tint = ThemeColorUtils.black()
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                            },
                            label = { Text(stringResource(R.string.signup_confirm_password_label)) },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    defaultKeyboardAction(ImeAction.Done)
                                    val isPasswordValid = Validators.isValidPassword(password)
                                    val isMatch = password == confirmPassword
                                    isPasswordError = !isPasswordValid || !isMatch

                                    if (!isPasswordError) {
                                        isLoading = true
                                        viewModel.signup(
                                            email,
                                            password,
                                            "$firstName $lastName",
                                            birthDate,
                                            gender,
                                            fullContact,
                                            context
                                        ) { success, msg ->
                                            isLoading = false
                                            message = msg
                                            if (success) {
                                                showSuccessToast = true
                                                message = ""
                                                scope.launch {
                                                    delay(600)
                                                    showSuccessToast = false
                                                    navController.navigate("login") {
                                                        popUpTo("signup") { inclusive = true }
                                                    }
                                                }
                                            } else {
                                                message = msg
                                            }
                                        }
                                    }
                                }
                            )
                        )

                        if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
                            Text(
                                stringResource(R.string.signup_password_mismatch),
                                color = Color.Red,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // Password validation checklist
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Validation states
                            val hasLowercase = password.any { it.isLowerCase() }
                            val hasUppercase = password.any { it.isUpperCase() }
                            val hasNumber = password.any { it.isDigit() }
                            val hasSpecialChar = password.any { !it.isLetterOrDigit() }
                            val hasMinLength = password.length >= 6

                            PasswordRequirementItem(
                                text = stringResource(R.string.password_req_lowercase),
                                isMet = hasLowercase
                            )
                            PasswordRequirementItem(
                                text = stringResource(R.string.password_req_uppercase),
                                isMet = hasUppercase
                            )
                            PasswordRequirementItem(
                                text = stringResource(R.string.password_req_number),
                                isMet = hasNumber
                            )
                            PasswordRequirementItem(
                                text = stringResource(R.string.password_req_special),
                                isMet = hasSpecialChar
                            )
                            PasswordRequirementItem(
                                text = stringResource(R.string.password_req_length),
                                isMet = hasMinLength
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val isPasswordValid = Validators.isValidPassword(password)
                                val isMatch = password == confirmPassword
                                isPasswordError = !isPasswordValid || !isMatch

                                if (!isPasswordError) {
                                    isLoading = true
                                    viewModel.signup(
                                        email,
                                        password,
                                        "$firstName $lastName",
                                        birthDate,
                                        gender,
                                        fullContact,
                                        context
                                    ) { success, msg ->
                                        isLoading = false
                                        message = msg
                                        if (success) {
                                            showSuccessToast = true
                                            message = ""
                                            scope.launch {
                                                delay(600)
                                                showSuccessToast = false
                                                navController.navigate("login") {
                                                    popUpTo("signup") { inclusive = true }
                                                }
                                            }
                                        } else {
                                            message = msg
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = password.isNotEmpty() && confirmPassword.isNotEmpty(),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD27D2D),
                                contentColor = ThemeColorUtils.white(),
                                disabledContainerColor = Color(0xFFD27D2D).copy(alpha = 0.7f),
                                disabledContentColor = ThemeColorUtils.white(alpha = 0.8f)
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
                                    color = ThemeColorUtils.white()
                                )
                            } else {
                                Text(stringResource(R.string.signup_button))
                            }
                        }

                        if (message.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = message,
                                color = Color.Red
                            )
                        }
                    }
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
                        text = "Sign up successful!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        if (showCountryDialog) {
            val searchQuery = remember { mutableStateOf("") }
            val filteredCountries = COUNTRIES.filter {
                it.name.contains(searchQuery.value, ignoreCase = true) ||
                        it.code.contains(searchQuery.value)
            }

            AlertDialog(
                onDismissRequest = { showCountryDialog = false },
                title = { Text(stringResource(R.string.signup_select_country)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = searchQuery.value,
                            onValueChange = { searchQuery.value = it },
                            label = { Text("Search") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.value.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery.value = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier
                                .height(300.dp)
                                .fillMaxWidth()
                        ) {
                            items(filteredCountries) { country ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCountry = country
                                            showCountryDialog = false
                                            // Reset contact input if switching to/from PH to re-validate length if needed,
                                            // or just let the user edit.
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${country.name} (${country.code})",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showCountryDialog = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            )
        }
    }

    @Composable
    fun PasswordRequirementItem(
        text: String,
        isMet: Boolean
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isMet) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                    contentDescription = "Requirement met",
                    tint = Color(0xFF78DE34),
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = ThemeColorUtils.darkGray(Color.Gray),
                            shape = CircleShape
                        )
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = if (isMet) Color(0xFF78DE34) else ThemeColorUtils.black(),
                fontWeight = if (isMet) FontWeight.Bold else FontWeight.Normal
            )
        }
    }

