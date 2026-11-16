package app.lasttime.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)

@Entity(
    tableName = "event",
    foreignKeys = [ForeignKey(CategoryEntity::class, ["id"], ["category_id"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("category_id")],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Int,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    @ColumnInfo(name = "target_interval_days") val targetIntervalDays: Int? = null,
    val notes: String = "",
    @ColumnInfo(name = "archived_at") val archivedAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "entry",
    foreignKeys = [ForeignKey(EventEntity::class, ["id"], ["event_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("event_id", "occurred_at")],
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "event_id") val eventId: Long,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    val note: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** An event row together with its latest entry time (computed by the query, never stored). */
data class EventWithLast(
    @Embedded val event: EventEntity,
    @ColumnInfo(name = "last_done") val lastDone: Long?,
)

data class EntryNote(
    @ColumnInfo(name = "event_id") val eventId: Long,
    val note: String,
)
