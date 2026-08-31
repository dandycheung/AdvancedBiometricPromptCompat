package dev.skomlach.biometric.compat.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AccessibilityDataSensitivityCompatTest {

    @Test
    fun missingPlatformMethodIsTreatedAsUnsupported() {
        val result = invokeAccessibilityDataSensitiveApiOrNull(true) {
            throw NoSuchMethodError("View.isAccessibilityDataSensitive")
        }

        assertNull(result)
    }

    @Test
    fun unsupportedPlatformDoesNotInvokeAccessibilityDataSensitiveApi() {
        var invoked = false

        val result = invokeAccessibilityDataSensitiveApiOrNull(false) {
            invoked = true
            42
        }

        assertNull(result)
        assertFalse(invoked)
    }

    @Test
    fun availablePlatformInvokesAccessibilityDataSensitiveApi() {
        val result = invokeAccessibilityDataSensitiveApiOrNull(true) { 42 }

        assertEquals(42, result)
    }
}
