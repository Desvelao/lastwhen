package app.lasttime.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** How long ago something happened, as data; the UI turns it into localised text. */
sealed interface Elapsed {
    data object Never : Elapsed
    /** Made today; [hours] is 0 when less than an hour ago. */
    data class Today(val hours: Int) : Elapsed
    data object Yesterday : Elapsed
    data class Days(val days: Int) : Elapsed
    data class Months(val months: Int) : Elapsed
    data class Years(val years: Int, val months: Int) : Elapsed
}

object ElapsedCalc {
    /** Past this many days the label switches from days to months/years. */
    const val DAYS_LIMIT = 60

    /** Whole local calendar days between [lastMillis] and [nowMillis] (never negative). */
    fun daysBetween(lastMillis: Long, nowMillis: Long, zone: ZoneId): Long {
        val last = Instant.ofEpochMilli(lastMillis).atZone(zone).toLocalDate()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        return maxOf(0L, ChronoUnit.DAYS.between(last, now))
    }

    fun elapsed(lastMillis: Long?, nowMillis: Long, zone: ZoneId): Elapsed {
        if (lastMillis == null) return Elapsed.Never
        val days = daysBetween(lastMillis, nowMillis, zone)
        return when {
            days == 0L -> {
                val hours = ((nowMillis - lastMillis) / 3_600_000L).toInt().coerceAtLeast(0)
                Elapsed.Today(hours)
            }
            days == 1L -> Elapsed.Yesterday
            days <= DAYS_LIMIT -> Elapsed.Days(days.toInt())
            else -> {
                val last: LocalDate = Instant.ofEpochMilli(lastMillis).atZone(zone).toLocalDate()
                val now: LocalDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
                val totalMonths = ChronoUnit.MONTHS.between(last, now).toInt()
                if (totalMonths < 12) Elapsed.Months(maxOf(totalMonths, 2))
                else Elapsed.Years(totalMonths / 12, totalMonths % 12)
            }
        }
    }
}
