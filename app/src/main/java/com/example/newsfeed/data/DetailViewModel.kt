package com.example.newsfeed.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.newsfeed.data.AppDatabase
import com.example.newsfeed.data.Article
import com.example.newsfeed.data.SavedArticlesRepository
import kotlinx.coroutines.launch

// Логіка екрана деталей: зберегти статтю або видалити зі збережених
class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository =
        SavedArticlesRepository(AppDatabase.get(application).savedArticleDao())

    fun isSaved(url: String): LiveData<Boolean> = repository.isSaved(url)

    fun toggleSaved(article: Article, currentlySaved: Boolean) {
        viewModelScope.launch {
            if (currentlySaved) repository.remove(article.url) else repository.save(article)
        }
    }
}