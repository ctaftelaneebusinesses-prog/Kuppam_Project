package com.onetowncity.app

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Every browse screen's `selectedCity` used to be `remember`/`rememberSaveable`
 * with no Saver over a plain (non-Parcelable, non-Serializable) CitySuggestion
 * data class. rememberSaveable without a matching Saver throws once Android
 * actually tries to persist instance state (rotation, backgrounding) — this
 * guards the save()/restore() round trip standing in for that real Bundle
 * round trip, pure and Android-framework-free (SaverScope is plain Kotlin,
 * no Bundle/Parcel involved).
 */
class CitySuggestionSaverTest {
    private val scope = SaverScope { true }

    @Test
    fun `a real selection survives a save-restore round trip`() {
        val original = CitySuggestion(slug = "kuppam", name = "Kuppam")
        val saved = with(CitySuggestionSaver) { scope.save(original) }
        val restored = saved?.let { CitySuggestionSaver.restore(it) }
        assertEquals(original, restored)
    }

    @Test
    fun `no selection saves as an empty list and restores back to null`() {
        val saved = with(CitySuggestionSaver) { scope.save(null) }
        val restored = saved?.let { CitySuggestionSaver.restore(it) }
        assertNull(restored)
    }

    @Test
    fun `a malformed saved value restores to null instead of throwing`() {
        assertNull(CitySuggestionSaver.restore(listOf("only-one-element")))
    }
}
