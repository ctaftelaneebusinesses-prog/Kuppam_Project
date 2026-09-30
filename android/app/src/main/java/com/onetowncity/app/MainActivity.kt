package com.onetowncity.app

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onetowncity.app.core.designsystem.theme.OneTownTheme
import com.onetowncity.app.feature.auth.BlockedScreen
import com.onetowncity.app.feature.auth.ConsentScreen
import com.onetowncity.app.feature.auth.ProblemScreen
import com.onetowncity.app.feature.auth.SignInScreen
import com.onetowncity.app.feature.home.BentoBoxDashboard
import com.onetowncity.app.feature.home.HomeViewModel
import com.onetowncity.app.feature.onboarding.LocationOnboarding
import kotlinx.coroutines.launch

/**
 * Single activity. Onboarding runs Location → Sign-in → (one-time consent) → Home; the browser returns from Google
 * sign-in through `onetowncity://auth/callback`, handled in [handleIntent].
 */
class MainActivity : ComponentActivity() {
    private val app get() = applicationContext as OneTownApplication

    private val appViewModel: AppViewModel by viewModels {
        AppViewModel.factory(app.accountRepository, app.signInService, app.onboardingPrefs)
    }

    /** True while a browser sign-in tab is open, so coming back without a redirect can be treated as "cancelled". */
    private var awaitingBrowser = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                appViewModel.effects.collect { effect ->
                    if (effect is AppEffect.OpenBrowser) {
                        awaitingBrowser = true
                        openInBrowser(effect.url)
                    }
                }
            }
        }
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            OneTownTheme {
                val screen by appViewModel.screen.collectAsStateWithLifecycle()
                when (val current = screen) {
                    AppScreen.Launching -> Unit // Black window while the account status loads.
                    AppScreen.Location -> LocationOnboarding(onFinished = { appViewModel.onLocationFinished() })
                    is AppScreen.SignIn -> SignInScreen(
                        busy = current.busy,
                        error = current.error,
                        onContinueWithGoogle = appViewModel::onContinueWithGoogle,
                    )
                    is AppScreen.Consent -> ConsentScreen(
                        busy = current.busy,
                        saveFailed = current.saveFailed,
                        onConfirm = appViewModel::onConfirmConsent,
                        onReadTerms = { openInBrowser(BuildConfig.API_BASE_URL + "terms-of-service/") },
                        onReadPrivacy = { openInBrowser(BuildConfig.API_BASE_URL + "privacy-policy/") },
                        onSignOut = appViewModel::onSignOut,
                    )
                    AppScreen.Blocked -> BlockedScreen(onSignOut = appViewModel::onSignOut)
                    AppScreen.Problem -> ProblemScreen(onRetry = appViewModel::retry)
                    AppScreen.Home -> {
                        val home: HomeViewModel = viewModel(factory = HomeViewModel.factory(app.categoryRepository))
                        val state by home.state.collectAsStateWithLifecycle()
                        BentoBoxDashboard(
                            state = state,
                            cityName = null,
                            onCategoryClick = {}, // Category screens do not exist yet.
                            onRetry = home::retry,
                            onSignOut = appViewModel::onSignOut,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Resumed without a redirect after opening the browser = the user backed out of the sign-in tab.
        if (awaitingBrowser && intent?.data == null) appViewModel.onBrowserDismissed()
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "onetowncity" && data.host == "auth") {
            awaitingBrowser = false
            appViewModel.onAuthRedirect(data.toString())
        }
    }

    private fun openInBrowser(url: String) {
        CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse(url))
    }
}
