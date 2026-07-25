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
import com.wildwatch.app.feature.claims.CompensationClaimScreen
import com.wildwatch.app.feature.claims.NewClaimScreen
import com.wildwatch.app.core.database.ClaimCategory
import com.wildwatch.app.feature.incidentdetail.IncidentDetailScreen
import com.wildwatch.app.feature.map.CommunityMapScreen
import com.wildwatch.app.feature.notifications.NotificationsScreen
import com.wildwatch.app.feature.profile.ProfileScreen
import com.wildwatch.app.feature.report.CameraCaptureScreen
import com.wildwatch.app.feature.report.ConflictReportScreen
import com.wildwatch.app.feature.report.ReportIncidentViewModel
import com.wildwatch.app.feature.report.ReportSubmittedScreen
import com.wildwatch.app.feature.report.WildlifeSightingReportScreen
import com.wildwatch.app.feature.welcome.WelcomeScreen

@Composable
fun WildWatchNavHost(navController: NavHostController = rememberNavController()) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            navController.navigate(Route.Welcome) {
                popUpTo(0) { inclusive = true }
            }
        } else if (navController.currentDestination?.route == Route.Welcome::class.qualifiedName ||
                   navController.currentDestination?.route?.contains("Auth") == true) {
            navController.navigate(Route.Main) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Route.Welcome) {
        composable<Route.Welcome> {
            WelcomeScreen(
                onGetStarted = { navController.navigate(Route.Auth(startOnSignIn = false)) },
                onAlreadyHaveAccount = { navController.navigate(Route.Auth(startOnSignIn = true)) },
            )
        }

        composable<Route.Auth> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.Auth>()
            AuthScreen(startOnSignIn = args.startOnSignIn)
        }

        composable<Route.Main> {
            MainTabShell(
                userRole = currentUser?.role ?: UserRole.COMMUNITY,
                onIncidentClick = { id -> navController.navigate(Route.IncidentDetail(id)) },
                onProfileClick = { navController.navigate(Route.Profile) },
                onReportSighting = { navController.navigate(Route.WildlifeSightingReport) },
                onReportConflict = { navController.navigate(Route.ConflictReport) },
                onReportCompensation = { navController.navigate(Route.CompensationClaim) },
                onCommunityAlertsClick = { navController.navigate(Route.CommunityAlerts) },
                onOpenCommunityMap = { navController.navigate(Route.CommunityMap) },
                onNotificationsClick = { navController.navigate(Route.Notifications) }
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
                onFileCompensationClaim = {
                    navController.navigate(Route.CompensationClaim) {
                        popUpTo(Route.Main) { inclusive = false }
                    }
                },
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

        composable<Route.CompensationClaim> {
            CompensationClaimScreen(
                onBack = { navController.popBackStack() },
                onCategoryClick = { category -> navController.navigate(Route.NewClaim(category.name)) },
            )
        }

        composable<Route.NewClaim> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.NewClaim>()
            NewClaimScreen(
                category = ClaimCategory.valueOf(args.category),
                onBack = { navController.popBackStack() },
                onSubmitted = { navController.popBackStack() },
            )
        }

        composable<Route.CommunityMap> {
            CommunityMapScreen(
                onBack = { navController.popBackStack() },
                onReportSighting = { navController.navigate(Route.WildlifeSightingReport) },
            )
        }

        composable<Route.Notifications> {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }
    }
}
