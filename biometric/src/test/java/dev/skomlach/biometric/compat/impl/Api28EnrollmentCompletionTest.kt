package dev.skomlach.biometric.compat.impl

import dev.skomlach.biometric.compat.BiometricConfirmation
import dev.skomlach.biometric.compat.BiometricType
import org.junit.Assert.*
import org.junit.Test

class Api28EnrollmentCompletionTest {
    private val face = BiometricType.BIOMETRIC_FACE
    private val finger = BiometricType.BIOMETRIC_FINGERPRINT
    private val voice = BiometricType.BIOMETRIC_VOICE
    private val success = AuthResult(AuthResult.AuthResultState.SUCCESS, null)
    private val failed = AuthResult(AuthResult.AuthResultState.FATAL_ERROR, null)

    @Test fun hardwareConfirmationCannotCompleteVoiceEnrollment() {
        for (confirmation in BiometricConfirmation.entries) {
            val results = mutableMapOf(face to success, finger to success)
            assertEquals(AuthenticationCompletion.PENDING, resolveApi28Completion(
                confirmation, listOf(face, finger, voice), listOf(voice),
                AuthResult.AuthResultState.SUCCESS, results
            ))
            results[voice] = success
            assertEquals(AuthenticationCompletion.SUCCEEDED, resolveApi28Completion(
                confirmation, listOf(face, finger, voice), listOf(voice),
                AuthResult.AuthResultState.SUCCESS, results
            ))
        }
    }

    @Test fun softwareFailureCannotBeMaskedByHardwareSuccess() {
        assertEquals(AuthenticationCompletion.FAILED, resolveApi28Completion(
            BiometricConfirmation.ANY, listOf(face, voice), listOf(voice),
            AuthResult.AuthResultState.SUCCESS, mapOf(face to success, voice to failed)
        ))
    }

    @Test fun softwareCallbackCannotSkipPendingHardwareConfirmation() {
        assertEquals(AuthenticationCompletion.PENDING, resolveApi28Completion(
            BiometricConfirmation.ANY, listOf(face, voice), listOf(voice),
            null, mapOf(voice to success)
        ))
    }

    @Test fun setupRequiresHardwareConfirmationButAuthCanFallBackAfterSystemFailure() {
        val results = mapOf(face to failed)
        assertEquals(AuthenticationCompletion.FAILED, resolveApi28Completion(
            BiometricConfirmation.ANY, listOf(face, voice), listOf(voice),
            AuthResult.AuthResultState.FATAL_ERROR, results
        ))
        assertEquals(AuthenticationCompletion.PENDING, resolveApi28Completion(
            BiometricConfirmation.ANY, listOf(face, voice), emptyList(),
            AuthResult.AuthResultState.FATAL_ERROR, results
        ))
    }

    @Test fun genericSystemPromptCannotProveEitherIndividualSensor() {
        assertTrue(requiresSensorSpecificRoute(face, true, false))
        assertTrue(requiresSensorSpecificRoute(finger, true, false))
        assertFalse(requiresSensorSpecificRoute(BiometricType.BIOMETRIC_ANY, true, false))
        assertFalse(requiresSensorSpecificRoute(finger, false, false))
        assertFalse(requiresSensorSpecificRoute(face, true, true))
    }

    @Test fun softwareFirstEnrollmentWaitsForSystemSuccessForAnyAndAll() {
        for (confirmation in BiometricConfirmation.entries) {
            val results = mapOf(voice to success)
            assertEquals(AuthenticationCompletion.PENDING, resolveApi28Completion(
                confirmation, listOf(face, voice), listOf(voice), null, results
            ))
            assertEquals(AuthenticationCompletion.SUCCEEDED, resolveApi28Completion(
                confirmation, listOf(face, voice), listOf(voice), AuthResult.AuthResultState.SUCCESS, results
            ))
            assertEquals(AuthenticationCompletion.FAILED, resolveApi28Completion(
                confirmation, listOf(face, voice), listOf(voice), AuthResult.AuthResultState.FATAL_ERROR, results
            ))
        }
    }
}
