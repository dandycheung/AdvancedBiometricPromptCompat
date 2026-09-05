package dev.skomlach.biometric.compat.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckBiometricUIPolicyTest {
    @Test
    fun apkEntrySearchStopsAtFirstMatch() {
        var visited = 0
        val names = sequenceOf(
            "res/drawable/icon.xml",
            "res/layout/biometric_prompt.xml",
            "res/layout/unused_after_match.xml"
        ).onEach { visited++ }

        val match = firstMatchingEntryName(names) { it.contains("biometric") }

        assertEquals("res/layout/biometric_prompt.xml", match)
        assertEquals(2, visited)
        assertTrue(match != null)
    }
}
