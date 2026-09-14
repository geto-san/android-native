package com.silversentry.sentry.core.domain.usecase

import com.silversentry.sentry.core.data.feed.ArticleRepository
import com.silversentry.sentry.core.model.Article
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetArticlesUseCase @Inject constructor(
    private val articleRepository: ArticleRepository
) {
    operator fun invoke(): Flow<List<Article>> = articleRepository.observeAll()
}
