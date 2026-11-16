package app.lasttime.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Schema v1 already holds everything the later phases need. Any change needs a version bump plus a real
 * Migration (never destructive); the exported schema in app/schemas/ is committed.
 */
@Database(
    entities = [EventEntity::class, EntryEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun entryDao(): EntryDao
    abstract fun categoryDao(): CategoryDao
}
