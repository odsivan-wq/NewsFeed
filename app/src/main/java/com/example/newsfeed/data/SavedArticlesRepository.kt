package com.example.newsfeed.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

// Фонова область для завантаження офлайн-копій: працює, навіть якщо користувач вийшов з екрана статті
private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

// Збережені статті: база даних + завантаження повного тексту і картинки
class SavedArticlesRepository private constructor(
    private val dao: SavedArticleDao,
    private val fetcher: ArticleContentFetcher
) {

    // список збережених статей (працює без інтернету)
    val saved: LiveData<List<Article>> =
        dao.observeAll().map { list -> list.map { it.toArticle() } }

    // посилання статей, текст яких зараз завантажується
    private val inFlight = ConcurrentHashMap.newKeySet<String>()
    private val _downloading = MutableLiveData<Set<String>>(emptySet())
    val downloading: LiveData<Set<String>> = _downloading

    // збережена копія однієї статті (null, якщо не збережена)
    fun observeSaved(url: String): LiveData<Article?> =
        dao.observeByUrl(url).map { it?.toArticle() }

    // Зберігаємо у два етапи: одразу дані з API, потім у фоні повний текст і картинка
    suspend fun save(article: Article) {
        markDownloading(article.url, true)
        dao.insert(article.copy(fullText = null, localImagePath = null).toEntity())
        backgroundScope.launch { downloadOfflineContent(article) }
    }

    // повторна спроба завантажити текст (наприклад, якщо сайт не відповів)
    fun retryDownload(article: Article) {
        markDownloading(article.url, true)
        backgroundScope.launch { downloadOfflineContent(article) }
    }

    suspend fun remove(url: String) {
        val entity = dao.getByUrl(url)
        dao.deleteByUrl(url)
        entity?.localImagePath?.let { fetcher.deleteImage(it) }
    }

    private suspend fun downloadOfflineContent(article: Article) {
        try {
            val content = withContext(Dispatchers.IO) { fetcher.fetch(article) }
            if (dao.getByUrl(article.url) != null) {
                dao.updateOfflineContent(article.url, content.fullText, content.imagePath)
            } else {
                // статтю видалили, поки йшло завантаження: прибираємо зайву картинку
                content.imagePath?.let { fetcher.deleteImage(it) }
            }
        } finally {
            markDownloading(article.url, false)
        }
    }

    private fun markDownloading(url: String, active: Boolean) {
        if (active) inFlight.add(url) else inFlight.remove(url)
        _downloading.postValue(inFlight.toSet())
    }

    companion object {
        @Volatile
        private var instance: SavedArticlesRepository? = null

        // один спільний репозиторій на весь застосунок
        fun get(context: Context): SavedArticlesRepository =
            instance ?: synchronized(this) {
                instance ?: SavedArticlesRepository(
                    AppDatabase.get(context).savedArticleDao(),
                    ArticleContentFetcher(context.applicationContext)
                ).also { instance = it }
            }
    }
}