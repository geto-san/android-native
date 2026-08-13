package com.wildwatch.app.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.wildwatch.app.core.database.ArticleTheme
import com.wildwatch.app.core.model.Article
import com.wildwatch.app.core.ui.component.StatusPill
import com.wildwatch.app.core.ui.theme.ForestGreen
import com.wildwatch.app.core.ui.theme.ForestGreenGlow
import com.wildwatch.app.core.ui.theme.Grey500
import com.wildwatch.app.core.ui.theme.Grey900
import com.wildwatch.app.core.ui.theme.InstaBlue
import com.wildwatch.app.core.ui.theme.SoftBlue
import com.wildwatch.app.core.ui.theme.Success
import com.wildwatch.app.core.ui.theme.SunsetAmber
import com.wildwatch.app.core.ui.theme.Warning
import com.wildwatch.app.core.ui.theme.White
import com.wildwatch.app.core.util.relativeDay
import java.time.Instant

// Staggered-grid layout in the spirit of Now in Android's "For You" screen (adaptive card
// width, header imagery) rather than a flat single-column list of text-only cards - see
// wildwatch's project notes for why. Reuses this app's existing palette (ForestGreen/
// SunsetAmber/InstaBlue/etc, see Color.kt) for the no-image fallback header instead of
// introducing new theme colors.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onArticleClick: (String) -> Unit,
    viewModel: ArticleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Community News",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(180.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(12.dp),
            verticalItemSpacing = 12.dp,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.articles, key = { it.id }) { article ->
                ArticleCard(
                    article = article,
                    onClick = { onArticleClick(article.id) }
                )
            }
        }
    }
}

private data class ThemeGradient(val start: Color, val end: Color)

private fun ArticleTheme.gradient(): ThemeGradient = when (this) {
    ArticleTheme.FOREST -> ThemeGradient(ForestGreen, ForestGreenGlow)
    ArticleTheme.WILDLIFE -> ThemeGradient(ForestGreenGlow, Success)
    ArticleTheme.SECURITY -> ThemeGradient(Grey900, Color(0xFF2C2C2C))
    ArticleTheme.SUNSET -> ThemeGradient(SunsetAmber, Warning)
    ArticleTheme.SKY -> ThemeGradient(InstaBlue, SoftBlue)
}

@Composable
private fun ArticleCard(
    article: Article,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                if (article.imageUrl != null) {
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    val gradient = article.theme.gradient()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(listOf(gradient.start, gradient.end))
                            )
                    )
                }
                StatusPill(
                    text = article.category,
                    contentColor = White,
                    containerColor = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                )
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                )

                Text(
                    text = article.excerpt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 2,
                )

                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = relativeDay(Instant.ofEpochMilli(article.publishedAt).toString()),
                        style = MaterialTheme.typography.labelSmall,
                        color = Grey500,
                    )
                    Text(text = " · ", style = MaterialTheme.typography.labelSmall, color = Grey500)
                    Text(
                        text = article.readTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = Grey500,
                    )
                }
            }
        }
    }
}
