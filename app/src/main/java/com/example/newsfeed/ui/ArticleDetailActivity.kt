package com.example.newsfeed.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.newsfeed.R
import com.example.newsfeed.data.Article

class ArticleDetailActivity : AppCompatActivity() {

    private val viewModel: DetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_article_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detailRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // отримуємо статтю, яку передала стрічка
        val article = IntentCompat.getSerializableExtra(intent, EXTRA_ARTICLE, Article::class.java)
        if (article == null) {
            finish()
            return
        }

        val image = findViewById<ImageView>(R.id.image)
        val title = findViewById<TextView>(R.id.title)
        val meta = findViewById<TextView>(R.id.meta)
        val description = findViewById<TextView>(R.id.description)
        val backButton = findViewById<Button>(R.id.backButton)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val openButton = findViewById<Button>(R.id.openButton)

        title.text = article.title
        meta.text = "${article.sourceName} • ${article.publishedAt}"
        description.text = article.description.ifBlank { "Опис відсутній." }

        if (article.imageUrl != null) {
            image.visibility = View.VISIBLE
            Glide.with(image).load(article.imageUrl).into(image)
        } else {
            image.visibility = View.GONE
        }

        backButton.setOnClickListener { finish() }

        // кнопка "Зберегти" змінює напис залежно від того, чи є стаття в базі
        var currentlySaved = false
        viewModel.isSaved(article.url).observe(this) { saved ->
            currentlySaved = saved
            saveButton.text = if (saved) "Видалити зі збережених" else "Зберегти для читання офлайн"
        }
        saveButton.setOnClickListener {
            viewModel.toggleSaved(article, currentlySaved)
        }

        // відкриваємо оригінальну статтю в браузері
        openButton.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Не знайдено браузер", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val EXTRA_ARTICLE = "article"

        // зручний запуск екрана: ArticleDetailActivity.start(context, article)
        fun start(context: Context, article: Article) {
            val intent = Intent(context, ArticleDetailActivity::class.java)
            intent.putExtra(EXTRA_ARTICLE, article)
            context.startActivity(intent)
        }
    }
}