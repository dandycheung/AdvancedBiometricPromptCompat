package dev.skomlach.biometric.compat.utils.logging

import org.junit.Assert.assertEquals
import org.junit.Test

class BiometricLoggerImplTest {
    @Test
    fun lazyDebugMessageIsNotEvaluatedWhenLoggingIsDisabled() {
        val previous = BiometricLoggerImpl.DEBUG
        var evaluations = 0
        try {
            BiometricLoggerImpl.DEBUG = false
            BiometricLoggerImpl.d {
                evaluations++
                "expensive"
            }
        } finally {
            BiometricLoggerImpl.DEBUG = previous
        }

        assertEquals(0, evaluations)
    }
}
