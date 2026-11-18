package tech.tamaki.alcoholmeter.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import tech.tamaki.alcoholmeter.domain.model.AppState
import tech.tamaki.alcoholmeter.domain.usecase.ObserveHomeState
import tech.tamaki.alcoholmeter.domain.usecase.EndParty
import tech.tamaki.alcoholmeter.domain.usecase.PartyEffects
import tech.tamaki.alcoholmeter.domain.usecase.RecordDrink
import tech.tamaki.alcoholmeter.domain.usecase.ReconcileBackgroundState

@AndroidEntryPoint
class DrinkActionReceiver : BroadcastReceiver() {
    @Inject lateinit var recordDrink: RecordDrink
    @Inject lateinit var endParty: EndParty
    @Inject lateinit var effects: PartyEffects
    @Inject lateinit var observeHomeState: ObserveHomeState
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_DRINK -> recordDrink(intent.getLongExtra("presetId", -1))
                    ACTION_END -> {
                        val expected = intent.getStringExtra("lastDrink")?.let(Instant::parse)
                        val active = observeHomeState().first() as? AppState.InParty
                        val last = active?.data?.logs?.maxOfOrNull { it.drankAt }
                        if (last != null && last == expected) endParty(last)
                    }
                    ACTION_CONTINUE -> { effects.postponeReminder() }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // 通知の操作時に対象が既に削除されている場合がある。
            } finally { pending.finish() }
        }
    }
    companion object {
        const val ACTION_DRINK = "tech.tamaki.alcoholmeter.DRINK"
        const val ACTION_END = "tech.tamaki.alcoholmeter.END_LAST"
        const val ACTION_CONTINUE = "tech.tamaki.alcoholmeter.CONTINUE"
    }
}

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var reconcile: ReconcileBackgroundState
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { reconcile() } finally { pending.finish() }
        }
    }
}
