package com.onetowncity.app

import com.onetowncity.app.core.data.LoadFailure
import com.onetowncity.app.core.data.auth.AccountRepository
import com.onetowncity.app.core.data.auth.AccountStatus
import com.onetowncity.app.core.data.auth.SignInFailure
import com.onetowncity.app.core.data.auth.SignInResult
import com.onetowncity.app.core.data.auth.SignInService
import com.onetowncity.app.feature.auth.SignInError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private class FakePrefs(override var locationStepDone: Boolean = false) : OnboardingPrefs

    private class FakeAccounts(var status: AccountStatus, var afterConsent: AccountStatus = AccountStatus.Ready("Asha")) : AccountRepository {
        var signedOut = false
        override suspend fun status() = status
        override suspend fun confirmConsent() = afterConsent
        override fun signOut() { signedOut = true; status = AccountStatus.SignedOut }
    }

    private class FakeSignIn(
        override val isConfigured: Boolean = true,
        var result: SignInResult = SignInResult.Success,
    ) : SignInService {
        var begun = 0
        override fun beginSignIn(): String { begun++; return "https://auth.example/authorize" }
        override suspend fun completeSignIn(redirect: String) = result
        override fun signOut() = Unit
    }

    private fun vm(prefs: OnboardingPrefs, accounts: FakeAccounts, signIn: FakeSignIn = FakeSignIn()) =
        AppViewModel(accounts, signIn, prefs)

    @Test fun firstLaunchStartsWithTheLocationStep() {
        assertEquals(AppScreen.Location, vm(FakePrefs(false), FakeAccounts(AccountStatus.SignedOut)).screen.value)
    }

    @Test fun finishingLocationRemembersItAndMovesToSignIn() = runTest(dispatcher) {
        val prefs = FakePrefs(false)
        val model = vm(prefs, FakeAccounts(AccountStatus.SignedOut))
        model.onLocationFinished()
        advanceUntilIdle()
        assertTrue(prefs.locationStepDone)
        assertEquals(AppScreen.SignIn(), model.screen.value)
    }

    @Test fun laterLaunchesSkipLocationAndGoStraightToHomeWhenSignedIn() = runTest(dispatcher) {
        val model = vm(FakePrefs(true), FakeAccounts(AccountStatus.Ready("Asha")))
        assertEquals(AppScreen.Launching, model.screen.value)
        advanceUntilIdle()
        assertEquals(AppScreen.Home, model.screen.value)
    }

    @Test fun anUnconfirmedAccountMustConsentBeforeHome() = runTest(dispatcher) {
        val model = vm(FakePrefs(true), FakeAccounts(AccountStatus.NeedsConsent))
        advanceUntilIdle()
        assertEquals(AppScreen.Consent(), model.screen.value)

        model.onConfirmConsent()
        advanceUntilIdle()
        assertEquals(AppScreen.Home, model.screen.value)
    }

    @Test fun aFailedConfirmationStaysOnConsentWithAnError() = runTest(dispatcher) {
        val accounts = FakeAccounts(AccountStatus.NeedsConsent, afterConsent = AccountStatus.Failed(LoadFailure.Network))
        val model = vm(FakePrefs(true), accounts)
        advanceUntilIdle()
        model.onConfirmConsent()
        advanceUntilIdle()
        assertEquals(AppScreen.Consent(saveFailed = true), model.screen.value)
    }

    @Test fun blockedAndUnreachableAccountsGetTheirOwnScreens() = runTest(dispatcher) {
        val blocked = vm(FakePrefs(true), FakeAccounts(AccountStatus.Blocked)); advanceUntilIdle()
        assertEquals(AppScreen.Blocked, blocked.screen.value)
        val offline = vm(FakePrefs(true), FakeAccounts(AccountStatus.Failed(LoadFailure.Network))); advanceUntilIdle()
        assertEquals(AppScreen.Problem, offline.screen.value)
    }

    @Test fun continuingWithGoogleOpensTheBrowserAndShowsBusy() = runTest(dispatcher) {
        val signIn = FakeSignIn()
        val model = vm(FakePrefs(true), FakeAccounts(AccountStatus.SignedOut), signIn)
        advanceUntilIdle()

        model.onContinueWithGoogle()

        assertEquals(AppScreen.SignIn(busy = true), model.screen.value)
        assertEquals(AppEffect.OpenBrowser("https://auth.example/authorize"), model.effects.first())
        assertEquals(1, signIn.begun)
    }

    @Test fun anUnconfiguredBuildExplainsInsteadOfOpeningABrowser() = runTest(dispatcher) {
        val signIn = FakeSignIn(isConfigured = false)
        val model = vm(FakePrefs(true), FakeAccounts(AccountStatus.SignedOut), signIn)
        advanceUntilIdle()
        model.onContinueWithGoogle()
        assertEquals(AppScreen.SignIn(error = SignInError.NotConfigured), model.screen.value)
        assertEquals(0, signIn.begun)
    }

    @Test fun aSuccessfulRedirectRoutesOnTheServersAnswer() = runTest(dispatcher) {
        val accounts = FakeAccounts(AccountStatus.SignedOut)
        val model = vm(FakePrefs(true), accounts)
        advanceUntilIdle()
        accounts.status = AccountStatus.NeedsConsent

        model.onAuthRedirect("onetowncity://auth/callback?code=x")
        advanceUntilIdle()

        assertEquals(AppScreen.Consent(), model.screen.value)
    }

    @Test fun signInFailuresMapToFriendlyErrors() = runTest(dispatcher) {
        val cases = mapOf(
            SignInFailure.Provider to SignInError.Cancelled,
            SignInFailure.Rejected to SignInError.Rejected,
            SignInFailure.InvalidRedirect to SignInError.Rejected,
            SignInFailure.Network to SignInError.Network,
        )
        for ((failure, expected) in cases) {
            val model = vm(FakePrefs(true), FakeAccounts(AccountStatus.SignedOut), FakeSignIn(result = SignInResult.Failure(failure)))
            advanceUntilIdle()
            model.onAuthRedirect("onetowncity://auth/callback?code=x")
            advanceUntilIdle()
            assertEquals("for $failure", AppScreen.SignIn(error = expected), model.screen.value)
        }
    }

    @Test fun backingOutOfTheBrowserIsReportedAsCancelled() = runTest(dispatcher) {
        val model = vm(FakePrefs(true), FakeAccounts(AccountStatus.SignedOut))
        advanceUntilIdle()
        model.onContinueWithGoogle()
        model.onBrowserDismissed()
        assertEquals(AppScreen.SignIn(error = SignInError.Cancelled), model.screen.value)
    }

    @Test fun signingOutReturnsToSignIn() = runTest(dispatcher) {
        val accounts = FakeAccounts(AccountStatus.Ready("Asha"))
        val model = vm(FakePrefs(true), accounts)
        advanceUntilIdle()
        model.onSignOut()
        assertTrue(accounts.signedOut)
        assertEquals(AppScreen.SignIn(), model.screen.value)
    }
}
