package dev.skomlach.biometric.compat.engine.internal.face.tensorflow

internal inline fun <T> useOwnedFrame(
    frame: T,
    release: (T) -> Unit,
    block: (T) -> Unit
) {
    try {
        block(frame)
    } finally {
        release(frame)
    }
}
