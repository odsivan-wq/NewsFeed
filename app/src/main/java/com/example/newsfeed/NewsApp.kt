package com.example.newsfeed

import android.app.Application
import com.example.newsfeed.ui.ThemeSettings

class NewsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // застосовуємо збережену тему (за замовчуванням темна) до створення екранів
        ThemeSettings.apply(ThemeSettings.isDark(this))
    }
}