package com.wildwatch.app.ui.nav

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
import com.wildwatch.app.feature.incidentdetail.IncidentDetailScreen
import com.wildwatch.app.feature.feed.ArticleDetailScreen
import com.wildwatch.app.feature.feed.FeedScreen
import com.wildwatch.app.feature.notifications.NotificationsScreen
import com.wildwatch.app.feature.profile.ProfileScreen
import com.wildwatch.app.feature.report.CameraCaptureScreen
import com.wildwatch.app.feature.report.ConflictReportScreen
import com.wildwatch.app.feature.report.ReportIncidentViewModel
import com.wildwatch.app.feature.report.ReportSubmittedScreen
import com.wildwatch.app.feature.report.WildlifeSightingReportScreen
import com.wildwatch.app.feature.settings.AccountManagementScreen
import com.wildwatch.app.ui.nav.MainTabShell
import com.wildwatch.app.ui.nav.Route

@Composable
fun WildWatchNavHost(navController: NavHostController = rememberNavController()) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

    // Use a stable start destination determined by the INITIAL value of currentUser
    // to prevent the "login screen flash" when the app opens while logged in.
    val startDestination = remember {
        if (authViewModel.currentUser.value != null) Route.Main else Route.Auth(startOnSignIn = true)
    }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            // Logout: Clear backstack and go to Auth, but only if not already there
            if (navController.currentBackStackEntry?.destination?.route?.contains("Auth") != true) {
                navController.navigate(Route.Auth(startOnSignIn = true)) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else if (navController.currentBackStackEntry?.destination?.route?.contains("Auth") == true) {
            // Login success: Go to Main
            navController.navigate(Route.Main) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable<Route.Auth> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.Auth>()
            AuthScreen(startOnSignIn = args.startOnSignIn)
        }

        composable<Route.Main> {
            MainTabShell(
                userRole = currentUser?.role ?: UserRole.PUBLIC,
                isGuest = currentUser?.isGuest ?: true,
                onIncidentClick = { id -> navController.navigate(Route.IncidentDetail(id)) },
                onSignInClick = { navController.navigate(Route.Auth(startOnSignIn = true)) },
                onReportSighting = { navController.navigate(Route.WildlifeSightingReport) },
                onReportConflict = { navController.navigate(Route.ConflictReport) },
                onCommunityAlertsClick = { navController.navigate(Route.CommunityAlerts) },
                onNotificationsClick = { navController.navigate(Route.Notifications) },
                onArticleClick = { id -> navController.navigate(Route.ArticleDetail(id)) },
                onAccountManagementClick = { navController.navigate(Route.AccountManagement) }
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
            val viewModel: ReportIncidentViewModel = hiltViewModel(backStackEntry)
            WildlifeSightingReportScreen(
                onBack = { navController.popBackStack() },
                onSubmitted = { incidentId ->
                    navController.navigate(Route.ReportSubmitted(incidentId)) {
                        popUpTo(Route.Main) { inclusive = false }
                    }
                },
                onNavigateToCamera = { navController.navigate(Route.CameraCapture()) },
                viewModel = viewModel,
            )
        }

        composable<Route.ConflictReport> { backStackEntry ->
            val viewModel: ReportIncidentViewModel = hiltViewModel(backStackEntry)
            ConflictReportScreen(
                onBack = { navController.popBackStack() },
                onSubmitted = { incidentId ->
                    navController.navigate(Route.ReportSubmitted(incidentId)) {
                        popUpTo(Route.Main) { inclusive = false }
                    }
                },
                onNavigateToCamera = { category -> navController.navigate(Route.CameraCapture(category, source = "conflict")) },
                viewModel = viewModel,
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
            val reportViewModel: ReportIncidentViewModel = hiltViewModel(parentEntry)
            CameraCaptureScreen(
                onPhotoCaptured = { uri ->
                    reportViewModel.addPhoto(uri, args.photoCategory)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() },
            )
        }

        composable<Route.CommunityAlerts> {
            CommunityAlertsScreen(onBack = { navController.popBackStack() })
        }

        composable<Route.Notifications> {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }

        composable<Route.AccountManagement> {
            AccountManagementScreen(onBack = { navController.popBackStack() })
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
