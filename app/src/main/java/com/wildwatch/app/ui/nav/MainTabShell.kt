package com.wildwatch.app.ui.nav

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wildwatch.app.core.model.UserRole
import com.wildwatch.app.feature.dashboard.DashboardScreen
import com.wildwatch.app.feature.dashboard.HomeScreen
import com.wildwatch.app.feature.feed.FeedScreen
import com.wildwatch.app.feature.profile.ProfileScreen
import com.wildwatch.app.feature.tracking.RangerTrackingScreen

private enum class MainTab {
    Dashboard,
    Feed,
    Tracking,
    Profile
}

@Composable
fun MainTabShell(
    userRole: UserRole,
    onIncidentClick: (String) -> Unit,
    onSignInClick: () -> Unit,
    onReportSighting: () -> Unit,
    onReportConflict: () -> Unit,
    onEditDraft: (String, com.wildwatch.app.core.database.IncidentType) -> Unit,
    onCommunityAlertsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onArticleClick: (String) -> Unit,
    onAccountManagementClick: () -> Unit,
    onSeeAllReportsClick: () -> Unit,
) {
    val tabs = if (userRole == UserRole.RANGER) {
        listOf(MainTab.Dashboard, MainTab.Tracking, MainTab.Profile)
    } else {
        listOf(MainTab.Dashboard, MainTab.Feed, MainTab.Profile)
    }

    var selectedTab by rememberSaveable {
        mutableStateOf(MainTab.Dashboard)
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
                            val isSelected = selectedTab == tab
                            
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.15f else 1.0f,
                                animationSpec = tween(durationMillis = 300),
                                label = "TabScale"
                            )

                            val icon = when (tab) {
                                MainTab.Dashboard -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
                                MainTab.Feed -> if (isSelected) Icons.Filled.Newspaper else Icons.Outlined.Newspaper
                                MainTab.Tracking -> if (isSelected) Icons.Filled.LocationOn else Icons.Outlined.LocationOn
                                MainTab.Profile -> if (isSelected) Icons.Filled.Person else Icons.Outlined.Person
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .scale(scale)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null // Remove default ripple
                                    ) { selectedTab = tab },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(26.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary 
                                               else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .size(4.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        )
                                    }
                                }
                            }
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
                MainTab.Dashboard -> if (userRole == UserRole.RANGER) {
                    DashboardScreen(
                        onIncidentClick = onIncidentClick,
                        onNotificationsClick = onNotificationsClick,
                    )
                } else {
                    HomeScreen(
                        onIncidentClick = onIncidentClick,
                        onReportSighting = onReportSighting,
                        onReportConflict = onReportConflict,
                        onEditDraft = onEditDraft,
                        onCommunityAlertsClick = onCommunityAlertsClick,
                        onNotificationsClick = onNotificationsClick,
                        onSeeAllClick = onSeeAllReportsClick,
                    )
                }
                MainTab.Feed -> FeedScreen(
                    onArticleClick = onArticleClick
                )
                MainTab.Tracking -> RangerTrackingScreen(onIncidentClick = onIncidentClick)
                MainTab.Profile -> ProfileScreen(
                    onSignInClick = onSignInClick,
                    onAccountManagementClick = onAccountManagementClick
                )
            }
        }
    }
}
