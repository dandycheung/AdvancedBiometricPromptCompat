package dev.skomlach.biometric.compat.engine.internal.face.miui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiuiFaceErrorPolicyTest {

    @Test
    fun `recognizes both documented MIUI cancellation codes`() {
        assertTrue(MiuiFaceErrorPolicy.isCancellation(34))
        assertTrue(MiuiFaceErrorPolicy.isCancellation(2000))
    }

    @Test
    fun `does not classify normalized zero as cancellation`() {
        assertFalse(MiuiFaceErrorPolicy.isCancellation(1000))
    }
}
