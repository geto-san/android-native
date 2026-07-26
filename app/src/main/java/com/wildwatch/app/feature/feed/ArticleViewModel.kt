package com.wildwatch.app.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.core.domain.usecase.GetArticleByIdUseCase
import com.wildwatch.app.core.domain.usecase.GetArticlesUseCase
import com.wildwatch.app.core.model.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ArticleUiState(
    val articles: List<Article> = emptyList(),
    val currentArticle: Article? = null,
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArticleViewModel @Inject constructor(
    private val getArticlesUseCase: GetArticlesUseCase,
    private val getArticleByIdUseCase: GetArticleByIdUseCase,
) : ViewModel() {

    private val _articleId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ArticleUiState> = combine(
        getArticlesUseCase(),
        _articleId.flatMapLatest { id ->
            if (id == null) flowOf(null) else getArticleByIdUseCase(id)
        }
    ) { articles, currentArticle ->
        ArticleUiState(
            articles = articles,
            currentArticle = currentArticle,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ArticleUiState(isLoading = true)
    )

    fun setArticleId(id: String) {
        _articleId.value = id
    }
}
