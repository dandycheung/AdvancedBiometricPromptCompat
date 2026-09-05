package dev.skomlach.biometric.compat.engine

import org.junit.Assert.*
import org.junit.Test

class LegacyAuthSessionTest {
    @Test fun readyModulesJoinWithoutReplacingPreviouslyStartedModules() {
        val session = LegacyAuthSession()
        val owner = Any()
        assertTrue(session.claim(owner))
        assertTrue(session.add(owner, 10))
        assertTrue(session.claim(owner))
        assertTrue(session.add(owner, 20))
        assertFalse(session.add(owner, 10))
        assertFalse(session.claim(Any()))
    }

    @Test fun cancellationInvalidatesTheOwnerAndAllowsANewRequest() {
        val session = LegacyAuthSession()
        val old = Any()
        session.claim(old)
        session.add(old, 10)
        session.clear()
        assertFalse(session.owns(old))
        assertFalse(session.add(old, 20))
        val next = Any()
        assertTrue(session.claim(next))
        assertTrue(session.add(next, 10))
        assertFalse(session.add(old, 30))
    }
}
