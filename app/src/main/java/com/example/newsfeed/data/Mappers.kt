package com.example.newsfeed.data

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun ArticleDto.toArticle(): Article? {
    val url = url ?: return null
    val title = title ?: return null
    if (title == "[Removed]") return null
    return Article(
        url = url,
        title = title,
        description = description.orEmpty(),
        imageUrl = urlToImage,
        sourceName = source?.name.orEmpty(),
        publishedAt = formatDate(publishedAt)
    )
}

private fun formatDate(iso: String?): String {
    if (iso == null) return ""
    return try {
        OffsetDateTime.parse(iso)
            .atZoneSameInstant(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    } catch (e: Exception) {
        ""
    }
}