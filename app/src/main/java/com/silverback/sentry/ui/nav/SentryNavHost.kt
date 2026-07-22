package com.silverback.sentry.ui.nav

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
import com.silverback.sentry.ui.addsighting.AddSightingScreen
import com.silverback.sentry.ui.addsighting.AddSightingViewModel
import com.silverback.sentry.ui.addsighting.CameraCaptureScreen
import com.silverback.sentry.ui.auth.AuthViewModel
import com.silverback.sentry.ui.auth.LoginScreen
import com.silverback.sentry.ui.auth.SignUpScreen
import com.silverback.sentry.ui.feed.FeedScreen
import com.silverback.sentry.ui.home.HomeScreen
import com.silverback.sentry.ui.splash.SplashScreen

@Composable
fun SentryNavHost(navController: NavHostController = rememberNavController()) {
    // Hoisted here (rather than per-screen) so auth state drives top-level
    // navigation decisions from one place.
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

    NavHost(navController = navController, startDestination = Route.Splash) {
        composable<Route.Splash> {
            SplashScreen()
            // currentUser starts as whatever FirebaseAuth already has cached
            // synchronously (see AuthRepositoryImpl's initialValue), so this fires
            // on first composition rather than waiting for a real state change.
            LaunchedEffect(currentUser) {
                val destination = if (currentUser != null) Route.Main else Route.Login
                navController.navigate(destination) {
                    popUpTo(Route.Splash) { inclusive = true }
                }
            }
        }

        composable<Route.Login> {
            LaunchedEffect(currentUser) {
                if (currentUser != null) {
                    navController.navigate(Route.Main) { popUpTo(Route.Login) { inclusive = true } }
                }
            }
            LoginScreen(onNavigateToSignUp = { navController.navigate(Route.SignUp) })
        }

        composable<Route.SignUp> {
            LaunchedEffect(currentUser) {
                if (currentUser != null) {
                    navController.navigate(Route.Main) { popUpTo(Route.Splash) { inclusive = true } }
                }
            }
            SignUpScreen(onNavigateToLogin = { navController.popBackStack() })
        }

        composable<Route.Main> {
            LaunchedEffect(currentUser) {
                if (currentUser == null) {
                    navController.navigate(Route.Login) { popUpTo(Route.Main) { inclusive = true } }
                }
            }
            // Stands in for the real tab shell (Map/Chat/Assistant/Diagnostics/
            // Settings) until later phases build it out.
            HomeScreen(
                userDisplayName = currentUser?.displayNameOrFallback,
                onViewSightings = { navController.navigate(Route.Feed) },
                onAddSighting = { navController.navigate(Route.AddSighting) },
                onSignOut = authViewModel::signOut,
            )
        }

        composable<Route.Feed> {
            FeedScreen(onAddSighting = { navController.navigate(Route.AddSighting) })
        }

        composable<Route.AddSighting> { backStackEntry ->
            val viewModel: AddSightingViewModel = hiltViewModel(backStackEntry)
            AddSightingScreen(
                viewModel = viewModel,
                onDone = { navController.popBackStack() },
                onNavigateToCamera = { navController.navigate(Route.CameraCapture) },
            )
        }

        composable<Route.CameraCapture> { backStackEntry ->
            // Scoped to AddSighting's own back stack entry so both screens share
            // the same ViewModel instance - a captured photo needs to land back
            // in the form's photo list without a separate result-passing channel.
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Route.AddSighting) }
            val addSightingViewModel: AddSightingViewModel = hiltViewModel(parentEntry)
            CameraCaptureScreen(
                onPhotoCaptured = { uri ->
                    addSightingViewModel.addPhoto(uri)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() },
            )
        }
    }
}
