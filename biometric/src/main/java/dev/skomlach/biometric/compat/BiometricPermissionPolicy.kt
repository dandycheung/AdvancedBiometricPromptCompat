/*
 *  Copyright (c) 2026 Sergey Komlach aka Salat-Cx65; Original project https://github.com/Salat-Cx65/AdvancedBiometricPromptCompat
 *  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package dev.skomlach.biometric.compat

import android.Manifest

internal fun hasUsableBiometricRoute(
    routes: Collection<SelectedBiometricRoute?>
): Boolean {
    return routes.any { it != null }
}

internal fun hasPendingLegacyBiometricRoute(
    routes: Collection<SelectedBiometricRoute?>
): Boolean {
    return routes.any { route -> route?.usesBiometricPromptHardware == false }
}

/**
 * Enrollment permission denial is terminal only when no other selected route remains usable.
 * Runtime authentication keeps other fallback routes available.
 */
internal fun shouldStopAfterPermissionDenied(
    enroll: Boolean,
    deniedPermissions: Collection<String>,
    hasUsableRouteAfterDeniedModules: Boolean = false
): Boolean {
    return enroll &&
            deniedPermissions.isNotEmpty() &&
            !hasUsableRouteAfterDeniedModules
}

internal fun isCameraSensorBlockedForPermissions(
    permissions: Collection<String>,
    isCameraBlocked: () -> Boolean
): Boolean {
    return permissions.contains(Manifest.permission.CAMERA) && isCameraBlocked()
}

internal enum class CameraSensorBlockAction {
    CONTINUE,
    DISABLE_CAMERA_ROUTES
}

internal fun resolveCameraSensorBlock(isCameraBlocked: Boolean): CameraSensorBlockAction {
    return if (isCameraBlocked) {
        CameraSensorBlockAction.DISABLE_CAMERA_ROUTES
    } else {
        CameraSensorBlockAction.CONTINUE
    }
}

internal fun biometricTypesUsingPermission(
    permissionsByType: Collection<Pair<BiometricType, List<String>>>,
    permission: String
): Set<BiometricType> {
    return permissionsByType
        .filter { (_, permissions) -> permissions.contains(permission) }
        .mapTo(LinkedHashSet()) { (type, _) -> type }
}
