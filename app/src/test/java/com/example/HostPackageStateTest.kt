package com.example

import com.example.model.DebloatCatalog
import com.example.util.HostDebloat
import com.example.util.HostPackageState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The debloater used a two-state view of the host (installed / absent) and a preset whose
 * category names did not exist in its own catalogue. Both are testable without a device.
 */
class HostPackageStateTest {

    @Test
    fun `absent hidden and visible packages are told apart`() {
        assertEquals(HostPackageState.ABSENT, HostDebloat.stateOf(installed = false, hidden = false))
        assertEquals(HostPackageState.HIDDEN, HostDebloat.stateOf(installed = true, hidden = true))
        assertEquals(HostPackageState.INSTALLED, HostDebloat.stateOf(installed = true, hidden = false))
    }

    @Test
    fun `the appliance preset names categories the catalogue actually has`() {
        val categories = DebloatCatalog.getDefaultPackages().map { it.category }.toSet()
        assertTrue("the preset must not be empty", DebloatCatalog.applianceCategories.isNotEmpty())
        val unknown = DebloatCatalog.applianceCategories - categories
        assertTrue("unknown categories would tick nothing: $unknown", unknown.isEmpty())
    }

    @Test
    fun `the appliance preset selects packages instead of an empty script`() {
        val selected = DebloatCatalog.getDefaultPackages()
            .filter { it.category in DebloatCatalog.applianceCategories }
        assertTrue(
            "the screenless preset ticked nothing and copied an empty command list",
            selected.isNotEmpty()
        )
    }
}
