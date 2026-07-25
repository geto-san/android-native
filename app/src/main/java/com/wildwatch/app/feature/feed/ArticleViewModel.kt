package com.wildwatch.app.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.core.domain.usecase.GetArticlesUseCase
import com.wildwatch.app.core.model.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ArticleUiState(
    val articles: List<Article> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ArticleViewModel @Inject constructor(
    getArticlesUseCase: GetArticlesUseCase,
) : ViewModel() {
    val uiState: StateFlow<ArticleUiState> = getArticlesUseCase()
        .map { ArticleUiState(articles = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ArticleUiState(isLoading = true)
        )
}
