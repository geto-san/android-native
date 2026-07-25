package com.wildwatch.app.core.domain.usecase

import com.wildwatch.app.core.data.feed.ArticleRepository
import com.wildwatch.app.core.model.Article
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetArticlesUseCase @Inject constructor(
    private val articleRepository: ArticleRepository
) {
    operator fun invoke(): Flow<List<Article>> = articleRepository.observeAll()
}
