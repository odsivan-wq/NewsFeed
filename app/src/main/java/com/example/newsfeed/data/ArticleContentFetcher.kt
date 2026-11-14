package com.example.newsfeed.data

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

// Завантажує сторінку статті (витягує основний текст) і картинку для читання офлайн.
// Методи блокуючі, тому їх викликають лише у фоновому потоці (Dispatchers.IO).
class ArticleContentFetcher(private val context: Context) {

    // окремий клієнт БЕЗ ключа NewsAPI, щоб не відправляти ключ на сторонні сайти
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .build()

    // результат завантаження: будь-яке з полів може бути null, якщо не вдалось
    data class OfflineContent(val fullText: String?, val imagePath: String?)

    fun fetch(article: Article): OfflineContent {
        val text = try {
            downloadText(article.url)
        } catch (e: Exception) {
            null
        }
        val imagePath = try {
            article.imageUrl?.let { downloadImage(article.url, it) }
        } catch (e: Exception) {
            null
        }
        return OfflineContent(text, imagePath)
    }

    fun deleteImage(path: String) {
        File(path).delete()
    }

    private fun downloadText(url: String): String? {
        val request = Request.Builder()
            .url(url.toHttps())
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val stream = response.body?.byteStream() ?: return null
            // кодування сторінки jsoup визначає сам
            val document = Jsoup.parse(stream, null, url)
            return extractText(document)
        }
    }

    // Основний текст: абзаци <p> всередині <article> (або всієї сторінки), без меню, реклами і скриптів
    private fun extractText(document: Document): String? {
        document.select("script, style, noscript, nav, header, footer, aside, form, figure, iframe").remove()
        val root = document.selectFirst("article") ?: document.body() ?: return null
        val paragraphs = root.select("p")
            .map { it.text().trim() }
            .filter { it.length >= MIN_PARAGRAPH_CHARS }
            .distinct()
        val text = paragraphs.joinToString("\n\n")
        // дуже короткий результат означає, що сайт не віддав статтю (paywall або скрипти)
        return if (text.length >= MIN_TEXT_CHARS) text.take(MAX_TEXT_CHARS) else null
    }

    private fun downloadImage(articleUrl: String, imageUrl: String): String? {
        val request = Request.Builder()
            .url(imageUrl.toHttps())
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bytes = response.body?.bytes() ?: return null
            if (bytes.isEmpty() || bytes.size > MAX_IMAGE_BYTES) return null
            val dir = File(context.filesDir, "saved_images").apply { mkdirs() }
            // ім'я файла залежить від посилання на статтю
            val file = File(dir, hash(articleUrl))
            file.writeBytes(bytes)
            return file.absolutePath
        }
    }

    private fun hash(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(32)

    // Android блокує http без шифрування, тому пробуємо https
    private fun String.toHttps(): String =
        if (startsWith("http://")) "https://" + removePrefix("http://") else this

    private companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/124.0 Mobile Safari/537.36"
        const val MIN_PARAGRAPH_CHARS = 50
        const val MIN_TEXT_CHARS = 300
        const val MAX_TEXT_CHARS = 30_000
        const val MAX_IMAGE_BYTES = 5 * 1024 * 1024
    }
}