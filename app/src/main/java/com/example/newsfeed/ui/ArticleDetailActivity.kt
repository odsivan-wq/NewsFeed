package com.example.newsfeed.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
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
import com.example.newsfeed.data.TranslationLanguages
import java.io.File

class ArticleDetailActivity : AppCompatActivity() {

    private val viewModel: DetailViewModel by viewModels()

    private lateinit var article: Article
    private lateinit var image: ImageView
    private lateinit var titleView: TextView
    private lateinit var descriptionView: TextView
    private lateinit var statusText: TextView
    private lateinit var fullTextView: TextView
    private lateinit var saveButton: Button
    private lateinit var retryButton: Button
    private lateinit var sourceSpinner: Spinner
    private lateinit var targetSpinner: Spinner
    private lateinit var translateButton: Button

    // що зараз відомо про статтю: збережена копія (null, якщо не збережена), завантаження, переклад
    private var savedCopy: Article? = null
    private var isDownloading = false
    private var translation: TranslationState = TranslationState.Idle
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
        titleView = findViewById(R.id.title)
        descriptionView = findViewById(R.id.description)
        statusText = findViewById(R.id.statusText)
        fullTextView = findViewById(R.id.fullText)
        saveButton = findViewById(R.id.saveButton)
        retryButton = findViewById(R.id.retryButton)
        sourceSpinner = findViewById(R.id.sourceSpinner)
        targetSpinner = findViewById(R.id.targetSpinner)
        translateButton = findViewById(R.id.translateButton)
        val meta = findViewById<TextView>(R.id.meta)
        val backButton = findViewById<Button>(R.id.backButton)
        val openButton = findViewById<Button>(R.id.openButton)

        meta.text = "${article.sourceName} • ${article.publishedAt}"

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

        // списки вибору мов: назви рідною мовою (English, Українська, Polski, Deutsch...)
        val languages = TranslationLanguages.all
        val languageNames = languages.map { it.nativeName }
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languageNames)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sourceSpinner.adapter = spinnerAdapter
        targetSpinner.adapter = spinnerAdapter
        sourceSpinner.setSelection(viewModel.sourceIndex)
        targetSpinner.setSelection(viewModel.targetIndex)
        sourceSpinner.onItemSelectedListener = onSelected { viewModel.sourceIndex = it }
        targetSpinner.onItemSelectedListener = onSelected { viewModel.targetIndex = it }

        // одна кнопка: «Перекласти» або (коли переклад готовий) «Показати оригінал»
        translateButton.setOnClickListener {
            if (translation is TranslationState.Done) {
                viewModel.showOriginal()
            } else {
                viewModel.translate(
                    article,
                    savedCopy,
                    languages[viewModel.sourceIndex],
                    languages[viewModel.targetIndex]
                )
            }
        }

        // екран сам оновлюється, коли стаття збережена, завантажився текст або готовий переклад
        viewModel.observeSaved(article.url).observe(this) { saved ->
            savedCopy = saved
            render()
        }
        viewModel.downloading.observe(this) { urls ->
            isDownloading = article.url in urls
            render()
        }
        viewModel.translation.observe(this) { state ->
            if (state is TranslationState.Error) {
                // помилку показуємо коротким повідомленням і повертаємось до оригіналу
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                viewModel.showOriginal()
                return@observe
            }
            translation = state
            render()
        }
    }

    // запам'ятовує вибраний пункт у списку мов
    private fun onSelected(action: (Int) -> Unit) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) =
            action(position)

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }

    // малюємо екран залежно від того, збережена стаття, що завантажилось і чи є переклад
    private fun render() {
        val saved = savedCopy
        val translated = (translation as? TranslationState.Done)?.result

        // заголовок і опис: переклад або оригінал
        titleView.text = translated?.title ?: article.title
        descriptionView.text = (translated?.description ?: article.description)
            .ifBlank { "Опис відсутній." }

        // повний текст: переклад (якщо є) або оригінал із бази
        val shownFullText = translated?.fullText ?: saved?.fullText

        saveButton.text =
            if (saved != null) "Видалити зі збережених" else "Зберегти для читання офлайн"
        showImage(saved)

        when {
            saved == null -> {
                statusText.visibility = View.GONE
                fullTextView.visibility = View.GONE
                retryButton.visibility = View.GONE
            }
            saved.fullText != null -> {
                statusText.visibility = View.GONE
                fullTextView.text = shownFullText
                fullTextView.visibility = View.VISIBLE
                retryButton.visibility = View.GONE
            }
            isDownloading -> {
                statusText.text = "Повний текст завантажується…"
                statusText.visibility = View.VISIBLE
                fullTextView.visibility = View.GONE
                retryButton.visibility = View.GONE
            }
            else -> {
                statusText.visibility = View.GONE
                fullTextView.visibility = View.GONE
                retryButton.visibility = View.VISIBLE
            }
        }

        // кнопка перекладу
        when (translation) {
            is TranslationState.Working -> {
                translateButton.text = "Перекладається…"
                translateButton.isEnabled = false
            }
            is TranslationState.Done -> {
                translateButton.text = "Показати оригінал"
                translateButton.isEnabled = true
            }
            else -> {
                translateButton.text = "Перекласти"
                translateButton.isEnabled = true
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