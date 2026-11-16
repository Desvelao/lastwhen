package app.lasttime.core

import kotlin.test.Test
import kotlin.test.assertEquals

class IntervalTest {
    @Test fun roundTrips() {
        listOf(1, 3, 7, 14, 30, 60, 90, 365, 730, 45).forEach {
            assertEquals(it, Interval.fromDays(it).totalDays)
        }
    }

    @Test fun nonPositiveDaysReadAsOneDay() {
        assertEquals(Interval(1, IntervalUnit.Days), Interval.fromDays(0))
        assertEquals(Interval(1, IntervalUnit.Days), Interval.fromDays(-30))
    }

    @Test fun totalDaysSaturatesInsteadOfOverflowing() {
        assertEquals(Int.MAX_VALUE, Interval(Int.MAX_VALUE, IntervalUnit.Years).totalDays)
        assertEquals(0, Interval(-5, IntervalUnit.Days).totalDays)
    }

    @Test fun picksLargestUnit() {
        assertEquals(Interval(2, IntervalUnit.Weeks), Interval.fromDays(14))
        assertEquals(Interval(6, IntervalUnit.Months), Interval.fromDays(180))
        assertEquals(Interval(2, IntervalUnit.Years), Interval.fromDays(730))
        assertEquals(Interval(10, IntervalUnit.Days), Interval.fromDays(10))
    }
}
