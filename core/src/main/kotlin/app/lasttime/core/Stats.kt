package app.lasttime.core

import java.time.ZoneId

data class EventStats(val count: Int, val averageGapDays: Double?, val longestGapDays: Int?)

object Stats {
    /** Gaps in whole calendar days between consecutive entries, oldest gap first. */
    fun gapsDays(occurredAt: List<Long>, zone: ZoneId): List<Int> =
        occurredAt.sorted().zipWithNext { a, b -> ElapsedCalc.daysBetween(a, b, zone).toInt() }

    fun of(occurredAt: List<Long>, zone: ZoneId): EventStats {
        val gaps = gapsDays(occurredAt, zone)
        return EventStats(
            count = occurredAt.size,
            averageGapDays = if (gaps.isEmpty()) null else gaps.average(),
            longestGapDays = gaps.maxOrNull(),
        )
    }
}
