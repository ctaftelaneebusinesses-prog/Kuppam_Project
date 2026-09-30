package com.onetowncity.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.onetowncity.app.core.designsystem.theme.OneTownTheme
import com.onetowncity.app.feature.home.BentoBoxDashboard
import com.onetowncity.app.feature.home.HomeViewModel
import com.onetowncity.app.feature.onboarding.LocationOnboarding

/**
 * Temporary host for the first two screens. Real navigation (splash → location → sign-in → home) comes next; for now
 * onboarding is followed directly by the dashboard, which loads the live categories from the backend.
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
                    val app = applicationContext as OneTownApplication
                    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(app.categoryRepository))
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    BentoBoxDashboard(
                        state = state,
                        cityName = null,
                        onCategoryClick = {}, // Category screens do not exist yet.
                        onRetry = viewModel::retry,
                    )
                }
            }
        }
    }
}
