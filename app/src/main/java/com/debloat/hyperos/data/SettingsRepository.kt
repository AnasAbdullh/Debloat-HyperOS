package com.debloat.hyperos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings_pref")

enum class AppLanguage {
    SYSTEM, ARABIC, ENGLISH
}

class SettingsRepository(private val context: Context) {

    private val THEME_KEY = stringPreferencesKey("app_theme_mode")
    private val LANG_KEY = stringPreferencesKey("app_language")

    val themeOption: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_KEY] ?: "SYSTEM"
    }

    val language: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        when (prefs[LANG_KEY]) {
            AppLanguage.ARABIC.name -> AppLanguage.ARABIC
            AppLanguage.ENGLISH.name -> AppLanguage.ENGLISH
            else -> AppLanguage.SYSTEM
        }
    }

    suspend fun setThemeMode(themeName: String) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = themeName
        }
    }

    suspend fun setLanguage(lang: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[LANG_KEY] = lang.name
        }
    }
}