package dev.skomlach.biometric.compat.impl

import dev.skomlach.biometric.compat.AuthenticationUiOwner

internal enum class FingerprintPlacement { UNDER_DISPLAY, SIDE, UNKNOWN }

internal data class AuthenticationUiDecision(
    val owner: AuthenticationUiOwner,
    val evidence: String
)

internal fun resolveAuthenticationUiOwner(
    singleFingerprint: Boolean,
    software: Boolean,
    frameworkFingerprint: Boolean,
    placement: FingerprintPlacement,
    missingSystemUi: Boolean,
    frameworkOverride: AuthenticationUiOwner,
    verifiedFrameworkOwner: AuthenticationUiOwner = AuthenticationUiOwner.UNKNOWN
): AuthenticationUiDecision {
    if (software || !singleFingerprint) {
        return AuthenticationUiDecision(AuthenticationUiOwner.COMPAT, "compat-required-for-software-or-mixed-request")
    }
    // Preserve known missing-UI exceptions even when the integration requests system UI.
    if (missingSystemUi) {
        return AuthenticationUiDecision(AuthenticationUiOwner.COMPAT, "system-ui-unavailable")
    }
    if (frameworkFingerprint && frameworkOverride != AuthenticationUiOwner.UNKNOWN) {
        return AuthenticationUiDecision(frameworkOverride, "integration-framework-override")
    }
    if (frameworkFingerprint && verifiedFrameworkOwner != AuthenticationUiOwner.UNKNOWN) {
        return AuthenticationUiDecision(verifiedFrameworkOwner, "verified-local-runtime-profile")
    }
    // Existing compatibility behavior, not a new claim about every OEM backend.
    if (placement == FingerprintPlacement.UNDER_DISPLAY) {
        return AuthenticationUiDecision(AuthenticationUiOwner.SYSTEM, "legacy-udfps-compatibility")
    }
    return AuthenticationUiDecision(AuthenticationUiOwner.UNKNOWN, "no-verified-backend-ui-contract")
}
