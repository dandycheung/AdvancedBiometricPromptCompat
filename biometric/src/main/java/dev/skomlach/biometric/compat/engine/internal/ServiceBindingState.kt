package dev.skomlach.biometric.compat.engine.internal

internal class ServiceBindingState {
    private var bindRequested = false

    @Synchronized
    fun recordBindResult(accepted: Boolean) {
        bindRequested = accepted
    }

    @Synchronized
    fun isBindingActive(): Boolean = bindRequested

    @Synchronized
    fun consumeUnbindRequest(): Boolean {
        if (!bindRequested) return false
        bindRequested = false
        return true
    }
}
