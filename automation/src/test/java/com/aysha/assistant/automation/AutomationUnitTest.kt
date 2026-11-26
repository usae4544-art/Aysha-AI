package com.aysha.assistant.automation

import org.junit.Assert.assertEquals
import org.junit.Test

class AutomationUnitTest {

    @Test
    fun `macro template substitutes variables cleanly`() {
        val result = MacroTemplate.substitute(
            "Open {app_name} and search {query}",
            mapOf("app name" to "YouTube", "query" to "lofi study beats"),
        )
        assertEquals("Open YouTube and search lofi study beats", result)
    }
}
