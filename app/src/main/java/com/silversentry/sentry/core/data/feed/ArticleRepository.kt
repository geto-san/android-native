package com.silversentry.sentry.core.data.feed

import com.silversentry.sentry.core.model.Article
import kotlinx.coroutines.flow.Flow

// Read-only, same rationale as AlertRepository: editorial content this app
// doesn't author, backed by a real seeded Room table instead of an
// in-memory list.
interface ArticleRepository {
    fun observeAll(): Flow<List<Article>>
    fun observeById(id: String): Flow<Article?>
}
