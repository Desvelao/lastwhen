package app.lasttime.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.lasttime.R
import app.lasttime.data.EventRepository
import app.lasttime.data.EventWithLast
import app.lasttime.ui.components.EventBadge
import app.lasttime.widget.WidgetRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArchivedViewModel @Inject constructor(private val repo: EventRepository, private val widgets: WidgetRefresher) : ViewModel() {
    val events: StateFlow<List<EventWithLast>?> =
        repo.observeArchived().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun restore(id: Long) { viewModelScope.launch { repo.restore(id); widgets.refresh() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedScreen(onBack: () -> Unit, onOpen: (Long) -> Unit, vm: ArchivedViewModel = hiltViewModel()) {
    val events by vm.events.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.archived_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        val list = events
        if (list != null && list.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.archived_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else LazyColumn(
            Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(list.orEmpty(), key = { it.event.id }) { e ->
                Card(onClick = { onOpen(e.event.id) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EventBadge(e.event.icon, e.event.color)
                        Text(e.event.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { vm.restore(e.event.id) }) { Text(stringResource(R.string.restore)) }
                    }
                }
            }
        }
    }
}
