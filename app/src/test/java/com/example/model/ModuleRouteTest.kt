package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The launch-intent route: a module id opens exactly the window that module declares, and an id
 * nobody declares opens nothing rather than the wrong thing.
 */
class ModuleRouteTest {

    @Test
    fun aModuleIdOpensItsOwnWindow() {
        assertEquals(WindowType.VS_CODE, ModuleRegistry.openable("vscode"))
        assertEquals(WindowType.SERVER_MANAGER, ModuleRegistry.openable(" Servers "))
        assertEquals(WindowType.TERMINAL, ModuleRegistry.openable("terminal"))
    }

    @Test
    fun anUnknownIdOpensNothing() {
        assertNull(ModuleRegistry.openable("not-a-module"))
        assertNull(ModuleRegistry.openable(""))
    }
}
