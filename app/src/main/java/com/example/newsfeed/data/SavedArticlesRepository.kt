package com.example.newsfeed.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.map

// Робота зі збереженими статтями: перетворює записи бази на Article
class SavedArticlesRepository(private val dao: SavedArticleDao) {

    // список збережених статей (працює без інтернету)
    val saved: LiveData<List<Article>> =
        dao.observeAll().map { list -> list.map { it.toArticle() } }

    fun isSaved(url: String): LiveData<Boolean> = dao.observeIsSaved(url)

    suspend fun save(article: Article) = dao.insert(article.toEntity())

    suspend fun remove(url: String) = dao.deleteByUrl(url)
}