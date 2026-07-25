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
import com.wildwatch.app.domain.model.UserRole
import com.wildwatch.app.ui.alerts.CommunityAlertsScreen
import com.wildwatch.app.ui.auth.AuthScreen
import com.wildwatch.app.ui.auth.AuthViewModel
import com.wildwatch.app.ui.claims.CompensationClaimScreen
import com.wildwatch.app.ui.claims.NewClaimScreen
import com.wildwatch.app.data.local.db.ClaimCategory
import com.wildwatch.app.ui.incidentdetail.IncidentDetailScreen
import com.wildwatch.app.ui.map.CommunityMapScreen
import com.wildwatch.app.ui.onboarding.LocationPermissionScreen
import com.wildwatch.app.ui.profile.ProfileScreen
import com.wildwatch.app.ui.reportincident.CameraCaptureScreen
import com.wildwatch.app.ui.reportincident.ConflictReportScreen
import com.wildwatch.app.ui.reportincident.ReportIncidentViewModel
import com.wildwatch.app.ui.reportincident.ReportSubmittedScreen
import com.wildwatch.app.ui.reportincident.WildlifeSightingReportScreen
import com.wildwatch.app.ui.welcome.WelcomeScreen

@Composable
fun WildWatchNavHost(navController: NavHostController = rememberNavController()) {
    // Hoisted here (rather than per-screen) so auth state drives top-level
    // navigation decisions from one place.
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = Route.Main) {
        // We skip Splash route because we use the system splash screen which
        // waits in MainActivity before starting the app.
        // But we still need a way to decide the start destination.
        // Actually, NIA uses a single destination and decides what to show.
        // Let's keep Main as start and handle auth redirection there.

        composable<Route.Welcome> {
            WelcomeScreen(
                onGetStarted = { navController.navigate(Route.Auth(startOnSignIn = false)) },
                onAlreadyHaveAccount = { navController.navigate(Route.Auth(startOnSignIn = true)) },
            )
        }

        composable<Route.Auth> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.Auth>()
            LaunchedEffect(currentUser) {
                if (currentUser != null) {
                    navController.navigate(Route.LocationPermission) {
                        popUpTo(Route.Welcome) { inclusive = true }
                    }
                }
            }
            AuthScreen(startOnSignIn = args.startOnSignIn)
        }

        composable<Route.LocationPermission> {
            LocationPermissionScreen(
                onContinue = {
                    navController.navigate(Route.Main) {
                        popUpTo(Route.Splash) { inclusive = true }
                    }
                },
            )
        }

        composable<Route.Main> {
            LaunchedEffect(currentUser) {
                if (currentUser == null) {
                    navController.navigate(Route.Welcome) { popUpTo(Route.Main) { inclusive = true } }
                }
            }
            MainTabShell(
                userRole = currentUser?.role ?: UserRole.COMMUNITY,
                onIncidentClick = { id -> navController.navigate(Route.IncidentDetail(id)) },
                onProfileClick = { navController.navigate(Route.Profile) },
                onReportSighting = { navController.navigate(Route.WildlifeSightingReport) },
                onReportConflict = { navController.navigate(Route.ConflictReport) },
                onReportCompensation = { navController.navigate(Route.CompensationClaim) },
                onCommunityAlertsClick = { navController.navigate(Route.CommunityAlerts) },
                onOpenCommunityMap = { navController.navigate(Route.CommunityMap) },
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
            // Scoped to the report form's own back stack entry so both screens share
            // the same ViewModel instance - a captured photo needs to land back
            // in the form's photo list without a separate result-passing channel.
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
    }
}
