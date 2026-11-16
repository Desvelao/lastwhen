package app.lasttime.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.lasttime.R
import app.lasttime.ui.formatDate
import app.lasttime.ui.formatTime
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Bottom sheet to log (or edit) an entry: date, time, a Yesterday shortcut and a note.
 * [initialMillis] is the starting moment; [onSave] gets the chosen moment and note.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSheet(
    title: String,
    initialMillis: Long,
    initialNote: String,
    onSave: (millis: Long, note: String) -> Unit,
    onDismiss: () -> Unit,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    var millis by rememberSaveable { mutableLongStateOf(initialMillis) }
    var note by rememberSaveable { mutableStateOf(initialNote) }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var pickTime by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickDate = true }) { Text(formatDate(millis, zone)) }
                OutlinedButton(onClick = { pickTime = true }) { Text(formatTime(millis, zone)) }
            }
            AssistChip(
                onClick = {
                    val t = Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()
                    val yesterday = LocalDate.now(zone).minusDays(1)
                    millis = yesterday.atTime(t).atZone(zone).toInstant().toEpochMilli()
                },
                label = { Text(stringResource(R.string.log_yesterday)) },
            )
            OutlinedTextField(
                value = note, onValueChange = { note = it }, label = { Text(stringResource(R.string.log_note)) },
                modifier = Modifier.fillMaxWidth(), minLines = 2,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End)) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Button(onClick = { onSave(millis, note) }) { Text(stringResource(R.string.save)) }
            }
        }
    }

    if (pickDate) {
        val localDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        // The date picker speaks UTC midnight; convert both ways so the local day is preserved.
        val state = rememberDatePickerState(initialSelectedDateMillis = localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { picked ->
                        val d = Instant.ofEpochMilli(picked).atZone(ZoneOffset.UTC).toLocalDate()
                        val t = Instant.ofEpochMilli(millis).atZone(zone).toLocalTime()
                        millis = d.atTime(t).atZone(zone).toInstant().toEpochMilli()
                    }
                    pickDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state) }
    }

    if (pickTime) {
        val current = Instant.ofEpochMilli(millis).atZone(zone)
        val state = rememberTimePickerState(current.hour, current.minute, android.text.format.DateFormat.is24HourFormat(androidx.compose.ui.platform.LocalContext.current))
        AlertDialog(
            onDismissRequest = { pickTime = false },
            title = { Text(stringResource(R.string.pick_time)) },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    val d = current.toLocalDate()
                    millis = d.atTime(LocalTime.of(state.hour, state.minute)).atZone(zone).toInstant().toEpochMilli()
                    pickTime = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
