package tech.tamaki.alcoholmeter.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import tech.tamaki.alcoholmeter.domain.model.AppState
import tech.tamaki.alcoholmeter.domain.repository.HealthConnectRepository

interface PendingHealthSync {
    suspend fun pendingSessionIds(): List<String>
    suspend fun hasPendingDeletions(): Boolean
}

class ReconcileBackgroundState @Inject constructor(
    private val pending: PendingHealthSync,
    private val health: HealthConnectRepository,
    private val observeHome: ObserveHomeState,
    private val effects: PartyEffects,
) {
    suspend operator fun invoke() {
        val ids = pending.pendingSessionIds()
        ids.forEach { health.enqueueRecovery(it) }
        if (pending.hasPendingDeletions() && ids.isEmpty()) health.enqueueRecovery(DELETION_QUEUE_ID)
        if (observeHome().first() is AppState.InParty) {
            effects.refreshNotification()
            effects.scheduleActiveWork()
        }
    }
    companion object { const val DELETION_QUEUE_ID = "__pending_deletions__" }
}
