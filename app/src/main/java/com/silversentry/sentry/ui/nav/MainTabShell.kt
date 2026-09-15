package com.silversentry.sentry.ui.nav

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Sos
import com.silversentry.sentry.core.model.UserRole
import com.silversentry.sentry.feature.dashboard.DashboardScreen
import com.silversentry.sentry.feature.dashboard.HomeScreen
import com.silversentry.sentry.feature.feed.FeedScreen
import com.silversentry.sentry.feature.profile.ProfileScreen
import com.silversentry.sentry.feature.tracking.RangerTrackingScreen

private enum class MainTab {
    Home,
    Professional,
    Feed,
    Tracking,
    Profile
}

@Composable
@Suppress("LongParameterList")
fun MainTabShell(
    userRole: UserRole,
    onIncidentClick: (String) -> Unit,
    onSignInClick: () -> Unit,
    onReportIncident: () -> Unit,
    onSos: () -> Unit,
    onEditDraft: (String, com.silversentry.sentry.core.database.IncidentType) -> Unit,
    onNotificationsClick: () -> Unit,
    onArticleClick: (String) -> Unit,
    onSeeAllReportsClick: () -> Unit,
) {
    val tabs = if (userRole == UserRole.RANGER) {
        listOf(MainTab.Home, MainTab.Professional, MainTab.Tracking, MainTab.Profile)
    } else {
        listOf(MainTab.Home, MainTab.Feed, MainTab.Profile)
    }

    var selectedTab by rememberSaveable {
        mutableStateOf(MainTab.Home)
    }

    Scaffold(
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                Column {
                    HorizontalDivider(
                        thickness = 0.5.dp, 
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .height(60.dp)
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEach { tab ->
                            MainTabButton(
                                modifier = Modifier.weight(1f),
                                tab = tab,
                                isSelected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                            )
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                MainTab.Home -> HomeScreen(
                    onIncidentClick = onIncidentClick,
                    onReportIncident = onReportIncident,
                    onEditDraft = onEditDraft,
                    onNotificationsClick = onNotificationsClick,
                    onArticleClick = onArticleClick,
                    onSeeAllClick = onSeeAllReportsClick,
                )
                MainTab.Professional -> DashboardScreen(
                    onIncidentClick = onIncidentClick,
                    onNotificationsClick = onNotificationsClick,
                )
                MainTab.Feed -> FeedScreen(
                    onArticleClick = onArticleClick
                )
                MainTab.Tracking -> RangerTrackingScreen(onIncidentClick = onIncidentClick)
                MainTab.Profile -> ProfileScreen(
                    onSignInClick = onSignInClick,
                )
            }

            // The SOS emergency action lives on every main screen as a persistent floating
            // button near the bottom-right corner (raised above the content, above the tab
            // bar), rather than taking a slot inside the bar. Same red button language as the
            // pre-redesign center action; it never moves when the user switches tabs. A soft
            // red pulse ring behind the button keeps the emergency affordance noticeable at a
            // glance without a spinner-like churn.
            SosFabButton(
                onClick = onSos,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 12.dp, end = 16.dp)
                    .shadow(12.dp, CircleShape, clip = false),
            )
        }
    }
}

@Composable
private fun SosFabButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // One breathing pulse per 1.4s: the ring swells outward while fading to nothing, then
    // restarts - the classic "emergency" affordance. The button itself stays static so a tap
    // and its ripple are never fought by the animation.
    val pulse = rememberInfiniteTransition(label = "SosPulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "SosPulseScale",
    )
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "SosPulseAlpha",
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .scale(pulseScale)
                .graphicsLayer { alpha = pulseAlpha }
                .background(
                    MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                    CircleShape,
                ),
        )
        FloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                Icons.Filled.Sos,
                contentDescription = "Send SOS",
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun MainTabButton(
    modifier: Modifier = Modifier,
    tab: MainTab,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = tween(durationMillis = 300),
        label = "TabScale",
    )

    // Professional/Tracking/Profile changed from the generic briefcase/pin/silhouette set
    // to icons that read as what the tab actually does: DirectionsRun for "go respond"
    // (same icon family as the Response Center's own CTA button), MyLocation for live GPS
    // patrol tracking (a pin reads as "a place", not "your live position"), AccountCircle
    // for a more recognizable profile affordance. Feed moved from Newspaper to Article to
    // match the icon NotificationsScreen already uses for NEW_FEED_ARTICLE, instead of two
    // icons for one concept.
    val icon = when (tab) {
        MainTab.Home -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
        MainTab.Professional -> if (isSelected) Icons.AutoMirrored.Filled.DirectionsRun else Icons.AutoMirrored.Outlined.DirectionsRun
        MainTab.Feed -> if (isSelected) Icons.AutoMirrored.Filled.Article else Icons.AutoMirrored.Outlined.Article
        MainTab.Tracking -> if (isSelected) Icons.Filled.MyLocation else Icons.Outlined.MyLocation
        MainTab.Profile -> if (isSelected) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(4.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
        }
    }
}
