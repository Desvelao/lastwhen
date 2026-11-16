package app.lasttime.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lasttime.core.AgeFilter
import app.lasttime.core.Elapsed
import app.lasttime.core.ElapsedCalc
import app.lasttime.core.EventFacts
import app.lasttime.core.Listing
import app.lasttime.core.Rhythm
import app.lasttime.core.SortOrder
import app.lasttime.core.Status
import app.lasttime.data.CategoryEntity
import app.lasttime.data.EventRepository
import app.lasttime.data.EventWithLast
import app.lasttime.data.SettingsRepository
import app.lasttime.widget.WidgetRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeItem(
    val id: Long,
    val name: String,
    val icon: String,
    val color: Int,
    val lastDone: Long?,
    val elapsed: Elapsed,
    val status: Status?,
)

data class HomeUiState(
    val loading: Boolean = true,
    /** True when no active event exists at all (drives the empty state, not the filters). */
    val noEvents: Boolean = false,
    val overdue: List<HomeItem> = emptyList(),
    val dueSoon: List<HomeItem> = emptyList(),
    val later: List<HomeItem> = emptyList(),
    val sort: SortOrder = SortOrder.LongestAgo,
    val query: String = "",
    val age: AgeFilter = AgeFilter.All,
    val categoryFilter: Long? = null,
    /** Categories that at least one active event uses, for the filter chips. */
    val categories: List<CategoryEntity> = emptyList(),
) {
    val sectioned: Boolean get() = overdue.isNotEmpty() || dueSoon.isNotEmpty()
    val isEmptyAfterFilter: Boolean get() = !noEvents && overdue.isEmpty() && dueSoon.isEmpty() && later.isEmpty()
}

/** Raised after Done so the UI can offer Undo. */
data class LoggedEvent(val entryId: Long, val eventName: String)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: EventRepository,
    private val settings: SettingsRepository,
    private val widgets: WidgetRefresher,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val age = MutableStateFlow(AgeFilter.All)
    private val categoryFilter = MutableStateFlow<Long?>(null)
    private val now = MutableStateFlow(repo.now())

    private val _logged = MutableSharedFlow<LoggedEvent>(extraBufferCapacity = 1)
    val logged: SharedFlow<LoggedEvent> = _logged

    val state: StateFlow<HomeUiState> = combine(
        repo.observeActive(), repo.observeEntryNotes(), repo.observeCategories(), settings.settings,
        combine(query, age, categoryFilter, now) { q, a, c, n -> Filters(q, a, c, n) },
    ) { events, notes, categories, s, f ->
        build(events, notes.groupBy({ it.eventId }, { it.note }), categories, s.sort, f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private data class Filters(val query: String, val age: AgeFilter, val category: Long?, val now: Long)

    private fun build(
        events: List<EventWithLast>,
        notes: Map<Long, List<String>>,
        categories: List<CategoryEntity>,
        sort: SortOrder,
        f: Filters,
    ): HomeUiState {
        val zone = repo.zone
        val byId = events.associateBy { it.event.id }
        val facts = events.map {
            EventFacts(it.event.id, it.event.name, it.event.notes, it.lastDone, it.event.targetIntervalDays, it.event.categoryId, notes[it.event.id].orEmpty())
        }
        val visible = Listing.sort(
            facts.filter {
                Listing.matchesQuery(it, f.query) &&
                    Listing.matchesAge(it.lastDone, f.age, f.now, zone) &&
                    (f.category == null || it.categoryId == f.category)
            },
            sort,
        )
        val items = visible.map { fact ->
            val e = byId.getValue(fact.id).event
            HomeItem(
                e.id, e.name, e.icon, e.color, fact.lastDone,
                ElapsedCalc.elapsed(fact.lastDone, f.now, zone),
                Rhythm.status(fact.lastDone, e.targetIntervalDays, f.now, zone),
            )
        }
        val usedCategories = events.mapNotNull { it.event.categoryId }.toSet()
        return HomeUiState(
            loading = false,
            noEvents = events.isEmpty(),
            overdue = items.filter { it.status == Status.Overdue },
            dueSoon = items.filter { it.status == Status.DueSoon },
            later = items.filter { it.status == null || it.status == Status.Later },
            sort = sort, query = f.query, age = f.age, categoryFilter = f.category,
            categories = categories.filter { it.id in usedCategories },
        )
    }

    /** Re-reads the clock, so labels stay right after midnight or a long pause. */
    fun refreshNow() { now.value = repo.now() }

    fun setQuery(q: String) { query.value = q }
    fun setAge(a: AgeFilter) { age.value = a }
    fun setCategory(id: Long?) { categoryFilter.value = if (categoryFilter.value == id) null else id }
    fun setSort(s: SortOrder) { viewModelScope.launch { settings.setSort(s) } }

    fun done(item: HomeItem) {
        viewModelScope.launch {
            val id = repo.log(item.id)
            now.value = repo.now()
            _logged.tryEmit(LoggedEvent(id, item.name))
            widgets.refresh()
        }
    }

    fun log(eventId: Long, millis: Long, note: String) {
        viewModelScope.launch { repo.log(eventId, millis, note); now.value = repo.now(); widgets.refresh() }
    }

    fun undo(entryId: Long) {
        viewModelScope.launch { repo.deleteEntry(entryId); now.value = repo.now(); widgets.refresh() }
    }

    fun eventName(id: Long): String? = state.value.let { s -> (s.overdue + s.dueSoon + s.later).firstOrNull { it.id == id }?.name }
}
