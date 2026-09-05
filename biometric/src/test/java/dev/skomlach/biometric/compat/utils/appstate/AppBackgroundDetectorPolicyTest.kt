package dev.skomlach.biometric.compat.utils.appstate

import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBackgroundDetectorPolicyTest {
    @Test
    fun foregroundEventsCancelPendingLifecycleDismiss() {
        assertTrue(shouldCancelPendingLifecycleDismiss(Lifecycle.Event.ON_START))
        assertTrue(shouldCancelPendingLifecycleDismiss(Lifecycle.Event.ON_RESUME))
        assertFalse(shouldCancelPendingLifecycleDismiss(Lifecycle.Event.ON_PAUSE))
        assertFalse(shouldCancelPendingLifecycleDismiss(Lifecycle.Event.ON_STOP))
    }
}
