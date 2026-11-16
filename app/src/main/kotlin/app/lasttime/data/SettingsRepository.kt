package app.lasttime.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.lasttime.core.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

enum class ThemeMode { System, Light, Dark }

data class Settings(
    val sort: SortOrder = SortOrder.LongestAgo,
    val theme: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = true,
    val digestEnabled: Boolean = false,
    /** Minutes after local midnight. */
    val digestMinutes: Int = 8 * 60,
    val lockEnabled: Boolean = false,
)

@Singleton
class SettingsRepository @Inject constructor(@dagger.hilt.android.qualifiers.ApplicationContext context: Context) {
    private val store = context.dataStore

    private object K {
        val sort = stringPreferencesKey("sort")
        val theme = stringPreferencesKey("theme")
        val dynamic = booleanPreferencesKey("dynamic_color")
        val digest = booleanPreferencesKey("digest_enabled")
        val digestMinutes = intPreferencesKey("digest_minutes")
        val lock = booleanPreferencesKey("lock_enabled")
    }

    val settings: Flow<Settings> = store.data.map { p ->
        val d = Settings()
        Settings(
            sort = p[K.sort]?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() } ?: d.sort,
            theme = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.theme,
            dynamicColor = p[K.dynamic] ?: d.dynamicColor,
            digestEnabled = p[K.digest] ?: d.digestEnabled,
            digestMinutes = p[K.digestMinutes] ?: d.digestMinutes,
            lockEnabled = p[K.lock] ?: d.lockEnabled,
        )
    }

    /** Applies settings from a backup in one edit. The app lock is a per-device choice and is left alone. */
    suspend fun restoreFrom(s: Settings) = store.edit {
        it[K.sort] = s.sort.name
        it[K.theme] = s.theme.name
        it[K.dynamic] = s.dynamicColor
        it[K.digest] = s.digestEnabled
        it[K.digestMinutes] = s.digestMinutes
    }

    suspend fun setSort(v: SortOrder) = store.edit { it[K.sort] = v.name }
    suspend fun setTheme(v: ThemeMode) = store.edit { it[K.theme] = v.name }
    suspend fun setDynamicColor(v: Boolean) = store.edit { it[K.dynamic] = v }
    suspend fun setDigestEnabled(v: Boolean) = store.edit { it[K.digest] = v }
    suspend fun setDigestMinutes(v: Int) = store.edit { it[K.digestMinutes] = v }
    suspend fun setLockEnabled(v: Boolean) = store.edit { it[K.lock] = v }
}
