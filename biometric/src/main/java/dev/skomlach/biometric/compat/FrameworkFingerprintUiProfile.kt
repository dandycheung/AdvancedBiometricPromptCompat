package dev.skomlach.biometric.compat

import android.os.Build
import android.util.AtomicFile
import dev.skomlach.biometric.compat.utils.CheckBiometricUI
import dev.skomlach.common.contextprovider.AndroidContext
import org.json.JSONObject
import java.io.File

/**
 * Explicitly verified presentation contract for this installation and platform runtime.
 * Not learned from authentication success, sensor placement, focus, or elapsed time.
 * Stored outside Android backup; firmware/SystemUI updates invalidate the record.
 */
object FrameworkFingerprintUiProfile {
    private fun file() = AtomicFile(File(AndroidContext.appContext.noBackupFilesDir, "framework-fingerprint-ui.json"))

    @Suppress("DEPRECATION")
    private fun runtimeKey(): String? = runCatching {
        val context = AndroidContext.appContext
        val pkg = CheckBiometricUI.getBiometricUiPackage(context)
        val info = context.packageManager.getPackageInfo(pkg, 0)
        val version = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        listOf("1", Build.FINGERPRINT, Build.VERSION.SDK_INT, pkg, version, info.lastUpdateTime).joinToString("\n")
    }.getOrNull()

    /** Call only after verifying the framework backend UI. UNKNOWN removes the profile. */
    @JvmStatic
    @Synchronized
    fun setVerifiedOwner(owner: AuthenticationUiOwner): Boolean {
        if (owner == AuthenticationUiOwner.UNKNOWN) return runCatching { file().delete(); true }.getOrDefault(false)
        val key = runtimeKey() ?: return false
        return runCatching {
            val target = file()
            val stream = target.startWrite()
            try {
                stream.write(JSONObject().put("runtime", key).put("owner", owner.name).toString().toByteArray(Charsets.UTF_8))
                target.finishWrite(stream)
            } catch (error: Throwable) {
                target.failWrite(stream)
                throw error
            }
            true
        }.getOrDefault(false)
    }

    @JvmStatic
    @Synchronized
    fun getVerifiedOwner(): AuthenticationUiOwner = runCatching {
        val record = JSONObject(file().readFully().toString(Charsets.UTF_8))
        resolveRecordedFingerprintUiOwner(runtimeKey(), record.optString("runtime"), record.optString("owner"))
    }.getOrDefault(AuthenticationUiOwner.UNKNOWN)
}

internal fun resolveRecordedFingerprintUiOwner(current: String?, recorded: String?, owner: String?): AuthenticationUiOwner {
    if (current.isNullOrEmpty() || current != recorded) return AuthenticationUiOwner.UNKNOWN
    return AuthenticationUiOwner.entries.firstOrNull { it.name == owner } ?: AuthenticationUiOwner.UNKNOWN
}
