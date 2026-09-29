package com.onetowncity.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.onetowncity.app.core.designsystem.theme.OneTownTheme
import com.onetowncity.app.feature.home.BentoBoxDashboard
import com.onetowncity.app.feature.home.HomeUiState
import com.onetowncity.app.feature.onboarding.LocationOnboarding

/**
 * Temporary host for the first two screens. Real navigation (splash → location → sign-in → home) and the data layer that
 * turns GET /api/v1/categories/ into [HomeUiState.Content] come next; until then the dashboard honestly shows its
 * loading state instead of made-up categories.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            OneTownTheme {
                var onboardingDone by rememberSaveable { mutableStateOf(false) }
                if (!onboardingDone) {
                    LocationOnboarding(onFinished = { onboardingDone = true })
                } else {
                    BentoBoxDashboard(
                        state = HomeUiState.Loading,
                        cityName = null,
                        onCategoryClick = {},
                        onRetry = {},
                    )
                }
            }
        }
    }
}
