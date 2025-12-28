package com.madeinbraza.app.util

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.madeinbraza.app.di.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

object LanguageManager {
    private val LANGUAGE_KEY = stringPreferencesKey("selected_language")

    val languages = listOf(
        Language("pt", "BR", "Português (BR)"),
        Language("en", "", "English"),
        Language("es", "", "Español")
    )

    fun getLanguageFlow(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[LANGUAGE_KEY] ?: "pt"
        }
    }

    suspend fun setLanguage(context: Context, languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = languageCode
        }
    }

    fun applyLanguage(context: Context, languageCode: String): Context {
        val locale = when (languageCode) {
            "pt" -> Locale("pt", "BR")
            "en" -> Locale("en")
            "es" -> Locale("es")
            else -> Locale("pt", "BR")
        }

        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    fun getLanguageCodeFromDisplayName(displayName: String): String {
        return languages.find { it.displayName == displayName }?.code ?: "pt"
    }

    fun getDisplayNameFromCode(code: String): String {
        return languages.find { it.code == code }?.displayName ?: "Português (BR)"
    }
}

data class Language(
    val code: String,
    val country: String,
    val displayName: String
)
