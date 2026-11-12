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

    // назва вкладки -> запит до API
    private val categories = listOf(
        "Технології" to "technology",
        "Спорт" to "sport",
        "Бізнес" to "business",
        "Здоров'я" to "health",
        "Наука" to "science",
        "Розваги" to "entertainment"
    )

    private fun reload() = viewModel.load(categories[viewModel.selectedIndex].second)

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
        val swipe = findViewById<SwipeRefreshLayout>(R.id.swipe)
        val list = findViewById<RecyclerView>(R.id.list)
        val messageBox = findViewById<View>(R.id.messageBox)
        val messageText = findViewById<TextView>(R.id.messageText)
        val retryButton = findViewById<Button>(R.id.retryButton)

        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        categories.forEach { tabs.addTab(tabs.newTab().setText(it.first)) }
        // вибираємо вкладку ДО додавання слухача, щоб не викликати зайвий запит
        tabs.getTabAt(viewModel.selectedIndex)?.select()
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.selectedIndex = tab.position
                reload()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        swipe.setOnRefreshListener { reload() }
        retryButton.setOnClickListener { reload() }

        viewModel.state.observe(this) { state ->
            when (state) {
                is FeedState.Loading -> {
                    swipe.isRefreshing = true
                    messageBox.visibility = View.GONE
                }
                is FeedState.Success -> {
                    swipe.isRefreshing = false
                    adapter.submitList(state.articles)
                    if (state.articles.isEmpty()) {
                        messageText.text = "Нічого не знайдено"
                        messageBox.visibility = View.VISIBLE
                    } else {
                        messageBox.visibility = View.GONE
                    }
                }
                is FeedState.Error -> {
                    swipe.isRefreshing = false
                    adapter.submitList(emptyList())
                    messageText.text = state.message
                    messageBox.visibility = View.VISIBLE
                }
            }
        }

        // перший запуск: завантажуємо першу категорію
        if (viewModel.state.value == null) reload()
    }
}