package tech.tamaki.alcoholmeter.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import kotlinx.coroutines.flow.first
import tech.tamaki.alcoholmeter.domain.model.AppState
import tech.tamaki.alcoholmeter.domain.usecase.ObserveHomeState
import tech.tamaki.alcoholmeter.domain.usecase.PartyEffects
import tech.tamaki.alcoholmeter.domain.usecase.shouldRemindToEnd

@HiltWorker
class BacUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observeHomeState: ObserveHomeState,
    private val effects: PartyEffects,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (observeHomeState().first() is AppState.InParty) effects.refreshNotification() else effects.cancelActiveWork()
        return Result.success()
    }
}

@HiltWorker
class ForgotToEndWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val observeHomeState: ObserveHomeState,
    private val notifications: PartyNotificationController,
    private val effects: PartyEffects,
    private val clock: Clock,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val active = observeHomeState().first() as? AppState.InParty ?: return Result.success()
        val last = active.data.logs.maxOfOrNull { it.drankAt } ?: return Result.success()
        if (shouldRemindToEnd(last, clock.instant())) notifications.showReminder(last) else effects.scheduleForgotToEnd()
        return Result.success()
    }
}
