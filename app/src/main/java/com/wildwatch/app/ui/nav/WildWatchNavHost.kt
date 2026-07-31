package com.wildwatch.app.ui.nav

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.wildwatch.app.core.model.UserRole
import com.wildwatch.app.feature.alerts.CommunityAlertsScreen
import com.wildwatch.app.feature.auth.AuthScreen
import com.wildwatch.app.feature.auth.AuthViewModel
import com.wildwatch.app.feature.dashboard.HistoryPlaceholderScreen
import com.wildwatch.app.feature.incidentdetail.IncidentDetailScreen
import com.wildwatch.app.feature.feed.ArticleDetailScreen
import com.wildwatch.app.feature.notifications.NotificationsScreen
import com.wildwatch.app.feature.profile.ProfileScreen
import com.wildwatch.app.core.database.IncidentType
import com.wildwatch.app.feature.report.dynamic.ui.DynamicReportScreen
import com.wildwatch.app.feature.report.dynamic.DynamicReportViewModel
import com.wildwatch.app.feature.report.CameraCaptureScreen
import com.wildwatch.app.feature.report.ReportSubmittedScreen
import com.wildwatch.app.feature.settings.AccountManagementScreen
import com.wildwatch.app.ui.nav.MainTabShell
import com.wildwatch.app.ui.nav.Route

@Composable
fun WildWatchNavHost(navController: NavHostController = rememberNavController()) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

    val startDestination = remember {
        if (authViewModel.currentUser.value != null) Route.Main else Route.Auth(startOnSignIn = true)
    }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            if (navController.currentBackStackEntry?.destination?.route?.contains("Auth") != true) {
                navController.navigate(Route.Auth(startOnSignIn = true)) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else if (navController.currentBackStackEntry?.destination?.route?.contains("Auth") == true) {
            navController.navigate(Route.Main) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController, 
        startDestination = startDestination,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(400)) + fadeIn(tween(400)) },
        exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(400)) + fadeOut(tween(400)) },
        popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(400)) + fadeIn(tween(400)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(400)) + fadeOut(tween(400)) }
    ) {
        composable<Route.Auth> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.Auth>()
            AuthScreen(startOnSignIn = args.startOnSignIn)
        }

        composable<Route.Main> {
            MainTabShell(
                userRole = currentUser?.role ?: UserRole.PUBLIC,
                onIncidentClick = { id -> navController.navigate(Route.IncidentDetail(id)) },
                onSignInClick = { navController.navigate(Route.Auth(startOnSignIn = true)) },
                onReportSighting = { navController.navigate(Route.WildlifeSightingReport()) },
                onReportConflict = { navController.navigate(Route.ConflictReport()) },
                onEditDraft = { id, type ->
                    val route = if (type == IncidentType.SIGHTING) Route.WildlifeSightingReport(id) else Route.ConflictReport(id)
                    navController.navigate(route)
                },
                onCommunityAlertsClick = { navController.navigate(Route.CommunityAlerts) },
                onNotificationsClick = { navController.navigate(Route.Notifications) },
                onArticleClick = { id -> navController.navigate(Route.ArticleDetail(id)) },
                onAccountManagementClick = { navController.navigate(Route.AccountManagement) },
                onSeeAllReportsClick = { navController.navigate(Route.IncidentHistory) }
            )
        }

        composable<Route.IncidentDetail> {
            IncidentDetailScreen(
                onBack = { navController.popBackStack() },
                onStartGps = { navController.popBackStack(Route.Main, inclusive = false) },
            )
        }

        composable<Route.Profile> {
            ProfileScreen()
        }

        composable<Route.WildlifeSightingReport> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.WildlifeSightingReport>()
            DynamicReportScreen(
                type = IncidentType.SIGHTING,
                draftId = args.draftId,
                onBack = { navController.popBackStack() },
                onSubmitted = { incidentId ->
                    navController.navigate(Route.ReportSubmitted(incidentId)) {
                        popUpTo(Route.Main) { inclusive = false }
                    }
                },
                onNavigateToCamera = { navController.navigate(Route.CameraCapture()) },
            )
        }

        composable<Route.ConflictReport> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.ConflictReport>()
            DynamicReportScreen(
                type = IncidentType.CONFLICT,
                draftId = args.draftId,
                onBack = { navController.popBackStack() },
                onSubmitted = { incidentId ->
                    navController.navigate(Route.ReportSubmitted(incidentId)) {
                        popUpTo(Route.Main) { inclusive = false }
                    }
                },
                onNavigateToCamera = { category -> navController.navigate(Route.CameraCapture(category, source = "conflict")) },
            )
        }

        composable<Route.ReportSubmitted> {
            ReportSubmittedScreen(
                onReturnHome = {
                    navController.popBackStack(Route.Main, inclusive = false)
                },
            )
        }

        composable<Route.CameraCapture> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.CameraCapture>()
            val parentEntry = remember(backStackEntry) {
                if (args.source == "conflict") {
                    navController.getBackStackEntry(Route.ConflictReport)
                } else {
                    navController.getBackStackEntry(Route.WildlifeSightingReport)
                }
            }
            val reportViewModel: DynamicReportViewModel = hiltViewModel(parentEntry)
            CameraCaptureScreen(
                onPhotoCaptured = { uri ->
                    val currentPhotos = (reportViewModel.uiState.value.answers["photos"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    reportViewModel.updateAnswer("photos", currentPhotos + uri)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() },
            )
        }

        composable<Route.CommunityAlerts> {
            CommunityAlertsScreen(onBack = { navController.popBackStack() })
        }

        composable<Route.Notifications> {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToIncident = { id -> navController.navigate(Route.IncidentDetail(id)) },
                onNavigateToArticle = { id -> navController.navigate(Route.ArticleDetail(id)) },
                onNavigateToAlerts = { navController.navigate(Route.CommunityAlerts) }
            )
        }

        composable<Route.AccountManagement> {
            AccountManagementScreen(onBack = { navController.popBackStack() })
        }

        composable<Route.IncidentHistory> {
            HistoryPlaceholderScreen(onBack = { navController.popBackStack() })
        }

        composable<Route.ArticleDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.ArticleDetail>()
            ArticleDetailScreen(
                articleId = args.id,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
