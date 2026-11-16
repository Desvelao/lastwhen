package app.lasttime.ui

import android.content.Context
import app.lasttime.R
import app.lasttime.core.Elapsed
import app.lasttime.core.Status
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Elapsed value as text ("3 days", "Yesterday"). Plain function of a Context so widgets and workers can use it. */
fun Elapsed.text(c: Context): String = when (this) {
    Elapsed.Never -> c.getString(R.string.elapsed_never)
    is Elapsed.Today ->
        if (hours == 0) c.getString(R.string.elapsed_today) else c.resources.getQuantityString(R.plurals.elapsed_hours_ago, hours, hours)
    Elapsed.Yesterday -> c.getString(R.string.elapsed_yesterday)
    is Elapsed.Days -> c.resources.getQuantityString(R.plurals.elapsed_days, days, days)
    is Elapsed.Months -> c.resources.getQuantityString(R.plurals.elapsed_months, months, months)
    is Elapsed.Years -> {
        val y = c.resources.getQuantityString(R.plurals.elapsed_years, years, years)
        if (months == 0) y else c.getString(
            R.string.elapsed_years_months, y, c.resources.getQuantityString(R.plurals.elapsed_months, months, months),
        )
    }
}

fun daysText(c: Context, days: Int): String = c.resources.getQuantityString(R.plurals.elapsed_days, days, days)

fun Status.label(c: Context): String = c.getString(
    when (this) {
        Status.Overdue -> R.string.status_overdue
        Status.DueSoon -> R.string.status_due_soon
        Status.Later -> R.string.status_later
    },
)

private fun locale(): Locale = Locale.getDefault()

fun formatDate(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale()).format(Instant.ofEpochMilli(millis).atZone(zone))

fun formatTime(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale()).format(Instant.ofEpochMilli(millis).atZone(zone))

fun formatDateTime(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String = "${formatDate(millis, zone)}, ${formatTime(millis, zone)}"
