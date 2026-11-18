package tech.tamaki.alcoholmeter.domain.usecase

import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import tech.tamaki.alcoholmeter.domain.model.AppState
import tech.tamaki.alcoholmeter.domain.repository.HealthConnectRepository
import tech.tamaki.alcoholmeter.domain.repository.PartyRepository

interface PartyEffects {
    suspend fun refreshNotification()
    suspend fun scheduleActiveWork()
    fun cancelActiveWork()
    suspend fun scheduleForgotToEnd()
    fun postponeReminder()
}

class StartParty @Inject constructor(private val party: PartyRepository, private val effects: PartyEffects) {
    suspend operator fun invoke() { party.startParty(); effects.refreshNotification(); effects.scheduleActiveWork() }
}
class RecordDrink @Inject constructor(private val party: PartyRepository, private val effects: PartyEffects) {
    suspend operator fun invoke(presetId: Long) = party.addDrink(presetId).also { effects.refreshNotification(); effects.scheduleForgotToEnd() }
}
class UndoDrink @Inject constructor(private val delete: DeleteDrink) {
    suspend operator fun invoke(logId: String) = delete(logId)
}
class EndParty @Inject constructor(private val party: PartyRepository, private val health: HealthConnectRepository, private val effects: PartyEffects) {
    suspend operator fun invoke(endAt: Instant) {
        val active = party.observeAppState().first() as? AppState.InParty ?: error("No active party")
        party.endParty(endAt)
        effects.cancelActiveWork()
        effects.refreshNotification()
        health.enqueueSync(active.data.session.id)
    }
}
class CloseSummary @Inject constructor(private val party: PartyRepository) {
    suspend operator fun invoke() = party.closeSummary()
}
class UpdateDrink @Inject constructor(private val party: PartyRepository, private val health: HealthConnectRepository, private val effects: PartyEffects) {
    suspend operator fun invoke(logId: String, presetId: Long?, drankAt: Instant?) {
        val log = party.observeAllLogs().first().first { it.id == logId }
        party.updateDrink(logId, presetId, drankAt)
        val session = party.observeSession(log.sessionId).first()
        if (session?.session?.endedAt != null) health.enqueueSync(log.sessionId) else { effects.refreshNotification(); effects.scheduleForgotToEnd() }
    }
}
class DeleteDrink @Inject constructor(private val party: PartyRepository, private val health: HealthConnectRepository, private val effects: PartyEffects) {
    suspend operator fun invoke(logId: String) {
        val log = party.observeAllLogs().first().first { it.id == logId }
        val session = party.observeSession(log.sessionId).first()
        party.deleteDrink(logId)
        if (session?.session?.endedAt != null) health.enqueueSync(log.sessionId) else { effects.refreshNotification(); effects.scheduleForgotToEnd() }
    }
}
class DeleteSession @Inject constructor(private val party: PartyRepository, private val health: HealthConnectRepository, private val effects: PartyEffects) {
    suspend operator fun invoke(id: String) {
        val wasActive = party.observeSession(id).first()?.let { it.session.endedAt == null } == true
        party.deleteSession(id)
        if (wasActive) effects.cancelActiveWork()
        health.enqueueSync(id)
        effects.refreshNotification()
    }
}
class ObserveHomeState @Inject constructor(private val party: PartyRepository) {
    operator fun invoke() = party.observeAppState()
}
