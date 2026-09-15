package com.onetowncity.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers classifyPermissionResult — the decision that distinguishes
 * "denied, can still ask again" from "denied repeatedly / permanently"
 * (LocationPermissionState.REVOKED), the Phase 4 "5. Permission denied
 * repeatedly" requirement.
 */
class LocationPermissionTest {

    @Test
    fun `any permission granted is GRANTED regardless of rationale flag`() {
        assertEquals(LocationPermissionState.GRANTED, classifyPermissionResult(anyGranted = true, canShowRationale = false))
        assertEquals(LocationPermissionState.GRANTED, classifyPermissionResult(anyGranted = true, canShowRationale = true))
    }

    @Test
    fun `denied but rationale can still be shown is a plain DENIED, not permanent`() {
        assertEquals(LocationPermissionState.DENIED, classifyPermissionResult(anyGranted = false, canShowRationale = true))
    }

    @Test
    fun `denied with no rationale available is REVOKED (denied repeatedly)`() {
        assertEquals(LocationPermissionState.REVOKED, classifyPermissionResult(anyGranted = false, canShowRationale = false))
    }
}
