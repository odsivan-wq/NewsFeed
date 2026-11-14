package com.example.newsfeed.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

// Запити до таблиці збережених статей
@Dao
interface SavedArticleDao {

    // усі збережені статті, найновіші зверху; список оновлюється сам при змінах у базі
    @Query("SELECT * FROM saved_articles ORDER BY savedAt DESC")
    fun observeAll(): LiveData<List<SavedArticleEntity>>

    // збережена копія однієї статті (null, якщо статті немає в базі)
    @Query("SELECT * FROM saved_articles WHERE url = :url")
    fun observeByUrl(url: String): LiveData<SavedArticleEntity?>

    @Query("SELECT * FROM saved_articles WHERE url = :url")
    suspend fun getByUrl(url: String): SavedArticleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(article: SavedArticleEntity)

    // дописуємо офлайн-копію; COALESCE лишає старе значення, якщо нове не завантажилось
    @Query(
        "UPDATE saved_articles " +
                "SET fullText = COALESCE(:fullText, fullText), " +
                "localImagePath = COALESCE(:localImagePath, localImagePath) " +
                "WHERE url = :url"
    )
    suspend fun updateOfflineContent(url: String, fullText: String?, localImagePath: String?)

    @Query("DELETE FROM saved_articles WHERE url = :url")
    suspend fun deleteByUrl(url: String)
}