package com.example.newsfeed.ui

import com.example.newsfeed.data.TranslatedText

// Стан перекладу на екрані деталей
sealed class TranslationState {
    object Idle : TranslationState()
    object Working : TranslationState()
    data class Done(val result: TranslatedText) : TranslationState()
    data class Error(val message: String) : TranslationState()
}