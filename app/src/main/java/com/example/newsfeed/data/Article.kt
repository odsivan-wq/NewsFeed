package com.example.newsfeed.data

import java.io.Serializable

data class Article(
    val url: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val sourceName: String,
    val publishedAt: String
) : Serializable