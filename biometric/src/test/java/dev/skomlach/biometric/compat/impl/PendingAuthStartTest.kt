package dev.skomlach.biometric.compat.impl

import org.junit.Assert.*
import org.junit.Test

class PendingAuthStartTest {
    @Test fun cancellationInvalidatesEvenAnAlreadyDequeuedStart() {
        val queue = mutableListOf<Runnable>()
        val removed = mutableListOf<Runnable>()
        val starts = mutableListOf<String>()
        val pending = PendingAuthStart({ task, _ -> queue += task }, { removed += it })
        pending.schedule(500) { starts += "old" }
        val dequeued = queue.removeAt(0)
        pending.cancel()
        pending.schedule(500) { starts += "new" }
        dequeued.run()
        queue.removeAt(0).run()
        assertEquals(listOf("new"), starts)
        assertTrue(removed.contains(dequeued))
    }

    @Test fun reschedulingRunsOnlyTheLatestAttemptAndCancelIsIdempotent() {
        val queue = mutableListOf<Runnable>()
        var starts = 0
        val pending = PendingAuthStart({ task, _ -> queue += task }, {})
        pending.schedule(0) { starts++ }
        pending.schedule(0) { starts += 10 }
        queue.toList().forEach { it.run() }
        assertEquals(10, starts)
        pending.cancel()
        pending.cancel()
    }

    @Test fun canceledTimeoutDoesNotConsumeTheReplacementTimeout() {
        val queue = mutableListOf<Runnable>()
        val removed = mutableListOf<Runnable>()
        val timeout = PendingAuthStart({ task, _ -> queue += task }, { removed += it })
        var cancellations = 0
        timeout.schedule(30000) { fail("old session timeout") }
        val stale = queue.removeAt(0)
        timeout.cancel()
        timeout.schedule(30000) { cancellations++ }
        stale.run()
        assertEquals(0, cancellations)
        queue.removeAt(0).run()
        assertEquals(1, cancellations)
        assertTrue(removed.contains(stale))
    }

    @Test fun cancelingSensorStartLeavesTheFailureTimerActiveUntilSessionClose() {
        val queue = mutableListOf<Runnable>()
        val start = PendingAuthStart({ task, _ -> queue += task }, { queue.remove(it) })
        val failure = PendingAuthStart({ task, _ -> queue += task }, { queue.remove(it) })
        var failures = 0
        start.schedule(500) { fail("paused sensor started") }
        failure.schedule(2000) { failures++ }
        start.cancel()
        queue.removeAt(0).run()
        assertEquals(1, failures)

        failure.schedule(2000) { fail("closed session failed") }
        failure.cancel()
        assertTrue(queue.isEmpty())
    }
}
