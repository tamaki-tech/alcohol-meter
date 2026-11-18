package tech.tamaki.alcoholmeter

import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import tech.tamaki.alcoholmeter.domain.usecase.ReconcileBackgroundState
import tech.tamaki.alcoholmeter.domain.usecase.HealthPermissions
import tech.tamaki.alcoholmeter.ui.navigation.AlcoholMeterNavigation
import tech.tamaki.alcoholmeter.ui.theme.AlcoholMeterTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var reconcile: ReconcileBackgroundState
    @Inject lateinit var health: HealthPermissions
    private val openEndDialog = MutableStateFlow(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openEndDialog.value = intent.getBooleanExtra("open_end_dialog", false)
        setContent {
            val requested by openEndDialog.collectAsState()
            AlcoholMeterTheme { AlcoholMeterNavigation(requested) { openEndDialog.value = false; intent.removeExtra("open_end_dialog") } }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("open_end_dialog", false)) openEndDialog.value = true
    }
    override fun onStart() {
        super.onStart()
        lifecycleScope.launch { health.refresh(); reconcile() }
    }
}
