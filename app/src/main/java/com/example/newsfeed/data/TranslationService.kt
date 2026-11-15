package com.example.newsfeed.data

import com.example.newsfeed.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

// Яким способом виконано переклад
enum class TranslationEngine { ONLINE, OFFLINE }

data class TranslationOutcome(val text: TranslatedText, val engine: TranslationEngine)

// Переклад статті: спочатку онлайн через Gemini (точніше), якщо не вийшло — офлайн на телефоні
class TranslationService(apiKey: String = BuildConfig.GEMINI_API_KEY) {

    private val online = GeminiTranslator(apiKey)
    private val offline = ArticleTranslator()

    suspend fun translate(
        title: String,
        description: String,
        fullText: String?,
        source: TranslationLanguage,
        target: TranslationLanguage
    ): TranslationOutcome {
        if (online.isConfigured) {
            try {
                val result = withContext(Dispatchers.IO) {
                    online.translate(title, description, fullText, source, target)
                }
                return TranslationOutcome(result, TranslationEngine.ONLINE)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // онлайн-переклад не вдався (немає інтернету, ліміт, помилка) — пробуємо офлайн
            }
        }
        val result = offline.translate(title, description, fullText, source.code, target.code)
        return TranslationOutcome(result, TranslationEngine.OFFLINE)
    }
}

// Помилка HTTP від сервера Gemini
private class HttpFailure(val code: Int) : IOException("HTTP $code")

// Переклад через Gemini API (безкоштовний ключ із Google AI Studio)
class GeminiTranslator(private val apiKey: String) {

    val isConfigured: Boolean
        get() = apiKey.isNotBlank()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    // Блокуючий метод: викликати лише у фоновому потоці (Dispatchers.IO)
    fun translate(
        title: String,
        description: String,
        fullText: String?,
        source: TranslationLanguage,
        target: TranslationLanguage
    ): TranslatedText {
        // сегменти для перекладу: 0 — заголовок, 1 — опис, далі абзаци повного тексту
        val segments = ArrayList<String>()
        segments.add(title)
        segments.add(description)
        fullText?.split("\n\n")?.filter { it.isNotBlank() }?.let { segments.addAll(it) }

        val translated = Array(segments.size) { "" }
        val toTranslate = segments.indices.filter { segments[it].isNotBlank() }
        for (batch in makeBatches(toTranslate, segments)) {
            val result = translateBatch(batch, segments, source, target)
            for ((index, text) in result) translated[index] = text
        }

        val hasFullText = segments.size > 2
        return TranslatedText(
            title = translated[0],
            description = translated[1],
            fullText = if (hasFullText) {
                translated.drop(2).filter { it.isNotBlank() }.joinToString("\n\n")
            } else {
                null
            }
        )
    }

    // ділимо сегменти на порції, щоб кожен запит був не надто великим
    private fun makeBatches(indices: List<Int>, segments: List<String>): List<List<Int>> {
        val batches = ArrayList<List<Int>>()
        var current = ArrayList<Int>()
        var size = 0
        for (i in indices) {
            val length = segments[i].length
            if (current.isNotEmpty() && size + length > MAX_BATCH_CHARS) {
                batches.add(current)
                current = ArrayList()
                size = 0
            }
            current.add(i)
            size += length
        }
        if (current.isNotEmpty()) batches.add(current)
        return batches
    }

    private fun translateBatch(
        batch: List<Int>,
        segments: List<String>,
        source: TranslationLanguage,
        target: TranslationLanguage
    ): Map<Int, String> {
        val prompt = StringBuilder()
        for (i in batch) {
            prompt.append("[[").append(i).append("]]\n").append(segments[i]).append("\n\n")
        }
        val system = "You are a professional news translator. Translate the text from " +
                "${source.englishName} to ${target.englishName}. Preserve the meaning, tone, names, " +
                "numbers, dates and quotes exactly. Use natural, grammatically correct " +
                "${target.englishName}. Do not summarize, shorten, explain or add anything. " +
                "The input consists of segments marked like [[0]], [[1]]. Return every segment " +
                "translated, keeping each marker exactly as in the input on its own line before the " +
                "translation. Output nothing except the marked segments."

        var lastError: Exception = IOException("Немає доступних моделей")
        for (model in MODELS) {
            try {
                val answer = callModel(model, system, prompt.toString())
                return parseSegments(answer, batch)
            } catch (e: HttpFailure) {
                lastError = e
                // 404 (немає моделі), 429 (ліміт), помилки сервера: пробуємо наступну модель
                if (e.code != 404 && e.code != 429 && e.code < 500) throw e
            } catch (e: IOException) {
                lastError = e
            }
        }
        throw lastError
    }

    private fun callModel(model: String, systemPrompt: String, userPrompt: String): String {
        val body = JSONObject()
            .put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            )
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
                )
            )
            .toString()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
            .header("x-goog-api-key", apiKey)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw HttpFailure(response.code)
            try {
                return extractText(text)
            } catch (e: JSONException) {
                throw IOException("Неправильна відповідь сервера", e)
            }
        }
    }

    // збираємо текст відповіді (службові «думки» моделі пропускаємо)
    private fun extractText(responseJson: String): String {
        val candidate = JSONObject(responseJson).getJSONArray("candidates").getJSONObject(0)
        val parts = candidate.getJSONObject("content").getJSONArray("parts")
        val result = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (!part.optBoolean("thought", false)) result.append(part.optString("text", ""))
        }
        return result.toString()
    }

    // розбираємо відповідь за маркерами [[номер]]
    private fun parseSegments(answer: String, expected: List<Int>): Map<Int, String> {
        val result = HashMap<Int, String>()
        for (match in MARKER.findAll(answer)) {
            result[match.groupValues[1].toInt()] = match.groupValues[2].trim()
        }
        if (!result.keys.containsAll(expected)) {
            throw IOException("Модель повернула не всі фрагменти")
        }
        return result
    }

    private companion object {
        // моделі пробуємо по черзі; якщо назви застаріли, онлайн-переклад не спрацює
        // і застосунок використає офлайн-переклад. Актуальні назви дивись у Google AI Studio
        val MODELS = listOf("gemini-3.6-flash", "gemini-3.5-flash-lite")
        const val MAX_BATCH_CHARS = 4000
        val MARKER = Regex("\\[\\[(\\d+)\\]\\]\\s*([\\s\\S]*?)(?=\\[\\[\\d+\\]\\]|\\z)")
    }
}