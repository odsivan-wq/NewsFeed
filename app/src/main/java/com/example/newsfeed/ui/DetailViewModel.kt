package com.example.newsfeed.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.newsfeed.data.Article
import com.example.newsfeed.data.SavedArticlesRepository
import kotlinx.coroutines.launch

// Логіка екрана деталей: зберегти статтю, видалити зі збережених, повторити завантаження тексту
class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SavedArticlesRepository.get(application)

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
}