package com.silverback.sentry.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Park
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.silverback.sentry.ui.components.WildwatchGradients
import com.silverback.sentry.ui.theme.ForestPrimaryGlow
import com.silverback.sentry.ui.theme.SavannaAccent
import com.silverback.sentry.ui.theme.SilverBackSentryTheme

/**
 * Screen 1/18 — Wildwatch splash (source: wildwatch.zip `src/routes/index.tsx`).
 *
 * Design note / deliberate deviation from the wireframe: in the wireframe this
 * screen is a static landing page with "Get Started" / "I already have an
 * account" buttons that both navigate to /auth. In this app, SplashScreen is
 * shown only for the brief moment while [com.silverback.sentry.ui.auth.AuthViewModel]
 * resolves whether a Firebase session already exists (see SentryNavHost), then
 * auto-navigates to Login or Main - it is not a screen the user lingers on or
 * taps through. So the branded visual (forest gradient, glow accents, logo,
 * tagline) is preserved but the CTA buttons are dropped in favor of the loading
 * indicator, since the immediate auto-navigation makes them dead UI. The actual
 * "Get Started" / "Sign in" choice now lives on the Login screen (next screen).
 * The wireframe's floating "UWA Web Portal" button is also omitted here per your
 * instruction to ignore the web-portal part of the design.
 */
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WildwatchGradients.forest()),
        contentAlignment = Alignment.Center,
    ) {
        // Soft glow accents, ported from the wireframe's blurred absolute-positioned
        // circles (bg-accent/30 blur-3xl top-right, bg-primary-glow/40 blur-3xl
        // bottom-left).
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 80.dp)
                .size(320.dp)
                .blur(80.dp)
                .background(SavannaAccent.copy(alpha = 0.3f), CircleShape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-60).dp, y = (-60).dp)
                .size(240.dp)
                .blur(80.dp)
                .background(ForestPrimaryGlow.copy(alpha = 0.4f), CircleShape),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Park,
                    contentDescription = null,
                    tint = SavannaAccent,
                    modifier = Modifier.size(48.dp),
                )
            }

            Text(
                text = "Wildwatch",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = "Protecting wildlife through community-powered reporting and ranger response.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 12.dp),
            )

            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier
                    .padding(top = 40.dp)
                    .size(28.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    SilverBackSentryTheme {
        SplashScreen()
    }
}