package app.lasttime.core

import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class ElapsedTest {
    private val utc = ZoneId.of("UTC")
    private fun at(y: Int, m: Int, d: Int, h: Int = 12, min: Int = 0, zone: ZoneId = utc) =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

    @Test fun never() = assertEquals(Elapsed.Never, ElapsedCalc.elapsed(null, 0, utc))

    @Test fun sameDayShowsHours() =
        assertEquals(Elapsed.Today(3), ElapsedCalc.elapsed(at(2026, 5, 1, 9), at(2026, 5, 1, 12, 30), utc))

    @Test fun midnightMakesItYesterday() =
        assertEquals(Elapsed.Yesterday, ElapsedCalc.elapsed(at(2026, 5, 1, 23, 50), at(2026, 5, 2, 0, 0), utc))

    @Test fun days() = assertEquals(Elapsed.Days(10), ElapsedCalc.elapsed(at(2026, 5, 1), at(2026, 5, 11), utc))

    @Test fun sixtyDaysStillDays() = assertEquals(Elapsed.Days(60), ElapsedCalc.elapsed(at(2026, 1, 1), at(2026, 3, 2), utc))

    @Test fun sixtyOneDaysSwitchesToMonthsClampedToTwo() {
        // 2026-01-01 to 2026-03-03 is 61 days but only 2 calendar months.
        assertEquals(Elapsed.Months(2), ElapsedCalc.elapsed(at(2026, 1, 1), at(2026, 3, 3), utc))
        // 61 days spanning fewer than 2 calendar months still reads as 2 months.
        assertEquals(Elapsed.Months(2), ElapsedCalc.elapsed(at(2026, 1, 31), at(2026, 4, 2), utc))
    }

    @Test fun elevenMonthsThenOneYear() {
        assertEquals(Elapsed.Months(11), ElapsedCalc.elapsed(at(2025, 1, 15), at(2025, 12, 20), utc))
        assertEquals(Elapsed.Years(1, 0), ElapsedCalc.elapsed(at(2025, 1, 15), at(2026, 1, 20), utc))
    }

    @Test fun monthsPastLimit() = assertEquals(Elapsed.Months(3), ElapsedCalc.elapsed(at(2026, 1, 1), at(2026, 4, 5), utc))

    @Test fun years() = assertEquals(Elapsed.Years(1, 6), ElapsedCalc.elapsed(at(2025, 1, 1), at(2026, 7, 3), utc))

    @Test fun futureEntryNeverNegative() = assertEquals(Elapsed.Today(0), ElapsedCalc.elapsed(at(2026, 5, 3), at(2026, 5, 1), utc))

    @Test fun timeZoneChangesTheDay() {
        val last = at(2026, 5, 1, 23, 30)           // 23:30 UTC
        val now = at(2026, 5, 2, 0, 30)             // 00:30 UTC next day
        assertEquals(Elapsed.Yesterday, ElapsedCalc.elapsed(last, now, utc))
        // In UTC+3 both instants fall on May 2.
        assertEquals(Elapsed.Today(1), ElapsedCalc.elapsed(last, now, ZoneId.of("Europe/Moscow")))
    }
}
