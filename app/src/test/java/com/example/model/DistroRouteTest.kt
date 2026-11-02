package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The distro id route: the bundled Alpine is what a bench session switches to, and an unknown id
 * must not silently activate something else.
 */
class DistroRouteTest {

    @Test
    fun theBundledAlpineIsReachableById() {
        val alpine = DistroCatalog.byId("alpine")
        assertEquals("alpine", alpine?.id)
        assertTrue("the bundled image is what ships, so it must be reachable", alpine?.isBundled == true)
        assertEquals(alpine, DistroCatalog.byId(" ALPINE "))
    }

    @Test
    fun anUnknownIdFindsNothing() {
        assertNull(DistroCatalog.byId("not-a-distro"))
        assertNull(DistroCatalog.byId(""))
    }
}
