package com.wildwatch.app.core.model

import com.wildwatch.app.core.database.ArticleEntity
import com.wildwatch.app.core.database.ArticleTheme

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

        fun fromFirestoreDocument(documentId: String, data: Map<String, Any?>): Article = Article(
            id = documentId,
            category = data["category"] as? String ?: "News",
            theme = parseTheme(data["theme"]),
            title = data["title"] as? String ?: "",
            excerpt = data["excerpt"] as? String ?: "",
            readTime = data["readTime"] as? String ?: "3 min",
            source = data["source"] as? String ?: "Uganda Wildlife Authority",
            likes = (data["likes"] as? Number)?.toInt() ?: 0,
            comments = (data["comments"] as? Number)?.toInt() ?: 0,
            publishedAt = parsePublishedAt(data["publishedAt"]),
        )

        private fun parseTheme(raw: Any?): ArticleTheme =
            when ((raw as? String)?.lowercase()) {
                "wildlife" -> ArticleTheme.WILDLIFE
                "security" -> ArticleTheme.SECURITY
                "sunset" -> ArticleTheme.SUNSET
                "sky" -> ArticleTheme.SKY
                else -> ArticleTheme.FOREST
            }

        private fun parsePublishedAt(raw: Any?): Long {
            val iso = raw as? String ?: return System.currentTimeMillis()
            return runCatching { java.time.Instant.parse(iso).toEpochMilli() }
                .getOrDefault(System.currentTimeMillis())
        }
    }
}
