package app.lasttime.data.backup

import app.lasttime.data.CategoryEntity
import app.lasttime.data.EntryEntity
import app.lasttime.data.EventEntity
import app.lasttime.data.Settings
import app.lasttime.data.ThemeMode
import app.lasttime.core.SortOrder
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

/** Everything the app stores, as plain data. Ids are kept so a restore reproduces the same list. */
data class BackupData(
    val exportedAt: Long,
    val categories: List<CategoryEntity>,
    val events: List<EventEntity>,
    val entries: List<EntryEntity>,
    /** Preferences worth carrying to a new device. Null when the file has none (older backups). The app lock is never included. */
    val settings: Settings? = null,
)

/** Why a backup was rejected. The UI maps each reason to a localised message. */
enum class BackupError { NotBackup, WrongApp, Version, OrphanEntries, OrphanCategory, DuplicateIds, Damaged }

class BackupException(val reason: BackupError) : Exception(reason.name)

/**
 * JSON backup and CSV export. Pure, so it is unit-tested on the JVM.
 *
 * Versioning: [VERSION] only changes for incompatible layouts. New optional parts (like "settings") are read with
 * defaults when absent, so older files keep restoring. A file with a higher version is rejected as unsupported;
 * when VERSION is bumped, decode must first upgrade each older layout to the current one before reading it.
 */
object BackupFormat {
    const val VERSION = 1

    fun encode(data: BackupData): String = JSONObject().apply {
        put("app", "lasttime")
        put("version", VERSION)
        put("exportedAt", data.exportedAt)
        data.settings?.let { s ->
            put("settings", JSONObject().put("sort", s.sort.name).put("theme", s.theme.name).put("dynamicColor", s.dynamicColor)
                .put("digestEnabled", s.digestEnabled).put("digestMinutes", s.digestMinutes))
        }
        put("categories", JSONArray(data.categories.map { c ->
            JSONObject().put("id", c.id).put("name", c.name).put("color", c.color).put("sortOrder", c.sortOrder)
        }))
        put("events", JSONArray(data.events.map { e ->
            JSONObject().put("id", e.id).put("name", e.name).put("icon", e.icon).put("color", e.color)
                .put("categoryId", e.categoryId ?: JSONObject.NULL)
                .put("targetIntervalDays", e.targetIntervalDays ?: JSONObject.NULL)
                .put("notes", e.notes)
                .put("archivedAt", e.archivedAt ?: JSONObject.NULL)
                .put("createdAt", e.createdAt)
        }))
        put("entries", JSONArray(data.entries.map { e ->
            JSONObject().put("id", e.id).put("eventId", e.eventId).put("occurredAt", e.occurredAt)
                .put("note", e.note).put("createdAt", e.createdAt)
        }))
    }.toString(2)

    fun decode(text: String): BackupData {
        val root = try { JSONObject(text) } catch (e: Exception) { throw BackupException(BackupError.NotBackup) }
        if (root.optString("app") != "lasttime") throw BackupException(BackupError.WrongApp)
        val version = root.optInt("version", -1)
        if (version < 1 || version > VERSION) throw BackupException(BackupError.Version)
        return try {
            val categories = root.getJSONArray("categories").objects().map {
                CategoryEntity(it.getLong("id"), it.getString("name"), it.getInt("color"), it.getInt("sortOrder"))
            }
            val events = root.getJSONArray("events").objects().map {
                EventEntity(
                    it.getLong("id"), it.getString("name"), it.getString("icon"), it.getInt("color"),
                    it.longOrNull("categoryId"), it.intOrNull("targetIntervalDays"), it.optString("notes", ""),
                    it.longOrNull("archivedAt"), it.getLong("createdAt"),
                )
            }
            val entries = root.getJSONArray("entries").objects().map {
                EntryEntity(it.getLong("id"), it.getLong("eventId"), it.getLong("occurredAt"), it.optString("note", ""), it.getLong("createdAt"))
            }
            // Ids must be unique (inserts use REPLACE and would silently drop rows) and references must resolve.
            if (categories.map { it.id }.toSet().size != categories.size || events.map { it.id }.toSet().size != events.size ||
                entries.map { it.id }.toSet().size != entries.size
            ) throw BackupException(BackupError.DuplicateIds)
            val categoryIds = categories.map { it.id }.toSet()
            if (events.any { it.categoryId != null && it.categoryId !in categoryIds }) throw BackupException(BackupError.OrphanCategory)
            val eventIds = events.map { it.id }.toSet()
            if (entries.any { it.eventId !in eventIds }) throw BackupException(BackupError.OrphanEntries)
            BackupData(root.optLong("exportedAt"), categories, events, entries, root.optJSONObject("settings")?.let(::decodeSettings))
        } catch (e: BackupException) {
            throw e
        } catch (e: Exception) {
            throw BackupException(BackupError.Damaged)
        }
    }

    /** Unknown or missing values fall back to the defaults, so a partial or newer settings object still restores. */
    private fun decodeSettings(o: JSONObject): Settings {
        val d = Settings()
        return Settings(
            sort = runCatching { SortOrder.valueOf(o.optString("sort")) }.getOrDefault(d.sort),
            theme = runCatching { ThemeMode.valueOf(o.optString("theme")) }.getOrDefault(d.theme),
            dynamicColor = o.optBoolean("dynamicColor", d.dynamicColor),
            digestEnabled = o.optBoolean("digestEnabled", d.digestEnabled),
            digestMinutes = o.optInt("digestMinutes", d.digestMinutes).coerceIn(0, 24 * 60 - 1),
        )
    }

    /** One row per entry, oldest first within each event. Dates are local ISO-8601 without zone. */
    fun csv(data: BackupData, zone: ZoneId): String {
        val eventsById = data.events.associateBy { it.id }
        val categoriesById = data.categories.associateBy { it.id }
        return buildString {
            append("event,category,date,time,note\r\n")
            data.entries.sortedWith(compareBy({ eventsById[it.eventId]?.name?.lowercase() }, { it.occurredAt })).forEach { en ->
                val ev = eventsById[en.eventId] ?: return@forEach
                val local = Instant.ofEpochMilli(en.occurredAt).atZone(zone).toLocalDateTime()
                append(cell(ev.name)).append(',')
                append(cell(categoriesById[ev.categoryId]?.name.orEmpty())).append(',')
                append(local.toLocalDate()).append(',')
                append(local.toLocalTime().withSecond(0).withNano(0)).append(',')
                append(cell(en.note)).append("\r\n")
            }
        }
    }

    private fun cell(v: String): String {
        // Quote, and neutralise leading characters spreadsheets would treat as a formula.
        val safe = if (v.isNotEmpty() && v[0] in "=+-@\t\r") "'$v" else v
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }

    private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }
    private fun JSONObject.longOrNull(k: String): Long? = if (isNull(k)) null else getLong(k)
    private fun JSONObject.intOrNull(k: String): Int? = if (isNull(k)) null else getInt(k)
}
