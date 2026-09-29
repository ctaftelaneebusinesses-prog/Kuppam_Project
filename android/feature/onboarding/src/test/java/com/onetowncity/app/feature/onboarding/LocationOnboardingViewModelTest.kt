package com.onetowncity.app.feature.onboarding

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationOnboardingViewModelTest {
    @Test fun startsOnTheRationaleStep() {
        assertEquals(LocationStage.Rationale, LocationOnboardingViewModel().state.value.stage)
    }

    @Test fun grantedPermissionFinishesWithGranted() = runTest {
        val vm = LocationOnboardingViewModel()
        vm.onPermissionResult(granted = true)
        assertEquals(LocationOutcome.Granted, vm.outcomes.first())
    }

    @Test fun deniedPermissionShowsDeniedAndDoesNotFinish() = runTest {
        val vm = LocationOnboardingViewModel()
        vm.onPermissionResult(granted = false)
        assertEquals(LocationStage.Denied, vm.state.value.stage)
    }

    @Test fun choosingManuallyFinishesWithManualCityFromEitherStep() = runTest {
        val vm = LocationOnboardingViewModel()
        vm.onPermissionResult(granted = false)
        vm.onChooseManually()
        assertEquals(LocationOutcome.ManualCity, vm.outcomes.first())
    }
}
