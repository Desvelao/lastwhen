package app.lasttime.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/** Redraws the home-screen widgets after the data changed. Failures never break the caller. */
@Singleton
class WidgetRefresher @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun refresh() {
        try {
            SingleEventWidget().updateAll(context)
            OverdueWidget().updateAll(context)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // A widget that cannot redraw is not worth failing a log for.
        }
    }
}
