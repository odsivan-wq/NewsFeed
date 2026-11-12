package com.example.newsfeed.ui

import com.example.newsfeed.data.Article

sealed class FeedState {
    object Loading : FeedState()
    data class Success(val articles: List<Article>) : FeedState()
    data class Error(val message: String) : FeedState()
}