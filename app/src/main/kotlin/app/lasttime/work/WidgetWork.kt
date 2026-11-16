package app.lasttime.work

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.lasttime.di.appEntryPoint
import app.lasttime.widget.OverdueWidgetReceiver
import app.lasttime.widget.SingleEventWidgetReceiver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private const val PERIODIC = "widget-refresh"
private const val ONCE = "widget-refresh-now"

/**
 * Widget labels ("3 days", Overdue) depend on the clock, not only on the data, so they are redrawn hourly while a
 * widget is on a home screen, and right away when the date, time or time zone changes.
 */
object WidgetRefreshScheduler {
    private fun hasWidgets(c: Context): Boolean {
        val m = AppWidgetManager.getInstance(c)
        return listOf(SingleEventWidgetReceiver::class.java, OverdueWidgetReceiver::class.java)
            .any { m.getAppWidgetIds(ComponentName(c, it)).isNotEmpty() }
    }

    /** Starts the hourly refresh when a widget exists, stops it when the last one is removed. */
    fun sync(c: Context) {
        val wm = WorkManager.getInstance(c)
        if (hasWidgets(c)) {
            wm.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<WidgetRefreshWorker>(1, TimeUnit.HOURS).build())
        } else {
            wm.cancelUniqueWork(PERIODIC)
        }
    }

    fun refreshNow(c: Context) {
        WorkManager.getInstance(c).enqueueUniqueWork(ONCE, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build())
    }
}

class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        applicationContext.appEntryPoint().widgets().refresh()
        return Result.success()
    }
}

/** Date, clock or time zone changed: redraw widgets and re-aim the digest at the new local time. */
class TimeChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val c = context.applicationContext
        WidgetRefreshScheduler.refreshNow(c)
        val pending = goAsync()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            try {
                val s = c.appEntryPoint().settings().settings.first()
                if (s.digestEnabled) DigestScheduler.schedule(c, s.digestMinutes, replace = true)
            } finally {
                pending.finish()
            }
        }
    }
}
