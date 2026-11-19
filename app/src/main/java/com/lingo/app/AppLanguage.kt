package com.lingo.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

object AppLanguage {
    var preference by mutableStateOf("auto")
        private set
    var systemCode by mutableStateOf(Locale.getDefault().language)
        private set

    val isEnglish: Boolean
        get() = when (preference) {
            "ru" -> false
            "en" -> true
            else -> systemCode != "ru"
        }

    fun changePreference(value: String) {
        preference = value
    }

    fun updateSystemLanguage(languageCode: String) {
        systemCode = languageCode.lowercase(Locale.ROOT)
    }

    fun text(russian: String, english: String): String =
        if (isEnglish) english else russian
}

fun ui(russian: String, english: String): String = AppLanguage.text(russian, english)
