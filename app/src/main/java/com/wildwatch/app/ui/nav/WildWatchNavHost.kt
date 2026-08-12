package com.wildwatch.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.wildwatch.app.core.model.UserRole
import kotlinx.coroutines.flow.first
import com.wildwatch.app.feature.alerts.CommunityAlertsScreen
import com.wildwatch.app.feature.auth.AuthScreen
import com.wildwatch.app.feature.auth.AuthViewModel
import com.wildwatch.app.feature.dashboard.HistoryPlaceholderScreen
import com.wildwatch.app.feature.incidentdetail.IncidentDetailScreen
import com.wildwatch.app.feature.feed.ArticleDetailScreen
import com.wildwatch.app.feature.notifications.NotificationsScreen
import com.wildwatch.app.feature.profile.ProfileScreen
import com.wildwatch.app.feature.report.CameraCaptureScreen
import com.wildwatch.app.feature.report.ReportIncidentScreen
import com.wildwatch.app.feature.report.ReportIncidentViewModel
import com.wildwatch.app.feature.report.ReportSubmittedScreen
import com.wildwatch.app.feature.settings.AccountManagementScreen
import com.wildwatch.app.ui.nav.MainTabShell
import com.wildwatch.app.ui.nav.NavMotion
import com.wildwatch.app.ui.nav.Route

@Composable
fun WildWatchNavHost(
    navController: NavHostController = rememberNavController(),
    pendingRoute: Route? = null,
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    var authReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authViewModel.currentUser.first()
        authReady = true
    }

    if (!authReady) {
        return
    }

    // A notification tap launched the app with a specific screen in mind -
    // navigate there once the graph below exists. Keyed on the route value
    // itself (not Unit) so recomposition never re-fires this for the same
    // instance, but a *new* MainActivity/pendingRoute (a second notification
    // tap while the process is still alive) does navigate again.
    LaunchedEffect(pendingRoute) {
        pendingRoute?.let { navController.navigate(it) }
    }

    // Guest browsing is supported (README "Capabilities"), so Main is always the start
    // destination regardless of auth state - unauthenticated users are never forced onto
    // the Auth screen. This effect only ever pushes the OTHER direction: once a sign-in/
    // sign-up/anonymous/Google flow actually succeeds while the Auth screen is showing,
    // leave it and land on Main. (A previous version of this effect also force-navigated
    // to Auth whenever currentUser was null, which broke guest browsing on cold start -
    // removed, not restored.)
    val startDestination = Route.Main
    LaunchedEffect(currentUser) {
        if (currentUser != null &&
            navController.currentBackStackEntry?.destination?.route?.contains("Auth") == true
        ) {
            navController.navigate(Route.Main) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController, 
        startDestination = startDestination,
        enterTransition = {
            if (initialState.destination.route?.contains("Auth") == true ||
                targetState.destination.route?.contains("Auth") == true
            ) {
                NavMotion.fadeThroughEnter()
            } else {
                NavMotion.forwardEnter()
            }
        },
        exitTransition = {
            if (initialState.destination.route?.contains("Auth") == true ||
                targetState.destination.route?.contains("Auth") == true
            ) {
                NavMotion.fadeThroughExit()
            } else {
                NavMotion.forwardExit()
            }
        },
        popEnterTransition = { NavMotion.backwardEnter() },
        popExitTransition = { NavMotion.backwardExit() },
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
                onReportIncident = { navController.navigate(Route.ReportIncident()) },
                onEditDraft = { id, _ -> navController.navigate(Route.ReportIncident(draftId = id)) },
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

        composable<Route.ReportIncident> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.ReportIncident>()
            ReportIncidentScreen(
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

        composable<Route.ReportSubmitted> {
            ReportSubmittedScreen(
                onReturnHome = {
                    navController.popBackStack(Route.Main, inclusive = false)
                },
            )
        }

        composable<Route.CameraCapture> { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.previousBackStackEntry
                    ?: error("CameraCapture requires a report screen on the back stack")
            }
            val reportViewModel: ReportIncidentViewModel = hiltViewModel(parentEntry)
            CameraCaptureScreen(
                onPhotoCaptured = { uri ->
                    reportViewModel.addPhoto(uri)
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
