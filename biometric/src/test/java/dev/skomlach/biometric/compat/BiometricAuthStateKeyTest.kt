package dev.skomlach.biometric.compat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricAuthStateKeyTest {

    @Test
    fun unchangedPersistedStateDoesNotNeedAnotherWrite() {
        val state = BiometricAuthState(
            hardwareDetected = true,
            enrolled = true,
            lockedOut = false,
            permanentlyLocked = false
        )

        assertFalse(shouldPersistAuthState(cached = state, current = state))
    }

    @Test
    fun changedPersistedStateNeedsWrite() {
        val cached = BiometricAuthState(false, false, false, false)
        val current = BiometricAuthState(true, false, false, false)

        assertTrue(shouldPersistAuthState(cached = cached, current = current))
    }

    @Test
    fun stateCacheKeyIncludesProvider() {
        val hardwareRequest = BiometricAuthRequest.default()
            .withApi(BiometricApi.AUTO)
            .withType(BiometricType.BIOMETRIC_FACE)
            .withProvider(BiometricProviderType.HARDWARE)
        val softwareRequest = hardwareRequest.withProvider(BiometricProviderType.SOFTWARE)

        assertNotEquals(
            hardwareRequest.stateCacheKey("isLockOut"),
            softwareRequest.stateCacheKey("isLockOut")
        )
    }

    @Test
    fun stateCacheKeyKeepsExistingPrefixReadable() {
        val request = BiometricAuthRequest.default()
            .withApi(BiometricApi.BIOMETRIC_API)
            .withType(BiometricType.BIOMETRIC_FACE)
            .withProvider(BiometricProviderType.HARDWARE)

        assertEquals(
            "isHardwareDetected-BIOMETRIC_API-BIOMETRIC_FACE-HARDWARE",
            request.stateCacheKey("isHardwareDetected")
        )
    }
}
