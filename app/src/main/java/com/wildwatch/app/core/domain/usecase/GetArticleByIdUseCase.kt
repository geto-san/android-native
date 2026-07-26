package com.wildwatch.app.core.domain.usecase

import com.wildwatch.app.core.data.feed.ArticleRepository
import com.wildwatch.app.core.model.Article
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetArticleByIdUseCase @Inject constructor(
    private val articleRepository: ArticleRepository
) {
    operator fun invoke(id: String): Flow<Article?> = articleRepository.observeById(id)
}
