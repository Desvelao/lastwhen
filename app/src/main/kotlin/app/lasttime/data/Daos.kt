package app.lasttime.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

private const val WITH_LAST =
    "SELECT e.*, (SELECT MAX(occurred_at) FROM entry WHERE event_id = e.id) AS last_done FROM event e"

@Dao
interface EventDao {
    @Query("$WITH_LAST WHERE e.archived_at IS NULL")
    fun observeActive(): Flow<List<EventWithLast>>

    @Query("$WITH_LAST WHERE e.archived_at IS NOT NULL ORDER BY e.archived_at DESC")
    fun observeArchived(): Flow<List<EventWithLast>>

    @Query("$WITH_LAST WHERE e.id = :id")
    fun observe(id: Long): Flow<EventWithLast?>

    @Query("$WITH_LAST WHERE e.id = :id")
    suspend fun get(id: Long): EventWithLast?

    @Query("$WITH_LAST")
    suspend fun getAll(): List<EventWithLast>

    @Query("SELECT * FROM event")
    suspend fun getAllRaw(): List<EventEntity>

    @Insert suspend fun insert(event: EventEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(events: List<EventEntity>)
    @Update suspend fun update(event: EventEntity)
    @Query("DELETE FROM event WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM event") suspend fun deleteAll()
    @Query("UPDATE event SET archived_at = :at WHERE id = :id") suspend fun setArchived(id: Long, at: Long?)
}

@Dao
interface EntryDao {
    @Query("SELECT * FROM entry WHERE event_id = :eventId ORDER BY occurred_at DESC, id DESC")
    fun observeFor(eventId: Long): Flow<List<EntryEntity>>

    @Query("SELECT event_id, note FROM entry WHERE note != ''")
    fun observeNotes(): Flow<List<EntryNote>>

    @Query("SELECT * FROM entry ORDER BY occurred_at")
    suspend fun getAll(): List<EntryEntity>

    @Query("SELECT * FROM entry WHERE id = :id")
    suspend fun get(id: Long): EntryEntity?

    @Insert suspend fun insert(entry: EntryEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(entries: List<EntryEntity>)
    @Update suspend fun update(entry: EntryEntity)
    @Delete suspend fun delete(entry: EntryEntity)
    @Query("DELETE FROM entry WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM entry") suspend fun deleteAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category ORDER BY sort_order, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category ORDER BY sort_order, id")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM category")
    suspend fun nextOrder(): Int

    @Insert suspend fun insert(category: CategoryEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(categories: List<CategoryEntity>)
    @Update suspend fun update(category: CategoryEntity)
    @Update suspend fun updateAll(categories: List<CategoryEntity>)
    @Query("DELETE FROM category WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM category") suspend fun deleteAll()
}
