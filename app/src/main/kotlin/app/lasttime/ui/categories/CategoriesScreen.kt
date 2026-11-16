package app.lasttime.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.lasttime.R
import app.lasttime.data.CategoryEntity
import app.lasttime.data.EventRepository
import app.lasttime.data.Palette
import app.lasttime.ui.form.ColorSwatch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(private val repo: EventRepository) : ViewModel() {
    val categories: StateFlow<List<CategoryEntity>> =
        repo.observeCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(name: String, color: Int) { if (name.isNotBlank()) viewModelScope.launch { repo.addCategory(name, color) } }
    fun update(c: CategoryEntity) { if (c.name.isNotBlank()) viewModelScope.launch { repo.updateCategory(c.copy(name = c.name.trim())) } }
    fun delete(c: CategoryEntity) { viewModelScope.launch { repo.deleteCategory(c.id) } }
    fun move(from: Int, to: Int) {
        val list = categories.value.toMutableList()
        if (to !in list.indices) return
        list.add(to, list.removeAt(from))
        viewModelScope.launch { repo.reorderCategories(list) }
    }
}

private sealed interface Dialog {
    data object Add : Dialog
    data class Edit(val category: CategoryEntity) : Dialog
    data class Delete(val category: CategoryEntity) : Dialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(onBack: () -> Unit, vm: CategoriesViewModel = hiltViewModel()) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<Dialog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { dialog = Dialog.Add }) { Icon(Icons.Filled.Add, stringResource(R.string.categories_add)) }
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(categories, key = { _, c -> c.id }) { index, c ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(20.dp).clip(CircleShape).background(Color(c.color)))
                        Text(c.name, Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
                        IconButton(onClick = { vm.move(index, index - 1) }, enabled = index > 0) { Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.categories_move_up)) }
                        IconButton(onClick = { vm.move(index, index + 1) }, enabled = index < categories.lastIndex) { Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.categories_move_down)) }
                        IconButton(onClick = { dialog = Dialog.Edit(c) }) { Icon(Icons.Filled.Edit, stringResource(R.string.edit)) }
                        IconButton(onClick = { dialog = Dialog.Delete(c) }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                    }
                }
            }
        }
    }

    when (val d = dialog) {
        Dialog.Add -> CategoryDialog(null, onDismiss = { dialog = null }) { name, color -> vm.add(name, color); dialog = null }
        is Dialog.Edit -> CategoryDialog(d.category, onDismiss = { dialog = null }) { name, color ->
            vm.update(d.category.copy(name = name, color = color)); dialog = null
        }
        is Dialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(stringResource(R.string.categories_delete_title, d.category.name)) },
            text = { Text(stringResource(R.string.categories_delete_body)) },
            confirmButton = { TextButton(onClick = { vm.delete(d.category); dialog = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.cancel)) } },
        )
        null -> Unit
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDialog(existing: CategoryEntity?, onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var color by remember { mutableStateOf(existing?.color ?: Palette.default) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (existing == null) R.string.categories_add else R.string.rename)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text(stringResource(R.string.categories_name)) })
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Palette.colors.forEachIndexed { i, c -> ColorSwatch(c, i + 1, selected = c == color) { color = c } }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, color) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
