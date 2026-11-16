package app.lasttime.data.backup

import app.lasttime.data.AppDatabase
import app.lasttime.data.EventRepository
import app.lasttime.data.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val db: AppDatabase,
    private val repo: EventRepository,
    private val settings: SettingsRepository,
) {
    /** One transaction, so a widget or the digest worker cannot change data between the three reads. */
    private suspend fun snapshot() = repo.inTransaction {
        BackupData(
            exportedAt = repo.now(),
            categories = db.categoryDao().getAll(),
            events = db.eventDao().getAllRaw(),
            entries = db.entryDao().getAll(),
            settings = settings.settings.first(),
        )
    }

    suspend fun exportJson(): String = BackupFormat.encode(snapshot())

    suspend fun exportCsv(zone: ZoneId = ZoneId.systemDefault()): String = BackupFormat.csv(snapshot(), zone)

    /** Replaces everything with the backup, all or nothing. Throws [BackupException] for a bad file. */
    suspend fun restore(text: String) {
        val data = BackupFormat.decode(text)
        repo.inTransaction {
            db.eventDao().deleteAll()      // cascades to entries
            db.categoryDao().deleteAll()
            db.categoryDao().insertAll(data.categories)
            db.eventDao().insertAll(data.events)
            db.entryDao().insertAll(data.entries)
        }
        data.settings?.let { settings.restoreFrom(it) }
    }
}
