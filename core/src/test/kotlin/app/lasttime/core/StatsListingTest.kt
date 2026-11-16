package app.lasttime.core

import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StatsListingTest {
    private val utc = ZoneId.of("UTC")
    private val day = 86_400_000L
    private val base = 1_800_000_000_000L

    @Test fun zeroEntries() = assertEquals(EventStats(0, null, null), Stats.of(emptyList(), utc))

    @Test fun oneEntry() {
        val s = Stats.of(listOf(base), utc)
        assertEquals(1, s.count); assertNull(s.averageGapDays); assertNull(s.longestGapDays)
    }

    @Test fun manyEntriesUnsorted() {
        val s = Stats.of(listOf(base + 10 * day, base, base + 4 * day), utc)
        assertEquals(3, s.count)
        assertEquals(5.0, s.averageGapDays!!, 0.001)
        assertEquals(6, s.longestGapDays)
        assertEquals(listOf(4, 6), Stats.gapsDays(listOf(base + 10 * day, base, base + 4 * day), utc))
    }

    private fun f(id: Long, name: String, last: Long?) = EventFacts(id, name, "", last, null, null)

    @Test fun sorting() {
        val items = listOf(f(1, "b", base), f(2, "a", null), f(3, "c", base + day))
        assertEquals(listOf(2L, 1L, 3L), Listing.sort(items, SortOrder.LongestAgo).map { it.id })
        assertEquals(listOf(3L, 1L, 2L), Listing.sort(items, SortOrder.MostRecent).map { it.id })
        assertEquals(listOf(2L, 1L, 3L), Listing.sort(items, SortOrder.Name).map { it.id })
    }

    @Test fun nameSortUsesCollationAndIdTiebreak() {
        val collator = java.text.Collator.getInstance(java.util.Locale.GERMAN).apply { strength = java.text.Collator.SECONDARY }
        val items = listOf(f(1, "zebra", null), f(2, "Ärzte", null), f(3, "apfel", null), f(5, "Same", null), f(4, "same", null))
        assertEquals(listOf(3L, 2L, 4L, 5L, 1L), Listing.sort(items, SortOrder.Name, collator).map { it.id })
        // Equal name and equal lastDone: id decides, whatever the input order.
        val twins = listOf(f(9, "x", base), f(7, "x", base))
        assertEquals(listOf(7L, 9L), Listing.sort(twins, SortOrder.LongestAgo, collator).map { it.id })
        assertEquals(listOf(7L, 9L), Listing.sort(twins, SortOrder.MostRecent, collator).map { it.id })
    }

    @Test fun ageFilterBoundaries() {
        val now = base + 400 * day
        assertTrue(Listing.matchesAge(now - 29 * day, AgeFilter.Weeks, now, utc))
        assertFalse(Listing.matchesAge(now - 30 * day, AgeFilter.Weeks, now, utc))
        assertTrue(Listing.matchesAge(now - 30 * day, AgeFilter.Months, now, utc))
        assertTrue(Listing.matchesAge(now - 364 * day, AgeFilter.Months, now, utc))
        assertFalse(Listing.matchesAge(now - 365 * day, AgeFilter.Months, now, utc))
        assertTrue(Listing.matchesAge(now - 365 * day, AgeFilter.Years, now, utc))
    }

    @Test fun sameDayEntriesHaveZeroGap() {
        assertEquals(listOf(0), Stats.gapsDays(listOf(base, base + 3_600_000L), utc))
        assertEquals(listOf(0), Stats.gapsDays(listOf(base, base), utc))
    }

    @Test fun ageFilters() {
        val now = base + 400 * day
        assertTrue(Listing.matchesAge(now - 10 * day, AgeFilter.Weeks, now, utc))
        assertFalse(Listing.matchesAge(now - 3 * day, AgeFilter.Weeks, now, utc))
        assertTrue(Listing.matchesAge(now - 100 * day, AgeFilter.Months, now, utc))
        assertTrue(Listing.matchesAge(now - 400 * day, AgeFilter.Years, now, utc))
        assertTrue(Listing.matchesAge(now, AgeFilter.Hours, now, utc))
        assertFalse(Listing.matchesAge(now - day, AgeFilter.Hours, now, utc))
        assertTrue(Listing.matchesAge(now - day, AgeFilter.Days, now, utc))
        assertTrue(Listing.matchesAge(now - 6 * day, AgeFilter.Days, now, utc))
        assertFalse(Listing.matchesAge(now - 7 * day, AgeFilter.Days, now, utc))
        assertFalse(Listing.matchesAge(null, AgeFilter.Hours, now, utc))
        assertFalse(Listing.matchesAge(null, AgeFilter.Years, now, utc))
        assertTrue(Listing.matchesAge(null, AgeFilter.All, now, utc))
    }

    @Test fun query() {
        val e = EventFacts(1, "Oil change", "Garage", null, null, null, listOf("synthetic oil"))
        assertTrue(Listing.matchesQuery(e, "oil")); assertTrue(Listing.matchesQuery(e, "garage"))
        assertTrue(Listing.matchesQuery(e, "SYNTH")); assertFalse(Listing.matchesQuery(e, "tyre"))
        assertTrue(Listing.matchesQuery(e, "  "))
    }
}
