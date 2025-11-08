package com.bisu.chickcare.frontend.screen

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bisu.chickcare.R
import com.bisu.chickcare.backend.viewmodels.LanguageViewModel
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
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
        LanguageOption("xh", "Xhosa", "isiXhosa"),
        LanguageOption("am", "Amharic", "አማርኛ"),
        LanguageOption("ha", "Hausa", "Hausa"),
        LanguageOption("yo", "Yoruba", "Yorùbá"),
        LanguageOption("ig", "Igbo", "Asụsụ Igbo"),
        LanguageOption("so", "Somali", "Soomaali"),
        LanguageOption("rw", "Kinyarwanda", "Ikinyarwanda"),
        LanguageOption("sn", "Shona", "ChiShona"),
        LanguageOption("st", "Sesotho", "Sesotho"),
        LanguageOption("tn", "Tswana", "Setswana"),
        LanguageOption("ve", "Venda", "Tshivenda"),
        LanguageOption("ts", "Tsonga", "Xitsonga"),
        LanguageOption("ss", "Swati", "SiSwati"),
        LanguageOption("nr", "Ndebele", "isiNdebele"),
        LanguageOption("nso", "Northern Sotho", "Sesotho sa Leboa")
    )

    // Update selected language when current language changes
    LaunchedEffect(currentLanguageCode) {
        selectedLanguage = currentLanguageCode
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Language",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ThemeColorUtils.darkGray(Color(0xFF231C16))
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFFFFF),
                    titleContentColor = ThemeColorUtils.darkGray(Color(0xFF231C16))
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ThemeColorUtils.beige(Color(0xFFFFF7E6)))
        ) {
            HorizontalDivider(
                color = ThemeColorUtils.lightGray(Color(0xFF7E7C7C)),
                thickness = 1.dp
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFD9D5D0)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_language_info_flaticon),
                                contentDescription = null,
                                modifier = Modifier.size(38.dp),
                                colorFilter = if (ThemeViewModel.isDarkMode) {
                                    ColorFilter.tint(
                                        color = ThemeColorUtils.lightGray(Color(0xFFA1AAB2)),
                                        blendMode = BlendMode.SrcAtop
                                    )
                                } else {
                                    null
                                }
                            )
                            Text(
                                text = "Select your preferred language for the app. The app will restart to apply changes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                            )
                        }
                    }
                }

                items(languages) { language ->
                    LanguageOptionCard(
                        language = language,
                        isSelected = selectedLanguage == language.code,
                        onClick = {
                            if (selectedLanguage != language.code) {
                                selectedLanguage = language.code
                                LanguageViewModel.setLanguage(context, language.code)
                                restartApp(context)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageOptionCard(
    language: LanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .indication(interactionSource, ripple(bounded = true))
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ThemeColorUtils.white() else Color(0xFFE5E2DE)
        ),
        shape = shape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 8.dp else 5.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 6.dp,
            focusedElevation = 6.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = language.nativeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ThemeColorUtils.darkGray(Color(0xFF231C16))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = language.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeColorUtils.lightGray(Color(0xFF666666))
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFF232221),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun restartApp(context: Context) {
    val intent = (context as? android.app.Activity)?.packageManager?.getLaunchIntentForPackage(
        context.packageName
    )
    intent?.let {
        it.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
        it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(it)
        context.finish()
    }
}