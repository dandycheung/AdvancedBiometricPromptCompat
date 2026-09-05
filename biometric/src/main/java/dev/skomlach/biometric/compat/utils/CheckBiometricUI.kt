/*
 *  Copyright (c) 2021 Sergey Komlach aka Salat-Cx65; Original project https://github.com/Salat-Cx65/AdvancedBiometricPromptCompat
 *  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package dev.skomlach.biometric.compat.utils

import android.content.Context
import android.os.Build
import dev.skomlach.biometric.compat.utils.logging.BiometricLoggerImpl
import dev.skomlach.common.misc.SystemStringsHelper
import java.util.zip.ZipFile

internal fun firstMatchingEntryName(
    entryNames: Sequence<String>,
    matches: (String) -> Boolean
): String? = entryNames.firstOrNull(matches)

object CheckBiometricUI {
    private fun getAPKs(context: Context, pkg: String): List<String> {
        val apks: MutableSet<String> = HashSet()
        try {
            val applicationInfo = context.packageManager.getApplicationInfo(pkg, 0)
            apks.add(applicationInfo.sourceDir)
            apks.add(applicationInfo.publicSourceDir)
            if (Build.VERSION.SDK_INT >= 21) {
                if (applicationInfo.splitSourceDirs != null) {
                    apks.addAll(listOf(*applicationInfo.splitSourceDirs ?: emptyArray()))
                }
                if (applicationInfo.splitPublicSourceDirs != null) {
                    apks.addAll(listOf(*applicationInfo.splitPublicSourceDirs ?: emptyArray()))
                }
            }
        } catch (e: Throwable) {
            BiometricLoggerImpl.e(e)
        }
        return ArrayList(apks)
    }

    @Throws(Exception::class)
    private fun checkApk(
        fileZip: String
    ): Boolean {

        ZipFile(fileZip).use { zipFile ->
            val match = firstMatchingEntryName(
                zipFile.entries().asSequence().map { it.name }
            ) { name ->
                name.contains("layout", true) &&
                        (name.contains("biometric", true) || name.contains("fingerprint") ||
                                name.contains("face", true) || name.contains("iris"))
            }
            if (match != null) {
                BiometricLoggerImpl.d("Resource in APK $match")
                return true
            }
        }
        return false
    }

    @Throws(Exception::class)
    private fun checkForFront(
        fileZip: String
    ): Boolean {

        ZipFile(fileZip).use { zipFile ->
            val match = firstMatchingEntryName(
                zipFile.entries().asSequence().map { it.name }
            ) { name ->
                name.contains("front", true) &&
                        (name.contains("biometric", true) || name.contains("fingerprint"))
            }
            if (match != null) {
                BiometricLoggerImpl.d("Resource in APK $match")
                return true
            }
        }
        return false
    }

    fun hasSomethingFrontSensor(context: Context): Boolean {
        try {
            val apks = getAPKs(context, getBiometricUiPackage(context))
            if (apks.isEmpty())
                return true

            for (f in apks) {
                if (checkForFront(f))
                    return true
            }
        } catch (e: Throwable) {
            BiometricLoggerImpl.e(e)
        }
        return false
    }

    fun hasExists(context: Context): Boolean {
        try {
            val apks = getAPKs(context, getBiometricUiPackage(context))
            if (apks.isEmpty())
                return true

            for (f in apks) {
                if (checkApk(f))
                    return true
            }
        } catch (e: Throwable) {
            BiometricLoggerImpl.e(e)
        }
        return false
    }

    fun getBiometricUiPackage(context: Context): String {
        return (SystemStringsHelper.getFromSystem(context, "config_biometric_prompt_ui_package")
            ?: "com.android.systemui").also {
            BiometricLoggerImpl.d("CheckBiometricUI", it)
        }
    }
}
