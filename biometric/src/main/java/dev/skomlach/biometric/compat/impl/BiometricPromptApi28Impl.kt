package dev.skomlach.biometric.compat.impl

import android.annotation.TargetApi
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import dev.skomlach.biometric.compat.BiometricConfirmation
import dev.skomlach.biometric.compat.BiometricPromptCompat
import dev.skomlach.biometric.compat.BiometricType
import dev.skomlach.biometric.compat.R
import dev.skomlach.biometric.compat.engine.*
import dev.skomlach.biometric.compat.engine.internal.core.RestartPredicatesImpl.defaultPredicate
import dev.skomlach.biometric.compat.impl.dialogs.BiometricPromptCompatDialogImpl
import dev.skomlach.biometric.compat.utils.BiometricAuthWasCanceledByError
import dev.skomlach.biometric.compat.utils.BiometricErrorLockoutPermanentFix
import dev.skomlach.biometric.compat.utils.CodeToString.getErrorCode
import dev.skomlach.biometric.compat.utils.DevicesWithKnownBugs.isDeviceWithMissedBiometricUI
import dev.skomlach.biometric.compat.utils.DevicesWithKnownBugs.isOnePlusWithBiometricBug
import dev.skomlach.biometric.compat.utils.HardwareAccessImpl.Companion.getInstance
import dev.skomlach.biometric.compat.utils.Vibro
import dev.skomlach.biometric.compat.utils.WindowFocusChangedListener
import dev.skomlach.biometric.compat.utils.logging.BiometricLoggerImpl.d
import dev.skomlach.biometric.compat.utils.logging.BiometricLoggerImpl.e
import dev.skomlach.biometric.compat.utils.notification.BiometricNotificationManager
import dev.skomlach.biometric.compat.utils.themes.DarkLightThemes.isNightMode
import dev.skomlach.common.misc.ExecutorHelper
import dev.skomlach.common.misc.Utils.isAtLeastR
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

@TargetApi(Build.VERSION_CODES.P)
@RestrictTo(RestrictTo.Scope.LIBRARY)
class BiometricPromptApi28Impl(override val builder: BiometricPromptCompat.Builder) :
    IBiometricPromptImpl, BiometricCodes, AuthCallback {
    private val biometricPromptInfo: PromptInfo
    private val biometricPrompt: BiometricPrompt
    private val restartPredicate = defaultPredicate()
    private var dialog: BiometricPromptCompatDialogImpl? = null
    private var callback: BiometricPromptCompat.Result? = null
    private val confirmed: MutableSet<BiometricType?> = java.util.HashSet()
    private val fmAuthCallback: BiometricAuthenticationListener =
        BiometricAuthenticationCallbackImpl()
    val authCallback: BiometricPrompt.AuthenticationCallback =
        object : BiometricPrompt.AuthenticationCallback() {
            //https://forums.oneplus.com/threads/oneplus-7-pro-fingerprint-biometricprompt-does-not-show.1035821/
            private var onePlusWithBiometricBugFailure = false
            override fun onAuthenticationFailed() {
                d("BiometricPromptApi28Impl.onAuthenticationFailed")
                if (isOnePlusWithBiometricBug) {
                    onePlusWithBiometricBugFailure = true
                    cancelAuthenticate()
                } else {
                    //...normal failed processing...//
                    if (dialog != null) dialog?.onFailure(false, builder.biometricAuthRequest.type)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                d("BiometricPromptApi28Impl.onAuthenticationError: " + getErrorCode(errorCode) + " " + errString)
                // Authentication failed on OnePlus device with broken BiometricPrompt implementation
                // Present the same screen with additional buttons to allow retry/fail
                if (onePlusWithBiometricBugFailure) {
                    onePlusWithBiometricBugFailure = false
                    //...present retryable error screen...
                    return
                }
                //...present normal failed screen...
                
                ExecutorHelper.INSTANCE.handler.post(Runnable {
                    var failureReason = AuthenticationFailureReason.UNKNOWN
                    when (errorCode) {
                        BiometricCodes.BIOMETRIC_ERROR_NO_BIOMETRICS -> failureReason =
                            AuthenticationFailureReason.NO_BIOMETRICS_REGISTERED
                        BiometricCodes.BIOMETRIC_ERROR_HW_NOT_PRESENT -> failureReason =
                            AuthenticationFailureReason.NO_HARDWARE
                        BiometricCodes.BIOMETRIC_ERROR_HW_UNAVAILABLE -> failureReason =
                            AuthenticationFailureReason.HARDWARE_UNAVAILABLE
                        BiometricCodes.BIOMETRIC_ERROR_LOCKOUT_PERMANENT -> {
                            BiometricErrorLockoutPermanentFix.INSTANCE.setBiometricSensorPermanentlyLocked(
                                builder.biometricAuthRequest.type
                            )
                            failureReason = AuthenticationFailureReason.HARDWARE_UNAVAILABLE
                        }
                        BiometricCodes.BIOMETRIC_ERROR_UNABLE_TO_PROCESS, BiometricCodes.BIOMETRIC_ERROR_NO_SPACE -> failureReason =
                            AuthenticationFailureReason.SENSOR_FAILED
                        BiometricCodes.BIOMETRIC_ERROR_TIMEOUT -> failureReason =
                            AuthenticationFailureReason.TIMEOUT
                        BiometricCodes.BIOMETRIC_ERROR_LOCKOUT -> {
                            getInstance(builder.biometricAuthRequest).lockout()
                            failureReason = AuthenticationFailureReason.LOCKED_OUT
                        }
                        BiometricCodes.BIOMETRIC_ERROR_USER_CANCELED, BiometricCodes.BIOMETRIC_ERROR_NEGATIVE_BUTTON -> {
                            callback?.onCanceled()
                            cancelAuthenticate()
                            return@Runnable
                        }
                        BiometricCodes.BIOMETRIC_ERROR_CANCELED ->                             // Don't send a cancelled message.
                            return@Runnable
                    }
                    if (restartPredicate.invoke(failureReason)) {
                        if (callback != null) {
                            if (dialog != null) dialog?.onFailure(
                                failureReason == AuthenticationFailureReason.LOCKED_OUT,
                                builder.biometricAuthRequest.type
                            )
                            authenticate(callback)
                        }
                    } else {
                        when (failureReason) {
                            AuthenticationFailureReason.SENSOR_FAILED, AuthenticationFailureReason.AUTHENTICATION_FAILED -> {
                                getInstance(builder.biometricAuthRequest).lockout()
                                failureReason = AuthenticationFailureReason.LOCKED_OUT
                            }
                        }
                        if (dialog != null) dialog?.onFailure(
                            failureReason == AuthenticationFailureReason.LOCKED_OUT,
                            builder.biometricAuthRequest.type
                        )
                        callback?.onFailed(failureReason)
                        BiometricAuthWasCanceledByError.INSTANCE.setCanceledByError()
                        if (failureReason == AuthenticationFailureReason.LOCKED_OUT) {
                            ExecutorHelper.INSTANCE.handler.postDelayed({
                                callback?.onCanceled()
                                cancelAuthenticate()
                            }, 2000)
                        } else {
                            callback?.onCanceled()
                            cancelAuthenticate()
                        }
                    }
                })
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                d("BiometricPromptApi28Impl.onAuthenticationSucceeded:")
                onePlusWithBiometricBugFailure = false
                
                var addded = false
                for(module in builder.primaryAvailableTypes) {
                    if(confirmed.add(module))
                        addded = true
                }
                if(addded)
                    Vibro.start()
                val confirmedList: List<BiometricType?> = ArrayList(confirmed)
                val allList: MutableList<BiometricType?> = ArrayList(
                    builder.allAvailableTypes
                )
                allList.removeAll(confirmedList)
                if (builder.biometricAuthRequest.confirmation == BiometricConfirmation.ANY ||
                    builder.biometricAuthRequest.confirmation == BiometricConfirmation.ALL && allList.isEmpty()
                ) {
                    ExecutorHelper.INSTANCE.handler.post {
                        cancelAuthenticate()
                        callback?.onSucceeded()
                    }
                } else {
                    if(dialog == null) {
                        dialog =
                            BiometricPromptCompatDialogImpl(
                                builder, this@BiometricPromptApi28Impl,
                                builder.secondaryAvailableTypes.contains(BiometricType.BIOMETRIC_FINGERPRINT)
                            )
                    }
                    dialog?.showDialog()
                }
            }
        }
    private val isFingerprint = AtomicBoolean(false)

    init {
        val promptInfoBuilder = PromptInfo.Builder()
        builder.title?.let {
            promptInfoBuilder.setTitle(it)
        }

        builder.subtitle?.let {
            promptInfoBuilder.setSubtitle(it)
        }

        builder.description?.let {
            promptInfoBuilder.setDescription(it)
        }
        builder.negativeButtonText?.let {
            if (isAtLeastR) promptInfoBuilder.setNegativeButtonText(it) else promptInfoBuilder.setNegativeButtonText(
                getFixedString(
                    it, ContextCompat.getColor(
                        builder.context, R.color.material_deep_teal_500
                    )
                )
            )
        }
        if (isAtLeastR) {
            promptInfoBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        } else {
            promptInfoBuilder.setDeviceCredentialAllowed(false)
        }
        promptInfoBuilder.setConfirmationRequired(true)
        biometricPromptInfo = promptInfoBuilder.build()
        biometricPrompt = BiometricPrompt(
            builder.context,
            ExecutorHelper.INSTANCE.executor, authCallback
        )
        isFingerprint.set(builder.primaryAvailableTypes.contains(BiometricType.BIOMETRIC_FINGERPRINT))
    }
    private fun getFixedString(str: CharSequence?, @ColorInt color: Int): CharSequence {
        val wordtoSpan: Spannable = SpannableString(str)
        wordtoSpan.setSpan(
            ForegroundColorSpan(color),
            0,
            wordtoSpan.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return wordtoSpan
    }

    override fun authenticate(cbk: BiometricPromptCompat.Result?) {
        try {
            d("BiometricPromptApi28Impl.authenticate():")
            callback = cbk
            if (isDeviceWithMissedBiometricUI && isFingerprint.get()) {
                //LG G8 and OnePlus devices do not have BiometricPrompt UI
                dialog =
                    BiometricPromptCompatDialogImpl(builder, this@BiometricPromptApi28Impl, isFingerprint.get())
                dialog?.showDialog()
                startAuth()
            } else {
                startAuth()
            }
            onUiOpened()
        } catch (e: Throwable) {
            e(e)
            callback?.onFailed(AuthenticationFailureReason.UNKNOWN)
        }
    }

    override fun cancelAuthenticateBecauseOnPause(): Boolean {
        d("BiometricPromptApi28Impl.cancelAuthenticateBecauseOnPause():")
        return if (dialog != null) {
            dialog?.cancelAuthenticateBecauseOnPause() == true
        } else {
            cancelAuthenticate()
            true
        }
    }

    override val isNightMode: Boolean
        get() = if (dialog != null) dialog?.isNightMode == true else {
            isNightMode(builder.context)
        }
    override val usedPermissions: List<String>
        get() {
            val permission: MutableSet<String> = HashSet()
            permission.add("android.permission.USE_FINGERPRINT")
            if (Build.VERSION.SDK_INT >= 28) {
                permission.add("android.permission.USE_BIOMETRIC")
            }
            return ArrayList(permission)
        }

    override fun cancelAuthenticate() {
        d("BiometricPromptApi28Impl.cancelAuthenticate():")
        if (dialog != null) dialog?.dismissDialog() else {
            stopAuth()
        }
        onUiClosed()
    }

    override fun startAuth() {
        d("BiometricPromptApi28Impl.startAuth():")
        if (builder.notificationEnabled) {
            BiometricNotificationManager.INSTANCE.showNotification(builder)
        }
        val secondary = HashSet<BiometricType>(builder.secondaryAvailableTypes)
        secondary.removeAll(builder.primaryAvailableTypes)
        if(secondary.isNotEmpty()){
            d("BiometricPromptApi28Impl.startAuth(): - secondaryAvailableTypes - secondary $secondary; primary - ${builder.primaryAvailableTypes}")
            BiometricAuthentication.authenticate(null, ArrayList<BiometricType>(secondary), fmAuthCallback)
        }
        biometricPrompt.authenticate(biometricPromptInfo)
    }

    override fun stopAuth() {
        d("BiometricPromptApi28Impl.stopAuth():")
        BiometricAuthentication.cancelAuthentication()
        biometricPrompt.cancelAuthentication()
        if (builder.notificationEnabled) {
            BiometricNotificationManager.INSTANCE.dismissAll()
        }
    }

    override fun cancelAuth() {
        callback?.onCanceled()
    }

    override fun onUiOpened() {
        callback?.onUIOpened()
    }

    override fun onUiClosed() {
        callback?.onUIClosed()
    }

    private inner class BiometricAuthenticationCallbackImpl : BiometricAuthenticationListener {

        override fun onSuccess(module: BiometricType?) {
            if(confirmed.add(module))
                Vibro.start()
            val confirmedList: List<BiometricType?> = ArrayList(confirmed)
            val allList: MutableList<BiometricType?> = ArrayList(
                builder.allAvailableTypes
            )
            allList.removeAll(confirmedList)
            if (builder.biometricAuthRequest.confirmation == BiometricConfirmation.ANY ||
                builder.biometricAuthRequest.confirmation == BiometricConfirmation.ALL && allList.isEmpty()
            ) {
                ExecutorHelper.INSTANCE.handler.post {
                    cancelAuthenticate()
                    callback?.onSucceeded()
                }
            }
        }

        override fun onHelp(helpReason: AuthenticationHelpReason?, msg: CharSequence?) {
            if (helpReason !== AuthenticationHelpReason.BIOMETRIC_ACQUIRED_GOOD && !TextUtils.isEmpty(
                    msg
                )
            ) {
                if (dialog != null) dialog?.onHelp(msg)
            }
        }

        override fun onFailure(
            failureReason: AuthenticationFailureReason?,
            module: BiometricType?
        ) {
            if (dialog != null) {
                dialog?.onFailure(failureReason == AuthenticationFailureReason.LOCKED_OUT, module)
            }
            if (failureReason !== AuthenticationFailureReason.LOCKED_OUT) {
                //non fatal
                when (failureReason) {
                    AuthenticationFailureReason.SENSOR_FAILED, AuthenticationFailureReason.AUTHENTICATION_FAILED -> return
                }
                ExecutorHelper.INSTANCE.handler.post {
                    cancelAuthenticate()
                    callback?.onFailed(failureReason)
                }
            } else {
                getInstance(builder.biometricAuthRequest).lockout()
                ExecutorHelper.INSTANCE.handler.postDelayed({
                    cancelAuthenticate()
                    callback?.onFailed(failureReason)
                }, 2000)
            }
        }
    }
}