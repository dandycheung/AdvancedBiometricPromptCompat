package dev.skomlach.biometric.compat.impl

import dev.skomlach.biometric.compat.BiometricType

/** Each prepared route is released independently, at most once. */
internal class ParallelSoftwareCapture(val types: Set<BiometricType>) {
    private val pending = types.toMutableSet()

    @Synchronized fun complete(type: BiometricType): Boolean = pending.remove(type)
}
