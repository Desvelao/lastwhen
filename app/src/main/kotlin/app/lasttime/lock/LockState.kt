package app.lasttime.lock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the app is unlocked. A fresh process starts locked, and the app locks again after it has been in the
 * background for [GRACE_MS] (long enough for a rotation, a permission dialog or a quick app switch).
 * Times are monotonic milliseconds (SystemClock.elapsedRealtime), passed in so the rule is testable.
 */
@Singleton
class LockState @Inject constructor() {
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked
    fun setUnlocked(v: Boolean) { _unlocked.value = v }

    private var stoppedAt: Long? = null

    fun onAppStopped(now: Long) { stoppedAt = now }

    fun onAppStarted(now: Long) {
        val stopped = stoppedAt
        stoppedAt = null
        if (stopped != null && now - stopped >= GRACE_MS) _unlocked.value = false
    }

    companion object {
        const val GRACE_MS = 30_000L
    }
}
