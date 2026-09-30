package com.onetowncity.app

import android.app.Application
import com.onetowncity.app.core.data.CategoryRepository
import com.onetowncity.app.core.data.ListingRepository
import com.onetowncity.app.core.data.OneTownAccount
import com.onetowncity.app.core.data.OneTownApi
import com.onetowncity.app.core.data.auth.AccountRepository
import com.onetowncity.app.core.data.auth.AuthConfig
import com.onetowncity.app.core.data.auth.KeystoreSessionStore
import com.onetowncity.app.core.data.auth.SignInService

/** Holds the app-wide singletons. Plain manual wiring is enough for now; a DI framework can replace it later. */
class OneTownApplication : Application() {
    val categoryRepository: CategoryRepository by lazy { OneTownApi.categoryRepository(BuildConfig.API_BASE_URL) }
    val listingRepository: ListingRepository by lazy { OneTownApi.listingRepository(BuildConfig.API_BASE_URL) }
    val onboardingPrefs: OnboardingPrefs by lazy { SharedPreferencesOnboardingPrefs(this) }

    private val account by lazy {
        OneTownAccount.create(
            apiBaseUrl = BuildConfig.API_BASE_URL,
            authConfig = AuthConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY, BuildConfig.AUTH_REDIRECT_URI),
            store = KeystoreSessionStore(this),
        )
    }
    val signInService: SignInService get() = account.first
    val accountRepository: AccountRepository get() = account.second
}
