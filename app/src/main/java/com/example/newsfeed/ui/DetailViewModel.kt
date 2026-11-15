package com.example.newsfeed.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.newsfeed.data.Article
import com.example.newsfeed.data.SavedArticlesRepository
import com.example.newsfeed.data.TranslationLanguage
import com.example.newsfeed.data.TranslationService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// Логіка екрана деталей: збереження, повторне завантаження тексту, переклад
class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SavedArticlesRepository.get(application)
    private val translator = TranslationService()

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
        source: TranslationLanguage,
        target: TranslationLanguage
    ) {
        if (source.code == target.code) {
            _translation.value = TranslationState.Error("Оберіть різні мови")
            return
        }
        _translation.value = TranslationState.Working
        viewModelScope.launch {
            try {
                val outcome = translator.translate(
                    title = article.title,
                    description = article.description,
                    fullText = savedCopy?.fullText,
                    source = source,
                    target = target
                )
                _translation.value = TranslationState.Done(outcome.text, outcome.engine)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _translation.value = TranslationState.Error(
                    "Не вдалося перекласти. Перевір інтернет: для офлайн-перекладу цією мовою спершу потрібно завантажити мовну модель"
                )
            }
        }
    }

    fun showOriginal() {
        _translation.value = TranslationState.Idle
    }
}