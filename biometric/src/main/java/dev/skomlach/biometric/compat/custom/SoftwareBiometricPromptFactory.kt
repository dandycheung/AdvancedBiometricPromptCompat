package dev.skomlach.biometric.compat.custom

import dev.skomlach.biometric.compat.BiometricType

interface SoftwareBiometricPromptFactory {
    val biometricType: BiometricType
    val requiresReadyExtrasBeforeAuthentication: Boolean
        get() = false

    fun create(host: SoftwareBiometricPromptHost): SoftwareBiometricPromptDelegate?
}

/** Opt-in for authentication preparation with a null rootView and status-only feedback. */
interface BackgroundSoftwareBiometricPromptFactory : SoftwareBiometricPromptFactory

/** Also supports enrollment preparation with a null rootView and status-only feedback. */
interface BackgroundSoftwareBiometricEnrollmentPromptFactory : BackgroundSoftwareBiometricPromptFactory

internal fun SoftwareBiometricPromptFactory.supportsBackgroundPreparation(enroll: Boolean): Boolean =
    if (enroll) this is BackgroundSoftwareBiometricEnrollmentPromptFactory
    else this is BackgroundSoftwareBiometricPromptFactory
