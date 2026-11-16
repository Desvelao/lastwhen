package app.lasttime.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lasttime.R
import app.lasttime.core.AgeFilter
import app.lasttime.core.SortOrder
import app.lasttime.ui.components.EventCard
import app.lasttime.ui.components.LogSheet
import app.lasttime.ui.formatDateTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenEvent: (Long) -> Unit,
    onAdd: (name: String?, icon: String?) -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    var searching by rememberSaveable { mutableStateOf(false) }
    var sheetFor by rememberSaveable { mutableStateOf<Long?>(null) }
    var sortMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LifecycleResumeEffect(Unit) { vm.refreshNow(); onPauseOrDispose { } }

    LaunchedEffect(Unit) {
        vm.logged.collect { logged ->
            // A new Done replaces the previous snackbar, so Undo always targets the latest entry.
            snackbar.currentSnackbarData?.dismiss()
            scope.launch {
                val result = snackbar.showSnackbar(
                    resources.getString(R.string.logged_snackbar, logged.eventName),
                    actionLabel = resources.getString(R.string.undo),
                    duration = SnackbarDuration.Short,
                    withDismissAction = false,
                )
                if (result == SnackbarResult.ActionPerformed) vm.undo(logged.entryId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searching) {
                        val focus = remember { FocusRequester() }
                        LaunchedEffect(Unit) { focus.requestFocus() }
                        TextField(
                            value = state.query, onValueChange = vm::setQuery, singleLine = true,
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(),
                            colors = TextFieldDefaults.colors(focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent, unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus),
                        )
                    } else Text(stringResource(R.string.app_name))
                },
                navigationIcon = {
                    if (searching) IconButton(onClick = { searching = false; vm.setQuery("") }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.close_search))
                    }
                },
                actions = {
                    if (searching) {
                        if (state.query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Filled.Clear, stringResource(R.string.clear)) }
                    } else if (!state.noEvents) {
                        IconButton(onClick = { searching = true }) { Icon(Icons.Filled.Search, stringResource(R.string.search)) }
                        Box {
                            IconButton(onClick = { sortMenu = true }) { Icon(Icons.Filled.List, stringResource(R.string.sort)) }
                            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                listOf(
                                    SortOrder.LongestAgo to R.string.sort_longest_ago,
                                    SortOrder.MostRecent to R.string.sort_most_recent,
                                    SortOrder.Name to R.string.sort_name,
                                ).forEach { (order, label) ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(label)) },
                                        leadingIcon = { RadioButton(selected = state.sort == order, onClick = null) },
                                        onClick = { vm.setSort(order); sortMenu = false },
                                    )
                                }
                            }
                        }
                    }
                    if (!searching) IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, stringResource(R.string.settings)) }
                },
            )
        },
        floatingActionButton = {
            if (!state.noEvents && !state.loading) FloatingActionButton(onClick = { onAdd(null, null) }) {
                Icon(Icons.Filled.Add, stringResource(R.string.add_event))
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> Unit
                state.noEvents -> EmptyState(onAdd)
                else -> {
                    FilterRows(state, vm)
                    if (state.isEmptyAfterFilter) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.no_matches), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        section(state.overdue, R.string.status_overdue, state.sectioned, vm, onOpenEvent) { sheetFor = it }
                        section(state.dueSoon, R.string.status_due_soon, state.sectioned, vm, onOpenEvent) { sheetFor = it }
                        section(state.later, R.string.status_later, state.sectioned, vm, onOpenEvent) { sheetFor = it }
                    }
                }
            }
        }
    }

    sheetFor?.let { id ->
        val name = vm.eventName(id).orEmpty()
        LogSheet(
            title = stringResource(R.string.log_title, name),
            initialMillis = remember { System.currentTimeMillis() },
            initialNote = "",
            onSave = { millis, note -> vm.log(id, millis, note); sheetFor = null },
            onDismiss = { sheetFor = null },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    items: List<HomeItem>,
    @androidx.annotation.StringRes title: Int,
    showHeader: Boolean,
    vm: HomeViewModel,
    onOpen: (Long) -> Unit,
    onSheet: (Long) -> Unit,
) {
    if (items.isEmpty()) return
    if (showHeader) item(key = "h$title") {
        Text(
            stringResource(title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
        )
    }
    items(items, key = { it.id }) { item ->
        EventCard(
            name = item.name, icon = item.icon, color = item.color, elapsed = item.elapsed, lastDone = item.lastDone, status = item.status,
            onClick = { onOpen(item.id) }, onDone = { vm.done(item) }, onLogSheet = { onSheet(item.id) },
        )
    }
}

@Composable
private fun FilterRows(state: HomeUiState, vm: HomeViewModel) {
    val ages = listOf(
        AgeFilter.All to R.string.filter_all, AgeFilter.Hours to R.string.filter_hours,
        AgeFilter.Days to R.string.filter_days, AgeFilter.Weeks to R.string.filter_weeks,
        AgeFilter.Months to R.string.filter_months, AgeFilter.Years to R.string.filter_years,
    )
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ages) { (age, label) ->
            FilterChip(selected = state.age == age, onClick = { vm.setAge(age) }, label = { Text(stringResource(label)) })
        }
        items(state.categories, key = { "c${it.id}" }) { c ->
            FilterChip(selected = state.categoryFilter == c.id, onClick = { vm.setCategory(c.id) }, label = { Text(c.name) })
        }
    }
}

@Composable
private fun EmptyState(onAdd: (String?, String?) -> Unit) {
    val suggestions = listOf(
        R.string.suggest_plants to "heart",
        R.string.suggest_filter to "build",
        R.string.suggest_haircut to "face",
        R.string.suggest_dentist to "calendar",
        R.string.suggest_call to "phone",
    )
    val resources = LocalResources.current
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.empty_body), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        suggestions.forEach { (label, icon) ->
            // The suggestion text is localised: prefill the form with the string resource, not an English literal.
            androidx.compose.material3.OutlinedButton(onClick = { onAdd(resources.getString(label), icon) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(label))
            }
        }
        TextButton(onClick = { onAdd(null, null) }) { Text(stringResource(R.string.empty_custom)) }
    }
}
