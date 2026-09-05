package dev.skomlach.biometric.compat.crypto

import org.junit.Assert.*
import org.junit.Test

class AppFlowSecretStoreTest {
    @Test fun randomSecretsPersistPerKeyAndAreNotDerivedFromPublicNames() {
        val storage = mutableMapOf<String, String>()
        val store = AppFlowSecretStore(storage::get) { name, value -> storage[name] = value; true }
        val name = "BiometricModule1"
        val first = store.getSecret(name, true)
        assertEquals(64, first.size)
        assertNotEquals(name.reversed(), String(first))
        assertArrayEquals(first, store.getSecret(name, false))
        assertNotEquals(String(first), String(store.getSecret("BiometricModule2", true)))
        first.fill('\u0000')
        assertEquals(storage[name], String(store.getSecret(name, false)))
    }

    @Test
    fun decryptFallsBackToLegacySecretWhenProtectedSecretIsMissing() {
        val keyName = "BiometricModule1"
        val secret = AppFlowSecretStore(
            read = { null },
            write = { _, _ -> fail("decryption must not persist before ciphertext is verified"); false }
        ).getSecret(keyName, false)

        assertArrayEquals(keyName.toCharArray().reversedArray(), secret)
    }

    @Test(expected = IllegalStateException::class)
    fun persistenceFailureCannotReturnAnEphemeralEncryptionKey() {
        AppFlowSecretStore({ null }, { _, _ -> false }).getSecret("key", true)
    }
}
