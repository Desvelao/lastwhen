package app.lasttime.core

enum class IntervalUnit(val days: Int) { Days(1), Weeks(7), Months(30), Years(365) }

/** A target rhythm as the user types it (a count and a unit); stored as plain days. */
data class Interval(val count: Int, val unit: IntervalUnit) {
    /** Saturates at Int.MAX_VALUE (and floors at 0) instead of overflowing on huge counts. */
    val totalDays: Int get() = (count.toLong() * unit.days).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

    companion object {
        /** Picks the largest unit that divides [days] exactly, so 14 reads "2 weeks". Values below 1 read as 1 day. */
        fun fromDays(days: Int): Interval {
            val d = days.coerceAtLeast(1)
            val unit = IntervalUnit.entries.reversed().firstOrNull { d % it.days == 0 } ?: IntervalUnit.Days
            return Interval(d / unit.days, unit)
        }
    }
}
