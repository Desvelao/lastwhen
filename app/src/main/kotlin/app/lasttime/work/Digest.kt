package app.lasttime.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.lasttime.MainActivity
import app.lasttime.R
import app.lasttime.ui.AppLanguage
import app.lasttime.core.Rhythm
import app.lasttime.core.Status
import app.lasttime.di.appEntryPoint
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "digest"
private const val WORK_NAME = "digest"
private const val NOTIFICATION_ID = 1

object DigestScheduler {
    /** Delay from [now] to the next local [minutes]-after-midnight. */
    fun delayUntil(minutes: Int, now: ZonedDateTime): Duration {
        var next = now.with(LocalTime.of(minutes / 60, minutes % 60))
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }

    /**
     * Schedules the next digest. Each run enqueues the one after it for the next local [minutes]-after-midnight, so
     * DST, time zone and clock changes never leave it at a stale offset. [replace] restarts the clock (the time or
     * the clock changed); otherwise an existing pending schedule is kept.
     */
    fun schedule(context: Context, minutes: Int, replace: Boolean) =
        enqueue(context, minutes, if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP)

    /** Called by a running worker: appends the next run instead of cancelling itself. */
    internal fun scheduleNext(context: Context, minutes: Int) = enqueue(context, minutes, ExistingWorkPolicy.APPEND_OR_REPLACE)

    private fun enqueue(context: Context, minutes: Int, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<DigestWorker>()
            .setInitialDelay(delayUntil(minutes, ZonedDateTime.now()).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
    }

    fun cancel(context: Context) { WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME) }
}

/** Posts one notification listing overdue events, or nothing when none are overdue, then schedules the next one. */
class DigestWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ep = applicationContext.appEntryPoint()
        val settings = ep.settings().settings.first()
        if (!settings.digestEnabled) return Result.success()
        try {
            post(ep, settings.lockEnabled)
        } finally {
            DigestScheduler.scheduleNext(applicationContext, settings.digestMinutes)
        }
        return Result.success()
    }

    private suspend fun post(ep: app.lasttime.di.AppEntryPoint, lockEnabled: Boolean) {
        if (!canNotify()) return

        val repo = ep.repository()
        val now = repo.now()
        val overdue = Rhythm.overdueFirst(repo.activeSnapshot(), now, repo.zone, { it.lastDone }, { it.event.targetIntervalDays })
        if (overdue.isEmpty()) return

        val c = AppLanguage.localized(applicationContext)
        ensureChannel(c)
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val title = c.resources.getQuantityString(R.plurals.digest_title, overdue.size, overdue.size)
        val builder = NotificationCompat.Builder(c, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(overdue.joinToString { it.event.name })
            .setStyle(NotificationCompat.BigTextStyle().bigText(overdue.joinToString("\n") { it.event.name }))
            .setContentIntent(open).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT)
        // With the app lock on, the device lock screen shows only the count, not the event names.
        if (lockEnabled) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(
                    NotificationCompat.Builder(c, CHANNEL_ID).setSmallIcon(R.drawable.ic_notification).setContentTitle(title).build(),
                )
        }
        val notification = builder.build()
        try {
            NotificationManagerCompat.from(c).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check above and now: skip today's digest.
        }
    }

    private fun canNotify(): Boolean {
        val c = applicationContext
        val granted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(c).areNotificationsEnabled()
    }

    private fun ensureChannel(c: Context) {
        val channel = NotificationChannel(CHANNEL_ID, c.getString(R.string.digest_channel), NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = c.getString(R.string.digest_channel_desc) }
        c.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
