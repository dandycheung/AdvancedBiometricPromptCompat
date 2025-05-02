/*
 *  Copyright (c) 2023 Sergey Komlach aka Salat-Cx65; Original project https://github.com/Salat-Cx65/AdvancedBiometricPromptCompat
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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import dev.skomlach.biometric.compat.utils.logging.BiometricLoggerImpl.d
import dev.skomlach.biometric.compat.utils.logging.BiometricLoggerImpl.e
import dev.skomlach.common.contextprovider.AndroidContext.appContext
import dev.skomlach.common.misc.BroadcastTools

class DeviceUnlockedReceiver : BroadcastReceiver() {
    companion object {

        fun registerDeviceUnlockListener() {
            try {
                val filter = IntentFilter()
                filter.addAction(Intent.ACTION_USER_PRESENT)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    filter.addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
                    filter.addAction(Intent.ACTION_USER_UNLOCKED)
                }
                BroadcastTools.registerGlobalBroadcastIntent(
                    appContext,
                    DeviceUnlockedReceiver(),
                    filter
                )
            } catch (e: Throwable) {
                e(e)
            }

        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action?.contains("boot", ignoreCase = true) == true) {
            d("Device boot completed")
            BiometricErrorLockoutPermanentFix.resetBiometricSensorPermanentlyLocked()
        } else
            if (!intent.action.isNullOrEmpty()) {
                d("Device unlocked completed")
                if (BiometricErrorLockoutPermanentFix.isRebootDetected())
                    BiometricErrorLockoutPermanentFix.resetBiometricSensorPermanentlyLocked()
                else
                    BiometricLockoutFix.reset()
            } else if (BiometricErrorLockoutPermanentFix.isRebootDetected())
                BiometricErrorLockoutPermanentFix.resetBiometricSensorPermanentlyLocked()
    }

}