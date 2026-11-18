package tech.tamaki.alcoholmeter.background

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import tech.tamaki.alcoholmeter.domain.model.AppState
import tech.tamaki.alcoholmeter.domain.repository.PartyRepository
import tech.tamaki.alcoholmeter.domain.usecase.PartyEffects
import tech.tamaki.alcoholmeter.domain.usecase.forgotToEndAt

@Singleton
class PartyEffectsImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val notifications: PartyNotificationController,
    private val party: PartyRepository,
    private val clock: Clock,
) : PartyEffects {
    private val work get() = WorkManager.getInstance(context)
    override suspend fun refreshNotification() = notifications.refresh()
    override suspend fun scheduleActiveWork() {
        work.enqueueUniquePeriodicWork("bac-update", ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<BacUpdateWorker>(15, TimeUnit.MINUTES).build())
        scheduleForgotToEnd()
    }
    override fun cancelActiveWork() {
        work.cancelUniqueWork("bac-update")
        work.cancelUniqueWork("forgot-to-end")
        notifications.cancelReminder()
    }
    override fun postponeReminder() {
        notifications.cancelReminder()
        val request = OneTimeWorkRequestBuilder<ForgotToEndWorker>().setInitialDelay(6, TimeUnit.HOURS).build()
        work.enqueueUniqueWork("forgot-to-end", ExistingWorkPolicy.REPLACE, request)
    }
    override suspend fun scheduleForgotToEnd() {
            val active = party.observeAppState().first() as? AppState.InParty
            val last = active?.data?.logs?.maxOfOrNull { it.drankAt }
            val due = forgotToEndAt(last)
            if (due == null) { work.cancelUniqueWork("forgot-to-end"); notifications.cancelReminder(); return }
            val delay = Duration.between(clock.instant(), due).toMillis().coerceAtLeast(0)
            val request = OneTimeWorkRequestBuilder<ForgotToEndWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
            work.enqueueUniqueWork("forgot-to-end", ExistingWorkPolicy.REPLACE, request)
    }
}
