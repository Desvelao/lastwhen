package app.lasttime.ui.settings

import android.Manifest
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lasttime.BuildConfig
import app.lasttime.R
import app.lasttime.data.ThemeMode
import app.lasttime.data.backup.BackupError
import app.lasttime.ui.AppLanguage
import app.lasttime.ui.Language
import app.lasttime.lock.Biometrics
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onCategories: () -> Unit, onArchived: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbar = remember { SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var timeDialog by rememberSaveable { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var deniedNote by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.messages.collect { m ->
            snackbar.showSnackbar(
                when (m) {
                    SettingsMessage.BackupSaved -> resources.getString(R.string.settings_backup_done)
                    SettingsMessage.CsvSaved -> resources.getString(R.string.settings_csv_done)
                    SettingsMessage.Restored -> resources.getString(R.string.settings_restore_done)
                    is SettingsMessage.RestoreFailed -> resources.getString(R.string.settings_restore_failed, resources.getString(m.reason.message()))
                    SettingsMessage.FileFailed -> resources.getString(R.string.settings_file_failed)
                },
            )
        }
    }

    val date = LocalDate.now().toString()
    // File names come from the app name parameter, reduced to safe characters.
    val base = stringResource(R.string.app_name).lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "app" }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(vm::writeBackup) }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { it?.let(vm::writeCsv) }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { restoreUri = it }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.setDigest(true) else deniedNote = true
    }
    val lockUnavailable = stringResource(R.string.settings_lock_unavailable)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            Header(R.string.settings_appearance)
            listOf(ThemeMode.System to R.string.theme_system, ThemeMode.Light to R.string.theme_light, ThemeMode.Dark to R.string.theme_dark).forEach { (mode, label) ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected = s.theme == mode, role = Role.RadioButton, onClick = { vm.setTheme(mode) }).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = s.theme == mode, onClick = null)
                    Text(stringResource(label), Modifier.padding(start = 16.dp))
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow(R.string.settings_dynamic, R.string.settings_dynamic_desc, s.dynamicColor, vm::setDynamic)
            }
            HorizontalDivider()

            Header(R.string.settings_language)
            var language by remember { mutableStateOf(AppLanguage.current()) }
            var languageOpen by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = languageOpen,
                onExpandedChange = { languageOpen = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                OutlinedTextField(
                    value = stringResource(language.label),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_language)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageOpen) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = languageOpen, onDismissRequest = { languageOpen = false }) {
                    Language.entries.forEach { l ->
                        DropdownMenuItem(
                            text = { Text(stringResource(l.label)) },
                            onClick = { languageOpen = false; language = l; AppLanguage.set(context, l) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
            }
            HorizontalDivider()

            Header(R.string.settings_organise)
            ClickRow(R.string.categories_title, onClick = onCategories)
            ClickRow(R.string.settings_archived, onClick = onArchived)
            HorizontalDivider()

            Header(R.string.settings_digest)
            SwitchRow(R.string.settings_digest, R.string.settings_digest_desc, s.digestEnabled) { on ->
                deniedNote = false
                if (on && Build.VERSION.SDK_INT >= 33 &&
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else vm.setDigest(on)
            }
            if (deniedNote) Text(stringResource(R.string.settings_digest_denied), Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error)
            if (s.digestEnabled) {
                val label = LocalTime.of(s.digestMinutes / 60, s.digestMinutes % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(androidx.compose.ui.platform.LocalLocale.current.platformLocale))
                ClickRow(R.string.settings_digest_time, onClick = { timeDialog = true }, trailing = label)
            }
            HorizontalDivider()

            Header(R.string.settings_privacy)
            SwitchRow(R.string.settings_lock, R.string.settings_lock_desc, s.lockEnabled) { on ->
                if (on && !Biometrics.isAvailable(context)) {
                    // No screen lock set up: nothing could unlock the app.
                    scope.launch { snackbar.showSnackbar(lockUnavailable) }
                } else vm.setLock(on)
            }
            HorizontalDivider()

            Header(R.string.settings_data)
            ClickRow(R.string.settings_backup) { backupLauncher.launch("$base-backup-$date.json") }
            ClickRow(R.string.settings_restore) { restoreLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
            ClickRow(R.string.settings_csv) { csvLauncher.launch("$base-history-$date.csv") }
            HorizontalDivider()

            Header(R.string.settings_about)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = { showAbout = true }).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME)) }
        }
    }

    if (showAbout) {
        val linkOpenFailed = stringResource(R.string.about_no_browser)
        val linkCopied = stringResource(R.string.about_copied)
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text(stringResource(R.string.about_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
                    Text(stringResource(R.string.settings_offline), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(BuildConfig.APP_URL, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showAbout = false
                    try {
                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(BuildConfig.APP_URL)))
                    } catch (e: android.content.ActivityNotFoundException) {
                        scope.launch { snackbar.showSnackbar(linkOpenFailed) }
                    }
                }) { Text(stringResource(R.string.about_open)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showAbout = false
                        val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText(null, BuildConfig.APP_URL))
                        scope.launch { snackbar.showSnackbar(linkCopied) }
                    }) { Text(stringResource(R.string.about_copy)) }
                    TextButton(onClick = { showAbout = false }) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
    }

    if (timeDialog) {
        val state = rememberTimePickerState(s.digestMinutes / 60, s.digestMinutes % 60, android.text.format.DateFormat.is24HourFormat(context))
        AlertDialog(
            onDismissRequest = { timeDialog = false },
            title = { Text(stringResource(R.string.settings_digest_time)) },
            text = { TimePicker(state) },
            confirmButton = { TextButton(onClick = { vm.setDigestMinutes(state.hour * 60 + state.minute); timeDialog = false }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton(onClick = { timeDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    restoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { restoreUri = null },
            title = { Text(stringResource(R.string.settings_restore_confirm_title)) },
            text = { Text(stringResource(R.string.settings_restore_confirm_body)) },
            confirmButton = { TextButton(onClick = { vm.restore(uri); restoreUri = null }) { Text(stringResource(R.string.settings_restore)) } },
            dismissButton = { TextButton(onClick = { restoreUri = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Header(res: Int) {
    Text(stringResource(res), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
}

@Composable
private fun ClickRow(label: Int, trailing: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchRow(label: Int, desc: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(selected = checked, role = Role.Switch, onClick = { onChange(!checked) }).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(stringResource(label))
            Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

private fun BackupError.message(): Int = when (this) {
    BackupError.NotBackup -> R.string.backup_error_not_backup
    BackupError.WrongApp -> R.string.backup_error_wrong_app
    BackupError.Version -> R.string.backup_error_version
    BackupError.OrphanEntries -> R.string.backup_error_orphans
    BackupError.OrphanCategory -> R.string.backup_error_category
    BackupError.DuplicateIds -> R.string.backup_error_duplicates
    BackupError.Damaged -> R.string.backup_error_damaged
}
