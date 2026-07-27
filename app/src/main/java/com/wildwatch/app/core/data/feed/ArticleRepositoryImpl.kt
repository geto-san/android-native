package com.wildwatch.app.core.data.feed

import com.wildwatch.app.core.database.ArticleDao
import com.wildwatch.app.core.database.ArticleEntity
import com.wildwatch.app.core.database.ArticleTheme
import com.wildwatch.app.core.di.IoDispatcher
import com.wildwatch.app.core.model.Article
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
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

    override fun observeById(id: String): Flow<Article?> =
        articleDao.observeById(id)
            .map { it?.let(Article::fromEntity) }

    private suspend fun seedIfEmpty() = withContext(ioDispatcher) {
        if (articleDao.count() > 0) return@withContext
        val now = System.currentTimeMillis()
        articleDao.insertAll(
            listOf(
                ArticleEntity(
                    id = "seed-art-1",
                    category = "UWA News",
                    theme = ArticleTheme.FOREST,
                    title = "New Buffer Zone Rules",
                    excerpt = "Updated grazing regulations for Bwindi buffer zones taking effect next Monday. Community members are advised to register their livestock at the nearest gate station.",
                    readTime = "4 min",
                    source = "Uganda Wildlife Authority",
                    likes = 24,
                    comments = 3,
                    publishedAt = now
                ),
                ArticleEntity(
                    id = "seed-art-2",
                    category = "Conservation",
                    theme = ArticleTheme.WILDLIFE,
                    title = "Gorilla Population Surge",
                    excerpt = "Recent census shows a 15% increase in the mountain gorilla population within Bwindi Impenetrable Forest. This marks a major victory for community-led conservation efforts.",
                    readTime = "6 min",
                    source = "WildWatch Observer",
                    likes = 152,
                    comments = 18,
                    publishedAt = now - 86400000 // Yesterday
                ),
                ArticleEntity(
                    id = "seed-art-3",
                    category = "Security",
                    theme = ArticleTheme.SECURITY,
                    title = "Ranger Patrol Upgrades",
                    excerpt = "New digital tracking tools deployed to all sector rangers this week to improve response times to wildlife conflicts. The system integrates real-time GPS and satellite mapping.",
                    readTime = "3 min",
                    source = "Ministry of Tourism",
                    likes = 89,
                    comments = 7,
                    publishedAt = now - 172800000 // 2 days ago
                )
            )
        )
    }
}
