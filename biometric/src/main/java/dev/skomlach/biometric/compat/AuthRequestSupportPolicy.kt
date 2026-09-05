package dev.skomlach.biometric.compat

/** Capability filtering only: enrollment and temporary availability remain separate state. */
internal fun isAuthRequestRouteSupported(
    request: BiometricAuthRequest,
    hasSystemHardware: Boolean,
    hasLegacyHardware: Boolean,
    hasFallbackModule: Boolean,
    preferSystemFace: Boolean,
    highPriorityFallback: Boolean
): Boolean {
    if (request.type == BiometricType.BIOMETRIC_ANY) return true
    return when (request.api) {
        BiometricApi.BIOMETRIC_API -> !hasSystemHardware && hasFallbackModule
        BiometricApi.LEGACY_API -> hasLegacyHardware || hasFallbackModule
        BiometricApi.AUTO -> hasLegacyHardware ||
            (hasFallbackModule && (!hasSystemHardware || (highPriorityFallback && !preferSystemFace)))
    }
}
