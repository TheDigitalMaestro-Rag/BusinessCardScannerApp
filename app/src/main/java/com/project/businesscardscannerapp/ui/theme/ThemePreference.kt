package com.project.businesscardscannerapp.ui.theme

import android.content.Context
import android.content.SharedPreferences

class ThemePreference(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveTheme(isDarkTheme: Boolean) {
        preferences.edit().putBoolean(KEY_DARK_THEME, isDarkTheme).apply()
    }

    fun loadTheme(): Boolean {
        // Default to false (light theme) if the preference hasn't been set yet
        return preferences.getBoolean(KEY_DARK_THEME, false)
    }

    companion object {
        private const val PREFS_NAME = "theme_prefs"
        private const val KEY_DARK_THEME = "is_dark_theme"
    }
}
