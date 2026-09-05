package dev.skomlach.biometric.compat.engine.internal.voice

import dev.skomlach.biometric.compat.custom.AbstractSoftwareBiometricManager
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class VoiceBiometricManagerFlowTest {
    @Test fun successIsDeliveredImmediatelyAndOnlyOnce() {
        val active = AtomicBoolean(true)
        val events = mutableListOf<String>()
        val callback = object : AbstractSoftwareBiometricManager.AuthenticationCallback() {
            override fun onAuthenticationHelp(helpMsgId: Int, helpString: CharSequence?) {
                events += "help"
            }
            override fun onAuthenticationSucceeded(result: AbstractSoftwareBiometricManager.AuthenticationResult?) {
                assertFalse(active.get())
                events += "success"
            }
        }
        completeVoiceAuthentication(active, callback, null, "accepted")
        assertEquals(listOf("help", "success"), events)
        completeVoiceAuthentication(active, callback, null, "duplicate")
        assertEquals(2, events.size)
    }

    @Test fun cancelledSessionCannotDeliverSuccess() {
        completeVoiceAuthentication(AtomicBoolean(false), object : AbstractSoftwareBiometricManager.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: AbstractSoftwareBiometricManager.AuthenticationResult?) {
                fail("cancelled session succeeded")
            }
        }, null, "accepted")
    }
}
