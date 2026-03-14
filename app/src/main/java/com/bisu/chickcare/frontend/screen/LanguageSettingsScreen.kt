package com.bisu.chickcare.frontend.screen

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.viewmodels.LanguageViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

data class LanguageOption(
    val code: String,
    val name: String,
    val nativeName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val currentLanguageCode = LanguageViewModel.currentLanguage
    var selectedLanguage by remember { mutableStateOf(currentLanguageCode) }
    var searchQuery by remember { mutableStateOf("") }

    // Comprehensive list of world languages
    val languages = listOf(
        // Most Common Languages
        LanguageOption("en", "English", "English"),
        LanguageOption("es", "Spanish", "Español"),
        LanguageOption("fr", "French", "Français"),
        LanguageOption("de", "German", "Deutsch"),
        LanguageOption("it", "Italian", "Italiano"),
        LanguageOption("pt", "Portuguese", "Portuguêse"),
        LanguageOption("zh", "Chinese", "中文"),
        LanguageOption("ja", "Japanese", "日本語"),
        LanguageOption("ko", "Korean", "한국어"),
        LanguageOption("ar", "Arabic", "العربية"),
        LanguageOption("hi", "Hindi", "हिन्दी"),
        LanguageOption("ru", "Russian", "Русский"),
        LanguageOption("vi", "Vietnamese", "Tiếng Việt"),
        LanguageOption("th", "Thai", "ไทย"),
        LanguageOption("id", "Indonesian", "Bahasa Indonesia"),
        LanguageOption("ms", "Malay", "Bahasa Melayu"),
        LanguageOption("tl", "Tagalog", "Tagalog"),
        LanguageOption("tr", "Turkish", "Türkçe"),
        LanguageOption("pl", "Polish", "Polski"),
        LanguageOption("nl", "Dutch", "Nederlands"),
        LanguageOption("sv", "Swedish", "Svenska"),
        LanguageOption("da", "Danish", "Dansk"),
        LanguageOption("fi", "Finnish", "Suomi"),
        LanguageOption("no", "Norwegian", "Norsk"),
        LanguageOption("cs", "Czech", "Čeština"),
        LanguageOption("sk", "Slovak", "Slovenčina"),
        LanguageOption("ro", "Romanian", "Română"),
        LanguageOption("hu", "Hungarian", "Magyar"),
        LanguageOption("el", "Greek", "Ελληνικά"),
        LanguageOption("he", "Hebrew", "עברית"),
        LanguageOption("fa", "Persian", "فارسی"),
        LanguageOption("ur", "Urdu", "اردو"),
        LanguageOption("bn", "Bengali", "বাংলা"),
        LanguageOption("ta", "Tamil", "தமிழ்"),
        LanguageOption("te", "Telugu", "తెలుగు"),
        LanguageOption("mr", "Marathi", "मराठी"),
        LanguageOption("gu", "Gujarati", "ગુજરાતી"),
        LanguageOption("kn", "Kannada", "ಕನ್ನಡ"),
        LanguageOption("or", "Odia", "ଓଡ଼ିଆ"),
        LanguageOption("pa", "Punjabi", "ਪੰਜਾਬੀ"),
        LanguageOption("ml", "Malayalam", "മലയാളം"),
        LanguageOption("ne", "Nepali", "नेपाली"),
        LanguageOption("si", "Sinhala", "සිංහල"),
        LanguageOption("my", "Burmese", "မြန်မာ"),
        LanguageOption("km", "Khmer", "ខ្មែរ"),
        LanguageOption("lo", "Lao", "ລາວ"),
        LanguageOption("ka", "Georgian", "ქართული"),
        LanguageOption("hy", "Armenian", "հայերեն"),
        LanguageOption("az", "Azerbaijani", "Azərbaycan"),
        LanguageOption("kk", "Kazakh", "Қазақ"),
        LanguageOption("ky", "Kyrgyz", "Кыргызча"),
        LanguageOption("uz", "Uzbek", "O'zbek"),
        LanguageOption("mn", "Mongolian", "Монгол"),
        LanguageOption("be", "Belarusian", "Беларуская"),
        LanguageOption("uk", "Ukrainian", "Українська"),
        LanguageOption("bg", "Bulgarian", "Български"),
        LanguageOption("sr", "Serbian", "Српски"),
        LanguageOption("hr", "Croatian", "Hrvatski"),
        LanguageOption("bs", "Bosnian", "Bosanski"),
        LanguageOption("sl", "Slovenian", "Slovenščina"),
        LanguageOption("mk", "Macedonian", "Македонски"),
        LanguageOption("sq", "Albanian", "Shqip"),
        LanguageOption("et", "Estonian", "Eesti"),
        LanguageOption("lv", "Latvian", "Latviešu"),
        LanguageOption("lt", "Lithuanian", "Lietuvių"),
        LanguageOption("is", "Icelandic", "Íslenska"),
        LanguageOption("ga", "Irish", "Gaeilge"),
        LanguageOption("cy", "Welsh", "Cymraeg"),
        LanguageOption("mt", "Maltese", "Malti"),
        LanguageOption("eu", "Basque", "Euskara"),
        LanguageOption("ca", "Catalan", "Català"),
        LanguageOption("gl", "Galician", "Galego"),
        LanguageOption("sw", "Swahili", "Kiswahili"),
        LanguageOption("zu", "Zulu", "isiZulu"),
        LanguageOption("af", "Afrikaans", "Afrikaans"),
        LanguageOption("en", "English", "English"),
        LanguageOption("fil", "Filipino", "Filipino"),
        LanguageOption("ceb", "Cebuano", "Cebuano"),
        LanguageOption("es", "Spanish", "Español"),
        LanguageOption("fr", "French", "Français"),
        LanguageOption("zh", "Chinese", "中文")
    )

    val filteredLanguages = languages.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.nativeName.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        androidx.compose.ui.res.stringResource(R.string.language_settings_title_screen),
                        fontWeight = FontWeight.Bold
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
                    containerColor = ThemeColorUtils.white(),
                    titleContentColor = ThemeColorUtils.black()
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
                .padding(innerPadding)
        ) {
            // Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ThemeColorUtils.primary().copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = ThemeColorUtils.primary(),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.language_info_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeColorUtils.black()
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(androidx.compose.ui.res.stringResource(R.string.settings_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ThemeColorUtils.white(),
                    unfocusedContainerColor = ThemeColorUtils.white(),
                    focusedBorderColor = ThemeColorUtils.primary(),
                    unfocusedBorderColor = Color.LightGray
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Language List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLanguages) { language ->
                    LanguageItem(
                        language = language,
                        isSelected = language.name == selectedLanguage,
                        onSelect = { selectedLanguage = language.name }
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageItem(
    language: LanguageOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ThemeColorUtils.primary().copy(alpha = 0.1f)
                           else ThemeColorUtils.white()
        ),
        border = if (isSelected) BorderStroke(1.dp, ThemeColorUtils.primary()) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = language.nativeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = language.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeColorUtils.darkGray(Color.Gray)
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = ThemeColorUtils.primary()
                )
            }
        }
    }
}

private fun restartApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.let {
        it.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
        it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(it)
        if (context is android.app.Activity) {
            context.finish()
        }
    }
}