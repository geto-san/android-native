package com.silverback.sentry.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Direct port of `ScreenHeader` from ui-prototype.tsx: a title + optional
 * subtitle, optional back button, optional trailing content, and a `dark`
 * variant used for screens with a forest-gradient header band (e.g. Auth,
 * Community Home). Every converted screen below the splash/auth flow reuses
 * this rather than a bespoke TopAppBar.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    dark: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    val titleColor = if (dark) Color.White else MaterialTheme.colorScheme.onBackground
    val subtitleColor = if (dark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val backButtonBackground = if (dark) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .background(backButtonBackground, CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = titleColor,
                    )
                }
                Box(modifier = Modifier.size(12.dp))
            }
            Column {
                Text(text = title, style = MaterialTheme.typography.titleLarge, color = titleColor)
                if (subtitle != null) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = subtitleColor)
                }
            }
        }
        if (trailing != null) {
            Row(horizontalArrangement = Arrangement.End) { trailing() }
        }
    }
}