package com.wildwatch.app.domain.model

import com.wildwatch.app.data.local.db.ArticleEntity
import com.wildwatch.app.data.local.db.ArticleTheme

data class Article(
    val id: String,
    val category: String,
    val theme: ArticleTheme,
    val title: String,
    val excerpt: String,
    val readTime: String,
    val source: String,
    val likes: Int,
    val comments: Int,
    val publishedAt: Long,
) {
    fun toEntity(): ArticleEntity = ArticleEntity(
        id = id,
        category = category,
        theme = theme,
        title = title,
        excerpt = excerpt,
        readTime = readTime,
        source = source,
        likes = likes,
        comments = comments,
        publishedAt = publishedAt,
    )

    companion object {
        fun fromEntity(entity: ArticleEntity): Article = Article(
            id = entity.id,
            category = entity.category,
            theme = entity.theme,
            title = entity.title,
            excerpt = entity.excerpt,
            readTime = entity.readTime,
            source = entity.source,
            likes = entity.likes,
            comments = entity.comments,
            publishedAt = entity.publishedAt,
        )
    }
}
