package com.wildwatch.app.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wildwatch.app.data.feed.ArticleRepository
import com.wildwatch.app.domain.model.Article
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// Backs FeedScreen with a real ArticleRepository.observeAll() Flow - see
// ArticleEntity's doc comment for why this is a seeded local table.
@HiltViewModel
class ArticleViewModel @Inject constructor(
    articleRepository: ArticleRepository,
) : ViewModel() {

    val articles: StateFlow<List<Article>> = articleRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
