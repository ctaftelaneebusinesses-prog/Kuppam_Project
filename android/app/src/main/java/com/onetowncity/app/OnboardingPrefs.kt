package com.onetowncity.app

import android.content.Context

/** Which onboarding steps the user has already passed, so a restart resumes instead of repeating them. */
interface OnboardingPrefs {
    var locationStepDone: Boolean
}

class SharedPreferencesOnboardingPrefs(context: Context) : OnboardingPrefs {
    private val prefs = context.applicationContext.getSharedPreferences("onetowncity_onboarding", Context.MODE_PRIVATE)

    override var locationStepDone: Boolean
        get() = prefs.getBoolean("location_step_done", false)
        set(value) = prefs.edit().putBoolean("location_step_done", value).apply()
}
