package com.silverback.sentry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.silverback.sentry.ui.auth.AuthViewModel
import com.silverback.sentry.ui.nav.SentryNavHost
import com.silverback.sentry.ui.theme.SilverBackSentryTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen() must run before super.onCreate(): it reads the
        // windowSplashScreenBackground/AnimatedIcon set on this activity's manifest
        // theme (Theme.SilverBackSentry.Starting) and takes over drawing them
        // natively at cold start. Without this call, Android 12+ still shows its own
        // default splash first (launcher icon centered on a plain background) before
        // the app's first Compose frame - which is the "double splash" this fixes.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        enableEdgeToEdge()
        setContent {
            SilverBackSentryTheme {
                val authViewModel: AuthViewModel = hiltViewModel()

                // Release the OS splash only once we actually know whether a user is
                // signed in - not after a fixed delay. AuthRepositoryImpl's
                // currentUser StateFlow already carries a synchronously-resolved
                // initial value (FirebaseAuth's local cache, no network wait), so in
                // practice this fires on the very first frame: the same "on screen
                // only as long as genuinely needed" duration a default splash has,
                // just using our branded design instead of the generic one. If
                // currentUser resolution ever becomes asynchronous (e.g. a future
                // silent token refresh), this keeps working correctly without change.
                LaunchedEffect(Unit) {
                    authViewModel.currentUser.first()
                    keepSplashOnScreen = false
                }

                // By the time the line above releases the splash, SentryNavHost below
                // has already composed Route.Splash and (via its own
                // LaunchedEffect(currentUser)) navigated on to Login or Main - so the
                // OS splash dismisses directly into that real first screen. The
                // in-app SplashScreen composable is still reachable as a route, but
                // in practice it's never visible on top of the OS splash; it only
                // shows if navigation is ever slow enough for a frame to be visible.
                SentryNavHost()
            }
        }
    }
}
