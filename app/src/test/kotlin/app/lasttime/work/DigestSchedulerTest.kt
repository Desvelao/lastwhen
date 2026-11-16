package app.lasttime.work

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

class DigestSchedulerTest {
    private val berlin = ZoneId.of("Europe/Berlin")
    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int = 0, zone: ZoneId = berlin) = ZonedDateTime.of(y, m, d, h, min, 0, 0, zone)

    @Test fun laterTodayWhenTheTimeHasNotPassed() =
        assertEquals(Duration.ofHours(2), DigestScheduler.delayUntil(8 * 60, at(2026, 5, 1, 6)))

    @Test fun tomorrowWhenTheTimeHasPassed() =
        assertEquals(Duration.ofHours(23), DigestScheduler.delayUntil(8 * 60, at(2026, 5, 1, 9)))

    @Test fun exactlyNowMeansTomorrow() =
        assertEquals(Duration.ofHours(24), DigestScheduler.delayUntil(8 * 60, at(2026, 5, 1, 8)))

    @Test fun midnight() = assertEquals(Duration.ofHours(1), DigestScheduler.delayUntil(0, at(2026, 5, 1, 23)))

    @Test fun springForwardShortensTheWait() {
        // Berlin skips 02:00-03:00 on 2026-03-29: 09:00 on the 28th to 08:00 on the 29th is 22 real hours, not 23.
        assertEquals(Duration.ofHours(22), DigestScheduler.delayUntil(8 * 60, at(2026, 3, 28, 9)))
    }

    @Test fun gapTimeResolvesForwardOnTheTransitionDay() {
        // 02:30 does not exist on 2026-03-29 in Berlin; the target lands on 03:30 and is still in the future.
        val d = DigestScheduler.delayUntil(2 * 60 + 30, at(2026, 3, 29, 0, 30))
        assertEquals(Duration.ofHours(2), d)
    }

    @Test fun fallBackDayIsTwentyFiveHours() {
        // Berlin repeats 02:00-03:00 on 2026-10-25: from 08:00 on the 24th to 08:00 on the 25th is 25 real hours.
        assertEquals(Duration.ofHours(25), DigestScheduler.delayUntil(8 * 60, at(2026, 10, 24, 8, 0)))
    }
}
