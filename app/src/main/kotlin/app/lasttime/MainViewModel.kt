package app.lasttime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lasttime.data.Settings
import app.lasttime.data.SettingsRepository
import app.lasttime.lock.LockState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(settings: SettingsRepository, private val lock: LockState) : ViewModel() {
    /** Null until the first read, so the app never flashes unlocked content before knowing the lock setting. */
    val settings: StateFlow<Settings?> = settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val unlocked: StateFlow<Boolean> = lock.unlocked
    fun unlock() = lock.setUnlocked(true)
    fun onStopped(now: Long) = lock.onAppStopped(now)
    fun onStarted(now: Long) = lock.onAppStarted(now)
}
