package com.example.newsfeed.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.newsfeed.data.Article
import com.example.newsfeed.data.NewsRepository
import com.example.newsfeed.data.SavedArticlesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NewsRepository()
    private val savedRepository = SavedArticlesRepository.get(application)

    private val _state = MutableLiveData<FeedState>()
    val state: LiveData<FeedState> = _state

    // збережені статті з бази (доступні без інтернету)
    val saved: LiveData<List<Article>> = savedRepository.saved

    // вибрана вкладка: 0 — «Збережені», далі категорії (за замовчуванням перша категорія)
    var selectedIndex = 1

    private var job: Job? = null

    fun load(query: String) {
        job?.cancel() // скасовуємо попередній запит, якщо він ще йде
        _state.value = FeedState.Loading
        job = viewModelScope.launch {
            try {
                _state.value = FeedState.Success(repository.getNews(query))
            } catch (e: HttpException) {
                _state.value = FeedState.Error(
                    when (e.code()) {
                        401 -> "Неправильний ключ API"
                        429 -> "Вичерпано ліміт запитів на сьогодні"
                        else -> "Помилка сервера (${e.code()})"
                    }
                )
            } catch (e: IOException) {
                _state.value = FeedState.Error(
                    "Немає з'єднання з інтернетом. Збережені статті доступні у вкладці «Збережені»"
                )
            }
        }
    }
}