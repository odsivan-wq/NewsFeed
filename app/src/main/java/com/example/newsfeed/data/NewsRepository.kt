package com.example.newsfeed.data

class NewsRepository(private val api: NewsApi = NetworkModule.api) {

    suspend fun getNews(query: String): List<Article> =
        api.everything(query).articles.orEmpty().mapNotNull { it.toArticle() }
}