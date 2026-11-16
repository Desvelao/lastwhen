package app.lasttime.core

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class Status { Overdue, DueSoon, Later }

object Rhythm {
    /** Fraction of the interval, counted from the end, in which an event counts as "due soon". */
    const val DUE_SOON_FRACTION = 0.2

    /** Next due instant (epoch millis), or null without a rhythm or any entry. Calendar days in [zone] so DST cannot shift it. */
    fun nextDue(lastMillis: Long?, intervalDays: Int?, zone: ZoneId): Long? {
        if (lastMillis == null || intervalDays == null || intervalDays <= 0) return null
        return Instant.ofEpochMilli(lastMillis).atZone(zone).plusDays(intervalDays.toLong()).toInstant().toEpochMilli()
    }

    /**
     * Overdue once next due is in the past; due soon in the last 20% of the interval; otherwise later.
     * Null when the event has no rhythm. An event with a rhythm but no entries is not judged (null).
     */
    fun status(lastMillis: Long?, intervalDays: Int?, nowMillis: Long, zone: ZoneId): Status? {
        val due = nextDue(lastMillis, intervalDays, zone) ?: return null
        if (nowMillis >= due) return Status.Overdue
        val window = ((due - lastMillis!!) * DUE_SOON_FRACTION).toLong()
        return if (due - nowMillis <= window) Status.DueSoon else Status.Later
    }

    /** The overdue items, the one that became due earliest first. Shared by the digest and the Overdue widget. */
    fun <T> overdueFirst(items: List<T>, nowMillis: Long, zone: ZoneId, lastDone: (T) -> Long?, intervalDays: (T) -> Int?): List<T> =
        items.mapNotNull { item ->
            val due = nextDue(lastDone(item), intervalDays(item), zone) ?: return@mapNotNull null
            if (status(lastDone(item), intervalDays(item), nowMillis, zone) == Status.Overdue) due to item else null
        }.sortedBy { it.first }.map { it.second }

    /** 0f..1f+ progress through the interval (above 1 when overdue), measured against the same due instant as [status]. Null without a rhythm. */
    fun progress(lastMillis: Long?, intervalDays: Int?, nowMillis: Long, zone: ZoneId): Float? {
        val due = nextDue(lastMillis, intervalDays, zone) ?: return null
        return ((nowMillis - lastMillis!!).toDouble() / (due - lastMillis)).toFloat().coerceAtLeast(0f)
    }

    /** Whole days until due (negative when overdue), by local calendar date. */
    fun daysUntilDue(lastMillis: Long?, intervalDays: Int?, nowMillis: Long, zone: ZoneId): Long? {
        val due = nextDue(lastMillis, intervalDays, zone) ?: return null
        val d = Instant.ofEpochMilli(due).atZone(zone).toLocalDate()
        val n = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        return ChronoUnit.DAYS.between(n, d)
    }
}
