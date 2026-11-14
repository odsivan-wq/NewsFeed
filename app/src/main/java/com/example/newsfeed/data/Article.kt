package com.example.newsfeed.data

import java.io.Serializable

data class Article(
    val url: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val sourceName: String,
    val publishedAt: String,
    // ці два поля заповнені лише для збережених статей (офлайн-копія)
    val fullText: String? = null,
    val localImagePath: String? = null
) : Serializable