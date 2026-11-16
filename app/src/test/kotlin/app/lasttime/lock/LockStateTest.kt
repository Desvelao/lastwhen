package app.lasttime.lock

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockStateTest {
    @Test fun startsLocked() = assertFalse(LockState().unlocked.value)

    @Test fun staysUnlockedAfterAShortTrip() {
        val s = LockState().apply { setUnlocked(true) }
        s.onAppStopped(1_000)
        s.onAppStarted(1_000 + LockState.GRACE_MS - 1)
        assertTrue(s.unlocked.value)
    }

    @Test fun locksAfterTheGracePeriod() {
        val s = LockState().apply { setUnlocked(true) }
        s.onAppStopped(1_000)
        s.onAppStarted(1_000 + LockState.GRACE_MS)
        assertFalse(s.unlocked.value)
    }

    @Test fun firstStartDoesNotChangeAnything() {
        val s = LockState().apply { setUnlocked(true) }
        s.onAppStarted(5)
        assertTrue(s.unlocked.value)
    }

    @Test fun eachTripIsMeasuredOnItsOwn() {
        val s = LockState().apply { setUnlocked(true) }
        s.onAppStopped(0); s.onAppStarted(10)
        s.onAppStarted(10 * LockState.GRACE_MS) // no stop in between: nothing to measure
        assertTrue(s.unlocked.value)
    }
}
