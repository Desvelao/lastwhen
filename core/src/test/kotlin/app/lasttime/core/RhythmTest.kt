package app.lasttime.core

import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RhythmTest {
    private val utc = ZoneId.of("UTC")
    private fun at(d: Int, h: Int = 12) = LocalDateTime.of(2026, 5, d, h, 0).atZone(utc).toInstant().toEpochMilli()

    @Test fun noRhythmNoStatus() = assertNull(Rhythm.status(at(1), null, at(20), utc))

    @Test fun noEntriesNoStatus() = assertNull(Rhythm.status(null, 10, at(20), utc))

    @Test fun later() = assertEquals(Status.Later, Rhythm.status(at(1), 20, at(5), utc))

    @Test fun dueSoonAtBoundary() {
        // interval 10 days: window is 2 days; due on the 11th at noon.
        assertEquals(Status.DueSoon, Rhythm.status(at(1), 10, at(9), utc))
        assertEquals(Status.Later, Rhythm.status(at(1), 10, at(9, 11), utc))
    }

    @Test fun dueSoonWindowEdgeIsInclusive() {
        // interval 10 days: window is exactly 2 days before due (the 11th at noon).
        assertEquals(Status.DueSoon, Rhythm.status(at(1), 10, at(9), utc))
        assertEquals(Status.Later, Rhythm.status(at(1), 10, at(9) - 1, utc))
    }

    @Test fun overdueExactlyAtDue() = assertEquals(Status.Overdue, Rhythm.status(at(1), 10, at(11), utc))

    @Test fun overdueAfter() = assertEquals(Status.Overdue, Rhythm.status(at(1), 10, at(25), utc))

    @Test fun nextDue() = assertEquals(at(15), Rhythm.nextDue(at(1), 14, utc))

    @Test fun daysUntilDueNegativeWhenOverdue() = assertEquals(-4L, Rhythm.daysUntilDue(at(1), 10, at(15), utc))

    @Test fun progressAndStatusAgreeAcrossDst() {
        // Europe/Berlin springs forward on 2026-03-29: a 10-day interval from the 25th is 239h, not 240h.
        val berlin = ZoneId.of("Europe/Berlin")
        val last = LocalDateTime.of(2026, 3, 25, 12, 0).atZone(berlin).toInstant().toEpochMilli()
        val due = Rhythm.nextDue(last, 10, berlin)!!
        assertEquals(1f, Rhythm.progress(last, 10, due, berlin)!!, 0.0001f)
        assertEquals(Status.Overdue, Rhythm.status(last, 10, due, berlin))
        val justBefore = due - 60_000L
        assertEquals(Status.DueSoon, Rhythm.status(last, 10, justBefore, berlin))
        assert(Rhythm.progress(last, 10, justBefore, berlin)!! < 1f)
    }

    @Test fun overdueFirstKeepsOnlyOverdueEarliestDueFirst() {
        data class Item(val name: String, val last: Long?, val days: Int?)
        val items = listOf(
            Item("later", at(1), 30), Item("old", at(1), 5), Item("older", at(1) - 86_400_000L, 5),
            Item("noRhythm", at(1), null), Item("never", null, 5),
        )
        val result = Rhythm.overdueFirst(items, at(20), utc, { it.last }, { it.days }).map { it.name }
        assertEquals(listOf("older", "old"), result)
    }

    @Test fun progress() = assertEquals(0.5f, Rhythm.progress(at(1), 10, at(6), utc)!!, 0.001f)
}
