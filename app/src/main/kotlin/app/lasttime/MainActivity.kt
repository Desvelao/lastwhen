package app.lasttime

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatActivity
import app.lasttime.data.ThemeMode
import app.lasttime.lock.Biometrics
import app.lasttime.ui.AppNav
import app.lasttime.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

// AppCompatActivity (a FragmentActivity) gives BiometricPrompt what it needs and applies the per-app language.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by vm.settings.collectAsState()
            val unlocked by vm.unlocked.collectAsState()
            val s = settings
            AppTheme(s?.theme ?: ThemeMode.System, s?.dynamicColor ?: true) {
                // The surface fills the whole window (including behind the system bars); only its content is inset.
                Surface(Modifier.fillMaxSize()) {
                    when {
                        s == null -> Unit
                        s.lockEnabled && !unlocked -> {
                            LockScreen(onUnlock = { Biometrics.prompt(this, onSuccess = vm::unlock, onFailure = {}) })
                        }
                        else -> AppNav()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vm.onStarted(SystemClock.elapsedRealtime())
    }

    override fun onStop() {
        super.onStop()
        vm.onStopped(SystemClock.elapsedRealtime())
    }

    @androidx.compose.runtime.Composable
    private fun LockScreen(onUnlock: () -> Unit) {
        LaunchedEffect(Unit) { onUnlock() }
        Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.lock_locked), style = MaterialTheme.typography.headlineMedium)
                Button(onClick = onUnlock) { Text(stringResource(R.string.lock_retry)) }
            }
        }
    }
}
