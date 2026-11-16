package app.lasttime

import android.app.Application
import app.lasttime.data.SettingsRepository
import app.lasttime.ui.AppLanguage
import app.lasttime.work.DigestScheduler
import app.lasttime.work.WidgetRefreshScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    @Inject lateinit var settings: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        AppLanguage.restore(this)
        WidgetRefreshScheduler.sync(this)
        // Make sure the digest is scheduled after an update or restore; KEEP leaves a healthy schedule alone.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            val s = settings.settings.first()
            if (s.digestEnabled) DigestScheduler.schedule(this@App, s.digestMinutes, replace = false)
        }
    }
}
