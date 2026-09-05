package dev.skomlach.biometric.compat.crypto

import java.util.concurrent.ConcurrentHashMap

object AppFlowSessionStore {

    private data class Session(
        val secret: CharArray,
        val createdAt: Long,
        val allowLegacyDecryption: Boolean
    )

    internal data class Unlock(val secret: CharArray, val allowLegacyDecryption: Boolean)

    private val sessions = ConcurrentHashMap<String, Session>()
    private const val SESSION_TTL_MS = 15_000L

    fun unlock(keyName: String, secret: CharArray) {
        unlock(keyName, secret, false)
    }

    internal fun unlock(keyName: String, secret: CharArray, allowLegacyDecryption: Boolean) {
        close(keyName)
        sessions[keyName] = Session(secret.copyOf(), System.currentTimeMillis(), allowLegacyDecryption)
    }

    /**
     * One-shot consumption for production safety.
     */
    fun consumeSecretOrNull(keyName: String): CharArray? = consumeUnlockOrNull(keyName)?.secret

    internal fun consumeUnlockOrNull(keyName: String): Unlock? {
        val session = sessions.remove(keyName) ?: return null
        val expired = System.currentTimeMillis() - session.createdAt > SESSION_TTL_MS
        return if (expired) {
            session.secret.fill('\u0000')
            null
        } else {
            Unlock(session.secret.copyOf(), session.allowLegacyDecryption).also { session.secret.fill('\u0000') }
        }
    }

    fun close(keyName: String) {
        val removed = sessions.remove(keyName) ?: return
        removed.secret.fill('\u0000')
    }

    fun closeAll() {
        val keys = sessions.keys().toList()
        keys.forEach(::close)
    }
}
