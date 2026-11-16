package app.lasttime.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lasttime.core.Elapsed
import app.lasttime.core.ElapsedCalc
import app.lasttime.core.EventStats
import app.lasttime.core.Rhythm
import app.lasttime.core.Stats
import app.lasttime.core.Status
import app.lasttime.data.EntryEntity
import app.lasttime.data.EventEntity
import app.lasttime.data.EventRepository
import app.lasttime.widget.WidgetRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryRow(val entry: EntryEntity, val gapDays: Int?)

data class DetailUiState(
    val loading: Boolean = true,
    val event: EventEntity? = null,
    val elapsed: Elapsed = Elapsed.Never,
    val lastDone: Long? = null,
    val status: Status? = null,
    val progress: Float? = null,
    val nextDue: Long? = null,
    /** Whole days until due; negative when overdue. */
    val daysUntilDue: Long? = null,
    val stats: EventStats = EventStats(0, null, null),
    val history: List<HistoryRow> = emptyList(),
    val categoryName: String? = null,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repo: EventRepository,
    private val widgets: WidgetRefresher,
) : ViewModel() {
    val eventId: Long = checkNotNull(handle["id"])
    private val now = MutableStateFlow(repo.now())

    private val _removed = MutableSharedFlow<EntryEntity>(extraBufferCapacity = 1)
    /** Emitted after an entry is removed so the UI can offer Undo. */
    val removed: SharedFlow<EntryEntity> = _removed

    val state: StateFlow<DetailUiState> = combine(
        repo.observeEvent(eventId), repo.observeEntries(eventId), repo.observeCategories(), now,
    ) { ev, entries, categories, n ->
        if (ev == null) return@combine DetailUiState(loading = false)
        val zone = repo.zone
        val e = ev.event
        val last = ev.lastDone
        // entries arrive newest first; each row's gap is the distance to the next-older one.
        val rows = entries.mapIndexed { i, entry ->
            HistoryRow(entry, entries.getOrNull(i + 1)?.let { ElapsedCalc.daysBetween(it.occurredAt, entry.occurredAt, zone).toInt() })
        }
        DetailUiState(
            loading = false, event = e,
            elapsed = ElapsedCalc.elapsed(last, n, zone), lastDone = last,
            status = Rhythm.status(last, e.targetIntervalDays, n, zone),
            progress = Rhythm.progress(last, e.targetIntervalDays, n, zone),
            nextDue = Rhythm.nextDue(last, e.targetIntervalDays, zone),
            daysUntilDue = Rhythm.daysUntilDue(last, e.targetIntervalDays, n, zone),
            stats = Stats.of(entries.map { it.occurredAt }, zone),
            history = rows,
            categoryName = categories.firstOrNull { it.id == e.categoryId }?.name,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun refreshNow() { now.value = repo.now() }

    fun logNow() = log(repo.now(), "")

    fun log(millis: Long, note: String) {
        viewModelScope.launch { repo.log(eventId, millis, note); now.value = repo.now(); widgets.refresh() }
    }

    fun updateEntry(entry: EntryEntity, millis: Long, note: String) {
        viewModelScope.launch { repo.updateEntry(entry.copy(occurredAt = millis, note = note.trim())); widgets.refresh() }
    }

    fun removeEntry(entry: EntryEntity) {
        viewModelScope.launch { repo.deleteEntry(entry.id); _removed.tryEmit(entry); widgets.refresh() }
    }

    fun undoRemove(entry: EntryEntity) {
        viewModelScope.launch { repo.restoreEntry(entry); widgets.refresh() }
    }
}
