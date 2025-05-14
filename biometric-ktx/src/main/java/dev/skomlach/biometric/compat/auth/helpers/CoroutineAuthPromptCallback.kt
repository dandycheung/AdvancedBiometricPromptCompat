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

package dev.skomlach.biometric.compat.auth.helpers

import dev.skomlach.biometric.compat.AuthenticationFailureReason
import dev.skomlach.biometric.compat.AuthenticationResult
import dev.skomlach.biometric.compat.BiometricPromptCompat
import dev.skomlach.biometric.compat.auth.AuthPromptCanceledException
import dev.skomlach.biometric.compat.auth.AuthPromptErrorException
import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resumeWithException

/**
 * Implementation of [AuthPromptCallback] used to transform callback results for coroutine APIs.
 */
internal class CoroutineAuthPromptCallback(
    private val continuation: CancellableContinuation<Set<AuthenticationResult>>
) : BiometricPromptCompat.AuthenticationCallback() {
    override fun onFailed(confirmed: Set<AuthenticationResult>) {
        if (!continuation.isCompleted)
            continuation.resumeWithException(
                AuthPromptErrorException(confirmed)
            )
    }

    override fun onSucceeded(confirmed: Set<AuthenticationResult>) {
        super.onSucceeded(confirmed)
        if (!continuation.isCompleted)
            continuation.resumeWith(Result.success(confirmed))
    }

    override fun onCanceled(confirmed: Set<AuthenticationResult>) {
        if (!continuation.isCompleted)
            continuation.resumeWithException(AuthPromptCanceledException(confirmed))
    }
}
