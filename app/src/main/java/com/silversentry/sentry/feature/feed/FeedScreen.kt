package com.silversentry.sentry.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.silversentry.sentry.core.database.ArticleTheme
import com.silversentry.sentry.core.model.Article
import com.silversentry.sentry.core.ui.component.StatusPill
import com.silversentry.sentry.core.ui.theme.ForestGreen
import com.silversentry.sentry.core.ui.theme.ForestGreenGlow
import com.silversentry.sentry.core.ui.theme.Grey500
import com.silversentry.sentry.core.ui.theme.Grey900
import com.silversentry.sentry.core.ui.theme.InstaBlue
import com.silversentry.sentry.core.ui.theme.SoftBlue
import com.silversentry.sentry.core.ui.theme.Success
import com.silversentry.sentry.core.ui.theme.SunsetAmber
import com.silversentry.sentry.core.ui.theme.Warning
import com.silversentry.sentry.core.ui.theme.White
import com.silversentry.sentry.core.util.relativeDay
import java.time.Instant

// Professional news-feed layout (single editorial column in the spirit of a park news
// desk, not a photo grid): the newest article gets a full-width hero card; everything
// else is a compact headline row with a small thumbnail, a category tag, title, excerpt
// and timestamp, divided by hairline separators so it reads like a real news roster.
// The hero keeps the palette-driven gradient header for articles without imagery.
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
        when {
            uiState.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            uiState.articles.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.Article,
                        contentDescription = null,
                        tint = Grey500,
                        modifier = Modifier.height(48.dp),
                    )
                    Text(
                        text = "No news yet",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        text = "Park updates will show up here once they're published.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Grey500,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
            ) {
                val articles = uiState.articles
                item(key = "hero_${articles.first().id}") {
                    HeroArticleCard(
                        article = articles.first(),
                        onClick = { onArticleClick(articles.first().id) }
                    )
                }
                items(
                    items = articles.drop(1),
                    key = { it.id }
                ) { article ->
                    FeedArticleRow(
                        article = article,
                        onClick = { onArticleClick(article.id) }
                    )
                }
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
private fun ArticleHeader(
    article: Article,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
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
    }
}

// The lead story of the feed: a full-width image (or gradient) header with the category tag
// overlayed, an oversized headline, a longer excerpt and the byline row below.
@Composable
private fun HeroArticleCard(
    article: Article,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                ArticleHeader(article = article, modifier = Modifier.fillMaxSize())
                StatusPill(
                    text = article.category,
                    contentColor = White,
                    containerColor = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                )
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = article.excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                ArticleMeta(article = article, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }

    HorizontalDivider(
        thickness = 6.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
    )
}

// Compact editorial row for non-hero items: small rounded thumbnail (image or theme
// gradient), category tag, two-line headline and excerpt, meta row underneath.
@Composable
private fun FeedArticleRow(
    article: Article,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .size(width = 92.dp, height = 76.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                ArticleHeader(article = article, modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                StatusPill(
                    text = article.category,
                    contentColor = White,
                    containerColor = article.theme.gradient().start,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Text(
            text = article.excerpt,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        ArticleMeta(article = article, modifier = Modifier.padding(top = 8.dp))
    }

    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
    )
}

@Composable
private fun ArticleMeta(
    article: Article,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
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
