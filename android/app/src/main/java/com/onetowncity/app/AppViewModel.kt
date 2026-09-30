package com.onetowncity.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.onetowncity.app.core.data.auth.AccountRepository
import com.onetowncity.app.core.data.auth.AccountStatus
import com.onetowncity.app.core.data.auth.SignInFailure
import com.onetowncity.app.core.data.auth.SignInResult
import com.onetowncity.app.core.data.auth.SignInService
import com.onetowncity.app.feature.auth.SignInError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** The screen the app is on. Each step of onboarding (location → sign-in → consent) leads to [Home] in that order. */
sealed interface AppScreen {
    data object Launching : AppScreen
    data object Location : AppScreen
    data class SignIn(val busy: Boolean = false, val error: SignInError? = null) : AppScreen
    data class Consent(val busy: Boolean = false, val saveFailed: Boolean = false) : AppScreen
    data object Blocked : AppScreen
    data object Problem : AppScreen
    data object Home : AppScreen
}

sealed interface AppEffect {
    /** Open this URL in a browser tab (Google sign-in). */
    data class OpenBrowser(val url: String) : AppEffect
}

/**
 * Decides which onboarding screen to show. Every rule about the account itself (is it blocked, has it consented) is
 * answered by the server through [AccountRepository]; this class only routes on those answers.
 */
class AppViewModel(
    private val accounts: AccountRepository,
    private val signIn: SignInService,
    private val prefs: OnboardingPrefs,
) : ViewModel() {
    private val _screen = MutableStateFlow<AppScreen>(AppScreen.Launching)
    val screen: StateFlow<AppScreen> = _screen.asStateFlow()

    private val _effects = Channel<AppEffect>(Channel.BUFFERED)
    val effects: Flow<AppEffect> = _effects.receiveAsFlow()

    init {
        if (prefs.locationStepDone) refreshAccount() else _screen.value = AppScreen.Location
    }

    fun onLocationFinished() {
        prefs.locationStepDone = true
        refreshAccount()
    }

    fun onContinueWithGoogle() {
        if (!signIn.isConfigured) {
            _screen.value = AppScreen.SignIn(error = SignInError.NotConfigured)
            return
        }
        _screen.value = AppScreen.SignIn(busy = true)
        _effects.trySend(AppEffect.OpenBrowser(signIn.beginSignIn()))
    }

    /** The browser handed a `onetowncity://auth/callback…` link back to the app. */
    fun onAuthRedirect(url: String) {
        viewModelScope.launch {
            _screen.value = AppScreen.SignIn(busy = true)
            when (val result = signIn.completeSignIn(url)) {
                SignInResult.Success -> refreshAccountNow()
                is SignInResult.Failure -> _screen.value = AppScreen.SignIn(error = result.reason.toUiError())
            }
        }
    }

    /** The user came back from the browser without finishing (pressed Back). */
    fun onBrowserDismissed() {
        val current = _screen.value
        if (current is AppScreen.SignIn && current.busy) _screen.value = AppScreen.SignIn(error = SignInError.Cancelled)
    }

    fun onConfirmConsent() {
        viewModelScope.launch {
            _screen.value = AppScreen.Consent(busy = true)
            when (val status = accounts.confirmConsent()) {
                is AccountStatus.Ready, AccountStatus.Blocked, AccountStatus.SignedOut -> route(status)
                AccountStatus.NeedsConsent, is AccountStatus.Failed -> _screen.value = AppScreen.Consent(saveFailed = true)
            }
        }
    }

    fun onSignOut() {
        accounts.signOut()
        _screen.value = AppScreen.SignIn()
    }

    fun retry() = refreshAccount()

    private fun refreshAccount() {
        _screen.value = AppScreen.Launching
        viewModelScope.launch { refreshAccountNow() }
    }

    private suspend fun refreshAccountNow() = route(accounts.status())

    private fun route(status: AccountStatus) {
        _screen.value = when (status) {
            AccountStatus.SignedOut -> AppScreen.SignIn()
            AccountStatus.NeedsConsent -> AppScreen.Consent()
            is AccountStatus.Ready -> AppScreen.Home
            AccountStatus.Blocked -> AppScreen.Blocked
            is AccountStatus.Failed -> AppScreen.Problem
        }
    }

    private fun SignInFailure.toUiError() = when (this) {
        SignInFailure.Provider -> SignInError.Cancelled
        SignInFailure.InvalidRedirect, SignInFailure.Rejected -> SignInError.Rejected
        SignInFailure.Network -> SignInError.Network
    }

    companion object {
        fun factory(accounts: AccountRepository, signIn: SignInService, prefs: OnboardingPrefs): ViewModelProvider.Factory =
            viewModelFactory { initializer { AppViewModel(accounts, signIn, prefs) } }
    }
}
