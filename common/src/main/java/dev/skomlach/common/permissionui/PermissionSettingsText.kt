package dev.skomlach.common.permissionui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import dev.skomlach.common.R
import dev.skomlach.common.translate.LocalizationHelper

/** AOSP PermissionController text; OEMs may rename/remove it, so never require this resource. */
internal fun permissionSettingsMessage(context: Context, permissionDescriptions: String): String {
    val systemMessage = if (Build.VERSION.SDK_INT >= 23) runCatching {
        val packageName = context.packageManager.resolveActivity(
            Intent("android.intent.action.MANAGE_APP_PERMISSIONS"), PackageManager.MATCH_SYSTEM_ONLY
        )?.activityInfo?.packageName ?: return@runCatching null
        val controller = context.createPackageContext(packageName, Context.CONTEXT_RESTRICTED)
            .createConfigurationContext(Configuration(context.resources.configuration))
        val resources = controller.resources
        // Some OEM APKs retain the AOSP resource namespace after changing the package name.
        val namespaces = listOf(packageName, "com.android.permissioncontroller")
        namespaces.firstNotNullOfOrNull { namespace ->
            val id = resources.getIdentifier(
                "permission_rationale_permission_settings_message", "string", namespace
            )
            if (id == 0) null else resources.getString(id)
        }
    }.getOrNull() else null
    return resolvePermissionSettingsMessage(systemMessage, permissionDescriptions) {
        LocalizationHelper.getLocalizedString(
            context, R.string.biometriccompat_permissions_settings_message, permissionDescriptions
        )
    }
}

internal fun resolvePermissionSettingsMessage(
    systemMessage: String?,
    permissionDescriptions: String,
    fallback: () -> String
): String {
    // The selected resource has no format arguments. Reject incompatible OEM replacements.
    return systemMessage?.takeIf { it.isNotBlank() && '%' !in it && '<' !in it && '>' !in it }
        ?.let { "$it\n\n$permissionDescriptions" } ?: fallback()
}
