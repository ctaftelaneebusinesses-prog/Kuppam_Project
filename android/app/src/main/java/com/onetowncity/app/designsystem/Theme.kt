package com.onetowncity.app.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Root theme — light/dark ColorScheme, the OneTownCity type scale, and rounded-corner Shapes, in one place. */
@Composable
fun OneTownCityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) OneTownCityColors.Dark else OneTownCityColors.Light
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = OneTownCityTypography.default,
        shapes = OneTownCityShapes,
        content = content,
    )
}

@Composable
fun OneTownCityScreenContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        content()
    }
}

/** Exposes the current window width class so a screen can switch column counts / master-detail layout at Medium+ breakpoints instead of hardcoding dp breakpoints. */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun OneTownCityWindowSizeAware(
    activity: androidx.activity.ComponentActivity,
    content: @Composable (WindowWidthSizeClass) -> Unit,
) {
    val widthClass = calculateWindowSizeClass(activity = activity).widthSizeClass
    content(widthClass)
}
