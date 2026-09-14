package com.silversentry.sentry.core.domain.usecase

import com.silversentry.sentry.core.data.feed.ArticleRepository
import com.silversentry.sentry.core.model.Article
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetArticleByIdUseCase @Inject constructor(
    private val articleRepository: ArticleRepository
) {
    operator fun invoke(id: String): Flow<Article?> = articleRepository.observeById(id)
}
