package dev.skomlach.biometric.compat.engine.internal.face.miui.impl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiuiServiceBindingPolicyTest {

    @Test
    fun `uses reflected MIUI service package when available`() {
        assertEquals(
            "com.xiaomi.biometric",
            resolveMiuiServicePackage("com.xiaomi.biometric", hasMiuiFacePackage = true)
        )
    }

    @Test
    fun `uses installed MIUI face package when reflection did not provide one`() {
        assertEquals(
            "com.miui.face",
            resolveMiuiServicePackage(null, hasMiuiFacePackage = true)
        )
    }

    @Test
    fun `does not bind when no MIUI service package is available`() {
        assertEquals(
            null,
            resolveMiuiServicePackage("   ", hasMiuiFacePackage = false)
        )
    }

    @Test
    fun `waits for service only after bind request was accepted`() {
        assertTrue(shouldAwaitMiuiServiceConnection(bindAccepted = true))
        assertFalse(shouldAwaitMiuiServiceConnection(bindAccepted = false))
    }

    @Test
    fun `reads optional MIUI fields regardless of Android SDK or service package`() {
        assertTrue(
            shouldReadOptionalMiuiMessageFields()
        )
        assertTrue(
            shouldReadOptionalMiuiMessageFields()
        )
        assertTrue(
            shouldReadOptionalMiuiMessageFields()
        )
    }
}
