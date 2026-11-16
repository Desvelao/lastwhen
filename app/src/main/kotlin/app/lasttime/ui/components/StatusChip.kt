package app.lasttime.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.lasttime.core.Status
import app.lasttime.ui.label

/** Status shown with an icon and a word, so colour is never the only cue. */
@Composable
fun StatusChip(status: Status, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) = when (status) {
        Status.Overdue -> scheme.errorContainer to scheme.onErrorContainer
        Status.DueSoon -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        Status.Later -> scheme.secondaryContainer to scheme.onSecondaryContainer
    }
    val icon = when (status) {
        Status.Overdue -> Icons.Filled.Warning
        Status.DueSoon -> Icons.Filled.DateRange
        Status.Later -> Icons.Filled.Check
    }
    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(status.label(LocalContext.current), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 4.dp))
        }
    }
}
