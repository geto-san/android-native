package com.wildwatch.app.feature.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wildwatch.app.core.ui.component.WildWatchLogoMark
import com.wildwatch.app.core.ui.theme.ForestGreen
import com.wildwatch.app.core.ui.theme.ForestGreenGlow
import com.wildwatch.app.core.ui.theme.MagilioFontFamily
import com.wildwatch.app.core.ui.theme.White

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onAlreadyHaveAccount: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(ForestGreen, ForestGreenGlow.copy(alpha = 0.8f))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.fillMaxHeight(0.2f))

            WildWatchLogoMark(size = 100.dp, color = White)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "WildWatch",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = MagilioFontFamily,
                    color = White
                ),
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "COMMUNITY",
                style = MaterialTheme.typography.labelLarge,
                color = White.copy(alpha = 0.7f),
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Protecting wildlife through community reporting and real-time alerts.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                color = White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = White,
                    contentColor = ForestGreen
                )
            ) {
                Text("Get Started", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onAlreadyHaveAccount) {
                Text(
                    text = "I already have an account",
                    color = White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
