package dev.skomlach.biometric.compat.impl

import dev.skomlach.biometric.compat.BiometricType
import org.junit.Assert.*
import org.junit.Test

class ParallelSoftwareCaptureTest {
    @Test fun failedOrSilentProviderDoesNotHoldOtherProviders() {
        val voice = BiometricType.BIOMETRIC_VOICE
        val face = BiometricType.BIOMETRIC_FACE
        val iris = BiometricType.BIOMETRIC_IRIS
        val capture = ParallelSoftwareCapture(setOf(voice, face, iris))
        // Voice fails; iris never becomes ready. Face must still be released.
        assertTrue(capture.complete(voice))
        assertTrue(capture.complete(face))
        assertFalse(capture.complete(face))
        assertFalse(capture.complete(BiometricType.BIOMETRIC_FINGERPRINT))
    }

    @Test fun releasesEachPayloadIndependentlyWithoutDuplicateStarts() {
        val voice = BiometricType.BIOMETRIC_VOICE
        val face = BiometricType.BIOMETRIC_FACE
        val capture = ParallelSoftwareCapture(setOf(voice, face))
        assertTrue(capture.complete(voice))
        assertFalse(capture.complete(voice))
        assertTrue(capture.complete(face))
        assertFalse(capture.complete(face))
    }
}
