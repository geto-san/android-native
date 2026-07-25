package com.wildwatch.app.ui.nav

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wildwatch.app.core.model.UserRole
import com.wildwatch.app.feature.dashboard.DashboardScreen
import com.wildwatch.app.feature.dashboard.HomeScreen
import com.wildwatch.app.feature.feed.FeedScreen
import com.wildwatch.app.feature.sos.SosScreen
import com.wildwatch.app.core.ui.theme.Destructive
import com.wildwatch.app.feature.tracking.TrackingScreen

private enum class MainTab {
    Home,
    Feed,
    Sos,
    Dashboard,
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
    onOpenCommunityMap: () -> Unit,
    onNotificationsClick: () -> Unit,
) {
    val tabs = if (userRole == UserRole.RANGER) {
        listOf(MainTab.Dashboard, MainTab.Tracking, MainTab.Sos)
    } else {
        listOf(MainTab.Home, MainTab.Feed, MainTab.Sos)
    }

    var selectedTab by rememberSaveable {
        mutableStateOf(if (userRole == UserRole.RANGER) MainTab.Dashboard else MainTab.Home)
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
                            .height(80.dp)
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEach { tab ->
                            val isSelected = selectedTab == tab
                            if (tab == MainTab.Sos) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Destructive)
                                        .clickable { selectedTab = MainTab.Sos },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.WarningAmber,
                                        contentDescription = "SOS",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            } else {
                                val icon = when (tab) {
                                    MainTab.Home -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
                                    MainTab.Feed -> if (isSelected) Icons.Filled.Newspaper else Icons.Outlined.Newspaper
                                    MainTab.Dashboard -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
                                    MainTab.Tracking -> if (isSelected) Icons.Filled.LocationOn else Icons.Outlined.LocationOn
                                    else -> Icons.Filled.Home
                                }
                                
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { selectedTab = tab }
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(26.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.onBackground 
                                               else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = tab.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onBackground 
                                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab-transition",
            ) { targetTab ->
                when (targetTab) {
                    MainTab.Home -> HomeScreen(
                        onIncidentClick = onIncidentClick,
                        onProfileClick = onProfileClick,
                        onReportSighting = onReportSighting,
                        onReportConflict = onReportConflict,
                        onCommunityAlertsClick = onCommunityAlertsClick,
                        onOpenCommunityMap = onOpenCommunityMap,
                        onNotificationsClick = onNotificationsClick,
                    )
                    MainTab.Feed -> FeedScreen()
                    MainTab.Sos -> SosScreen()
                    MainTab.Dashboard -> DashboardScreen(
                        onIncidentClick = onIncidentClick,
                        onProfileClick = onProfileClick,
                        onNotificationsClick = onNotificationsClick,
                    )
                    MainTab.Tracking -> TrackingScreen()
                    MainTab.Profile -> Box(modifier = Modifier) // Navigation handled by callback
                }
            }
        }
    }
}
