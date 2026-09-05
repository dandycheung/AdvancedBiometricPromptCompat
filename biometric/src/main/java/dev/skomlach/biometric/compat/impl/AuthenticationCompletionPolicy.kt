package dev.skomlach.biometric.compat.impl

import dev.skomlach.biometric.compat.BiometricConfirmation
import dev.skomlach.biometric.compat.BiometricType

internal enum class AuthenticationCompletion { PENDING, SUCCEEDED, FAILED }

// A system prompt reports no individual modality. One success cannot prove both face and finger.
internal fun canConfirmSystemModalities(
    confirmation: BiometricConfirmation,
    primaryTypes: Collection<BiometricType>
): Boolean = confirmation != BiometricConfirmation.ALL || primaryTypes.distinct().size <= 1

internal fun resolveAuthenticationCompletion(
    confirmation: BiometricConfirmation,
    requiredTypes: Collection<BiometricType>,
    results: Map<out BiometricType?, AuthResult>
): AuthenticationCompletion {
    if (requiredTypes.isEmpty()) return AuthenticationCompletion.FAILED
    val selected = requiredTypes.map { results[it]?.authResultState }
    return when (confirmation) {
        BiometricConfirmation.ALL -> when {
            selected.any { it == AuthResult.AuthResultState.FATAL_ERROR } -> AuthenticationCompletion.FAILED
            selected.all { it == AuthResult.AuthResultState.SUCCESS } -> AuthenticationCompletion.SUCCEEDED
            else -> AuthenticationCompletion.PENDING
        }
        BiometricConfirmation.ANY -> when {
            selected.any { it == AuthResult.AuthResultState.SUCCESS } -> AuthenticationCompletion.SUCCEEDED
            selected.all { it == AuthResult.AuthResultState.FATAL_ERROR } -> AuthenticationCompletion.FAILED
            else -> AuthenticationCompletion.PENDING
        }
    }
}
