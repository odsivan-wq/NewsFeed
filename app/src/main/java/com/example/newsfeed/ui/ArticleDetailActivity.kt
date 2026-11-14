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
import java.io.File

class ArticleDetailActivity : AppCompatActivity() {

    private val viewModel: DetailViewModel by viewModels()

    private lateinit var article: Article
    private lateinit var image: ImageView
    private lateinit var statusText: TextView
    private lateinit var fullTextView: TextView
    private lateinit var hint: TextView
    private lateinit var saveButton: Button
    private lateinit var retryButton: Button

    // що зараз відомо про статтю: збережена копія (null, якщо не збережена) і чи йде завантаження
    private var savedCopy: Article? = null
    private var isDownloading = false
    private var shownImage: Any? = null

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
        val passed = IntentCompat.getSerializableExtra(intent, EXTRA_ARTICLE, Article::class.java)
        if (passed == null) {
            finish()
            return
        }
        article = passed

        image = findViewById(R.id.image)
        statusText = findViewById(R.id.statusText)
        fullTextView = findViewById(R.id.fullText)
        hint = findViewById(R.id.hint)
        saveButton = findViewById(R.id.saveButton)
        retryButton = findViewById(R.id.retryButton)
        val title = findViewById<TextView>(R.id.title)
        val meta = findViewById<TextView>(R.id.meta)
        val description = findViewById<TextView>(R.id.description)
        val backButton = findViewById<Button>(R.id.backButton)
        val openButton = findViewById<Button>(R.id.openButton)

        title.text = article.title
        meta.text = "${article.sourceName} • ${article.publishedAt}"
        description.text = article.description.ifBlank { "Опис відсутній." }

        backButton.setOnClickListener { finish() }
        saveButton.setOnClickListener { viewModel.toggleSaved(article, savedCopy != null) }
        retryButton.setOnClickListener { viewModel.retryDownload(article) }

        // відкриваємо оригінальну статтю в браузері
        openButton.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Не знайдено браузер", Toast.LENGTH_SHORT).show()
            }
        }

        // екран сам оновлюється, коли стаття збережена або завантажився текст
        viewModel.observeSaved(article.url).observe(this) { saved ->
            savedCopy = saved
            render()
        }
        viewModel.downloading.observe(this) { urls ->
            isDownloading = article.url in urls
            render()
        }
    }

    // малюємо екран залежно від того, чи збережена стаття і що завантажилось
    private fun render() {
        val saved = savedCopy
        saveButton.text =
            if (saved != null) "Видалити зі збережених" else "Зберегти для читання офлайн"
        showImage(saved)

        when {
            saved == null -> {
                hint.visibility = View.VISIBLE
                statusText.visibility = View.GONE
                fullTextView.visibility = View.GONE
                retryButton.visibility = View.GONE
            }
            saved.fullText != null -> {
                hint.visibility = View.GONE
                statusText.text = "Повний текст збережено, він доступний без інтернету"
                statusText.visibility = View.VISIBLE
                fullTextView.text = saved.fullText
                fullTextView.visibility = View.VISIBLE
                retryButton.visibility = View.GONE
            }
            isDownloading -> {
                hint.visibility = View.GONE
                statusText.text = "Завантажую повний текст для офлайн…"
                statusText.visibility = View.VISIBLE
                fullTextView.visibility = View.GONE
                retryButton.visibility = View.GONE
            }
            else -> {
                hint.visibility = View.GONE
                statusText.text =
                    "Повний текст завантажити не вдалося (сайт не віддав текст). Збережено короткий опис."
                statusText.visibility = View.VISIBLE
                fullTextView.visibility = View.GONE
                retryButton.visibility = View.VISIBLE
            }
        }
    }

    // картинка: локальна копія (якщо є), інакше з інтернету; перезавантажуємо лише при зміні джерела
    private fun showImage(saved: Article?) {
        val local = saved?.localImagePath?.let { File(it) }?.takeIf { it.exists() }
        val source: Any? = local ?: article.imageUrl
        if (source == null) {
            image.visibility = View.GONE
            shownImage = null
            return
        }
        image.visibility = View.VISIBLE
        if (source != shownImage) {
            shownImage = source
            Glide.with(image).load(source).into(image)
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