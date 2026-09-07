package dev.skomlach.biometric.compat.engine.internal.face.tensorflow.provider

/** Keeps a reader alive until the asynchronous consumer releases its acquired image. */
internal class FrameResourceLifetime(private val closeResource: () -> Unit) {
    private var closed = false
    private var released = false
    private var frameAcquired = false

    @Synchronized
    fun <T> acquireFrame(acquire: () -> T?): T? {
        if (closed || frameAcquired) return null
        val frame = acquire() ?: return null
        frameAcquired = true
        return frame
    }

    @Synchronized
    fun completeFrame(closeFrame: () -> Unit) {
        if (!frameAcquired) return
        try {
            closeFrame()
        } finally {
            frameAcquired = false
            releaseIfIdle()
        }
    }

    @Synchronized
    fun close() {
        closed = true
        releaseIfIdle()
    }

    @Synchronized
    fun isOpen(): Boolean = !closed

    private fun releaseIfIdle() {
        if (closed && !frameAcquired && !released) {
            released = true
            closeResource()
        }
    }
}
