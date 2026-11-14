package com.example.newsfeed.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.newsfeed.data.Article
import com.example.newsfeed.data.ArticleTranslator
import com.example.newsfeed.data.SavedArticlesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// Логіка екрана деталей: збереження, повторне завантаження тексту, переклад
class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SavedArticlesRepository.get(application)
    private val translator = ArticleTranslator()

    // посилання статей, текст яких зараз завантажується
    val downloading: LiveData<Set<String>> = repository.downloading

    // збережена копія статті (null, якщо не збережена)
    fun observeSaved(url: String): LiveData<Article?> = repository.observeSaved(url)

    fun toggleSaved(article: Article, currentlySaved: Boolean) {
        viewModelScope.launch {
            if (currentlySaved) repository.remove(article.url) else repository.save(article)
        }
    }

    fun retryDownload(article: Article) = repository.retryDownload(article)

    // вибір мов у списках (зберігаємо, щоб не губився при повороті екрана)
    var sourceIndex = 0 // англійська
    var targetIndex = 1 // українська

    private val _translation = MutableLiveData<TranslationState>(TranslationState.Idle)
    val translation: LiveData<TranslationState> = _translation

    fun translate(
        article: Article,
        savedCopy: Article?,
        sourceLanguage: String,
        targetLanguage: String
    ) {
        if (sourceLanguage == targetLanguage) {
            _translation.value = TranslationState.Error("Оберіть різні мови")
            return
        }
        _translation.value = TranslationState.Working
        viewModelScope.launch {
            try {
                val result = translator.translate(
                    title = article.title,
                    description = article.description,
                    fullText = savedCopy?.fullText,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage
                )
                _translation.value = TranslationState.Done(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _translation.value = TranslationState.Error(
                    "Не вдалося перекласти. Для першого перекладу цією мовою потрібен інтернет (завантаження мовної моделі)"
                )
            }
        }
    }

    fun showOriginal() {
        _translation.value = TranslationState.Idle
    }
}