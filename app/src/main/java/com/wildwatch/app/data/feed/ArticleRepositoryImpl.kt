package com.wildwatch.app.data.feed

import com.wildwatch.app.data.local.db.ArticleDao
import com.wildwatch.app.data.local.db.ArticleEntity
import com.wildwatch.app.data.local.db.ArticleTheme
import com.wildwatch.app.di.IoDispatcher
import com.wildwatch.app.domain.model.Article
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepositoryImpl @Inject constructor(
    private val articleDao: ArticleDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ArticleRepository {

    override fun observeAll(): Flow<List<Article>> =
        articleDao.observeAll()
            .onStart { seedIfEmpty() }
            .map { entities -> entities.map(Article::fromEntity) }

    private suspend fun seedIfEmpty() = withContext(ioDispatcher) {
        if (articleDao.count() > 0) return@withContext
        val now = System.currentTimeMillis()
        articleDao.insertAll(
            listOf(
                ArticleEntity(
                    id = "seed-article-1",
                    category = "Conservation",
                    theme = ArticleTheme.FOREST,
                    title = "How community sightings doubled crane counts in 2026",
                    excerpt = "Citizen reports from 14 villages helped UWA map nesting grounds with 92% accuracy...",
                    readTime = "5 min read",
                    source = "UWA News",
                    likes = 128,
                    comments = 24,
                    publishedAt = now - TimeUnit.DAYS.toMillis(1),
                ),
                ArticleEntity(
                    id = "seed-article-2",
                    category = "Safety",
                    theme = ArticleTheme.SUNSET,
                    title = "Living with elephants: 7 field-tested tips",
                    excerpt = "Practical advice from rangers in Murchison Falls on protecting crops without harming wildlife.",
                    readTime = "8 min read",
                    source = "UWA News",
                    likes = 128,
                    comments = 24,
                    publishedAt = now - TimeUnit.DAYS.toMillis(2),
                ),
                ArticleEntity(
                    id = "seed-article-3",
                    category = "Stories",
                    theme = ArticleTheme.SKY,
                    title = "The ranger who tracked a snare line for three days",
                    excerpt = "A first-person account from Bwindi Impenetrable's community patrol team.",
                    readTime = "6 min read",
                    source = "UWA News",
                    likes = 96,
                    comments = 18,
                    publishedAt = now - TimeUnit.DAYS.toMillis(4),
                ),
            ),
        )
    }
}
