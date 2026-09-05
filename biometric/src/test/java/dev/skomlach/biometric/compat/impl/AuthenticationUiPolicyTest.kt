package dev.skomlach.biometric.compat.impl

import dev.skomlach.biometric.compat.AuthenticationUiOwner
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthenticationUiPolicyTest {
    private fun owner(
        single: Boolean = true,
        software: Boolean = false,
        framework: Boolean = true,
        placement: FingerprintPlacement = FingerprintPlacement.SIDE,
        missing: Boolean = false,
        override: AuthenticationUiOwner = AuthenticationUiOwner.UNKNOWN
    ) = resolveAuthenticationUiOwner(single, software, framework, placement, missing, override).owner

    @Test fun sideSensorDoesNotProveSystemUi() {
        assertEquals(AuthenticationUiOwner.UNKNOWN, owner())
    }

    @Test fun verifiedFrameworkOverrideChoosesSystemUi() {
        assertEquals(AuthenticationUiOwner.SYSTEM, owner(override = AuthenticationUiOwner.SYSTEM))
    }

    @Test fun frameworkOverrideCannotHideSoftwareUi() {
        assertEquals(AuthenticationUiOwner.COMPAT, owner(software = true, override = AuthenticationUiOwner.SYSTEM))
    }

    @Test fun frameworkOverrideCannotHideMixedRequestUi() {
        assertEquals(AuthenticationUiOwner.COMPAT, owner(single = false, override = AuthenticationUiOwner.SYSTEM))
    }

    @Test fun frameworkOverrideDoesNotApplyToOemBackend() {
        assertEquals(AuthenticationUiOwner.UNKNOWN, owner(framework = false, override = AuthenticationUiOwner.SYSTEM))
    }

    @Test fun missingSystemUiWinsOverOverrideAndPlacement() {
        assertEquals(AuthenticationUiOwner.COMPAT, owner(missing = true,
            placement = FingerprintPlacement.UNDER_DISPLAY, override = AuthenticationUiOwner.SYSTEM))
    }

    @Test fun existingUdfpsCompatibilityIsPreserved() {
        assertEquals(AuthenticationUiOwner.SYSTEM, owner(placement = FingerprintPlacement.UNDER_DISPLAY))
    }

    @Test fun explicitCompatCanDisableFrameworkUdfpsCompatibility() {
        assertEquals(AuthenticationUiOwner.COMPAT, owner(placement = FingerprintPlacement.UNDER_DISPLAY,
            override = AuthenticationUiOwner.COMPAT))
    }

    @Test fun nextSoftwareStageDoesNotInheritSystemOwnership() {
        assertEquals(AuthenticationUiOwner.SYSTEM, owner(override = AuthenticationUiOwner.SYSTEM))
        assertEquals(AuthenticationUiOwner.COMPAT, owner(single = false, software = true,
            framework = false, override = AuthenticationUiOwner.SYSTEM))
    }
}
