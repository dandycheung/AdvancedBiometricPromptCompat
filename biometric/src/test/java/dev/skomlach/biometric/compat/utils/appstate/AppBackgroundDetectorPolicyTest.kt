package dev.skomlach.biometric.compat.utils.appstate

import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBackgroundDetectorPolicyTest {
    @Test
    fun visiblePausedSplitScreenHostDoesNotCancelAuthentication() {
        assertFalse(shouldDismissForHostLifecycle(Lifecycle.Event.ON_PAUSE))
        assertFalse(shouldDismissForHostLifecycle(Lifecycle.Event.ON_START))
        assertFalse(shouldDismissForHostLifecycle(Lifecycle.Event.ON_RESUME))
    }

    @Test
    fun hiddenHostCancelsEvenIfAnotherActivityKeepsProcessResumed() {
        assertTrue(shouldDismissForHostLifecycle(Lifecycle.Event.ON_STOP))
        assertTrue(shouldDismissForHostLifecycle(Lifecycle.Event.ON_DESTROY))
    }
    @Test
    fun foregroundEventsCancelPendingLifecycleDismiss() {
        assertTrue(shouldCancelPendingLifecycleDismiss(Lifecycle.Event.ON_START))
        assertTrue(shouldCancelPendingLifecycleDismiss(Lifecycle.Event.ON_RESUME))
        assertFalse(shouldCancelPendingLifecycleDismiss(Lifecycle.Event.ON_PAUSE))
        assertFalse(shouldCancelPendingLifecycleDismiss(Lifecycle.Event.ON_STOP))
    }
}
