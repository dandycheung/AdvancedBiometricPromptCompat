package dev.skomlach.common.permissionui

import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import dev.skomlach.common.misc.ExecutorHelper

/** Completes once on the original resumed owner; destruction discards the result. */
internal class ResumedFragmentResult(private val owner: Fragment, callback: Runnable) : DefaultLifecycleObserver {
    private var callback: Runnable? = callback
    private var ready = false
    private val deliver = Runnable {
        if (ready && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            val notify = this.callback
            this.callback = null
            notify?.run()
        }
    }

    init { owner.lifecycle.addObserver(this) }

    fun complete() {
        ready = true
        ExecutorHelper.removeCallbacks(deliver)
        ExecutorHelper.post(deliver)
    }

    override fun onResume(owner: LifecycleOwner) {
        if (ready) ExecutorHelper.post(deliver)
    }

    override fun onPause(owner: LifecycleOwner) { ExecutorHelper.removeCallbacks(deliver) }

    override fun onDestroy(owner: LifecycleOwner) {
        callback = null
        ExecutorHelper.removeCallbacks(deliver)
        owner.lifecycle.removeObserver(this)
    }
}
