package com.wildwatch.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// Editorial content (UWA News etc.) - authored outside this app, same as
// AlertEntity. Seeded locally on first read; see ArticleRepositoryImpl.
@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val id: String,
    val category: String,
    val theme: ArticleTheme,
    val title: String,
    val excerpt: String,
    val readTime: String,
    val source: String,
    val likes: Int,
    val comments: Int,
    val publishedAt: Long,
)
