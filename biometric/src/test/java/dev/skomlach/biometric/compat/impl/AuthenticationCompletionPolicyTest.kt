package dev.skomlach.biometric.compat.impl

import dev.skomlach.biometric.compat.BiometricConfirmation
import dev.skomlach.biometric.compat.BiometricType
import org.junit.Assert.*
import org.junit.Test

class AuthenticationCompletionPolicyTest {
    private val finger = BiometricType.BIOMETRIC_FINGERPRINT
    private val face = BiometricType.BIOMETRIC_FACE
    private val required = listOf(finger, face)
    private val success = AuthResult(AuthResult.AuthResultState.SUCCESS)
    private val failure = AuthResult(AuthResult.AuthResultState.FATAL_ERROR)

    @Test fun allNeverAcceptsPartialSuccessInEitherCallbackOrder() {
        for (results in listOf(linkedMapOf(finger to success, face to failure), linkedMapOf(face to failure, finger to success))) {
            assertEquals(AuthenticationCompletion.FAILED, resolveAuthenticationCompletion(BiometricConfirmation.ALL, required, results))
        }
    }

    @Test fun allWaitsForEverySuccessAndFailsAsSoonAsARequiredRouteFails() {
        assertEquals(AuthenticationCompletion.PENDING, resolveAuthenticationCompletion(BiometricConfirmation.ALL, required, mapOf(finger to success)))
        assertEquals(AuthenticationCompletion.SUCCEEDED, resolveAuthenticationCompletion(BiometricConfirmation.ALL, required, mapOf(finger to success, face to success)))
        assertEquals(AuthenticationCompletion.FAILED, resolveAuthenticationCompletion(BiometricConfirmation.ALL, required, mapOf(face to failure)))
    }

    @Test fun anyStillAcceptsOneSuccessAndWaitsForOtherRoutesAfterOneFailure() {
        assertEquals(AuthenticationCompletion.SUCCEEDED, resolveAuthenticationCompletion(BiometricConfirmation.ANY, required, mapOf(finger to success, face to failure)))
        assertEquals(AuthenticationCompletion.PENDING, resolveAuthenticationCompletion(BiometricConfirmation.ANY, required, mapOf(face to failure)))
        assertEquals(AuthenticationCompletion.FAILED, resolveAuthenticationCompletion(BiometricConfirmation.ANY, required, mapOf(finger to failure, face to failure)))
    }

    @Test fun unrelatedResultsAndEmptyScopeCannotSatisfyAll() {
        assertEquals(AuthenticationCompletion.PENDING, resolveAuthenticationCompletion(BiometricConfirmation.ALL, required, mapOf(BiometricType.BIOMETRIC_VOICE to success)))
        assertEquals(AuthenticationCompletion.FAILED, resolveAuthenticationCompletion(BiometricConfirmation.ALL, emptyList(), emptyMap()))
    }

    @Test fun oneSystemResultCannotConfirmMultipleModalities() {
        assertFalse(canConfirmSystemModalities(BiometricConfirmation.ALL, required))
        assertTrue(canConfirmSystemModalities(BiometricConfirmation.ALL, listOf(finger)))
        assertTrue(canConfirmSystemModalities(BiometricConfirmation.ANY, required))
    }
}
