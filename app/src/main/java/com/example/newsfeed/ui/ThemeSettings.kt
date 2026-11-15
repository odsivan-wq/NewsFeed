package com.example.newsfeed.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

// Збереження і застосування теми: темна (чорно-червона) або світла (біло-червона)
object ThemeSettings {
    private const val PREFS = "settings"
    private const val KEY_DARK = "dark_theme"

    // за замовчуванням тема темна
    fun isDark(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DARK, true)

    fun setDark(context: Context, dark: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK, dark)
            .apply()
        apply(dark)
    }

    fun apply(dark: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}