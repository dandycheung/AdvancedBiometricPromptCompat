package dev.skomlach.biometric.compat.engine.internal.face.miui.impl

internal const val MIUI_FACE_SERVICE_PACKAGE = "com.miui.face"

internal fun resolveMiuiServicePackage(
    reflectedPackage: String?,
    hasMiuiFacePackage: Boolean
): String? {
    return reflectedPackage?.takeIf { it.isNotBlank() }
        ?: MIUI_FACE_SERVICE_PACKAGE.takeIf { hasMiuiFacePackage }
}

internal fun shouldAwaitMiuiServiceConnection(bindAccepted: Boolean): Boolean = bindAccepted

/**
 * MIUI framework fields are always probed directly. OEM/API-specific failures are handled by
 * the individual reflective calls, never by suppressing the entire vendor API surface.
 */
internal fun shouldReadOptionalMiuiMessageFields(): Boolean = true
