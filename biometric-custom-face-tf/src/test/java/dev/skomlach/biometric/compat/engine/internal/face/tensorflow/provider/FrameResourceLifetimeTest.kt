package dev.skomlach.biometric.compat.engine.internal.face.tensorflow.provider

import org.junit.Assert.*
import org.junit.Test

class FrameResourceLifetimeTest {
    @Test
    fun stopDefersReaderCloseUntilTheDetectorReleasesItsImage() {
        val events = mutableListOf<String>()
        val lifetime = FrameResourceLifetime { events += "reader closed" }
        assertEquals("image", lifetime.acquireFrame { "image" })
        lifetime.close()
        assertEquals(emptyList<String>(), events)
        assertNull(lifetime.acquireFrame { error("closed reader accessed") })
        assertFalse(lifetime.isOpen())
        lifetime.completeFrame { events += "image closed" }
        assertEquals(listOf("image closed", "reader closed"), events)
        lifetime.close()
        assertEquals(2, events.size)
    }

    @Test
    fun completedFrameAllowsTheNextFrameWithoutClosingAnActiveReader() {
        var readerCloses = 0
        val lifetime = FrameResourceLifetime { readerCloses++ }
        assertEquals(1, lifetime.acquireFrame { 1 })
        assertNull(lifetime.acquireFrame { error("two simultaneous frame consumers") })
        assertTrue(lifetime.isOpen())
        lifetime.completeFrame { }
        assertEquals(0, readerCloses)
        assertEquals(2, lifetime.acquireFrame { 2 })
        lifetime.completeFrame { }
        lifetime.close()
        assertEquals(1, readerCloses)
    }

    @Test
    fun missingOrFailedAcquisitionDoesNotRetainTheReader() {
        var readerCloses = 0
        val lifetime = FrameResourceLifetime { readerCloses++ }
        assertNull(lifetime.acquireFrame<String> { null })
        assertThrows(IllegalStateException::class.java) {
            lifetime.acquireFrame<String> { error("acquisition failed") }
        }
        lifetime.close()
        assertEquals(1, readerCloses)
    }

    @Test
    fun frameCleanupFailureStillClosesTheRetiredReader() {
        var readerCloses = 0
        val lifetime = FrameResourceLifetime { readerCloses++ }
        lifetime.acquireFrame { "image" }
        lifetime.close()
        assertThrows(IllegalStateException::class.java) {
            lifetime.completeFrame { error("frame cleanup failed") }
        }
        assertEquals(1, readerCloses)
    }

    @Test
    fun oldCompletionDoesNotReleaseTheNewSession() {
        val events = mutableListOf<String>()
        val old = FrameResourceLifetime { events += "old reader" }
        old.acquireFrame { "old frame" }
        old.close()
        val current = FrameResourceLifetime { events += "new reader" }
        current.acquireFrame { "new frame" }
        old.completeFrame { events += "old frame" }
        assertTrue(current.isOpen())
        assertEquals(listOf("old frame", "old reader"), events)
        current.close()
        current.completeFrame { events += "new frame" }
        assertEquals(listOf("old frame", "old reader", "new frame", "new reader"), events)
    }
}
