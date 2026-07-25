package com.wildwatch.app.feature.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wildwatch.app.core.ui.component.WildWatchLogoMark
import com.wildwatch.app.core.ui.theme.Cream
import com.wildwatch.app.core.ui.theme.Grey500

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onAlreadyHaveAccount: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            WildWatchLogoMark(size = 80.dp)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "WildWatch",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                "Protecting wildlife through community reporting and real-time alerts.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                color = Grey500
            )

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = MaterialTheme.shapes.small
            ) {
                WildWatchLogoMark(size = 120.dp)

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    "WildWatch",
                    style = MaterialTheme.typography.displayMedium,
                    color = Cream,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "COMMUNITY",
                    style = MaterialTheme.typography.labelLarge,
                    color = Cream.copy(alpha = 0.7f),
                    letterSpacing = 4.sp
                )

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    "Log In",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
