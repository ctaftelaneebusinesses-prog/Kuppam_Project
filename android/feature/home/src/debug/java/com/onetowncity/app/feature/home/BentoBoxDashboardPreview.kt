package com.onetowncity.app.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.onetowncity.app.core.designsystem.theme.OneTownTheme

// Debug-only: layout preview with placeholder labels. This file is not part of the release build and
// nothing here is ever shown to users — real categories come from GET /api/v1/categories/.
private val previewLabels = listOf(
    "Preview A" to "bi-house-door", "Preview B" to "bi-shop", "Preview C" to "bi-briefcase",
    "Preview D" to "bi-calendar-event", "Preview E" to "bi-cup-hot", "Preview F" to "bi-hospital",
    "Preview G" to "bi-bus-front", "Preview H" to "bi-search-heart",
)

private fun previewTiles() = previewLabels.mapIndexed { index, (label, icon) ->
    CategoryTile(key = "preview-$index", label = label, iconKey = icon, span = spanForPosition(index))
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 390, heightDp = 800)
@Composable
private fun DashboardContentPreview() = OneTownTheme {
    BentoBoxDashboard(HomeUiState.Content(previewTiles()), cityName = "Preview City", onCategoryClick = {}, onRetry = {})
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 390, heightDp = 800)
@Composable
private fun DashboardLoadingPreview() = OneTownTheme {
    BentoBoxDashboard(HomeUiState.Loading, cityName = null, onCategoryClick = {}, onRetry = {})
}
