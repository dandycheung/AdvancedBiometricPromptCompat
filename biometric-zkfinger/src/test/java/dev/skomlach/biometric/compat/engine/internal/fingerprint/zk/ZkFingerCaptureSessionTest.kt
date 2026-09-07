package dev.skomlach.biometric.compat.engine.internal.fingerprint.zk

import org.junit.Assert.*
import org.junit.Test

class ZkFingerCaptureSessionTest {
    @Test
    fun `vendor buffer is copied before the callback returns`() {
        val queue = ArrayDeque<() -> Unit>()
        val session = ZkFingerCaptureSession { queue.addLast(it) }
        val buffer = byteArrayOf(1, 2, 3)
        var result: ByteArray? = null
        session.postTemplate(buffer) { result = it }
        buffer.fill(0)
        queue.removeFirst().invoke()
        assertArrayEquals(byteArrayOf(1, 2, 3), result)
    }

    @Test
    fun `queued templates and errors from a retired session cannot affect its replacement`() {
        val queue = ArrayDeque<() -> Unit>()
        val events = mutableListOf<String>()
        val old = ZkFingerCaptureSession { queue.addLast(it) }
        old.postTemplate(byteArrayOf(1)) { events += "old template" }
        old.post { events += "old error" }
        assertTrue(old.invalidate())
        assertFalse(old.invalidate())
        val next = ZkFingerCaptureSession { queue.addLast(it) }
        next.postTemplate(byteArrayOf(2)) { events += "new template" }
        old.post { events += "late callback" }
        while (queue.isNotEmpty()) queue.removeFirst().invoke()
        assertEquals(listOf("new template"), events)
        assertTrue(next.isActive)
    }

    @Test
    fun `cancelled session never opens native resources from a queued permission grant`() {
        val queue = ArrayDeque<() -> Unit>()
        var opened = false
        val session = ZkFingerCaptureSession { queue.addLast(it) }
        session.post { opened = true }
        session.invalidate()
        while (queue.isNotEmpty()) queue.removeFirst().invoke()
        assertFalse(opened)
    }
}
