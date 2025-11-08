package com.bisu.chickcare.backend.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

object LocaleHelper {
    
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = getLocale(languageCode)
        Locale.setDefault(locale)
        
        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration

        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)

    }
    
    @Suppress("DEPRECATION")
    fun getLocale(languageCode: String): Locale {
        @Suppress("DUPLICATE_BRANCH_CONDITION_IN_WHEN")
        return when (languageCode) {
            "en" -> Locale.ENGLISH
            "es" -> Locale("es")
            "fr" -> Locale.FRENCH
            "de" -> Locale.GERMAN
            "it" -> Locale.ITALIAN
            "pt" -> Locale("pt")
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "ja" -> Locale.JAPANESE
            "ko" -> Locale.KOREAN
            "ar" -> Locale("ar")
            "hi" -> Locale("hi")
            "tl" -> Locale("tl")
            "ru" -> Locale("ru")
            "vi" -> Locale("vi")
            "th" -> Locale("th")
            "id" -> Locale("id")
            "ms" -> Locale("ms")
            "tr" -> Locale("tr")
            "pl" -> Locale("pl")
            "nl" -> Locale("nl")
            "sv" -> Locale("sv")
            "da" -> Locale("da")
            "fi" -> Locale("fi")
            "no" -> Locale("no")
            "cs" -> Locale("cs")
            "sk" -> Locale("sk")
            "ro" -> Locale("ro")
            "hu" -> Locale("hu")
            "el" -> Locale("el")
            "he" -> Locale("he")
            "fa" -> Locale("fa")
            "ur" -> Locale("ur")
            "bn" -> Locale("bn")
            "ta" -> Locale("ta")
            "te" -> Locale("te")
            "mr" -> Locale("mr")
            "gu" -> Locale("gu")
            "kn" -> Locale("kn")
            "or" -> Locale("or")
            "pa" -> Locale("pa")
            "ml" -> Locale("ml")
            "ne" -> Locale("ne")
            "si" -> Locale("si")
            "my" -> Locale("my")
            "km" -> Locale("km")
            "lo" -> Locale("lo")
            "ka" -> Locale("ka")
            "hy" -> Locale("hy")
            "az" -> Locale("az")
            "kk" -> Locale("kk")
            "ky" -> Locale("ky")
            "uz" -> Locale("uz")
            "mn" -> Locale("mn")
            "be" -> Locale("be")
            "uk" -> Locale("uk")
            "bg" -> Locale("bg")
            "sr" -> Locale("sr")
            "hr" -> Locale("hr")
            "bs" -> Locale("bs")
            "sl" -> Locale("sl")
            "mk" -> Locale("mk")
            "sq" -> Locale("sq")
            "et" -> Locale("et")
            "lv" -> Locale("lv")
            "lt" -> Locale("lt")
            "is" -> Locale("is")
            "ga" -> Locale("ga")
            "cy" -> Locale("cy")
            "mt" -> Locale("mt")
            "eu" -> Locale("eu")
            "ca" -> Locale("ca")
            "gl" -> Locale("gl")
            "sw" -> Locale("sw")
            "zu" -> Locale("zu")
            "af" -> Locale("af")
            "xh" -> Locale("xh")
            "am" -> Locale("am")
            "ha" -> Locale("ha")
            "yo" -> Locale("yo")
            "ig" -> Locale("ig")
            "so" -> Locale("so")
            "rw" -> Locale("rw")
            "sn" -> Locale("sn")
            "st" -> Locale("st")
            "tn" -> Locale("tn")
            "ve" -> Locale("ve")
            "ts" -> Locale("ts")
            "ss" -> Locale("ss")
            "nr" -> Locale("nr")
            "nso" -> Locale("nso")
            "zu" -> Locale("zu")
            "xh" -> Locale("xh")
            "af" -> Locale("af")
            else -> Locale.ENGLISH
        }
    }
    
    fun attachBaseContext(context: Context): Context {
        val languageCode = getStoredLanguageCode(context)
        return setLocale(context, languageCode)
    }
    
    fun getStoredLanguageCode(context: Context): String {
        val prefs = context.getSharedPreferences("ChickCarePrefs", Context.MODE_PRIVATE)
        return prefs.getString("selected_language", Locale.getDefault().language) ?: "en"
    }
}

