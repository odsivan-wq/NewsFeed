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

    // чи збережена стаття з таким посиланням
    @Query("SELECT EXISTS(SELECT 1 FROM saved_articles WHERE url = :url)")
    fun observeIsSaved(url: String): LiveData<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(article: SavedArticleEntity)

    @Query("DELETE FROM saved_articles WHERE url = :url")
    suspend fun deleteByUrl(url: String)
}