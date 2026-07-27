package com.wildwatch.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.wildwatch.app.core.data.user.UserDataRepository
import com.wildwatch.app.ui.nav.WildWatchNavHost
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

        setContent {
            val darkThemeConfig by userDataRepository.darkThemeConfig.collectAsStateWithLifecycle(initialValue = null)
            val useDarkTheme = darkThemeConfig ?: isSystemInDarkTheme()

            WildWatchTheme(darkTheme = useDarkTheme) {
                WildWatchNavHost()
            }
        }
    }
}
