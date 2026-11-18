package tech.tamaki.alcoholmeter.data.health

import androidx.room.withTransaction
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.tamaki.alcoholmeter.data.local.AlcoholDatabase
import tech.tamaki.alcoholmeter.data.local.PendingDeletionEntity
import tech.tamaki.alcoholmeter.domain.model.HcSyncStatus
import tech.tamaki.alcoholmeter.domain.repository.PartyRepository
import tech.tamaki.alcoholmeter.domain.usecase.HealthSyncOperation

class RoomHealthSyncOperation @Inject constructor(
    private val db: AlcoholDatabase,
    private val party: PartyRepository,
    private val health: HealthSyncGateway,
) : HealthSyncOperation {
    override suspend fun sync(sessionId: String): Boolean = syncMutex.withLock { try {
        if (!health.canWriteAlcohol()) {
            db.withTransaction { db.partyDao().session(sessionId)?.let { db.partyDao().saveSession(it.copy(hcSyncStatus = HcSyncStatus.NOT_SYNCED)) } }
            return@withLock true
        }
        val deletions = db.partyDao().pendingDeletions()
        health.delete(deletions.map { it.clientRecordId })
        db.withTransaction { deletions.forEach { db.partyDao().clearDeletion(it.clientRecordId) } }
        val session = party.observeSession(sessionId).first()
        if (session == null) true else {
            val result = health.insert(session.logs)
            db.withTransaction {
                var unchanged = true
                session.logs.forEach { log ->
                    val current = db.partyDao().log(log.id)
                    if (current == null) {
                        db.partyDao().queueDeletion(PendingDeletionEntity(log.id))
                        unchanged = false
                    } else {
                        if (current.hcVersion != log.hcVersion) unchanged = false
                        db.partyDao().saveLog(current.copy(hcRecordId = result[log.id] ?: current.hcRecordId))
                    }
                }
                val ids = db.partyDao().logsForSession(sessionId).map { it.id }.toSet()
                if (ids != session.logs.map { it.id }.toSet()) unchanged = false
                db.partyDao().session(sessionId)?.let {
                    db.partyDao().saveSession(it.copy(hcSyncStatus = if (unchanged) HcSyncStatus.SYNCED else HcSyncStatus.NOT_SYNCED))
                }
                unchanged
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        val permitted = health.canWriteAlcohol()
        db.withTransaction {
            db.partyDao().session(sessionId)?.let {
                db.partyDao().saveSession(it.copy(hcSyncStatus = if (permitted) HcSyncStatus.FAILED else HcSyncStatus.NOT_SYNCED))
            }
        }
        !permitted
    } }
    companion object { private val syncMutex = Mutex() }
}
