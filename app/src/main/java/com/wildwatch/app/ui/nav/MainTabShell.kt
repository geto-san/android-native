package com.wildwatch.app.ui.nav

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wildwatch.app.R
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
    onReportCompensation: () -> Unit,
    onCommunityAlertsClick: () -> Unit,
    onOpenCommunityMap: () -> Unit,
) {
    val tabs = if (userRole == UserRole.RANGER) {
        listOf(MainTab.Dashboard, MainTab.Tracking, MainTab.Sos, MainTab.Profile)
    } else {
        listOf(MainTab.Home, MainTab.Feed, MainTab.Sos, MainTab.Profile)
    }

    var selectedTab by rememberSaveable {
        mutableStateOf(if (userRole == UserRole.RANGER) MainTab.Dashboard else MainTab.Home)
    }

    Scaffold(
        bottomBar = {
            Column {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp,
                    modifier = Modifier.padding(top = 0.dp)
                ) {
                    tabs.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val icon = when (tab) {
                            MainTab.Home -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
                            MainTab.Feed -> if (isSelected) Icons.Filled.Newspaper else Icons.Outlined.Newspaper
                            MainTab.Sos -> if (isSelected) Icons.Filled.WarningAmber else Icons.Outlined.WarningAmber
                            MainTab.Dashboard -> if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
                            MainTab.Tracking -> if (isSelected) Icons.Filled.LocationOn else Icons.Outlined.LocationOn
                            MainTab.Profile -> if (isSelected) Icons.Filled.Person else Icons.Outlined.Person
                        }
                        
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { 
                                if (tab == MainTab.Profile) {
                                    onProfileClick()
                                } else {
                                    selectedTab = tab 
                                }
                            },
                            icon = {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp),
                                    tint = if (tab == MainTab.Sos && isSelected) Destructive 
                                           else if (isSelected) MaterialTheme.colorScheme.onBackground 
                                           else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            },
                            label = null, // Instagram style: no labels
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent, // Instagram style: no pill indicator
                            ),
                        )
                    }
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
                    )
                    MainTab.Feed -> FeedScreen()
                    MainTab.Sos -> SosScreen()
                    MainTab.Dashboard -> DashboardScreen(
                        onIncidentClick = onIncidentClick,
                        onProfileClick = onProfileClick,
                    )
                    MainTab.Tracking -> TrackingScreen()
                    MainTab.Profile -> Box(modifier = Modifier) // Navigation handled by callback
                }
            }
        }
    }
}
