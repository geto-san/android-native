package com.wildwatch.app.ui.nav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wildwatch.app.core.model.UserRole
import com.wildwatch.app.feature.dashboard.DashboardScreen
import com.wildwatch.app.feature.dashboard.HomeScreen
import com.wildwatch.app.feature.feed.FeedScreen
import com.wildwatch.app.feature.tracking.RangerTrackingScreen

// wireframe RangerTabBar is Dashboard/Map/Tracking (Profile lives behind the
// header avatar instead, same as CommunityTabBar's Home/Feed). This app
// keeps Profile as a 4th persistent tab for both roles - an established
// Android convention this codebase already committed to - rather than
// matching the wireframe's header-avatar placement exactly.
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
    onProfileClick: () -> Unit,
    onReportSighting: () -> Unit,
    onReportConflict: () -> Unit,
    onCommunityAlertsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onArticleClick: (String) -> Unit,
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
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .height(56.dp)
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEach { tab ->
                            val isSelected = selectedTab == tab
                            val icon = when (tab) {
                                MainTab.Dashboard -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
                                MainTab.Feed -> if (isSelected) Icons.Filled.Newspaper else Icons.Outlined.Newspaper
                                MainTab.Tracking -> if (isSelected) Icons.Filled.LocationOn else Icons.Outlined.LocationOn
                                MainTab.Profile -> if (isSelected) Icons.Filled.Person else Icons.Outlined.Person
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { 
                                        if (tab == MainTab.Profile) {
                                            onProfileClick()
                                        } else {
                                            selectedTab = tab 
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary 
                                           else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
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
            // Use direct conditional composition for instant tab switching.
            // This avoids the overhead of Crossfade/AnimatedContent.
            when (selectedTab) {
                MainTab.Dashboard -> if (userRole == UserRole.RANGER) {
                    DashboardScreen(
                        onIncidentClick = onIncidentClick,
                        onProfileClick = onProfileClick,
                        onNotificationsClick = onNotificationsClick,
                    )
                } else {
                    HomeScreen(
                        onIncidentClick = onIncidentClick,
                        onProfileClick = onProfileClick,
                        onReportSighting = onReportSighting,
                        onReportConflict = onReportConflict,
                        onCommunityAlertsClick = onCommunityAlertsClick,
                        onNotificationsClick = onNotificationsClick,
                    )
                }
                MainTab.Feed -> FeedScreen(
                    onArticleClick = onArticleClick
                )
                MainTab.Tracking -> RangerTrackingScreen(onIncidentClick = onIncidentClick)
                MainTab.Profile -> Box(modifier = Modifier) // Navigation handled by callback
            }
        }
    }
}
