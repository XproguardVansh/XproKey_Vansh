package com.xprokeey2.presentation.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The drawer's menus, as the web sidebar nests them. */
class WorkspaceMenuTest {

    @Test
    fun pagesOpenTheMenusThatHoldThem() {
        assertEquals(setOf(MenuGroup.ABOUT, MenuGroup.SETTINGS), WorkspaceSection.FAQ.openGroups)
        assertEquals(setOf(MenuGroup.ABOUT, MenuGroup.SETTINGS), WorkspaceSection.APP_INFO.openGroups)
        assertEquals(setOf(MenuGroup.TOOLS), WorkspaceSection.EXPORT.openGroups)
        assertEquals(emptySet<MenuGroup>(), WorkspaceSection.DASHBOARD.openGroups)
    }

    @Test
    fun everySubMenuHasOneEntryThatOpensIt() {
        MenuGroup.entries.forEach { group ->
            assertEquals(1, WorkspaceSection.entries.count { it.opens == group })
            assertTrue(WorkspaceSection.entries.any { it.group == group })
        }
        assertTrue(WorkspaceSection.ABOUT.hasSubmenu)
        assertFalse(WorkspaceSection.FAQ.hasSubmenu)
    }
}
