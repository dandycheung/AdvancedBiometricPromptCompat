package dev.skomlach.biometric.compat.engine.internal.face.tensorflow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OwnedFrameTest {
    @Test
    fun ownedFrameIsReleasedWhenProcessingFails() {
        val calls = mutableListOf<String>()

        assertThrows(IllegalStateException::class.java) {
            useOwnedFrame(
                frame = "frame",
                release = { calls += "release-$it" }
            ) {
                calls += "process-$it"
                error("failed")
            }
        }

        assertEquals(listOf("process-frame", "release-frame"), calls)
    }
}
