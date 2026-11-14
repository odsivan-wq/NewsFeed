package com.example.newsfeed.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// База даних застосунку (файл newsfeed.db у пам'яті телефона)
@Database(entities = [SavedArticleEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun savedArticleDao(): SavedArticleDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        // одна спільна копія бази на весь застосунок
        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "newsfeed.db"
                ).build().also { instance = it }
            }
    }
}