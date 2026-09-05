package dev.skomlach.biometric.compat

/** Presentation ownership; never evidence of successful authentication or sensor identity. */
enum class AuthenticationUiOwner {
    SYSTEM,
    COMPAT,
    /** No verified ownership contract. The library keeps its compat UI. */
    UNKNOWN
}
