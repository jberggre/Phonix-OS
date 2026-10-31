package com.example

import com.example.model.PhonixApps
import com.example.model.WindowType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The catalog is what makes removing a desktop icon or a taskbar pin safe: Start and All apps list
 * it, not the layouts. These assertions are cheap and guard the one thing that must not regress.
 */
class AppCatalogTest {

    @Test
    fun `every shipped app has a unique id`() {
        val ids = PhonixApps.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `local AI is always reachable from the catalog`() {
        val ai = PhonixApps.byId("app_ai")
        assertTrue("Local AI must exist regardless of layout", ai != null)
        assertEquals(WindowType.AI_ASSISTANT, ai!!.windowType)
    }
}
