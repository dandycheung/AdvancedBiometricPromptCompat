package dev.skomlach.biometric.compat.utils.activityView

import org.junit.Assert.assertEquals
import org.junit.Test

class BlurCleanupTest {

    @Test
    fun androidSRenderEffectDoesNotNeedBitmapCapture() {
        assertEquals(false, shouldCaptureBlurBitmap(isAtLeastS = true))
    }

    @Test
    fun legacyBlurStillNeedsBitmapCapture() {
        assertEquals(true, shouldCaptureBlurBitmap(isAtLeastS = false))
    }

    @Test
    fun androidSCapturesBackdropPaletteOnceWithoutContinuousBitmapBlur() {
        assertEquals(true, shouldCaptureBackdropPalette(isAtLeastS = true))
        assertEquals(false, shouldCaptureBackdropPalette(isAtLeastS = false))
    }

    @Test
    fun timedOutBlurCaptureDoesNotCompleteANewerCapture() {
        val latch = BlurCaptureLatch()
        val first = latch.tryStart()

        assertEquals(true, first != null)
        assertEquals(false, latch.tryStart() != null)
        assertEquals(true, latch.finish(first!!))

        val second = latch.tryStart()
        assertEquals(true, second != null)
        assertEquals(false, latch.finish(first))
        assertEquals(false, latch.tryStart() != null)
        assertEquals(true, latch.finish(second!!))
        assertEquals(true, latch.tryStart() != null)
    }

    @Test
    fun cleanupClearsEffectBeforeRemovingOverlayAndInvalidatingHost() {
        val calls = mutableListOf<String>()

        runBlurCleanup(
            clearRenderEffect = { calls += "clear-effect" },
            removeOverlay = { calls += "remove-overlay" },
            invalidateHost = { calls += "invalidate-host" },
            onFailure = { throw it }
        )

        assertEquals(
            listOf("clear-effect", "remove-overlay", "invalidate-host"),
            calls
        )
    }

    @Test
    fun cleanupStillRemovesOverlayAndInvalidatesWhenEffectClearFails() {
        val calls = mutableListOf<String>()
        val failures = mutableListOf<Throwable>()

        runBlurCleanup(
            clearRenderEffect = {
                calls += "clear-effect"
                error("clear failed")
            },
            removeOverlay = { calls += "remove-overlay" },
            invalidateHost = { calls += "invalidate-host" },
            onFailure = { failures += it }
        )

        assertEquals(
            listOf("clear-effect", "remove-overlay", "invalidate-host"),
            calls
        )
        assertEquals(listOf("clear failed"), failures.map { it.message })
    }
}
