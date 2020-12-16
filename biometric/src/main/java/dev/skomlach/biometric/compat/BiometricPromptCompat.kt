package dev.skomlach.biometric.compat

import android.annotation.TargetApi
import android.app.Activity
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.ViewTreeObserver.OnWindowFocusChangeListener
import androidx.annotation.ColorRes
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.FragmentActivity
import dev.skomlach.biometric.compat.engine.AuthenticationFailureReason
import dev.skomlach.biometric.compat.engine.BiometricAuthentication
import dev.skomlach.biometric.compat.engine.BiometricInitListener
import dev.skomlach.biometric.compat.engine.BiometricMethod
import dev.skomlach.biometric.compat.engine.internal.core.interfaces.BiometricModule
import dev.skomlach.biometric.compat.impl.BiometricPromptApi28Impl
import dev.skomlach.biometric.compat.impl.BiometricPromptGenericImpl
import dev.skomlach.biometric.compat.impl.IBiometricPromptImpl
import dev.skomlach.biometric.compat.impl.PermissionsFragment
import dev.skomlach.biometric.compat.utils.ActiveWindow
import dev.skomlach.biometric.compat.utils.BiometricErrorLockoutPermanentFix
import dev.skomlach.biometric.compat.utils.DeviceUnlockedReceiver
import dev.skomlach.biometric.compat.utils.HardwareAccessImpl
import dev.skomlach.biometric.compat.utils.logging.BiometricLoggerImpl
import dev.skomlach.common.misc.ExecutorHelper
import dev.skomlach.common.misc.Utils.startActivity
import dev.skomlach.common.misc.multiwindow.MultiWindowSupport
import java.util.*

class BiometricPromptCompat private constructor(private val impl: IBiometricPromptImpl) {
    companion object {
        private val pendingTasks: MutableList<Runnable?> = ArrayList()
        @Volatile var isInit = false
            private set

        @Volatile
        private var initInProgress = false
        fun init(execute: Runnable?) {
            if (isInit) {
                if (initInProgress) {
                    pendingTasks.add(execute)
                } else execute?.let { ExecutorHelper.INSTANCE.handler.post(it) }
            } else {
                initInProgress = true
                pendingTasks.add(execute)
                BiometricLoggerImpl.e("BiometricPromptCompat.init()")
                BiometricAuthentication.init(object : BiometricInitListener {
                    override fun initFinished(method: BiometricMethod, module: BiometricModule) {}
                    override fun onBiometricReady() {
                        isInit = true
                        initInProgress = false
                        for (task in pendingTasks) {
                            task?.let { ExecutorHelper.INSTANCE.handler.post(it) }
                        }
                        pendingTasks.clear()
                    }
                })
                DeviceUnlockedReceiver.registerDeviceUnlockListener()
            }
        }

        fun isBiometricSensorPermanentlyLocked(api: BiometricAuthRequest = BiometricAuthRequest(
                BiometricApi.AUTO,
                BiometricType.BIOMETRIC_UNDEFINED
            )): Boolean {
            check(isInit) { "Please call BiometricPromptCompat.init(null);  first" }
            return BiometricErrorLockoutPermanentFix.INSTANCE.isBiometricSensorPermanentlyLocked(api.type)
        }

        fun isHardwareDetected(api: BiometricAuthRequest = BiometricAuthRequest(
                BiometricApi.AUTO,
                BiometricType.BIOMETRIC_UNDEFINED
            )): Boolean {
            check(isInit) { "Please call BiometricPromptCompat.init(null);  first" }
            return HardwareAccessImpl.getInstance(api).isHardwareAvailable
        }

        fun hasEnrolled(api: BiometricAuthRequest = BiometricAuthRequest(
                BiometricApi.AUTO,
                BiometricType.BIOMETRIC_UNDEFINED
            )): Boolean {
            check(isInit) { "Please call BiometricPromptCompat.init(null);  first" }
            return HardwareAccessImpl.getInstance(api).isBiometricEnrolled
        }

        fun isLockOut(api: BiometricAuthRequest = BiometricAuthRequest(
                BiometricApi.AUTO,
                BiometricType.BIOMETRIC_UNDEFINED
            )): Boolean {
            check(isInit) { "Please call BiometricPromptCompat.init(null);  first" }
            return HardwareAccessImpl.getInstance(api).isLockedOut
        }

        fun isNewBiometricApi(api: BiometricAuthRequest = BiometricAuthRequest(
                BiometricApi.AUTO,
                BiometricType.BIOMETRIC_UNDEFINED
            )): Boolean {
            check(isInit) { "Please call BiometricPromptCompat.init(null);  first" }
            return HardwareAccessImpl.getInstance(api).isNewBiometricApi
        }

        fun openSettings(activity: Activity, api: BiometricAuthRequest = BiometricAuthRequest(
            BiometricApi.AUTO,
            BiometricType.BIOMETRIC_UNDEFINED
        )) {
            check(isInit) { "Please call BiometricPromptCompat.init(null);  first" }
            if (!HardwareAccessImpl.getInstance(api).isNewBiometricApi) {
                BiometricAuthentication.openSettings(activity)
            } else {
                //for unknown reasons on some devices happens SecurityException - "Permission.MANAGE_BIOMETRIC required" - but not should be
                if (startActivity(Intent("android.settings.BIOMETRIC_ENROLL"), activity)) {
                    return
                }
                if (startActivity(Intent("android.settings.BIOMETRIC_SETTINGS"), activity)) {
                    return
                }
                if (startActivity(
                        Intent().setComponent(
                            ComponentName(
                                "com.android.settings",
                                "com.android.settings.Settings\$BiometricsAndSecuritySettingsActivity"
                            )
                        ), activity
                    )
                ) {
                    return
                }
                if (startActivity(
                        Intent().setComponent(
                            ComponentName(
                                "com.android.settings",
                                "com.android.settings.Settings\$SecuritySettingsActivity"
                            )
                        ), activity
                    )
                ) {
                    return
                }
                startActivity(
                    Intent(Settings.ACTION_SETTINGS), activity
                )
            }
        }

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    fun authenticate(callback: Result) {
        if (!isHardwareDetected(impl.builder.biometricAuthRequest)) {
            callback.onFailed(AuthenticationFailureReason.NO_HARDWARE)
            return
        }
        if (!hasEnrolled(impl.builder.biometricAuthRequest)) {
            callback.onFailed(AuthenticationFailureReason.NO_BIOMETRICS_REGISTERED)
            return
        }
        if (isLockOut(impl.builder.biometricAuthRequest)) {
            callback.onFailed(AuthenticationFailureReason.LOCKED_OUT)
            return
        }
        if (isBiometricSensorPermanentlyLocked(impl.builder.biometricAuthRequest)) {
            callback.onFailed(AuthenticationFailureReason.HARDWARE_UNAVAILABLE)
            return
        }
        PermissionsFragment.askForPermissions(
            impl.builder.context,
            impl.usedPermissions
        ) { authenticateInternal(callback) }
    }

    private fun authenticateInternal(callback: Result) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val d = ActiveWindow.getActiveView(impl.builder.context)
            if (!d.isAttachedToWindow) {
                checkForAttachAndStart(d, callback)
            } else {
                checkForFocusAndStart(callback)
            }
        } else {
            impl.authenticate(callback)
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private fun checkForAttachAndStart(d: View, callback: Result) {
        d.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                d.removeOnAttachStateChangeListener(this)
                checkForFocusAndStart(callback)
                d.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) {}
                    override fun onViewDetachedFromWindow(v: View) {
                        d.removeOnAttachStateChangeListener(this)
                        impl.cancelAuthenticate()
                    }
                })
            }

            override fun onViewDetachedFromWindow(v: View) {
                d.removeOnAttachStateChangeListener(this)
            }
        })
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private fun checkForFocusAndStart(callback: Result) {
        val activity = ActiveWindow.getActiveView(impl.builder.context)
        if (!activity.hasWindowFocus()) {
            val windowFocusChangeListener: OnWindowFocusChangeListener =
                object : OnWindowFocusChangeListener {
                    override fun onWindowFocusChanged(focus: Boolean) {
                        if (activity.hasWindowFocus()) {
                            activity.viewTreeObserver.removeOnWindowFocusChangeListener(this)
                            impl.authenticate(callback)
                        }
                    }
                }
            activity.viewTreeObserver.addOnWindowFocusChangeListener(windowFocusChangeListener)
        } else {
            impl.authenticate(callback)
        }
    }

    fun cancelAuthenticate() {
        impl.cancelAuthenticate()
    }

    fun cancelAuthenticateBecauseOnPause(): Boolean {
        return impl.cancelAuthenticateBecauseOnPause()
    }

    @ColorRes fun getDialogMainColor(): Int {
        return if (impl.isNightMode) {
            android.R.color.black
        } else {
            R.color.material_grey_50
        }
    }
    
    interface Result {
        @MainThread
        fun onSucceeded()

        @MainThread
        fun onCanceled()

        @MainThread
        fun onFailed(reason: AuthenticationFailureReason?)

        @MainThread
        fun onUIShown()
    }

    class Builder(
        @field:RestrictTo(RestrictTo.Scope.LIBRARY) val biometricAuthRequest: BiometricAuthRequest,
        @field:RestrictTo(
            RestrictTo.Scope.LIBRARY
        ) val context: FragmentActivity
    ) {
        @JvmField @RestrictTo(RestrictTo.Scope.LIBRARY)
        var title: CharSequence? = null

        @JvmField @RestrictTo(RestrictTo.Scope.LIBRARY)
        var subtitle: CharSequence? = null

        @JvmField @RestrictTo(RestrictTo.Scope.LIBRARY)
        var description: CharSequence? = null

        @JvmField @RestrictTo(RestrictTo.Scope.LIBRARY)
        var negativeButtonText: CharSequence? = null

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        var negativeButtonListener: DialogInterface.OnClickListener? = null

        @JvmField @RestrictTo(RestrictTo.Scope.LIBRARY)
        var multiWindowSupport: MultiWindowSupport = MultiWindowSupport(context)

        constructor(context: FragmentActivity) : this(
            BiometricAuthRequest(
                BiometricApi.AUTO,
                BiometricType.BIOMETRIC_UNDEFINED
            ), context
        ) {
        }

        fun setTitle(title: CharSequence?): Builder {
            this.title = title
            return this
        }

        fun setTitle(@StringRes titleRes: Int): Builder {
            title = context.getString(titleRes)
            return this
        }

        fun setSubtitle(subtitle: CharSequence?): Builder {
            this.subtitle = subtitle
            return this
        }

        fun setSubtitle(@StringRes subtitleRes: Int): Builder {
            subtitle = context.getString(subtitleRes)
            return this
        }

        fun setDescription(description: CharSequence?): Builder {
            this.description = description
            return this
        }

        fun setDescription(@StringRes descriptionRes: Int): Builder {
            description = context.getString(descriptionRes)
            return this
        }

        fun setNegativeButton(
            text: CharSequence,
            listener: DialogInterface.OnClickListener?
        ): Builder {
            negativeButtonText = text
            negativeButtonListener = listener
            return this
        }

        fun setNegativeButton(
            @StringRes textResId: Int,
            listener: DialogInterface.OnClickListener?
        ): Builder {
            negativeButtonText = context.getString(textResId)
            negativeButtonListener = listener
            return this
        }

        fun build(): BiometricPromptCompat {
            check(isInit) { "Please call BiometricPromptCompat.init(null);  first" }
            requireNotNull(title) { "You should set a title for BiometricPrompt." }
            requireNotNull(negativeButtonText) { "You should set a negativeButtonText for BiometricPrompt." }
            return if (biometricAuthRequest.api === BiometricApi.BIOMETRIC_API
                || (biometricAuthRequest.api === BiometricApi.AUTO
                        && isNewBiometricApi(biometricAuthRequest))
            ) {
                BiometricPromptCompat(BiometricPromptApi28Impl(this))
            } else {
                BiometricPromptCompat(BiometricPromptGenericImpl(this))
            }
        }
    }
}