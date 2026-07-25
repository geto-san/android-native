package com.wildwatch.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Two-option pill toggle: "Sign In / Register" and "Email / Phone" in the
// auth wireframes both use this exact shape - a tan track with a single
// colored pill that slides to whichever option is active.
@Composable
fun TwoOptionSegmentedControl(
    firstLabel: String,
    secondLabel: String,
    firstSelected: Boolean,
    onSelectFirst: () -> Unit,
    onSelectSecond: () -> Unit,
    modifier: Modifier = Modifier,
    firstIcon: ImageVector? = null,
    secondIcon: ImageVector? = null,
    trackColor: Color = MaterialTheme.colorScheme.secondary,
    optionVerticalPadding: Dp = 12.dp,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(trackColor, RoundedCornerShape(50))
            .padding(4.dp),
    ) {
        SegmentedOption(
            label = firstLabel,
            icon = firstIcon,
            selected = firstSelected,
            onClick = onSelectFirst,
            verticalPadding = optionVerticalPadding,
            modifier = Modifier.weight(1f),
        )
        SegmentedOption(
            label = secondLabel,
            icon = secondIcon,
            selected = !firstSelected,
            onClick = onSelectSecond,
            verticalPadding = optionVerticalPadding,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SegmentedOption(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    onClick: () -> Unit,
    verticalPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(50))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = verticalPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(it, contentDescription = null, tint = contentColor, modifier = Modifier.padding(end = 6.dp).size(18.dp))
        }
        Text(
            label,
            color = contentColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
