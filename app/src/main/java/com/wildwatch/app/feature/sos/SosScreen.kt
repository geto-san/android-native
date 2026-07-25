package com.wildwatch.app.feature.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wildwatch.app.core.ui.theme.Destructive
import com.wildwatch.app.core.ui.theme.Grey500

@Composable
fun SosScreen(viewModel: SosViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Emergency SOS",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "One-tap ranger dispatch",
                style = MaterialTheme.typography.bodyMedium,
                color = Grey500
            )

            Spacer(modifier = Modifier.height(48.dp))

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(
                        if (uiState.isSent) Color.Green.copy(alpha = 0.1f) else Destructive.copy(alpha = 0.1f), 
                        CircleShape
                    )
                    .clickable(enabled = !uiState.isSending) {
                        viewModel.triggerSos()
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(if (uiState.isSent) Color.Green else Destructive, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.isSending) "..." else if (uiState.isSent) "SENT" else "SOS",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                "Press to alert Bwindi Impenetrable rangers with your live location.",
                style = MaterialTheme.typography.bodySmall,
                color = Grey500,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            if (uiState.isSent) {
                TextButton(onClick = viewModel::reset, modifier = Modifier.padding(top = 24.dp)) {
                    Text("Reset SOS", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
