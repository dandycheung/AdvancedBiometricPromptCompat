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

package dev.skomlach.biometric.compat.engine.internal.fingerprint

import android.annotation.SuppressLint
import androidx.core.os.CancellationSignal
import com.tencent.soter.core.biometric.BiometricManagerCompat
import com.tencent.soter.core.model.ConstantsSoter
import dev.skomlach.biometric.compat.AuthenticationFailureReason
import dev.skomlach.biometric.compat.BiometricCryptoObject
import dev.skomlach.biometric.compat.engine.BiometricInitListener
import dev.skomlach.biometric.compat.engine.BiometricMethod
import dev.skomlach.biometric.compat.engine.core.Core
import dev.skomlach.biometric.compat.engine.core.interfaces.AuthenticationListener
import dev.skomlach.biometric.compat.engine.core.interfaces.RestartPredicate
import dev.skomlach.biometric.compat.engine.internal.AbstractBiometricModule
import dev.skomlach.biometric.compat.utils.BiometricErrorLockoutPermanentFix
import dev.skomlach.biometric.compat.utils.logging.BiometricLoggerImpl.d
import dev.skomlach.biometric.compat.utils.logging.BiometricLoggerImpl.e
import dev.skomlach.common.misc.ExecutorHelper


class SoterFingerprintUnlockModule @SuppressLint("WrongConstant") constructor(private val listener: BiometricInitListener?) :
    AbstractBiometricModule(BiometricMethod.FINGERPRINT_SOTERAPI) {
    companion object {
        const val FINGERPRINT_ACQUIRED_GOOD = 0
        const val FINGERPRINT_ACQUIRED_IMAGER_DIRTY = 3
        const val FINGERPRINT_ACQUIRED_INSUFFICIENT = 2
        const val FINGERPRINT_ACQUIRED_PARTIAL = 1
        const val FINGERPRINT_ACQUIRED_TOO_FAST = 5
        const val FINGERPRINT_ACQUIRED_TOO_SLOW = 4
        const val FINGERPRINT_ERROR_CANCELED = 5
        const val FINGERPRINT_ERROR_HW_NOT_PRESENT = 12
        const val FINGERPRINT_ERROR_HW_UNAVAILABLE = 1
        const val FINGERPRINT_ERROR_LOCKOUT = 7
        const val FINGERPRINT_ERROR_LOCKOUT_PERMANENT = 9
        const val FINGERPRINT_ERROR_NO_FINGERPRINTS = 11
        const val FINGERPRINT_ERROR_NO_SPACE = 4
        const val FINGERPRINT_ERROR_TIMEOUT = 3
        const val FINGERPRINT_ERROR_UNABLE_TO_PROCESS = 2
        const val FINGERPRINT_ERROR_USER_CANCELED = 10
        const val FINGERPRINT_ERROR_VENDOR = 8
    }

    private var manager: BiometricManagerCompat? = null

    init {
        manager = try {
            BiometricManagerCompat.from(context, ConstantsSoter.FINGERPRINT_AUTH)
        } catch (e: Throwable) {
            if (DEBUG_MANAGERS)
                e(e, name)
            null
        }
        listener?.initFinished(biometricMethod, this@SoterFingerprintUnlockModule)
    }

    override fun getManagers(): Set<Any> {
        //No way to detect enrollments
        return emptySet()
    }

    override val isManagerAccessible: Boolean
        get() = manager != null
    override val isHardwarePresent: Boolean
        get() {

            try {
                return manager?.isHardwareDetected == true
            } catch (e: Throwable) {

            }

            return false
        }

    override val hasEnrolled: Boolean
        get() {

            try {
                return manager?.hasEnrolledBiometric() == true
            } catch (e: Throwable) {

            }

            return false
        }

    @Throws(SecurityException::class)
    override fun authenticate(
        biometricCryptoObject: BiometricCryptoObject?,
        cancellationSignal: CancellationSignal?,
        listener: AuthenticationListener?,
        restartPredicate: RestartPredicate?
    ) {
        manager?.let {
            try {
                // Why getCancellationSignalObject returns an Object is unexplained
                (if (cancellationSignal == null) null else cancellationSignal.cancellationSignalObject as android.os.CancellationSignal?)
                    ?: throw IllegalArgumentException("CancellationSignal cann't be null")

                this.originalCancellationSignal = cancellationSignal
                authenticateInternal(biometricCryptoObject, listener, restartPredicate)
                return
            } catch (e: Throwable) {
                e(e, "$name: authenticate failed unexpectedly")
            }
        }
        listener?.onFailure(tag(), AuthenticationFailureReason.INTERNAL_ERROR, "Can't start authenticate for $name")
        return
    }

    private fun authenticateInternal(
        biometricCryptoObject: BiometricCryptoObject?,
        listener: AuthenticationListener?,
        restartPredicate: RestartPredicate?
    ) {
        d("$name.authenticate - $biometricMethod; Crypto=$biometricCryptoObject")
        manager?.let {
            try {
                val cancellationSignal = CancellationSignal()
                originalCancellationSignal?.setOnCancelListener {
                    if (!cancellationSignal.isCanceled)
                        cancellationSignal.cancel()
                }
                // Why getCancellationSignalObject returns an Object is unexplained
                val signalObject =
                    (cancellationSignal.cancellationSignalObject as android.os.CancellationSignal?)
                        ?: throw IllegalArgumentException("CancellationSignal cann't be null")
                val callback: BiometricManagerCompat.AuthenticationCallback =
                    AuthCallback(
                        biometricCryptoObject,
                        restartPredicate,
                        cancellationSignal,
                        listener
                    )

                // Occasionally, an NPE will bubble up out of FingerprintManager.authenticate
                val crypto = if (biometricCryptoObject == null) null else {
                    if (biometricCryptoObject.cipher != null)
                        BiometricManagerCompat.CryptoObject(biometricCryptoObject.cipher)
                    else if (biometricCryptoObject.mac != null)
                        BiometricManagerCompat.CryptoObject(biometricCryptoObject.mac)
                    else if (biometricCryptoObject.signature != null)
                        BiometricManagerCompat.CryptoObject(biometricCryptoObject.signature)
                    else
                        null
                }

                d("$name.authenticate:  Crypto=$crypto")
                authCallTimestamp.set(System.currentTimeMillis())
                it.authenticate(
                    crypto,
                    0,
                    signalObject,
                    callback,
                    ExecutorHelper.handler,
                    bundle ?: throw IllegalArgumentException("Bundle should be not NULL")
                )
                return
            } catch (e: Throwable) {
                e(e, "$name: authenticate failed unexpectedly")
            }
        }
        listener?.onFailure(tag(), AuthenticationFailureReason.INTERNAL_ERROR, "Can't start authenticate for $name")
        return
    }

    internal inner class AuthCallback(
        private val biometricCryptoObject: BiometricCryptoObject?,
        private val restartPredicate: RestartPredicate?,
        private val cancellationSignal: CancellationSignal?,
        private val listener: AuthenticationListener?
    ) : BiometricManagerCompat.AuthenticationCallback() {
        private var errorTs = 0L
        private val skipTimeout =
            context.resources.getInteger(android.R.integer.config_shortAnimTime)
        private var selfCanceled = false
        override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
            d("$name.onAuthenticationError: $errMsgId-$errString")
            val tmp = System.currentTimeMillis()
            if (tmp - errorTs <= skipTimeout)
                return
            errorTs = tmp
            var failureReason = AuthenticationFailureReason.UNKNOWN
            when (if (errMsgId < 1000) errMsgId else errMsgId % 1000) {
                FINGERPRINT_ERROR_NO_FINGERPRINTS -> failureReason =
                    AuthenticationFailureReason.NO_BIOMETRICS_REGISTERED

                FINGERPRINT_ERROR_HW_NOT_PRESENT -> failureReason =
                    AuthenticationFailureReason.NO_HARDWARE

                FINGERPRINT_ERROR_HW_UNAVAILABLE -> failureReason =
                    AuthenticationFailureReason.HARDWARE_UNAVAILABLE

                FINGERPRINT_ERROR_LOCKOUT_PERMANENT -> {
                    BiometricErrorLockoutPermanentFix.setBiometricSensorPermanentlyLocked(
                        biometricMethod.biometricType
                    )
                    failureReason = AuthenticationFailureReason.HARDWARE_UNAVAILABLE
                }

                FINGERPRINT_ERROR_UNABLE_TO_PROCESS -> failureReason =
                    AuthenticationFailureReason.SENSOR_FAILED

                FINGERPRINT_ERROR_NO_SPACE -> failureReason =
                    AuthenticationFailureReason.SENSOR_FAILED

                FINGERPRINT_ERROR_TIMEOUT -> failureReason =
                    AuthenticationFailureReason.TIMEOUT

                FINGERPRINT_ERROR_LOCKOUT -> {
                    lockout()
                    failureReason = AuthenticationFailureReason.LOCKED_OUT
                }

                FINGERPRINT_ERROR_USER_CANCELED -> {
                    if (!selfCanceled) {
                        listener?.onCanceled(
                            tag(),
                            AuthenticationFailureReason.CANCELED_BY_USER,
                            errString
                        )
                        Core.cancelAuthentication(this@SoterFingerprintUnlockModule)
                    }
                    return
                }

                FINGERPRINT_ERROR_CANCELED -> {
                    if (!selfCanceled) {
                        listener?.onCanceled(tag(), AuthenticationFailureReason.CANCELED, errString)
                        Core.cancelAuthentication(this@SoterFingerprintUnlockModule)
                    }
                    return
                }

                else -> {
                    if (!selfCanceled) {
                        listener?.onFailure(tag(), failureReason, "$errMsgId-$errString")
                        postCancelTask {

                            if (cancellationSignal?.isCanceled == false) {
                                selfCanceled = true
                                listener?.onCanceled(
                                    tag(),
                                    AuthenticationFailureReason.CANCELED,
                                    null
                                )
                                Core.cancelAuthentication(this@SoterFingerprintUnlockModule)
                            }
                        }
                    }
                    return
                }
            }
            if (restartCauseTimeout(failureReason)) {
                selfCanceled = true
                cancellationSignal?.cancel()
                ExecutorHelper.postDelayed({
                    authenticateInternal(biometricCryptoObject, listener, restartPredicate)
                }, skipTimeout.toLong())
            } else
                if (failureReason == AuthenticationFailureReason.TIMEOUT || restartPredicate?.invoke(
                        failureReason
                    ) == true
                ) {
                    listener?.onFailure(tag(), failureReason, "$errMsgId-$errString")
                    selfCanceled = true
                    cancellationSignal?.cancel()
                    ExecutorHelper.postDelayed({
                        authenticateInternal(biometricCryptoObject, listener, restartPredicate)
                    }, skipTimeout.toLong())
                } else {
                    if (mutableListOf(
                            AuthenticationFailureReason.SENSOR_FAILED,
                            AuthenticationFailureReason.AUTHENTICATION_FAILED
                        ).contains(failureReason)
                    ) {
                        lockout()
                        failureReason = AuthenticationFailureReason.LOCKED_OUT
                    }
                    listener?.onFailure(tag(), failureReason, "$errMsgId-$errString")
                    postCancelTask {

                        if (cancellationSignal?.isCanceled == false) {
                            selfCanceled = true
                            listener?.onCanceled(tag(), AuthenticationFailureReason.CANCELED, null)
                            Core.cancelAuthentication(this@SoterFingerprintUnlockModule)
                        }
                    }
                }
        }

        override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence?) {
            d("$name.onAuthenticationHelp: $helpMsgId-$helpString")
            listener?.onHelp(helpString)
        }

        override fun onAuthenticationSucceeded(result: BiometricManagerCompat.AuthenticationResult?) {
            d("$name.onAuthenticationSucceeded: $result; Crypto=${result?.cryptoObject}")
            val tmp = System.currentTimeMillis()
            if (tmp - errorTs <= skipTimeout || tmp - authCallTimestamp.get() <= skipTimeout)
                return
            errorTs = tmp
            listener?.onSuccess(
                tag(),
                BiometricCryptoObject(
                    result?.cryptoObject?.signature,
                    result?.cryptoObject?.cipher,
                    result?.cryptoObject?.mac
                )
            )
        }

        override fun onAuthenticationFailed() {
            d("$name.onAuthenticationFailed: ")
            listener?.onFailure(tag(), AuthenticationFailureReason.AUTHENTICATION_FAILED, null)
        }
    }

}