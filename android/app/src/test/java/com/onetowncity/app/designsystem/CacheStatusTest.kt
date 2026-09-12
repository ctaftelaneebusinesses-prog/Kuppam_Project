package com.onetowncity.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

class CacheStatusTest {

    @Test
    fun `under a minute reads just now`() {
        assertEquals("just now", formatCacheAge(30_000))
    }

    @Test
    fun `minutes format reads N min ago`() {
        assertEquals("12 min ago", formatCacheAge(12 * 60_000L))
    }

    @Test
    fun `hours format reads N hr ago`() {
        assertEquals("3 hr ago", formatCacheAge(3 * 60 * 60_000L))
    }

    @Test
    fun `days format reads N d ago`() {
        assertEquals("2 d ago", formatCacheAge(2 * 24 * 60 * 60_000L))
    }
}
