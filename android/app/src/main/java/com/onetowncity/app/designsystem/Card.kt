package com.onetowncity.app.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun OneTownCityCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OneTownCityCornerRadii.md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = OneTownCityElevation.low),
    ) {
        Box(modifier = Modifier.padding(OneTownCitySpacing.lg)) {
            content()
        }
    }
}

@Composable
fun OneTownCityImageContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(OneTownCityCornerRadii.lg))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = OneTownCityBorders.thin,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                shape = RoundedCornerShape(OneTownCityCornerRadii.lg),
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * The one card every listing browse grid/row (Business/Property/Project/
 * Event/Marketplace/etc.) should use instead of each screen hand-rolling its
 * own Surface+Column — replaces ~8 near-duplicate card implementations found
 * in the Phase 1 audit. No fixed height: only the image keeps a 16:9 aspect
 * ratio, and the text column below it wraps naturally so a longer title
 * never gets clipped.
 *
 * Image rendering is left to the caller via [imageContent] (so this file has
 * no dependency on the app's Coil/image-loading setup) — pass a fallback
 * icon via [icon] for when there's no image at all.
 */
@Composable
fun OneTownCityListingCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    metaText: String? = null,
    icon: ImageVector = OneTownCityIcons.location,
    imageContent: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OneTownCityCornerRadii.lg),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = OneTownCityElevation.low,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                if (imageContent != null) {
                    imageContent()
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(OneTownCitySpacing.md),
                verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.xs),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (metaText != null || trailing != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (metaText != null) {
                            Text(
                                text = metaText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        trailing?.invoke()
                    }
                }
            }
        }
    }
}
