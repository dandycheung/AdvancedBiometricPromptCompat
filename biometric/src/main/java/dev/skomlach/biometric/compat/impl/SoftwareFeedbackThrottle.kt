package dev.skomlach.biometric.compat.impl

/** Limits toast feedback from frame-level software biometric callbacks. */
internal class SoftwareFeedbackThrottle {
    private var lastShownAtMs: Long? = null

    @Synchronized
    fun tryAcquire(nowMs: Long): Boolean {
        lastShownAtMs?.let { last ->
            if (nowMs - last < 4_000L) return false
        }
        lastShownAtMs = nowMs
        return true
    }

    @Synchronized
    fun remainingDelay(nowMs: Long): Long = lastShownAtMs?.let {
        (4_000L - (nowMs - it)).coerceAtLeast(0L)
    } ?: 0L

    @Synchronized
    fun reset() {
        lastShownAtMs = null
    }
}
