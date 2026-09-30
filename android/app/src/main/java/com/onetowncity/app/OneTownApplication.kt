package com.onetowncity.app

import android.app.Application
import com.onetowncity.app.core.data.CategoryRepository
import com.onetowncity.app.core.data.OneTownApi

/** Holds the app-wide singletons. Plain manual wiring is enough for now; a DI framework can replace it later. */
class OneTownApplication : Application() {
    val categoryRepository: CategoryRepository by lazy { OneTownApi.categoryRepository(BuildConfig.API_BASE_URL) }
}
