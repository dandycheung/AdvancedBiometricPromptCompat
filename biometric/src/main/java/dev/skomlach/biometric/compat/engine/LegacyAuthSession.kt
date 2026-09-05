package dev.skomlach.biometric.compat.engine

/** Ownership lasts until cancellation, even when one of several modules has completed. */
internal class LegacyAuthSession {
    private var owner: Any? = null
    private val modules = mutableSetOf<Int>()

    @Synchronized fun isOccupied(): Boolean = owner != null
    @Synchronized fun owns(candidate: Any): Boolean = owner === candidate
    @Synchronized fun claim(candidate: Any): Boolean {
        if (owner != null && owner !== candidate) return false
        owner = candidate
        return true
    }
    @Synchronized fun add(candidate: Any, moduleTag: Int): Boolean =
        owner === candidate && modules.add(moduleTag)

    @Synchronized fun clear() {
        owner = null
        modules.clear()
    }
}
