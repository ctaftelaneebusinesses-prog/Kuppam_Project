package com.onetowncity.app.feature.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeUiStateTest {
    @Test fun firstCategoriesInServerOrderAreFocal() {
        assertEquals(TileSpan.Focal, spanForPosition(0))
        assertEquals(TileSpan.Focal, spanForPosition(FOCAL_TILE_COUNT - 1))
    }

    @Test fun everythingAfterTheFocalTilesIsCompact() {
        assertEquals(TileSpan.Compact, spanForPosition(FOCAL_TILE_COUNT))
        assertEquals(TileSpan.Compact, spanForPosition(40))
    }
}
