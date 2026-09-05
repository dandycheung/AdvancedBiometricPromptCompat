package dev.skomlach.biometric.compat.crypto

import android.util.Base64
import androidx.core.content.edit
import dev.skomlach.common.contextprovider.AndroidContext.appContext
import dev.skomlach.common.storage.SharedPreferenceProvider
import java.security.SecureRandom

object AppFlowCryptoStorage {

    private const val PREFS_NAME = "app_flow_crypto_storage"
    private const val SALT_PREFIX = "salt."

    private val prefs by lazy {
        appContext.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    }

    private val secretPrefs by lazy {
        SharedPreferenceProvider.getProtectedPreferences("app_flow_crypto_secrets_v3")
    }
    private val secrets = AppFlowSecretStore(
        read = { secretPrefs.getString(it, null) },
        write = { name, secret -> secretPrefs.edit().putString(name, secret).commit() }
    )

    internal fun getProtectedSecret(keyName: String, create: Boolean, allowLegacy: Boolean = true): CharArray =
        secrets.getSecret(keyName, create, allowLegacy)

    fun getOrCreateSalt(keyName: String): ByteArray {
        val existing = prefs.getString(SALT_PREFIX + keyName, null)
        if (existing != null) {
            return Base64.decode(existing, Base64.NO_WRAP)
        }

        val salt = ByteArray(32)
        SecureRandom().nextBytes(salt)
        prefs.edit {
            putString(SALT_PREFIX + keyName, Base64.encodeToString(salt, Base64.NO_WRAP))
        }
        return salt
    }

    fun delete(keyName: String) {
        check(secretPrefs.edit().remove(keyName).commit()) { "Unable to delete app-flow key" }
        prefs.edit().remove(SALT_PREFIX + keyName).apply()
    }
}
