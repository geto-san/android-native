package com.wildwatch.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wildwatch.app.core.data.user.UserDataRepository
import com.wildwatch.app.core.notifications.NotificationPayload
import com.wildwatch.app.core.notifications.WildWatchMessagingService
import com.wildwatch.app.ui.nav.Route
import com.wildwatch.app.ui.nav.WildWatchNavHost
import com.wildwatch.app.ui.nav.routeForNotification
import com.wildwatch.app.core.ui.theme.WildWatchTheme
import com.wildwatch.app.feature.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userDataRepository: UserDataRepository

    private val authViewModel: AuthViewModel by viewModels()

    // Mutable (not just computed once in onCreate) because launchMode="singleTask" means a
    // second notification tap or emailed sign-in link tap while the app is already running
    // arrives via onNewIntent(), not a fresh onCreate() - see AndroidManifest's App Link
    // intent-filter comment for why MainActivity needs singleTask at all.
    private var pendingRoute by mutableStateOf<Route?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var uiState: Boolean by mutableStateOf(true)

        // Keep the splash screen on-screen until we have a user state (or timeout/null)
        // This achieves the "WhatsApp" startup effect.
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.currentUser
                    .onEach {
                        uiState = false
                    }
                    .collect()
            }
        }

        splashScreen.setKeepOnScreenCondition {
            uiState
        }

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        handleIntent(intent)

        setContent {
            val darkThemeConfig by userDataRepository.darkThemeConfig.collectAsStateWithLifecycle(initialValue = null)
            val useDarkTheme = darkThemeConfig ?: isSystemInDarkTheme()

            // enableEdgeToEdge() above only picks status-bar-icon contrast from system config
            // at launch, which can disagree with the app's actual resolved theme (an in-app
            // override beats system dark mode). Keep icon contrast reactive to the real theme
            // instead, while leaving the bar itself transparent/edge-to-edge as already set.
            val view = LocalView.current
            SideEffect {
                WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars = !useDarkTheme
            }

            WildWatchTheme(darkTheme = useDarkTheme) {
                WildWatchNavHost(pendingRoute = pendingRoute)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    // Two things can arrive here: a tapped push notification (extras-based, handled via
    // pendingRoute -> WildWatchNavHost) or a tapped Firebase email sign-in link (a plain
    // https URL in intent.data - handled directly against AuthViewModel, since completing
    // sign-in doesn't need a route, just needs currentUser to update; WildWatchNavHost's own
    // auth-state effect takes care of navigating away from the Auth screen once it does).
    private fun handleIntent(intent: Intent) {
        val link = intent.dataString
        if (link != null && authViewModel.isEmailSignInLink(link)) {
            authViewModel.completeEmailLinkSignIn(link)
            return
        }

        val notificationType = NotificationPayload.parseType(
            intent.getStringExtra(WildWatchMessagingService.EXTRA_NOTIFICATION_TYPE),
        )
        val notificationTargetId = intent.getStringExtra(WildWatchMessagingService.EXTRA_NOTIFICATION_TARGET_ID)
        pendingRoute = routeForNotification(notificationType, notificationTargetId)
    }
}
