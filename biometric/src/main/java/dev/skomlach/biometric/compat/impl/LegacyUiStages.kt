package dev.skomlach.biometric.compat.impl

import dev.skomlach.biometric.compat.BiometricType

/** One system-owned fingerprint stage followed by the original remaining compat routes. */
internal fun remainingCompatStage(
    deferred: Set<BiometricType>,
    executable: Set<BiometricType>,
    finished: Set<BiometricType?>
): Set<BiometricType> = deferred.intersect(executable).filterNot { it in finished }.toSet()
