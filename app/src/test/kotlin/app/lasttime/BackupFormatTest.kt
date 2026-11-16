package app.lasttime

import app.lasttime.data.CategoryEntity
import app.lasttime.data.EntryEntity
import app.lasttime.data.EventEntity
import app.lasttime.data.Settings
import app.lasttime.data.ThemeMode
import app.lasttime.core.SortOrder
import app.lasttime.data.backup.BackupData
import app.lasttime.data.backup.BackupError
import app.lasttime.data.backup.BackupException
import app.lasttime.data.backup.BackupFormat
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFormatTest {
    private val data = BackupData(
        exportedAt = 5,
        categories = listOf(CategoryEntity(1, "Home", 123, 0)),
        events = listOf(
            EventEntity(1, "Water, \"plants\"", "leaf", 9, 1, 14, "n", null, 10),
            EventEntity(2, "=cmd", "star", 9, null, null, "", 99, 11),
        ),
        entries = listOf(EntryEntity(1, 1, 1_700_000_000_000, "ok", 1), EntryEntity(2, 2, 1_700_000_100_000, "", 2)),
    )

    private fun reasonOf(text: String): BackupError =
        assertThrows(BackupException::class.java) { BackupFormat.decode(text) }.reason

    @Test fun roundTrip() = assertEquals(data, BackupFormat.decode(BackupFormat.encode(data)))

    @Test fun settingsRoundTripAndAreOptional() {
        val s = Settings(sort = SortOrder.Name, theme = ThemeMode.Dark, dynamicColor = false, digestEnabled = true, digestMinutes = 7 * 60 + 30)
        assertEquals(s, BackupFormat.decode(BackupFormat.encode(data.copy(settings = s))).settings)
        // Older files have no settings object; partial or unknown values fall back to defaults.
        assertEquals(null, BackupFormat.decode(BackupFormat.encode(data)).settings)
        val partial = BackupFormat.encode(data).replace("\"categories\"", "\"settings\": {\"theme\": \"Neon\", \"digestMinutes\": 99999}, \"categories\"")
        assertEquals(Settings(digestMinutes = 24 * 60 - 1), BackupFormat.decode(partial).settings)
    }

    @Test fun lockIsNeverBackedUp() {
        val json = BackupFormat.encode(data.copy(settings = Settings(lockEnabled = true)))
        assertEquals(false, json.contains("lock"))
    }

    @Test fun rejectsGarbage() {
        assertEquals(BackupError.NotBackup, reasonOf("nope"))
        assertEquals(BackupError.WrongApp, reasonOf("""{"app":"other","version":1}"""))
        assertEquals(BackupError.Version, reasonOf("""{"app":"lasttime","version":99}"""))
    }

    @Test fun rejectsOrphanEntries() {
        val bad = BackupFormat.encode(data.copy(entries = listOf(EntryEntity(1, 42, 1, "", 1))))
        assertEquals(BackupError.OrphanEntries, reasonOf(bad))
    }

    @Test fun rejectsOrphanCategory() {
        val bad = BackupFormat.encode(data.copy(events = data.events.map { it.copy(categoryId = 77) }))
        assertEquals(BackupError.OrphanCategory, reasonOf(bad))
    }

    @Test fun rejectsDuplicateIds() {
        val bad = BackupFormat.encode(data.copy(events = data.events.map { it.copy(id = 1) }))
        assertEquals(BackupError.DuplicateIds, reasonOf(bad))
    }

    @Test fun csvNeutralisesTabAndCarriageReturnPrefixes() {
        val d = data.copy(entries = listOf(EntryEntity(1, 1, 1_700_000_000_000, "\tx", 1), EntryEntity(2, 1, 1_700_000_100_000, "\ry", 2)))
        val csv = BackupFormat.csv(d, ZoneId.of("UTC"))
        assertTrue(csv.contains("'\tx"))
        assertTrue(csv.contains("\"'\ry\""))
    }

    @Test fun csvQuotesAndNeutralisesFormulas() {
        val csv = BackupFormat.csv(data, ZoneId.of("UTC"))
        assertTrue(csv.startsWith("event,category,date,time,note\r\n"))
        assertTrue(csv.contains("\"Water, \"\"plants\"\"\",Home,2023-11-14,22:13,ok"))
        assertTrue(csv.contains("'=cmd,,"))
    }
}
