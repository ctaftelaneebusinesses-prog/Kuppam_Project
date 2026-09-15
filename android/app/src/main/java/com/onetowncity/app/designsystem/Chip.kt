package com.onetowncity.app.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Multi-select chip, wrapped with a minimum 48dp touch target even though its visual chip stays Material's compact size. */
@Composable
fun OneTownCityChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, style = MaterialTheme.typography.labelLarge) },
        modifier = modifier.minimumInteractiveComponentSize(),
        leadingIcon = if (selected) {
            { Icon(imageVector = OneTownCityIcons.check, contentDescription = null) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
            enabled = true,
            selected = selected,
        ),
    )
}

/** Multi-select chip group that wraps to multiple rows — for filters where more than one value can be active at once. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OneTownCityChipGroup(
    items: List<String>,
    selected: Set<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(OneTownCitySpacing.sm),
        verticalArrangement = Arrangement.spacedBy(OneTownCitySpacing.sm),
    ) {
        items.forEach { item ->
            OneTownCityChip(
                text = item,
                selected = selected.contains(item),
                onClick = { onSelected(item) },
            )
        }
    }
}

/**
 * Single-select, horizontally scrolling category filter row — the pattern
 * every browse screen (Business/Property/Project/Students category, etc.)
 * needs, standardized in one place instead of each screen wiring its own
 * "All" + chips list. `null` selection means "All".
 */
@Composable
fun OneTownCityFilterBar(
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    allLabel: String = "All",
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(OneTownCitySpacing.sm),
        contentPadding = PaddingValues(horizontal = OneTownCitySpacing.none),
    ) {
        item(key = "__all__") {
            OneTownCityChip(
                text = allLabel,
                selected = selectedOption == null,
                onClick = { onOptionSelected(null) },
            )
        }
        items(options, key = { it }) { option ->
            OneTownCityChip(
                text = option,
                selected = selectedOption == option,
                onClick = { onOptionSelected(option) },
            )
        }
    }
}
