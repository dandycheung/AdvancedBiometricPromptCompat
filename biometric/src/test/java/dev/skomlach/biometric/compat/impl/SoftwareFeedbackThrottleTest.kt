package dev.skomlach.biometric.compat.impl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftwareFeedbackThrottleTest {
    @Test fun latestInstructionCanBeScheduledWithoutExtendingTheCooldown() {
        val throttle = SoftwareFeedbackThrottle()
        assertTrue(throttle.tryAcquire(0))
        assertFalse(throttle.tryAcquire(100))
        assertEquals(3_900L, throttle.remainingDelay(100))
        assertFalse(throttle.tryAcquire(3_999))
        assertEquals(1L, throttle.remainingDelay(3_999))
        assertTrue(throttle.tryAcquire(4_000))
    }

    @Test fun firstHintIsShownImmediatelyEvenAtClockZero() {
        assertTrue(SoftwareFeedbackThrottle().tryAcquire(0))
    }

    @Test fun cameraFrameHintsProduceAtMostOneToastEveryFourSeconds() {
        val throttle = SoftwareFeedbackThrottle()
        val shownAt = (0L..12_000L step 100L).filter { throttle.tryAcquire(it) }

        assertEquals(listOf(0L, 4_000L, 8_000L, 12_000L), shownAt)
    }

    @Test fun suppressedHintsDoNotPostponeTheNextReminder() {
        val throttle = SoftwareFeedbackThrottle()
        assertTrue(throttle.tryAcquire(50_000))
        assertFalse(throttle.tryAcquire(53_999))
        assertTrue(throttle.tryAcquire(54_000))
    }

    @Test fun newAuthenticationSessionCanShowItsFirstHintImmediately() {
        val throttle = SoftwareFeedbackThrottle()
        assertTrue(throttle.tryAcquire(50_000))
        throttle.reset()
        assertTrue(throttle.tryAcquire(50_001))
        assertFalse(throttle.tryAcquire(50_002))
    }

    @Test fun independentPromptsDoNotSuppressEachOthersHints() {
        assertTrue(SoftwareFeedbackThrottle().tryAcquire(50_000))
        assertTrue(SoftwareFeedbackThrottle().tryAcquire(50_001))
    }
}
