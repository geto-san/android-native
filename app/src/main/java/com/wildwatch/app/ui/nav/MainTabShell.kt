package com.wildwatch.app.ui.nav

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wildwatch.app.R
import com.wildwatch.app.domain.model.UserRole
import com.wildwatch.app.ui.dashboard.DashboardScreen
import com.wildwatch.app.ui.dashboard.HomeScreen
import com.wildwatch.app.ui.feed.FeedScreen
import com.wildwatch.app.ui.sos.SosScreen
import com.wildwatch.app.ui.theme.Destructive
import com.wildwatch.app.ui.tracking.TrackingScreen

private enum class MainTab(@StringRes val labelRes: Int) {
    Home(R.string.nav_home),
    Feed(R.string.nav_feed),
    Sos(R.string.nav_sos),
    Dashboard(R.string.nav_dashboard),
    Tracking(R.string.nav_tracking)
}

// A single Scaffold + NavigationBar with local tab-selection state, rather
// than a nested Navigation Compose graph - none of the 3 tabs need their own
// independent back stack. Everything else pushes onto the outer NavHost (see
// WildWatchNavHost.kt) via the callbacks below, from whichever tab is
// currently showing.
@Composable
fun MainTabShell(
    userRole: UserRole,
    onIncidentClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onReportSighting: () -> Unit,
    onReportConflict: () -> Unit,
    onReportCompensation: () -> Unit,
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
            NavigationBar {
                tabs.forEach { tab ->
                    val icon = when (tab) {
                        MainTab.Home -> Icons.Filled.Home
                        MainTab.Feed -> Icons.Filled.Newspaper
                        MainTab.Sos -> Icons.Filled.WarningAmber
                        MainTab.Dashboard -> Icons.Filled.Home
                        MainTab.Tracking -> Icons.Filled.LocationOn
                    }
                    val label = stringResource(tab.labelRes)
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                icon,
                                contentDescription = label,
                                tint = if ((tab == MainTab.Sos) && (selectedTab == tab)) Destructive
                                else if (selectedTab == tab) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = if (tab == MainTab.Sos) Destructive else MaterialTheme.colorScheme.primary
                        ),
                    )
                }
            }
        },
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
                        onReportCompensation = onReportCompensation,
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
                }
            }
        }
    }
}
