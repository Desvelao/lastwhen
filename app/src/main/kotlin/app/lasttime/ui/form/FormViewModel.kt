package app.lasttime.ui.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lasttime.core.Interval
import app.lasttime.core.IntervalUnit
import app.lasttime.data.CategoryEntity
import app.lasttime.data.EventEntity
import app.lasttime.data.EventRepository
import app.lasttime.data.Palette
import app.lasttime.widget.WidgetRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FormUiState(
    val loaded: Boolean = false,
    val editing: Boolean = false,
    val name: String = "",
    val nameError: Boolean = false,
    val icon: String = "star",
    val color: Int = Palette.default,
    val categoryId: Long? = null,
    val rhythmOn: Boolean = false,
    val rhythmCount: String = "1",
    val rhythmUnit: IntervalUnit = IntervalUnit.Weeks,
    val rhythmError: Boolean = false,
    val notes: String = "",
    val archived: Boolean = false,
    val categories: List<CategoryEntity> = emptyList(),
)

@HiltViewModel
class FormViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repo: EventRepository,
    private val widgets: WidgetRefresher,
) : ViewModel() {
    private val id: Long = handle.get<Long>("id") ?: -1L
    private var original: EventEntity? = null

    private val _state = MutableStateFlow(
        FormUiState(
            editing = id > 0,
            name = handle.get<String>("name").orEmpty(),
            icon = handle.get<String>("icon")?.takeIf { it.isNotBlank() } ?: "star",
        ),
    )
    val state: StateFlow<FormUiState> = _state

    init {
        viewModelScope.launch {
            val categories = repo.observeCategories().first()
            val existing = if (id > 0) repo.getEvent(id)?.event else null
            original = existing
            _state.update { s ->
                if (existing == null) s.copy(loaded = true, categories = categories)
                else {
                    val interval = existing.targetIntervalDays?.let { Interval.fromDays(it) }
                    s.copy(
                        loaded = true, categories = categories, name = existing.name, icon = existing.icon, color = existing.color,
                        categoryId = existing.categoryId, notes = existing.notes, archived = existing.archivedAt != null,
                        rhythmOn = interval != null,
                        rhythmCount = interval?.count?.toString() ?: "1",
                        rhythmUnit = interval?.unit ?: IntervalUnit.Weeks,
                    )
                }
            }
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v, nameError = false) }
    fun setIcon(v: String) = _state.update { it.copy(icon = v) }
    fun setColor(v: Int) = _state.update { it.copy(color = v) }
    fun setCategory(v: Long?) = _state.update { it.copy(categoryId = v) }
    fun addCategory(name: String, color: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newId = repo.addCategory(name, color)
            val categories = repo.observeCategories().first()
            _state.update { it.copy(categories = categories, categoryId = newId) }
        }
    }
    fun setRhythmOn(v: Boolean) = _state.update { it.copy(rhythmOn = v, rhythmError = false) }
    fun setRhythmCount(v: String) = _state.update { it.copy(rhythmCount = v.filter(Char::isDigit).take(4), rhythmError = false) }
    fun setRhythmUnit(v: IntervalUnit) = _state.update { it.copy(rhythmUnit = v) }
    fun setNotes(v: String) = _state.update { it.copy(notes = v) }

    /** Validates and saves; calls [onDone] with the event id once stored. */
    fun save(onDone: (Long) -> Unit) {
        val s = _state.value
        val count = s.rhythmCount.toIntOrNull() ?: 0
        val nameBad = s.name.isBlank()
        val rhythmBad = s.rhythmOn && count <= 0
        if (nameBad || rhythmBad) {
            _state.update { it.copy(nameError = nameBad, rhythmError = rhythmBad) }
            return
        }
        viewModelScope.launch {
            val base = original ?: EventEntity(name = "", icon = "", color = 0, createdAt = repo.now())
            val saved = repo.saveEvent(
                base.copy(
                    name = s.name.trim(), icon = s.icon, color = s.color, categoryId = s.categoryId, notes = s.notes.trim(),
                    targetIntervalDays = if (s.rhythmOn) Interval(count, s.rhythmUnit).totalDays else null,
                ),
            )
            widgets.refresh()
            onDone(saved)
        }
    }

    fun archive(onDone: () -> Unit) {
        viewModelScope.launch { repo.archive(id); widgets.refresh(); onDone() }
    }

    fun restore(onDone: () -> Unit) {
        viewModelScope.launch { repo.restore(id); widgets.refresh(); onDone() }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch { repo.deleteEvent(id); widgets.refresh(); onDone() }
    }
}
