package dev.skomlach.biometric.compat.impl

import java.util.concurrent.atomic.AtomicLong

/** Owns both the queued runnable and its generation, including already-dequeued work. */
internal class PendingAuthStart(
    private val post: (Runnable, Long) -> Unit,
    private val remove: (Runnable) -> Unit
) {
    private var generation = 0L
    private var pending: Runnable? = null

    @Synchronized
    fun schedule(delayMillis: Long, start: () -> Unit) {
        cancel()
        val expected = generation
        val task = Runnable {
            synchronized(this) {
                if (generation != expected) return@Runnable
                pending = null
                start()
            }
        }
        pending = task
        post(task, delayMillis)
    }

    @Synchronized
    fun cancel() {
        generation++
        pending?.let(remove)
        pending = null
    }
}

/** Owns a callback generation together with results that must not cross session boundaries. */
internal class AuthSessionState<T> {
    private val generation = AtomicLong(0L)
    private val results = LinkedHashSet<T>()

    @Synchronized
    fun begin(): Long {
        results.clear()
        return generation.incrementAndGet()
    }

    @Synchronized
    fun invalidate() {
        generation.incrementAndGet()
        results.clear()
    }

    fun owns(expectedGeneration: Long): Boolean =
        expectedGeneration > 0L && generation.get() == expectedGeneration

    @Synchronized
    fun add(expectedGeneration: Long, value: T): Boolean {
        if (!owns(expectedGeneration)) return false
        results.add(value)
        return true
    }

    @Synchronized
    fun snapshot(expectedGeneration: Long): Set<T> {
        return if (owns(expectedGeneration)) LinkedHashSet(results) else emptySet()
    }
}
