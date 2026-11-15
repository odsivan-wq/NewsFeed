package com.example.newsfeed.ui

import com.example.newsfeed.data.TranslatedText
import com.example.newsfeed.data.TranslationEngine

// Стан перекладу на екрані деталей
sealed class TranslationState {
    object Idle : TranslationState()
    object Working : TranslationState()
    data class Done(val result: TranslatedText, val engine: TranslationEngine) : TranslationState()
    data class Error(val message: String) : TranslationState()
}