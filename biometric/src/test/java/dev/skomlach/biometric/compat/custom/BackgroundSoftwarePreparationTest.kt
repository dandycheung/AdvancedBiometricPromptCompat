package dev.skomlach.biometric.compat.custom

import dev.skomlach.biometric.compat.BiometricType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundSoftwarePreparationTest {
    private open class Factory : SoftwareBiometricPromptFactory {
        override val biometricType = BiometricType.BIOMETRIC_VOICE
        override fun create(host: SoftwareBiometricPromptHost): SoftwareBiometricPromptDelegate? = null
    }
    private class AuthOnly : Factory(), BackgroundSoftwareBiometricPromptFactory
    private class Enrollment : Factory(), BackgroundSoftwareBiometricEnrollmentPromptFactory

    @Test fun existingAuthenticationOptInDoesNotEnableBackgroundEnrollment() {
        assertTrue(AuthOnly().supportsBackgroundPreparation(enroll = false))
        assertFalse(AuthOnly().supportsBackgroundPreparation(enroll = true))
    }

    @Test fun enrollmentOptInSupportsBothOperations() {
        assertTrue(Enrollment().supportsBackgroundPreparation(enroll = false))
        assertTrue(Enrollment().supportsBackgroundPreparation(enroll = true))
    }

    @Test fun UIOnlyFactoryNeverRunsWithoutItsView() {
        assertFalse(Factory().supportsBackgroundPreparation(enroll = false))
        assertFalse(Factory().supportsBackgroundPreparation(enroll = true))
    }
}
