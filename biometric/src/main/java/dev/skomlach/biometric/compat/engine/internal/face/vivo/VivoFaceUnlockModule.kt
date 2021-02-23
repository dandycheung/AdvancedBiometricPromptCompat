package dev.skomlach.biometric.compat.engine.internal.face.vivo

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.core.os.CancellationSignal
import com.vivo.framework.facedetect.FaceDetectManager
import com.vivo.framework.facedetect.FaceDetectManager.Companion.getInstance
import com.vivo.framework.facedetect.FaceDetectManager.FaceAuthenticationCallback
import dev.skomlach.biometric.compat.engine.AuthenticationFailureReason
import dev.skomlach.biometric.compat.engine.BiometricInitListener
import dev.skomlach.biometric.compat.engine.BiometricMethod
import dev.skomlach.biometric.compat.engine.internal.AbstractBiometricModule
import dev.skomlach.biometric.compat.engine.internal.core.interfaces.AuthenticationListener
import dev.skomlach.biometric.compat.engine.internal.core.interfaces.RestartPredicate
import dev.skomlach.biometric.compat.utils.logging.BiometricLoggerImpl.d
import dev.skomlach.biometric.compat.utils.logging.BiometricLoggerImpl.e
import me.weishu.reflection.Reflection

@RestrictTo(RestrictTo.Scope.LIBRARY)
class VivoFaceUnlockModule @SuppressLint("WrongConstant") constructor(listener: BiometricInitListener?) :
    AbstractBiometricModule(BiometricMethod.FACE_VIVO) {
    private var manager: FaceDetectManager? = null

    init {
        Reflection.unseal(context, listOf("com.vivo.framework.facedetect"))
        manager = try {
            getInstance()
        } catch (ignore: Throwable) {
            null
        }
        listener?.initFinished(biometricMethod, this@VivoFaceUnlockModule)
    }
    override val isManagerAccessible: Boolean
        get() = manager != null
    override val isHardwarePresent: Boolean
        get() {

                try {
                    return manager?.isFaceUnlockEnable == true
                } catch (e: Throwable) {
                    e(e, name)
                }

            return false
        }

    override fun hasEnrolled(): Boolean {

            try {
                return manager?.isFaceUnlockEnable == true && manager?.hasFaceID() == true
            } catch (e: Throwable) {
                e(e, name)
            }

        return false
    }

    @Throws(SecurityException::class)
    override fun authenticate(
        cancellationSignal: CancellationSignal?,
        listener: AuthenticationListener?,
        restartPredicate: RestartPredicate?
    ) {
        d("$name.authenticate - $biometricMethod")
        manager?.let{
            try {
                val callback: FaceAuthenticationCallback =
                    AuthCallback(restartPredicate, cancellationSignal, listener)

                // Why getCancellationSignalObject returns an Object is unexplained
                val signalObject =
                    (if (cancellationSignal == null) null else cancellationSignal.cancellationSignalObject as android.os.CancellationSignal?)
                        ?: throw IllegalArgumentException("CancellationSignal cann't be null")

                // Occasionally, an NPE will bubble up out of SemBioSomeManager.authenticate
                it.startFaceUnlock(callback)
                return
            } catch (e: Throwable) {
                e(e, "$name: authenticate failed unexpectedly")
            }
        }
        listener?.onFailure(AuthenticationFailureReason.UNKNOWN, tag())
        return
    }

    internal inner class AuthCallback(
        private val restartPredicate: RestartPredicate?,
        private val cancellationSignal: CancellationSignal?,
        private val listener: AuthenticationListener?
    ) : FaceAuthenticationCallback() {
        override fun onFaceAuthenticationResult(errorCode: Int, retry_times: Int) {
            d("$name.onFaceAuthenticationResult: $errorCode-$retry_times")
            if (errorCode == FaceDetectManager.FACE_DETECT_SUCEESS) {
                listener?.onSuccess(tag())
                return
            }
            var failureReason = AuthenticationFailureReason.UNKNOWN
            when (errorCode) {
                FaceDetectManager.FACE_DETECT_NO_FACE -> failureReason =
                    AuthenticationFailureReason.SENSOR_FAILED
                FaceDetectManager.FACE_DETECT_BUSY -> failureReason =
                    AuthenticationFailureReason.HARDWARE_UNAVAILABLE
                FaceDetectManager.FACE_DETECT_FAILED -> failureReason =
                    AuthenticationFailureReason.AUTHENTICATION_FAILED
            }
            if (restartPredicate?.invoke(failureReason) == true) {
                listener?.onFailure(failureReason, tag())
                authenticate(cancellationSignal, listener, restartPredicate)
            } else {
                when (failureReason) {
                    AuthenticationFailureReason.SENSOR_FAILED, AuthenticationFailureReason.AUTHENTICATION_FAILED -> {
                        lockout()
                        failureReason = AuthenticationFailureReason.LOCKED_OUT
                    }
                }
                listener?.onFailure(failureReason, tag())
            }
        }
    }

}