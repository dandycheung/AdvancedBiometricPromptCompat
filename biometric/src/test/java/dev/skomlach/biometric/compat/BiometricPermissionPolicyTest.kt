package dev.skomlach.biometric.compat

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricPermissionPolicyTest {

    @Test
    fun setupStopsAfterPermissionDeniedWhenNoRouteRemains() {
        assertTrue(
            shouldStopAfterPermissionDenied(
                enroll = true,
                deniedPermissions = listOf(Manifest.permission.RECORD_AUDIO),
                hasUsableRouteAfterDeniedModules = false
            )
        )
    }

    @Test
    fun setupContinuesAfterPermissionDeniedWhenAnotherRouteRemains() {
        assertFalse(
            shouldStopAfterPermissionDenied(
                enroll = true,
                deniedPermissions = listOf(Manifest.permission.RECORD_AUDIO),
                hasUsableRouteAfterDeniedModules = true
            )
        )
    }

    @Test
    fun authenticationDoesNotStopAfterPermissionDeniedPolicy() {
        assertFalse(
            shouldStopAfterPermissionDenied(
                enroll = false,
                deniedPermissions = listOf(Manifest.permission.RECORD_AUDIO)
            )
        )
    }

    @Test
    fun setupDoesNotStopWhenNoPermissionWasDenied() {
        assertFalse(
            shouldStopAfterPermissionDenied(
                enroll = true,
                deniedPermissions = emptyList()
            )
        )
    }

    @Test
    fun permissionDeniedRoutePolicyTreatsSystemRouteAsUsableWithoutLegacyFallback() {
        val systemRoute = SelectedBiometricRoute(
            type = BiometricType.BIOMETRIC_FINGERPRINT,
            provider = BiometricProviderType.HARDWARE,
            usesBiometricPromptHardware = true,
            permissions = listOf(Manifest.permission.USE_BIOMETRIC)
        )

        assertTrue(hasUsableBiometricRoute(listOf(systemRoute)))
        assertFalse(hasPendingLegacyBiometricRoute(listOf(systemRoute)))
    }

    @Test
    fun permissionDeniedRoutePolicyKeepsIndependentLegacyFallback() {
        val legacyRoute = SelectedBiometricRoute(
            type = BiometricType.BIOMETRIC_FINGERPRINT,
            provider = BiometricProviderType.SOFTWARE,
            usesBiometricPromptHardware = false,
            permissions = listOf(Manifest.permission.RECORD_AUDIO)
        )

        assertTrue(hasUsableBiometricRoute(listOf(legacyRoute)))
        assertTrue(hasPendingLegacyBiometricRoute(listOf(legacyRoute)))
    }

    @Test
    fun permissionDeniedRoutePolicyRejectsEmptyRoutes() {
        assertFalse(hasUsableBiometricRoute(emptyList()))
        assertFalse(hasPendingLegacyBiometricRoute(emptyList()))
    }

    @Test
    fun cameraSensorCheckIsSkippedWithoutCameraPermissionRoute() {
        var sensorChecks = 0

        val blocked = isCameraSensorBlockedForPermissions(
            permissions = listOf(Manifest.permission.USE_BIOMETRIC),
            isCameraBlocked = {
                sensorChecks++
                true
            }
        )

        assertFalse(blocked)
        assertEquals(0, sensorChecks)
    }

    @Test
    fun cameraSensorCheckRunsOnceForCameraPermissionRoute() {
        var sensorChecks = 0

        val blocked = isCameraSensorBlockedForPermissions(
            permissions = listOf(Manifest.permission.CAMERA),
            isCameraBlocked = {
                sensorChecks++
                true
            }
        )

        assertTrue(blocked)
        assertEquals(1, sensorChecks)
    }

    @Test
    fun blockedCameraSensorDisablesCameraRoutesWithoutOpeningFallbackUi() {
        val decision = resolveCameraSensorBlock(isCameraBlocked = true)

        assertEquals(CameraSensorBlockAction.DISABLE_CAMERA_ROUTES, decision)
    }

    @Test
    fun availableCameraSensorContinuesWithoutChangingRoutes() {
        val decision = resolveCameraSensorBlock(isCameraBlocked = false)

        assertEquals(CameraSensorBlockAction.CONTINUE, decision)
    }

    @Test
    fun sensorBlockExcludesEveryBiometricTypeThatUsesTheBlockedPermission() {
        val blockedTypes = biometricTypesUsingPermission(
            permissionsByType = listOf(
                BiometricType.BIOMETRIC_FACE to listOf(Manifest.permission.CAMERA),
                BiometricType.BIOMETRIC_FINGERPRINT to listOf(Manifest.permission.USE_BIOMETRIC)
            ),
            permission = Manifest.permission.CAMERA
        )

        assertEquals(setOf(BiometricType.BIOMETRIC_FACE), blockedTypes)
    }
}
