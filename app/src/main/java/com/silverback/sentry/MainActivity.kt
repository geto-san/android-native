package com.silverback.sentry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.silverback.sentry.ui.nav.SentryNavHost
import com.silverback.sentry.ui.theme.SilverBackSentryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SilverBackSentryTheme {
                SentryNavHost()
            }
        }
    }
}
