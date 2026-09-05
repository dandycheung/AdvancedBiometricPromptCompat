package dev.skomlach.biometric.compat.impl

import dev.skomlach.biometric.compat.BiometricConfirmation
import dev.skomlach.biometric.compat.BiometricType
import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyUiStagesTest {
    private val finger = BiometricType.BIOMETRIC_FINGERPRINT
    private val voice = BiometricType.BIOMETRIC_VOICE
    private val required = setOf(finger, voice)

    @Test fun anySuccessFinishesBeforeVoice() {
        assertEquals(AuthenticationCompletion.SUCCEEDED, resolveAuthenticationCompletion(
            BiometricConfirmation.ANY, required, mapOf(finger to AuthResult(AuthResult.AuthResultState.SUCCESS))))
    }

    @Test fun allSuccessNeedsDeferredVoiceThenFinishes() {
        val results = mutableMapOf(finger to AuthResult(AuthResult.AuthResultState.SUCCESS))
        assertEquals(AuthenticationCompletion.PENDING, resolveAuthenticationCompletion(BiometricConfirmation.ALL, required, results))
        assertEquals(setOf(voice), remainingCompatStage(setOf(voice), required, results.keys))
        results[voice] = AuthResult(AuthResult.AuthResultState.SUCCESS)
        assertEquals(AuthenticationCompletion.SUCCEEDED, resolveAuthenticationCompletion(BiometricConfirmation.ALL, required, results))
    }

    @Test fun anyFatalFingerprintErrorStillAllowsVoice() {
        val results = mutableMapOf(finger to AuthResult(AuthResult.AuthResultState.FATAL_ERROR))
        assertEquals(AuthenticationCompletion.PENDING, resolveAuthenticationCompletion(BiometricConfirmation.ANY, required, results))
        assertEquals(setOf(voice), remainingCompatStage(setOf(voice), required, results.keys))
        results[voice] = AuthResult(AuthResult.AuthResultState.SUCCESS)
        assertEquals(AuthenticationCompletion.SUCCEEDED, resolveAuthenticationCompletion(BiometricConfirmation.ANY, required, results))
    }

    @Test fun allFatalFingerprintErrorDoesNotContinue() {
        assertEquals(AuthenticationCompletion.FAILED, resolveAuthenticationCompletion(
            BiometricConfirmation.ALL, required, mapOf(finger to AuthResult(AuthResult.AuthResultState.FATAL_ERROR))))
    }

    @Test fun deferredStageDoesNotRestartFinishedOrDisabledTypes() {
        assertEquals(emptySet<BiometricType>(), remainingCompatStage(setOf(finger, voice), setOf(finger), setOf(finger)))
    }
}
