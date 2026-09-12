package com.onetowncity.app.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/**
 * "Clearly indicate that cached/offline data is being shown" (offline-first
 * spec) — a small, unmissable-but-not-alarming banner, never a silent
 * substitution of stale content for live content.
 */
@Composable
fun OneTownCityCacheStatusBanner(
    message: String,
    modifier: Modifier = Modifier,
    isOffline: Boolean = false,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OneTownCityCornerRadii.md),
        color = if (isOffline) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = OneTownCitySpacing.md, vertical = OneTownCitySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OneTownCitySpacing.sm),
        ) {
            Icon(
                imageVector = if (isOffline) Icons.Filled.CloudOff else Icons.Filled.History,
                contentDescription = null,
                tint = if (isOffline) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.labelLarge,
                color = if (isOffline) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Formats a cache age into the short, human phrase the banner shows ("just now", "12 min ago", "3 hr ago"). */
fun formatCacheAge(ageMillis: Long): String {
    val minutes = ageMillis / 60000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 24 * 60 -> "${minutes / 60} hr ago"
        else -> "${minutes / (24 * 60)} d ago"
    }
}
