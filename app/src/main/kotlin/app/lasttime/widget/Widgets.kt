package app.lasttime.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Button
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn as GlanceLazyColumn
import androidx.glance.appwidget.lazy.items as glanceItems
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.flowOf
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row as GlanceRow
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize as glanceFillMaxSize
import androidx.glance.layout.fillMaxWidth as glanceFillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding as glancePadding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text as GlanceText
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import app.lasttime.MainActivity
import app.lasttime.R
import app.lasttime.ui.AppLanguage
import app.lasttime.work.WidgetRefreshScheduler
import app.lasttime.core.Elapsed
import app.lasttime.core.ElapsedCalc
import app.lasttime.core.Rhythm
import app.lasttime.core.Status
import app.lasttime.data.EventWithLast
import app.lasttime.di.appEntryPoint
import app.lasttime.ui.text
import app.lasttime.ui.theme.AppTheme
import kotlinx.coroutines.launch

private val EventIdKey = longPreferencesKey("widget_event_id")
private val EventIdParam = ActionParameters.Key<Long>("event_id")

private val Bg = ColorProvider(day = Color(0xFFF3F4F8), night = Color(0xFF1B1F27))
private val Fg = ColorProvider(day = Color(0xFF14171C), night = Color(0xFFE6E8EE))
private val FgDim = ColorProvider(day = Color(0xFF555B66), night = Color(0xFFA9AFBB))
private val Warn = ColorProvider(day = Color(0xFFB3261E), night = Color(0xFFF2B8B5))

/**
 * One event with its counter and a Done button. The chosen event id lives in the widget's own state.
 * Everything is read inside the content (state and database flows): Glance keeps one session alive and
 * recomposes it on update(), it does not call provideGlance again.
 */
class SingleEventWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        @Suppress("NAME_SHADOWING") val context = AppLanguage.localized(context)
        val repo = context.appEntryPoint().repository()
        provideContent {
            val eventId = currentState<Preferences>()[EventIdKey]
            val flow = remember(eventId) { if (eventId == null) flowOf(null) else repo.observeEvent(eventId) }
            val event = flow.collectAsState(initial = null).value?.takeIf { it.event.archivedAt == null }
            val now = repo.now()
            val zone = repo.zone
            val title = event?.event?.name
            val elapsed = event?.let { ElapsedCalc.elapsed(it.lastDone, now, zone) }
            val status = event?.let { Rhythm.status(it.lastDone, it.event.targetIntervalDays, now, zone) }

            Column(
                GlanceModifier.glanceFillMaxSize().background(Bg).cornerRadius(20.dp).glancePadding(12.dp)
                    .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (title == null || elapsed == null || eventId == null) {
                    GlanceText(context.getString(R.string.widget_pick_prompt), style = TextStyle(color = Fg, fontSize = 14.sp))
                } else {
                    GlanceText(title, maxLines = 1, style = TextStyle(color = FgDim, fontSize = 13.sp))
                    GlanceText(elapsed.text(context), style = TextStyle(color = if (status == Status.Overdue) Warn else Fg, fontSize = 24.sp, fontWeight = FontWeight.Bold))
                    if (status == Status.Overdue) GlanceText(context.getString(R.string.status_overdue), style = TextStyle(color = Warn, fontSize = 12.sp))
                    Spacer(GlanceModifier.height(6.dp))
                    Button(context.getString(R.string.widget_done), onClick = actionRunCallback<DoneAction>(actionParametersOf(EventIdParam to eventId)))
                }
            }
        }
    }
}

class DoneAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val eventId = parameters[EventIdParam] ?: return
        val ep = context.appEntryPoint()
        ep.repository().log(eventId)
        ep.widgets().refresh()
    }
}

/** Overdue events, most overdue first. */
class OverdueWidget : GlanceAppWidget() {
    private data class Row(val name: String, val label: String)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        @Suppress("NAME_SHADOWING") val context = AppLanguage.localized(context)
        val repo = context.appEntryPoint().repository()
        provideContent {
            val events = repo.observeActive().collectAsState(initial = emptyList()).value
            val now = repo.now()
            val zone = repo.zone
            val rows = Rhythm.overdueFirst(events, now, zone, { it.lastDone }, { it.event.targetIntervalDays })
                .take(20)
                .map { e: EventWithLast -> Row(e.event.name, ElapsedCalc.elapsed(e.lastDone, now, zone).text(context)) }

            Column(
                GlanceModifier.glanceFillMaxSize().background(Bg).cornerRadius(20.dp).glancePadding(12.dp)
                    .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
            ) {
                GlanceText(context.getString(R.string.widget_overdue_name), style = TextStyle(color = FgDim, fontSize = 13.sp, fontWeight = FontWeight.Medium))
                Spacer(GlanceModifier.height(4.dp))
                if (rows.isEmpty()) GlanceText(context.getString(R.string.widget_none_overdue), style = TextStyle(color = Fg, fontSize = 14.sp))
                else GlanceLazyColumn {
                    glanceItems(rows) { r ->
                        GlanceRow(GlanceModifier.glanceFillMaxWidth().glancePadding(vertical = 3.dp)) {
                            GlanceText(r.name, maxLines = 1, modifier = GlanceModifier.defaultWeight(), style = TextStyle(color = Fg, fontSize = 14.sp))
                            GlanceText(r.label, style = TextStyle(color = Warn, fontSize = 13.sp))
                        }
                    }
                }
            }
        }
    }
}

class SingleEventWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SingleEventWidget()
    override fun onEnabled(context: Context) { super.onEnabled(context); WidgetRefreshScheduler.sync(context) }
    override fun onDisabled(context: Context) { super.onDisabled(context); WidgetRefreshScheduler.sync(context) }
}

class OverdueWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OverdueWidget()
    override fun onEnabled(context: Context) { super.onEnabled(context); WidgetRefreshScheduler.sync(context) }
    override fun onDisabled(context: Context) { super.onDisabled(context); WidgetRefreshScheduler.sync(context) }
}

/** Shown when a Single event widget is placed: the user picks which event it tracks. */
class EventWidgetConfigActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Cancelled unless an event is picked, so the launcher drops the widget.
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val repo = appEntryPoint().repository()
        setContent {
            val settings = appEntryPoint().settings().settings.collectAsState(initial = null).value
            AppTheme(settings?.theme ?: app.lasttime.data.ThemeMode.System, settings?.dynamicColor ?: true) {
                val events by repo.observeActive().collectAsState(initial = emptyList())
                val scope = rememberCoroutineScope()
                Surface(Modifier.fillMaxSize()) {
                    LazyColumn(Modifier.safeDrawingPadding()) {
                        item { Text(stringResource(R.string.widget_pick_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp)) }
                        items(events.sortedBy { it.event.name.lowercase() }, key = { it.event.id }) { e ->
                            Row(Modifier.fillMaxWidth().clickable {
                                scope.launch {
                                    val glanceId = GlanceAppWidgetManager(this@EventWidgetConfigActivity).getGlanceIdBy(appWidgetId)
                                    updateAppWidgetState(this@EventWidgetConfigActivity, PreferencesGlanceStateDefinition, glanceId) {
                                        it.toMutablePreferences().apply { this[EventIdKey] = e.event.id }
                                    }
                                    SingleEventWidget().update(this@EventWidgetConfigActivity, glanceId)
                                    setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                                    finish()
                                }
                            }.padding(16.dp)) { Text(e.event.name, style = MaterialTheme.typography.bodyLarge) }
                        }
                    }
                }
            }
        }
    }
}
