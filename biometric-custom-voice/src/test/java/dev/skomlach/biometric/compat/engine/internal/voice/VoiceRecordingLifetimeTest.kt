package dev.skomlach.biometric.compat.engine.internal.voice

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class VoiceRecordingLifetimeTest {
    @Test
    fun `cancellation stops a blocked read but release waits for the worker`() {
        val reading = CountDownLatch(1)
        val stopped = CountDownLatch(1)
        val allowReadToReturn = CountDownLatch(1)
        val released = AtomicBoolean(false)
        val lifetime = VoiceRecordingLifetime({}, { stopped.countDown() }, { released.set(true) })
        val worker = Thread {
            try {
                lifetime.start()
                reading.countDown()
                allowReadToReturn.await()
            } finally {
                lifetime.finish()
            }
        }
        worker.start()
        try {
            assertTrue(reading.await(2, TimeUnit.SECONDS))
            lifetime.cancel()
            assertTrue(stopped.await(2, TimeUnit.SECONDS))
            assertFalse(lifetime.isActive)
            assertFalse(released.get())
        } finally {
            allowReadToReturn.countDown()
            worker.join(2000)
        }
        assertFalse(worker.isAlive)
        assertTrue(released.get())
    }

    @Test
    fun `cancel before worker starts does not start recorder and releases once`() {
        val events = mutableListOf<String>()
        val lifetime = VoiceRecordingLifetime(
            { events += "start" }, { events += "stop" }, { events += "release" }
        )
        lifetime.cancel()
        assertFalse(lifetime.start())
        lifetime.finish()
        lifetime.finish()
        lifetime.cancel()
        assertEquals(listOf("release"), events)
    }

    @Test
    fun `old recording cleanup cannot stop a replacement recording`() {
        val events = mutableListOf<String>()
        val old = VoiceRecordingLifetime({}, { events += "old stop" }, { events += "old release" })
        val next = VoiceRecordingLifetime({}, { events += "next stop" }, { events += "next release" })
        old.start()
        old.cancel()
        next.start()
        old.finish()
        assertTrue(next.isActive)
        assertEquals(listOf("old stop", "old release"), events)
        next.finish()
    }

    @Test
    fun `cleanup releases even when stop fails`() {
        var released = false
        val lifetime = VoiceRecordingLifetime({}, { error("stop failed") }, { released = true })
        lifetime.start()
        assertThrows(IllegalStateException::class.java) { lifetime.finish() }
        assertTrue(released)
        assertFalse(lifetime.isActive)
    }
}
