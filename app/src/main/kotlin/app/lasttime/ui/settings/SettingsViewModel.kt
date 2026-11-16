package app.lasttime.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lasttime.data.EventRepository
import app.lasttime.data.Settings
import app.lasttime.data.SettingsRepository
import app.lasttime.data.ThemeMode
import app.lasttime.data.backup.BackupError
import app.lasttime.data.backup.BackupException
import app.lasttime.data.backup.BackupRepository
import app.lasttime.lock.LockState
import app.lasttime.widget.WidgetRefresher
import app.lasttime.work.DigestScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** One-shot messages for the screen. Kept as data so the screen picks the localised text. */
sealed interface SettingsMessage {
    data object BackupSaved : SettingsMessage
    data object CsvSaved : SettingsMessage
    data object Restored : SettingsMessage
    data class RestoreFailed(val reason: BackupError) : SettingsMessage
    data object FileFailed : SettingsMessage
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepo: SettingsRepository,
    private val backup: BackupRepository,
    private val repo: EventRepository,
    private val lock: LockState,
    private val widgets: WidgetRefresher,
) : ViewModel() {
    val settings: StateFlow<Settings> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    private val _messages = MutableSharedFlow<SettingsMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<SettingsMessage> = _messages

    fun setTheme(v: ThemeMode) { viewModelScope.launch { settingsRepo.setTheme(v) } }
    fun setDynamic(v: Boolean) { viewModelScope.launch { settingsRepo.setDynamicColor(v) } }

    fun setDigest(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setDigestEnabled(enabled)
            if (enabled) DigestScheduler.schedule(context, settings.value.digestMinutes, replace = true) else DigestScheduler.cancel(context)
        }
    }

    fun setDigestMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepo.setDigestMinutes(minutes)
            if (settings.value.digestEnabled) DigestScheduler.schedule(context, minutes, replace = true)
        }
    }

    fun setLock(enabled: Boolean) {
        viewModelScope.launch {
            // The person turning the lock on is already in the app, so do not lock them out of this session.
            if (enabled) lock.setUnlocked(true)
            settingsRepo.setLockEnabled(enabled)
        }
    }

    fun writeBackup(uri: Uri) = write(uri, SettingsMessage.BackupSaved) { backup.exportJson() }
    fun writeCsv(uri: Uri) = write(uri, SettingsMessage.CsvSaved) { backup.exportCsv(repo.zone) }

    private fun write(uri: Uri, done: SettingsMessage, content: suspend () -> String) {
        viewModelScope.launch {
            val message = try {
                val text = content()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                }
                done
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                SettingsMessage.FileFailed
            }
            _messages.tryEmit(message)
        }
    }

    fun restore(uri: Uri) {
        viewModelScope.launch {
            val message = try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
                }
                backup.restore(text)
                // The restored digest preference needs its work scheduled (or cancelled) to match.
                val restored = settingsRepo.settings.first()
                if (restored.digestEnabled) DigestScheduler.schedule(context, restored.digestMinutes, replace = true) else DigestScheduler.cancel(context)
                widgets.refresh()
                SettingsMessage.Restored
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: BackupException) {
                SettingsMessage.RestoreFailed(e.reason)
            } catch (e: Exception) {
                SettingsMessage.FileFailed
            }
            _messages.tryEmit(message)
        }
    }
}
