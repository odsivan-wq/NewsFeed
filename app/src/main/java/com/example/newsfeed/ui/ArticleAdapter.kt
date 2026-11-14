package com.example.newsfeed.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.newsfeed.R
import com.example.newsfeed.data.Article
import java.io.File

private object ArticleDiff : DiffUtil.ItemCallback<Article>() {
    override fun areItemsTheSame(a: Article, b: Article) = a.url == b.url
    override fun areContentsTheSame(a: Article, b: Article) = a == b
}

class ArticleAdapter(
    private val onClick: (Article) -> Unit
) : ListAdapter<Article, ArticleAdapter.VH>(ArticleDiff) {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.image)
        val title: TextView = view.findViewById(R.id.title)
        val meta: TextView = view.findViewById(R.id.meta)
        val description: TextView = view.findViewById(R.id.description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_article, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val article = getItem(position)
        holder.title.text = article.title
        holder.meta.text = "${article.sourceName} • ${article.publishedAt}"
        holder.description.text = article.description

        // для збережених статей беремо картинку з пам'яті телефона, інакше з інтернету
        val localImage = article.localImagePath?.let { File(it) }?.takeIf { it.exists() }
        val imageSource: Any? = localImage ?: article.imageUrl
        if (imageSource != null) {
            holder.image.visibility = View.VISIBLE
            Glide.with(holder.image).load(imageSource).into(holder.image)
        } else {
            holder.image.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(article) }
    }
}