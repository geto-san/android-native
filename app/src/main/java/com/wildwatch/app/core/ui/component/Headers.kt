package com.wildwatch.app.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wildwatch.app.core.ui.theme.Grey500

import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

@Composable
fun BackHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(48.dp) // Ensure explicit 48dp touch target
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, 
                contentDescription = "Navigate back"
            )
        }
        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f) // Added weight to allow actions to be pushed to the end
                .semantics { heading() } // Identify as a heading for screen readers
        ) {
            Text(
                title, 
                style = MaterialTheme.typography.titleLarge, // Increased for better hierarchy
                fontWeight = FontWeight.Bold
            )
            subtitle?.let {
                Text(
                    it, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = Grey500
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}
