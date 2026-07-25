package com.wildwatch.app.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wildwatch.app.ui.components.PillButton
import com.wildwatch.app.ui.components.WildWatchLogoMark
import com.wildwatch.app.ui.theme.Cream
import com.wildwatch.app.ui.theme.ForestGreen
import com.wildwatch.app.ui.theme.ForestGreenGlow

// wireframe 1: the full-bleed marketing splash shown to signed-out users
// before they choose Register ("Get Started") or Sign In.
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onAlreadyHaveAccount: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ForestGreen,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(ForestGreen, ForestGreenGlow)))
                .safeDrawingPadding()
                .padding(PaddingValues(horizontal = 32.dp, vertical = 48.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            WildWatchLogoMark(size = 96.dp)

            Text(
                text = "Wildwatch",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = Cream,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = "Protecting wildlife through community-powered reporting and ranger response.",
                style = MaterialTheme.typography.bodyLarge,
                color = Cream.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            PillButton(
                text = "Get Started",
                onClick = onGetStarted,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                containerColor = Cream,
                contentColor = ForestGreen,
            )
            TextButton(onClick = onAlreadyHaveAccount) {
                Text("I already have an account", color = Cream)
            }
        }
    }
}
