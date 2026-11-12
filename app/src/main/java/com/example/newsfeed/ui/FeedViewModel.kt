package com.example.newsfeed.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsfeed.data.NewsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class FeedViewModel : ViewModel() {

    private val repository = NewsRepository()
    private val _state = MutableLiveData<FeedState>()
    val state: LiveData<FeedState> = _state

    // вибрана вкладка (щоб не губилась при повороті екрана)
    var selectedIndex = 0

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
                _state.value = FeedState.Error("Немає з'єднання з інтернетом")
            }
        }
    }
}