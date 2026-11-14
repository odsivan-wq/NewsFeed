package com.example.newsfeed

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.newsfeed.ui.ArticleAdapter
import com.example.newsfeed.ui.ArticleDetailActivity
import com.example.newsfeed.ui.FeedState
import com.example.newsfeed.ui.FeedViewModel
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    private val viewModel: FeedViewModel by viewModels()

    // клік по картці відкриває екран деталей статті
    private val adapter = ArticleAdapter { article ->
        ArticleDetailActivity.start(this, article)
    }

    // назва вкладки -> запит до API (OR шукає статті з будь-яким із слів)
    private val categories = listOf(
        "Технології" to "technology",
        "Спорт" to "sport",
        "Бізнес" to "business",
        "Здоров'я" to "health",
        "Наука" to "science",
        "Розваги" to "entertainment",
        "Готування" to "cooking OR recipes",
        "Подорожі" to "travel",
        "Ігри" to "\"video games\""
    )

    // перша вкладка (індекс 0) — «Збережені», далі йдуть категорії
    private val isSavedTab: Boolean
        get() = viewModel.selectedIndex == 0

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var messageBox: View
    private lateinit var messageText: TextView
    private lateinit var retryButton: Button

    // завантажуємо новини з інтернету (лише для вкладок-категорій)
    private fun reload() {
        if (!isSavedTab) viewModel.load(categories[viewModel.selectedIndex - 1].second)
    }

    private fun showMessage(text: String, withRetry: Boolean) {
        messageText.text = text
        retryButton.visibility = if (withRetry) View.VISIBLE else View.GONE
        messageBox.visibility = View.VISIBLE
    }

    // малюємо екран: або збережені статті з бази, або результат запиту до API
    private fun render() {
        if (isSavedTab) {
            swipe.isEnabled = false
            swipe.isRefreshing = false
            val saved = viewModel.saved.value.orEmpty()
            adapter.submitList(saved)
            if (saved.isEmpty()) {
                showMessage("Немає збережених статей", withRetry = false)
            } else {
                messageBox.visibility = View.GONE
            }
            return
        }

        swipe.isEnabled = true
        when (val state = viewModel.state.value) {
            null -> Unit // ще нічого не завантажено
            is FeedState.Loading -> {
                swipe.isRefreshing = true
                messageBox.visibility = View.GONE
            }
            is FeedState.Success -> {
                swipe.isRefreshing = false
                adapter.submitList(state.articles)
                if (state.articles.isEmpty()) {
                    showMessage("Нічого не знайдено", withRetry = true)
                } else {
                    messageBox.visibility = View.GONE
                }
            }
            is FeedState.Error -> {
                swipe.isRefreshing = false
                adapter.submitList(emptyList())
                showMessage(state.message, withRetry = true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tabs = findViewById<TabLayout>(R.id.tabs)
        val list = findViewById<RecyclerView>(R.id.list)
        swipe = findViewById(R.id.swipe)
        messageBox = findViewById(R.id.messageBox)
        messageText = findViewById(R.id.messageText)
        retryButton = findViewById(R.id.retryButton)

        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        // вкладки: спочатку «Збережені» з іконкою, потім усі категорії
        tabs.addTab(tabs.newTab().setText("Збережені").setIcon(R.drawable.ic_bookmark))
        categories.forEach { tabs.addTab(tabs.newTab().setText(it.first)) }
        // вибираємо вкладку ДО додавання слухача, щоб не викликати зайвий запит
        tabs.getTabAt(viewModel.selectedIndex)?.select()
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.selectedIndex = tab.position
                if (isSavedTab) render() else reload()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        swipe.setOnRefreshListener { reload() }
        retryButton.setOnClickListener { reload() }

        // будь-яка зміна даних (новини або база) перемальовує екран
        viewModel.state.observe(this) { render() }
        viewModel.saved.observe(this) { render() }

        // перший запуск: завантажуємо першу категорію
        if (viewModel.state.value == null && !isSavedTab) reload()
    }
}