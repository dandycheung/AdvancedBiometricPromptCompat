package dev.skomlach.biometric.compat.crypto

import java.security.SecureRandom

/** The backing store must protect secrets with Android Keystore and persist before returning. */
internal class AppFlowSecretStore(
    private val read: (String) -> String?,
    private val write: (String, String) -> Boolean
) {
    @Synchronized
    fun getSecret(keyName: String, create: Boolean, allowLegacy: Boolean = true): CharArray {
        read(keyName)?.let { return it.toCharArray() }
        if (!create) {
            check(allowLegacy) { "Protected app-flow key is missing" }
            // Only unversioned historical ciphertext may use the public-name-derived key.
            return keyName.toCharArray().reversedArray()
        }
        val random = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val secret = random.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        random.fill(0)
        check(write(keyName, secret)) { "Unable to persist protected app-flow key" }
        return secret.toCharArray()
    }
}
