package com.silversentry.sentry

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.silversentry.sentry.core.data.user.UserDataRepository
import com.silversentry.sentry.core.notifications.NotificationPayload
import com.silversentry.sentry.core.notifications.SilverBackSentryMessagingService
import com.silversentry.sentry.ui.nav.Route
import com.silversentry.sentry.ui.nav.SilverBackSentryNavHost
import com.silversentry.sentry.ui.nav.routeForNotification
import com.silversentry.sentry.core.ui.theme.SilverBackSentryTheme
import com.silversentry.sentry.feature.auth.AuthViewModel
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

            SilverBackSentryTheme(darkTheme = useDarkTheme) {
                RequestNotificationPermissionOnLaunch()
                SilverBackSentryNavHost(pendingRoute = pendingRoute)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    // Two things can arrive here: a tapped push notification (extras-based, handled via
    // pendingRoute -> SilverBackSentryNavHost) or a tapped Firebase email sign-in link (a plain
    // https URL in intent.data - handled directly against AuthViewModel, since completing
    // sign-in doesn't need a route, just needs currentUser to update; SilverBackSentryNavHost's own
    // auth-state effect takes care of navigating away from the Auth screen once it does).
    private fun handleIntent(intent: Intent) {
        val link = intent.dataString
        if (link != null && authViewModel.isEmailSignInLink(link)) {
            authViewModel.completeEmailLinkSignIn(link)
            return
        }

        val notificationType = NotificationPayload.parseType(
            intent.getStringExtra(SilverBackSentryMessagingService.EXTRA_NOTIFICATION_TYPE),
        )
        val notificationTargetId = intent.getStringExtra(SilverBackSentryMessagingService.EXTRA_NOTIFICATION_TARGET_ID)
        pendingRoute = routeForNotification(notificationType, notificationTargetId)
    }
}

// Android 13+ requires an explicit runtime grant before any notification can be
// displayed - without this, a fresh install silently has notifications disabled
// even though the manifest declares POST_NOTIFICATIONS. Request it once at first
// launch (rememberSaveable survives recomposition; the system remembers a
// permanent denial itself, so we just don't nag past the first ask).
@androidx.compose.runtime.Composable
private fun RequestNotificationPermissionOnLaunch() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var requested by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result handled implicitly by the OS; no follow-up needed */ }
    LaunchedEffect(Unit) {
        if (!requested && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requested = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
