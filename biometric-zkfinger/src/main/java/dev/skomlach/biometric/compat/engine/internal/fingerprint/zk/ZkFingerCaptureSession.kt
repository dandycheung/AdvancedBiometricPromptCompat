package dev.skomlach.biometric.compat.engine.internal.fingerprint.zk

import java.util.concurrent.atomic.AtomicBoolean

/** Routes vendor callbacks to the native worker without carrying them into a later session. */
internal class ZkFingerCaptureSession(private val enqueue: (() -> Unit) -> Unit) {
    private val active = AtomicBoolean(true)

    val isActive: Boolean get() = active.get()

    fun post(action: () -> Unit) {
        if (!isActive) return
        enqueue { if (isActive) action() }
    }

    fun postTemplate(template: ByteArray?, consume: (ByteArray) -> Unit) {
        if (!isActive || template == null) return
        // Vendor SDKs may reuse the callback buffer as soon as extractOK returns.
        val snapshot = template.copyOf()
        post { consume(snapshot) }
    }

    fun invalidate(): Boolean = active.getAndSet(false)
}
