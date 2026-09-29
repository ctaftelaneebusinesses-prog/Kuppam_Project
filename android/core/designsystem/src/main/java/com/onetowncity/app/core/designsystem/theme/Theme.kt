package com.onetowncity.app.core.designsystem.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import com.onetowncity.app.core.designsystem.components.SharpIndication

/** Entry point to the design tokens: `OneTownTheme.colors.accent`, `OneTownTheme.typography.sansBody`, … */
object OneTownTheme {
    val colors: OneTownColors
        @Composable @ReadOnlyComposable get() = LocalOneTownColors.current
    val typography: OneTownTypography
        @Composable @ReadOnlyComposable get() = LocalOneTownTypography.current
    val elevation: OneTownElevation
        @Composable @ReadOnlyComposable get() = LocalOneTownElevation.current
    val shapes: OneTownShapes
        @Composable @ReadOnlyComposable get() = LocalOneTownShapes.current
}

/**
 * Wraps content in the OneTownCity design system: provides every token, replaces the default
 * indication with [SharpIndication] and paints the true-black background behind everything.
 * v1 is dark-only by design.
 */
@Composable
fun OneTownTheme(content: @Composable () -> Unit) {
    val colors = DefaultOneTownColors
    CompositionLocalProvider(
        LocalOneTownColors provides colors,
        LocalOneTownTypography provides DefaultOneTownTypography,
        LocalOneTownElevation provides OneTownElevation(),
        LocalOneTownShapes provides OneTownShapes(),
        LocalIndication provides SharpIndication,
    ) {
        Box(Modifier.fillMaxSize().background(colors.background)) { content() }
    }
}
