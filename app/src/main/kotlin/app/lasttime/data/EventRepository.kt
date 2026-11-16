package app.lasttime.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val db: AppDatabase,
    private val clock: Clock,
) {
    private val events = db.eventDao()
    private val entries = db.entryDao()
    private val categories = db.categoryDao()

    fun now(): Long = clock.millis()
    val zone: java.time.ZoneId get() = clock.zone

    fun observeActive(): Flow<List<EventWithLast>> = events.observeActive()
    /** One-off read of the active events, for widgets and workers. */
    suspend fun activeSnapshot(): List<EventWithLast> = events.getAll().filter { it.event.archivedAt == null }
    fun observeArchived(): Flow<List<EventWithLast>> = events.observeArchived()
    fun observeEvent(id: Long): Flow<EventWithLast?> = events.observe(id)
    suspend fun getEvent(id: Long): EventWithLast? = events.get(id)
    fun observeEntries(eventId: Long): Flow<List<EntryEntity>> = entries.observeFor(eventId)
    fun observeEntryNotes(): Flow<List<EntryNote>> = entries.observeNotes()
    fun observeCategories(): Flow<List<CategoryEntity>> = categories.observeAll()

    suspend fun saveEvent(event: EventEntity): Long =
        if (event.id == 0L) events.insert(event.copy(createdAt = now())) else { events.update(event); event.id }

    suspend fun deleteEvent(id: Long) = events.delete(id)
    suspend fun archive(id: Long) = events.setArchived(id, now())
    suspend fun restore(id: Long) = events.setArchived(id, null)

    /** Logs an entry; returns its id so the caller can offer Undo. */
    suspend fun log(eventId: Long, occurredAt: Long = now(), note: String = ""): Long =
        entries.insert(EntryEntity(eventId = eventId, occurredAt = occurredAt, note = note.trim(), createdAt = now()))

    suspend fun getEntry(id: Long): EntryEntity? = entries.get(id)
    suspend fun updateEntry(entry: EntryEntity) = entries.update(entry)
    suspend fun deleteEntry(id: Long) = entries.delete(id)

    /** Puts back an entry removed a moment ago (same id), for Undo. */
    suspend fun restoreEntry(entry: EntryEntity) { entries.insert(entry) }

    suspend fun addCategory(name: String, color: Int): Long =
        categories.insert(CategoryEntity(name = name.trim(), color = color, sortOrder = categories.nextOrder()))

    suspend fun updateCategory(category: CategoryEntity) = categories.update(category)
    suspend fun deleteCategory(id: Long) = categories.delete(id)

    /** Persists a new order: list position becomes sort_order. */
    suspend fun reorderCategories(ordered: List<CategoryEntity>) =
        categories.updateAll(ordered.mapIndexed { i, c -> c.copy(sortOrder = i) })

    suspend fun <T> inTransaction(block: suspend () -> T): T = db.withTransaction { block() }
}
