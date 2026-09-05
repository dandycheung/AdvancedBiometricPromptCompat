package dev.skomlach.biometric.compat

import dev.skomlach.biometric.compat.impl.FingerprintPlacement
import dev.skomlach.biometric.compat.impl.resolveAuthenticationUiOwner
import org.junit.Assert.assertEquals
import org.junit.Test

class FrameworkFingerprintUiProfileTest {
    @Test fun matchingRuntimeUsesExplicitRecord() {
        assertEquals(AuthenticationUiOwner.SYSTEM, resolveRecordedFingerprintUiOwner("firmware/provider1", "firmware/provider1", "SYSTEM"))
    }
    @Test fun updateOrUnavailableRuntimeInvalidatesRecord() {
        for (current in listOf(null, "", "firmware2/provider1", "firmware/provider2"))
            assertEquals(AuthenticationUiOwner.UNKNOWN, resolveRecordedFingerprintUiOwner(current, "firmware/provider1", "SYSTEM"))
    }
    @Test fun corruptOwnerRemainsUnknown() {
        assertEquals(AuthenticationUiOwner.UNKNOWN, resolveRecordedFingerprintUiOwner("a", "a", "invalid"))
    }
    @Test fun profileAppliesOnlyToFrameworkAndExplicitOverrideWins() {
        fun owner(framework: Boolean, override: AuthenticationUiOwner) = resolveAuthenticationUiOwner(
            true, false, framework, FingerprintPlacement.SIDE, false, override, AuthenticationUiOwner.SYSTEM).owner
        assertEquals(AuthenticationUiOwner.SYSTEM, owner(true, AuthenticationUiOwner.UNKNOWN))
        assertEquals(AuthenticationUiOwner.COMPAT, owner(true, AuthenticationUiOwner.COMPAT))
        assertEquals(AuthenticationUiOwner.UNKNOWN, owner(false, AuthenticationUiOwner.UNKNOWN))
    }
}
