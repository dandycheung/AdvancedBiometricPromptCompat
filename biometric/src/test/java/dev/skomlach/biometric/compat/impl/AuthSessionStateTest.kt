package dev.skomlach.biometric.compat.impl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthSessionStateTest {
    @Test
    fun replacementSessionRejectsStaleCallbacksAndStartsWithEmptyResults() {
        val state = AuthSessionState<String>()

        val first = state.begin()
        assertTrue(state.add(first, "old"))
        assertEquals(setOf("old"), state.snapshot(first))

        val second = state.begin()
        assertFalse(state.owns(first))
        assertTrue(state.owns(second))
        assertEquals(emptySet<String>(), state.snapshot(second))
        assertFalse(state.add(first, "stale"))
        assertTrue(state.add(second, "current"))
        assertEquals(setOf("current"), state.snapshot(second))

        state.invalidate()
        assertFalse(state.owns(second))
        assertEquals(emptySet<String>(), state.snapshot(second))
    }
}
