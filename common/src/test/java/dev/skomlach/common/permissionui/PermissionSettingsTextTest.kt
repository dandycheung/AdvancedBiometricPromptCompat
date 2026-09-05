package dev.skomlach.common.permissionui

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionSettingsTextTest {
    @Test fun systemTranslationIncludesRequestedPermissionsWithoutReformatting() {
        assertEquals("Змініть доступ у налаштуваннях\n\nМікрофон", resolvePermissionSettingsMessage(
            "Змініть доступ у налаштуваннях", "Мікрофон"
        ) { error("A valid system translation must win") })
    }

    @Test fun missingBlankOrIncompatibleOemTextUsesLocalFallback() {
        listOf(null, "", "  ", "Allow %1\$s", "<b>Settings</b>").forEach { system ->
            assertEquals("fallback", resolvePermissionSettingsMessage(system, "Microphone") { "fallback" })
        }
    }
}
