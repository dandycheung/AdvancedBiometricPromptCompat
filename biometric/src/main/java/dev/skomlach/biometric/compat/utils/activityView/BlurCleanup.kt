/*
 *  Copyright (c) 2026 Sergey Komlach aka Salat-Cx65; Original project https://github.com/Salat-Cx65/AdvancedBiometricPromptCompat
 *  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package dev.skomlach.biometric.compat.utils.activityView

internal fun shouldCaptureBlurBitmap(isAtLeastS: Boolean): Boolean = !isAtLeastS

internal fun shouldCaptureBackdropPalette(isAtLeastS: Boolean): Boolean = isAtLeastS

internal class BlurCaptureLatch {
    private var nextToken = 0L
    private var activeToken: Long? = null

    @Synchronized
    fun tryStart(): Long? {
        if (activeToken != null) return null
        nextToken += 1
        return nextToken.also { activeToken = it }
    }

    @Synchronized
    fun finish(token: Long): Boolean {
        if (activeToken != token) return false
        activeToken = null
        return true
    }

    @Synchronized
    fun reset() {
        nextToken += 1
        activeToken = null
    }
}

internal fun runBlurCleanup(
    clearRenderEffect: () -> Unit,
    removeOverlay: () -> Unit,
    invalidateHost: () -> Unit,
    onFailure: (Throwable) -> Unit
) {
    listOf(clearRenderEffect, removeOverlay, invalidateHost).forEach { cleanupStep ->
        try {
            cleanupStep.invoke()
        } catch (error: Throwable) {
            onFailure.invoke(error)
        }
    }
}
