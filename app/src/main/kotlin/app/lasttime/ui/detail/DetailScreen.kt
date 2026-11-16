package app.lasttime.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lasttime.R
import app.lasttime.data.EntryEntity
import app.lasttime.ui.components.EventBadge
import app.lasttime.ui.components.LogSheet
import app.lasttime.ui.components.StatusChip
import app.lasttime.ui.daysText
import app.lasttime.ui.formatDate
import app.lasttime.ui.formatDateTime
import app.lasttime.ui.text

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(onBack: () -> Unit, onEdit: (Long) -> Unit, vm: DetailViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbar = remember { SnackbarHostState() }
    var logSheet by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }

    LifecycleResumeEffect(Unit) { vm.refreshNow(); onPauseOrDispose { } }
    LaunchedEffect(Unit) {
        vm.removed.collect { entry ->
            val r = snackbar.showSnackbar(resources.getString(R.string.entry_removed), resources.getString(R.string.undo))
            if (r == SnackbarResult.ActionPerformed) vm.undoRemove(entry)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.event?.name.orEmpty(), maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                actions = {
                    if (state.event != null) IconButton(onClick = { onEdit(vm.eventId) }) { Icon(Icons.Filled.Edit, stringResource(R.string.edit)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val event = state.event
        if (event == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (!state.loading) Text(stringResource(R.string.not_found))
            }
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    EventBadge(event.icon, event.color, size = 64.dp)
                    Text(state.elapsed.text(context), style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
                    Text(
                        state.lastDone?.let { stringResource(R.string.last_done_on, formatDateTime(it)) } ?: stringResource(R.string.never_done),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    state.status?.let { StatusChip(it) }
                    if (event.archivedAt != null) Text(stringResource(R.string.archived_label), style = MaterialTheme.typography.labelLarge)
                }
            }
            if (state.progress != null) item { RhythmProgress(state) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = vm::logNow, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.detail_log_now)) }
                    OutlinedButton(onClick = { logSheet = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.detail_log)) }
                }
            }
            item { StatsRow(state) }
            item { Text(stringResource(R.string.history), style = MaterialTheme.typography.titleMedium) }
            if (state.history.isEmpty()) {
                item { Text(stringResource(R.string.history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else items(state.history, key = { it.entry.id }) { row ->
                HistoryItem(row, onEdit = { editingId = row.entry.id }, onDelete = { vm.removeEntry(row.entry) })
            }
        }
    }

    if (logSheet) LogSheet(
        title = stringResource(R.string.log_title, state.event?.name.orEmpty()),
        initialMillis = remember { System.currentTimeMillis() }, initialNote = "",
        onSave = { millis, note -> vm.log(millis, note); logSheet = false },
        onDismiss = { logSheet = false },
    )

    val editing: EntryEntity? = state.history.firstOrNull { it.entry.id == editingId }?.entry
    if (editing != null) LogSheet(
        title = stringResource(R.string.log_edit_title),
        initialMillis = editing.occurredAt, initialNote = editing.note,
        onSave = { millis, note -> vm.updateEntry(editing, millis, note); editingId = null },
        onDismiss = { editingId = null },
    )
}

@Composable
private fun RhythmProgress(state: DetailUiState) {
    val ctx = LocalContext.current
    val days = state.daysUntilDue
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(progress = { (state.progress ?: 0f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        val label = when {
            days == null -> ""
            days < 0 -> stringResource(R.string.detail_overdue_by, daysText(ctx, (-days).toInt()))
            days == 0L -> stringResource(R.string.detail_due_today)
            else -> stringResource(R.string.detail_due_in, daysText(ctx, days.toInt()))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            state.nextDue?.let { Text(stringResource(R.string.detail_next_due, formatDate(it)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun StatsRow(state: DetailUiState) {
    val ctx = LocalContext.current
    val none = stringResource(R.string.stats_none)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard(stringResource(R.string.stats_entries), state.stats.count.toString(), Modifier.weight(1f))
        StatCard(
            stringResource(R.string.stats_average),
            state.stats.averageGapDays?.let { daysText(ctx, Math.round(it).toInt()) } ?: none,
            Modifier.weight(1f),
        )
        StatCard(
            stringResource(R.string.stats_longest),
            state.stats.longestGapDays?.let { daysText(ctx, it) } ?: none,
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HistoryItem(row: HistoryRow, onEdit: () -> Unit, onDelete: () -> Unit) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(formatDateTime(row.entry.occurredAt), style = MaterialTheme.typography.bodyLarge)
                row.gapDays?.let {
                    Text(stringResource(R.string.history_gap, daysText(ctx, it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (row.entry.note.isNotBlank()) Text(row.entry.note, style = MaterialTheme.typography.bodyMedium)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.edit)) }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}
