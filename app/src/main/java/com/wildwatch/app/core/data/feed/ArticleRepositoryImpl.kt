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
                    excerpt = "Updated grazing regulations for Bwindi buffer zones taking effect next Monday.",
                    readTime = "4 min",
                    source = "Uganda Wildlife Authority",
                    likes = 24,
                    comments = 3,
                    publishedAt = now
                )
            )
        )
    }
}
