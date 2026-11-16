package app.lasttime.core

import java.text.Collator
import java.time.ZoneId

enum class SortOrder { LongestAgo, MostRecent, Name }

enum class AgeFilter { All, Hours, Days, Weeks, Months, Years }

/** The facts about an event the list needs; the UI maps its own rows onto this. */
data class EventFacts(
    val id: Long,
    val name: String,
    val notes: String,
    val lastDone: Long?,
    val intervalDays: Int?,
    val categoryId: Long?,
    val entryNotes: List<String> = emptyList(),
)

object Listing {
    /** Names compare with the locale's collation (so "Ärzte" sorts with "A", case-insensitively); [id] breaks every tie. */
    fun sort(items: List<EventFacts>, order: SortOrder, collator: Collator = defaultCollator()): List<EventFacts> {
        val byName = Comparator<EventFacts> { a, b -> collator.compare(a.name, b.name) }
        val byId = compareBy<EventFacts> { it.id }
        return when (order) {
            // Never-done events count as the longest ago, so they stay visible at the top.
            SortOrder.LongestAgo -> items.sortedWith(compareBy<EventFacts> { it.lastDone ?: Long.MIN_VALUE }.then(byName).then(byId))
            SortOrder.MostRecent -> items.sortedWith(compareByDescending<EventFacts> { it.lastDone ?: Long.MIN_VALUE }.then(byName).then(byId))
            SortOrder.Name -> items.sortedWith(byName.then(byId))
        }
    }

    private fun defaultCollator(): Collator = Collator.getInstance().apply { strength = Collator.SECONDARY }

    /** Hours: today, Days: 1–6 days, Weeks: 7–29, Months: 30–364, Years: 365+. Never-done events only match All. */
    fun matchesAge(lastDone: Long?, filter: AgeFilter, nowMillis: Long, zone: ZoneId): Boolean {
        if (filter == AgeFilter.All) return true
        if (lastDone == null) return false
        val days = ElapsedCalc.daysBetween(lastDone, nowMillis, zone)
        return when (filter) {
            AgeFilter.All -> true
            AgeFilter.Hours -> days == 0L
            AgeFilter.Days -> days in 1..6
            AgeFilter.Weeks -> days in 7..29
            AgeFilter.Months -> days in 30..364
            AgeFilter.Years -> days >= 365
        }
    }

    fun matchesQuery(item: EventFacts, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return item.name.contains(q, ignoreCase = true) ||
            item.notes.contains(q, ignoreCase = true) ||
            item.entryNotes.any { it.contains(q, ignoreCase = true) }
    }
}
