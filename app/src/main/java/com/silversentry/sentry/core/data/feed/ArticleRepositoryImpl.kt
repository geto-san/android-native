package com.silversentry.sentry.core.data.feed

import com.silversentry.sentry.core.database.ArticleDao
import com.silversentry.sentry.core.di.ApplicationScope
import com.silversentry.sentry.core.di.IoDispatcher
import com.silversentry.sentry.core.model.Article
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// Room cache + Firestore listener: UI reads Room immediately (offline-capable),
// background listener merges live feed/{id} documents from the portal pipeline.
@Singleton
class ArticleRepositoryImpl @Inject constructor(
    private val articleDao: ArticleDao,
    private val feedRemote: FeedRemoteDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope applicationScope: CoroutineScope,
) : ArticleRepository {

    init {
        applicationScope.launch {
            feedRemote.observeFeedChanges().collect { change ->
                withContext(ioDispatcher) {
                    if (change.isRemoved) {
                        articleDao.deleteById(change.article.id)
                    } else {
                        articleDao.upsert(change.article.toEntity())
                    }
                }
            }
        }
    }

    override fun observeAll(): Flow<List<Article>> =
        articleDao.observeAll().map { entities -> entities.map(Article::fromEntity) }

    override fun observeById(id: String): Flow<Article?> =
        articleDao.observeById(id).map { it?.let(Article::fromEntity) }
}
