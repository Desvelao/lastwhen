package app.lasttime.ui.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lasttime.R
import app.lasttime.core.IntervalUnit
import app.lasttime.data.Palette
import app.lasttime.ui.EventIcon
import app.lasttime.ui.categories.CategoryDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onGone: () -> Unit,
    vm: FormViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var addingCategory by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (s.editing) R.string.form_edit_title else R.string.form_new_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
                actions = { TextButton(onClick = { vm.save(onSaved) }, enabled = s.loaded) { Text(stringResource(R.string.save)) } },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = s.name, onValueChange = vm::setName, singleLine = true, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.form_name)) }, isError = s.nameError,
                supportingText = if (s.nameError) ({ Text(stringResource(R.string.form_name_required)) }) else null,
            )

            Text(stringResource(R.string.form_icon), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EventIcon.entries.forEach { icon ->
                    val selected = icon.key == s.icon
                    IconButton(
                        onClick = { vm.setIcon(icon.key) },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) { Icon(icon.vector, stringResource(icon.label)) }
                }
            }

            Text(stringResource(R.string.form_color), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Palette.colors.forEachIndexed { i, c -> ColorSwatch(c, i + 1, selected = c == s.color) { vm.setColor(c) } }
            }

            Text(stringResource(R.string.form_category), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = s.categoryId == null, onClick = { vm.setCategory(null) }, label = { Text(stringResource(R.string.form_no_category)) })
                s.categories.forEach { c ->
                    FilterChip(selected = s.categoryId == c.id, onClick = { vm.setCategory(c.id) }, label = { Text(c.name) })
                }
                AssistChip(
                    onClick = { addingCategory = true },
                    label = { Text(stringResource(R.string.form_add_category)) },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.form_rhythm), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.form_rhythm_switch), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = s.rhythmOn, onCheckedChange = vm::setRhythmOn)
            }
            if (s.rhythmOn) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                    OutlinedTextField(
                        value = s.rhythmCount, onValueChange = vm::setRhythmCount, singleLine = true,
                        label = { Text(stringResource(R.string.form_rhythm_every)) }, isError = s.rhythmError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = if (s.rhythmError) ({ Text(stringResource(R.string.form_rhythm_invalid)) }) else null,
                        modifier = Modifier.weight(1f),
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        IntervalUnit.Days to R.string.unit_days, IntervalUnit.Weeks to R.string.unit_weeks,
                        IntervalUnit.Months to R.string.unit_months, IntervalUnit.Years to R.string.unit_years,
                    ).forEach { (unit, label) ->
                        FilterChip(selected = s.rhythmUnit == unit, onClick = { vm.setRhythmUnit(unit) }, label = { Text(stringResource(label)) })
                    }
                }
            }

            OutlinedTextField(
                value = s.notes, onValueChange = vm::setNotes, minLines = 2, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.form_notes)) },
            )

            if (s.editing) {
                OutlinedButton(
                    onClick = { if (s.archived) vm.restore(onGone) else vm.archive(onGone) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(if (s.archived) R.string.restore else R.string.form_archive)) }
                OutlinedButton(
                    onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.form_delete), color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    if (addingCategory) CategoryDialog(null, onDismiss = { addingCategory = false }) { name, color ->
        vm.addCategory(name, color); addingCategory = false
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.delete_confirm_title, s.name)) },
        text = { Text(stringResource(R.string.delete_confirm_body)) },
        confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(onGone) }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun ColorSwatch(color: Int, number: Int, selected: Boolean, onClick: () -> Unit) {
    val desc = stringResource(R.string.color_swatch, number)
    val fill = Color(color)
    // The whole 48dp cell is the touch target; the visible circle is inset by 4dp.
    Box(
        Modifier.size(48.dp).selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .androidx_semantics(desc).padding(4.dp).clip(CircleShape).background(fill),
        contentAlignment = Alignment.Center,
    ) {
        // A check mark, not just a ring, marks the selection. Black or white, whichever contrasts more with the swatch.
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = if (fill.luminance() > 0.179f) Color.Black else Color.White)
    }
}

private fun Modifier.androidx_semantics(desc: String): Modifier =
    this.then(Modifier.semantics { contentDescription = desc })
