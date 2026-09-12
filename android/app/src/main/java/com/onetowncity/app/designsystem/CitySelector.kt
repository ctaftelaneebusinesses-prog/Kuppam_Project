package com.onetowncity.app.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/**
 * The persistent "which city am I browsing" affordance the Phase 1 audit
 * calls for on every screen, not just Home — a compact pill showing the
 * current city (or a neutral placeholder before one is chosen) that opens
 * the city picker on tap. Stateless: the caller owns city selection/
 * permission/recent-cities logic (see MainActivity's existing city handling)
 * and only passes the resolved name + a click callback here.
 */
@Composable
fun OneTownCityCityBar(
    cityName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select a city",
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OneTownCityCornerRadii.pill),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = OneTownCityTouchTarget.minSize)
                .padding(horizontal = OneTownCitySpacing.lg, vertical = OneTownCitySpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(OneTownCitySpacing.sm),
        ) {
            Icon(
                imageVector = OneTownCityIcons.location,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = cityName ?: placeholder,
                style = MaterialTheme.typography.titleSmall,
                color = if (cityName != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

data class OneTownCityCitySuggestion(val slug: String, val name: String)

/**
 * A row of quick city picks (recent cities or search suggestions) — the same
 * "up to 3, sharing the row equally" pattern MainActivity's city picker
 * already uses, generalized here so it isn't reimplemented per screen.
 */
@Composable
fun OneTownCityCitySuggestionRow(
    suggestions: List<OneTownCityCitySuggestion>,
    onSelect: (OneTownCityCitySuggestion) -> Unit,
    modifier: Modifier = Modifier,
    selectedSlug: String? = null,
    maxVisible: Int = 3,
) {
    if (suggestions.isEmpty()) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(OneTownCitySpacing.sm),
    ) {
        suggestions.take(maxVisible).forEach { suggestion ->
            OneTownCityButton(
                text = suggestion.name,
                onClick = { onSelect(suggestion) },
                variant = if (suggestion.slug == selectedSlug) OneTownCityButtonVariant.Secondary else OneTownCityButtonVariant.Outlined,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
