package com.example.newsfeed.data

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Мови, які можна вибрати для перекладу: назва на екрані -> код мови ML Kit
object TranslationLanguages {
    val all: List<Pair<String, String>> = listOf(
        "Англійська" to TranslateLanguage.ENGLISH,
        "Українська" to TranslateLanguage.UKRAINIAN,
        "Польська" to TranslateLanguage.POLISH,
        "Німецька" to TranslateLanguage.GERMAN,
        "Французька" to TranslateLanguage.FRENCH,
        "Іспанська" to TranslateLanguage.SPANISH,
        "Італійська" to TranslateLanguage.ITALIAN,
        "Португальська" to TranslateLanguage.PORTUGUESE
    )
}

// Результат перекладу статті (повного тексту може не бути, якщо стаття не збережена офлайн)
data class TranslatedText(
    val title: String,
    val description: String,
    val fullText: String?
)

// Переклад на самому телефоні (Google ML Kit): мовна модель завантажується один раз,
// після цього переклад працює без інтернету
class ArticleTranslator {

    suspend fun translate(
        title: String,
        description: String,
        fullText: String?,
        sourceLanguage: String,
        targetLanguage: String
    ): TranslatedText {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguage)
            .setTargetLanguage(targetLanguage)
            .build()
        val translator = Translation.getClient(options)
        try {
            translator.downloadModel()
            val translatedTitle = translator.translateText(title)
            val translatedDescription =
                if (description.isBlank()) "" else translator.translateText(description)
            val translatedFullText =
                if (fullText.isNullOrBlank()) null else translateLong(translator, fullText)
            return TranslatedText(translatedTitle, translatedDescription, translatedFullText)
        } finally {
            translator.close()
        }
    }

    // довгий текст перекладаємо по абзацах
    private suspend fun translateLong(translator: Translator, text: String): String {
        val parts = ArrayList<String>()
        for (paragraph in text.split("\n\n")) {
            if (paragraph.isNotBlank()) parts.add(translator.translateText(paragraph))
        }
        return parts.joinToString("\n\n")
    }

    // завантажує мовну модель, якщо її ще немає на телефоні
    private suspend fun Translator.downloadModel() = suspendCancellableCoroutine<Unit> { cont ->
        downloadModelIfNeeded(DownloadConditions.Builder().build())
            .addOnSuccessListener { cont.resume(Unit) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    private suspend fun Translator.translateText(text: String): String =
        suspendCancellableCoroutine { cont ->
            translate(text)
                .addOnSuccessListener { result -> cont.resume(result) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
}