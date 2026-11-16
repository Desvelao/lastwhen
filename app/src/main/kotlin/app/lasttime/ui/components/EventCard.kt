package app.lasttime.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lasttime.R
import app.lasttime.core.Elapsed
import app.lasttime.core.Status
import app.lasttime.ui.EventIcon
import app.lasttime.ui.formatDate
import app.lasttime.ui.text

/** Round coloured badge holding the event's icon. */
@Composable
fun EventBadge(icon: String, color: Int, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 44.dp) {
    val c = Color(color)
    Box(modifier.size(size).clip(CircleShape).background(c.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
        Icon(EventIcon.of(icon).vector, contentDescription = null, tint = c, modifier = Modifier.size(size * 0.55f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventCard(
    name: String,
    icon: String,
    color: Int,
    elapsed: Elapsed,
    lastDone: Long?,
    status: Status?,
    onClick: () -> Unit,
    onDone: () -> Unit,
    onLogSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EventBadge(icon, color)
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(elapsed.text(context), style = MaterialTheme.typography.headlineSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (lastDone == null) stringResource(R.string.never_done) else formatDate(lastDone),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (status != null) StatusChip(status)
                }
            }
            DoneButton(onDone, onLogSheet)
        }
    }
}

/** Tap logs now; long-press opens the log sheet. At least 48dp tall and wide. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DoneButton(onDone: () -> Unit, onLogSheet: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClickLabel = stringResource(R.string.done_action_label),
                onLongClickLabel = stringResource(R.string.done_long_label),
                onLongClick = onLogSheet,
                onClick = onDone,
            ),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.done), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp))
        }
    }
}
