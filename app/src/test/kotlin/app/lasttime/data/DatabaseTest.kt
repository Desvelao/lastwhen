package app.lasttime.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.lasttime.data.backup.BackupData
import app.lasttime.data.backup.BackupError
import app.lasttime.data.backup.BackupException
import app.lasttime.data.backup.BackupFormat
import app.lasttime.data.backup.BackupRepository
import app.lasttime.di.seedCategories
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class) // not App: it starts WorkManager and Hilt
class DatabaseTest {
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: EventRepository
    private lateinit var backup: BackupRepository

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = EventRepository(db, Clock.fixed(Instant.ofEpochMilli(1_800_000_000_000L), ZoneId.of("UTC")))
        backup = BackupRepository(db, repo, SettingsRepository(context))
    }

    @After fun tearDown() = db.close()

    private fun event(name: String, categoryId: Long? = null) = EventEntity(name = name, icon = "star", color = 1, categoryId = categoryId, createdAt = 1)

    @Test fun seedsFiveDefaultCategories() = runBlocking {
        seedCategories(db.openHelper.writableDatabase, context)
        val names = db.categoryDao().getAll().map { it.name }
        assertEquals(listOf("Home", "Car", "Family", "Pets", "Me"), names)
    }

    @Test fun lastDoneIsTheLatestEntry() = runBlocking {
        val id = repo.saveEvent(event("Water"))
        assertNull(repo.getEvent(id)!!.lastDone)
        repo.log(id, occurredAt = 100)
        repo.log(id, occurredAt = 300)
        repo.log(id, occurredAt = 200)
        assertEquals(300L, repo.getEvent(id)!!.lastDone)
        assertEquals(listOf(id), repo.activeSnapshot().map { it.event.id })
        repo.archive(id)
        assertEquals(emptyList<Long>(), repo.activeSnapshot().map { it.event.id })
    }

    @Test fun deletingAnEventCascadesToEntriesAndDeletingACategoryUncategorises() = runBlocking {
        val cat = repo.addCategory("Home", 1)
        val id = repo.saveEvent(event("Filter", cat))
        repo.log(id, occurredAt = 5)
        repo.deleteCategory(cat)
        assertNull(repo.getEvent(id)!!.event.categoryId)
        repo.deleteEvent(id)
        assertEquals(0, db.entryDao().getAll().size)
    }

    @Test fun restoreReplacesEverythingAndExportRoundTrips() = runBlocking {
        val cat = repo.addCategory("Old", 1)
        val old = repo.saveEvent(event("Old event", cat))
        repo.log(old, occurredAt = 1)
        val data = BackupData(
            exportedAt = 1,
            categories = listOf(CategoryEntity(10, "New", 2, 0)),
            events = listOf(EventEntity(20, "New event", "star", 3, 10, 14, "", null, 5)),
            entries = listOf(EntryEntity(30, 20, 99, "n", 6)),
        )
        backup.restore(BackupFormat.encode(data))
        assertEquals(data.categories, db.categoryDao().getAll())
        assertEquals(data.events, db.eventDao().getAllRaw())
        assertEquals(data.entries, db.entryDao().getAll())
        val again = BackupFormat.decode(backup.exportJson())
        assertEquals(data.copy(exportedAt = again.exportedAt), again.copy(settings = null))
    }

    @Test fun badRestoreLeavesDataUntouched() = runBlocking {
        val id = repo.saveEvent(event("Keep me"))
        val bad = BackupFormat.encode(
            BackupData(1, emptyList(), listOf(EventEntity(1, "x", "star", 1, 99, null, "", null, 1)), emptyList()),
        )
        val e = assertThrows(BackupException::class.java) { runBlocking { backup.restore(bad) } }
        assertEquals(BackupError.OrphanCategory, e.reason)
        assertEquals(listOf(id), db.eventDao().getAllRaw().map { it.id })
    }
}
