package dev.skomlach.biometric.compat.engine.internal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceBindingStateTest {

    @Test
    fun `accepted bind is unbound even before service connection callback`() {
        val state = ServiceBindingState()

        state.recordBindResult(accepted = true)

        assertTrue(state.consumeUnbindRequest())
    }

    @Test
    fun `rejected bind is never unbound`() {
        val state = ServiceBindingState()

        state.recordBindResult(accepted = false)

        assertFalse(state.consumeUnbindRequest())
    }

    @Test
    fun `a binding is unbound at most once`() {
        val state = ServiceBindingState()
        state.recordBindResult(accepted = true)

        assertTrue(state.consumeUnbindRequest())
        assertFalse(state.consumeUnbindRequest())
    }
}
