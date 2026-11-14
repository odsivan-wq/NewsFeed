package com.example.newsfeed.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Таблиця збережених статей. Посилання на статтю унікальне, тому воно є ключем
@Entity(tableName = "saved_articles")
data class SavedArticleEntity(
    @PrimaryKey val url: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val sourceName: String,
    val publishedAt: String,
    val savedAt: Long,
    // офлайн-копія: повний текст статті і шлях до картинки у пам'яті телефона
    val fullText: String? = null,
    val localImagePath: String? = null
)

// Перетворення статті на запис для бази
fun Article.toEntity() = SavedArticleEntity(
    url = url,
    title = title,
    description = description,
    imageUrl = imageUrl,
    sourceName = sourceName,
    publishedAt = publishedAt,
    savedAt = System.currentTimeMillis(),
    fullText = fullText,
    localImagePath = localImagePath
)

// Перетворення запису з бази назад на статтю
fun SavedArticleEntity.toArticle() = Article(
    url = url,
    title = title,
    description = description,
    imageUrl = imageUrl,
    sourceName = sourceName,
    publishedAt = publishedAt,
    fullText = fullText,
    localImagePath = localImagePath
)