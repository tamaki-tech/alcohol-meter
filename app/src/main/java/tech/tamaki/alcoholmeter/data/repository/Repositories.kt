package tech.tamaki.alcoholmeter.data.repository

import androidx.room.withTransaction
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import tech.tamaki.alcoholmeter.data.local.AlcoholDatabase
import tech.tamaki.alcoholmeter.data.local.LogEntity
import tech.tamaki.alcoholmeter.data.local.PendingDeletionEntity
import tech.tamaki.alcoholmeter.data.local.PresetEntity
import tech.tamaki.alcoholmeter.data.local.SessionAndLogs
import tech.tamaki.alcoholmeter.data.local.SessionEntity
import tech.tamaki.alcoholmeter.data.local.SettingsStore
import tech.tamaki.alcoholmeter.domain.calc.PureAlcohol
import tech.tamaki.alcoholmeter.domain.model.AppState
import tech.tamaki.alcoholmeter.domain.model.DrinkLog
import tech.tamaki.alcoholmeter.domain.model.DrinkPreset
import tech.tamaki.alcoholmeter.domain.model.HcSyncStatus
import tech.tamaki.alcoholmeter.domain.model.PartySession
import tech.tamaki.alcoholmeter.domain.model.SessionWithLogs
import tech.tamaki.alcoholmeter.domain.repository.PartyRepository
import tech.tamaki.alcoholmeter.domain.repository.PresetRepository

private fun LogEntity.model() = DrinkLog(id, sessionId, drankAt, offset, name, beverageType, volumeMl, abvPercent, pureAlcoholG, colorIndex, hcRecordId, hcVersion, presetId)
private fun PresetEntity.model() = DrinkPreset(id, name, shortName, beverageType, volumeMl, abvPercent, icon, colorIndex, sortOrder, showInNotification)
private fun DrinkPreset.entity() = PresetEntity(id, name, shortName, beverageType, volumeMl, abvPercent, icon, colorIndex, sortOrder, showInNotification)
private fun SessionAndLogs.model() = SessionWithLogs(
    session.let { PartySession(it.id, it.startedAt, it.startOffset, it.endedAt, it.endOffset, it.title, it.hcSyncStatus) },
    logs.sortedBy { it.drankAt }.map { it.model() },
)

class RoomPartyRepository(private val db: AlcoholDatabase, private val settings: SettingsStore, private val clock: Clock) : PartyRepository {
    private val dao = db.partyDao()
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAppState(): Flow<AppState> = combine(dao.observeActive(), settings.lastFinishedSessionId) { active, finished -> active to finished }
        .flatMapLatest { (active, finished) ->
            when {
                active != null -> flowOf(AppState.InParty(active.model()))
                finished != null -> dao.observeSession(finished).map { if (it == null) AppState.BeforeParty else AppState.Finished(it.model()) }
                else -> flowOf(AppState.BeforeParty)
            }
        }
    override fun observeSession(id: String) = dao.observeSession(id).map { it?.model() }
    override fun observeHistory() = dao.observeHistory().map { rows -> rows.map { it.model() }.sortedByDescending { it.session.startedAt } }
    override fun observeAllLogs() = dao.observeLogs().map { rows -> rows.map { it.model() }.sortedBy { it.drankAt } }
    override suspend fun startParty() {
        db.withTransaction {
            check(dao.active() == null) { "Party already active" }
            val now = clock.instant()
            dao.insertSession(SessionEntity(UUID.randomUUID().toString(), now, clock.zone.rules.getOffset(now), null, null, null, HcSyncStatus.NOT_SYNCED))
        }
    }
    override suspend fun addDrink(presetId: Long): DrinkLog = db.withTransaction {
        val session = checkNotNull(dao.active()) { "No active party" }
        val preset = requireNotNull(db.presetDao().get(presetId)) { "Preset not found" }
        val now = clock.instant()
        val log = LogEntity(UUID.randomUUID().toString(), session.id, now, clock.zone.rules.getOffset(now), preset.name, preset.beverageType, preset.volumeMl, preset.abvPercent, PureAlcohol.grams(preset.volumeMl, preset.abvPercent), preset.colorIndex, null, presetId = preset.id)
        dao.saveLog(log)
        log.model()
    }
    override suspend fun deleteDrink(logId: String) {
        db.withTransaction {
            val log = dao.log(logId) ?: return@withTransaction
            val session = dao.session(log.sessionId)
            if (log.hcRecordId != null || session?.endedAt != null) dao.queueDeletion(PendingDeletionEntity(log.id))
            session?.let { dao.saveSession(it.copy(hcSyncStatus = HcSyncStatus.NOT_SYNCED)) }
            dao.deleteLog(logId)
        }
    }
    override suspend fun updateDrink(logId: String, presetId: Long?, drankAt: Instant?) {
        db.withTransaction {
            val log = requireNotNull(dao.log(logId))
            val preset = presetId?.let { requireNotNull(db.presetDao().get(it)) }
            val time = drankAt ?: log.drankAt
            dao.session(log.sessionId)?.let { dao.saveSession(it.copy(hcSyncStatus = HcSyncStatus.NOT_SYNCED)) }
            dao.saveLog(log.copy(presetId = preset?.id ?: log.presetId, hcVersion = log.hcVersion + 1, drankAt = time, offset = if (drankAt == null) log.offset else clock.zone.rules.getOffset(time), name = preset?.name ?: log.name, beverageType = preset?.beverageType ?: log.beverageType, volumeMl = preset?.volumeMl ?: log.volumeMl, abvPercent = preset?.abvPercent ?: log.abvPercent, pureAlcoholG = preset?.let { PureAlcohol.grams(it.volumeMl, it.abvPercent) } ?: log.pureAlcoholG, colorIndex = preset?.colorIndex ?: log.colorIndex))
        }
    }
    override suspend fun endParty(endAt: Instant) {
        db.withTransaction {
            val active = checkNotNull(dao.active()) { "No active party" }
            require(!endAt.isBefore(active.startedAt))
            settings.setFinished(active.id)
            dao.saveSession(active.copy(endedAt = endAt, endOffset = clock.zone.rules.getOffset(endAt)))
        }
    }
    override suspend fun closeSummary() = settings.setFinished(null)
    override suspend fun deleteSession(id: String) {
        db.withTransaction {
            val ended = dao.session(id)?.endedAt != null
            dao.logsForSession(id).filter { it.hcRecordId != null || ended }.forEach { dao.queueDeletion(PendingDeletionEntity(it.id)) }
            dao.deleteSession(id)
        }
    }
}

class RoomPresetRepository(private val db: AlcoholDatabase) : PresetRepository {
    private val dao = db.presetDao()
    override fun observePresets() = dao.observe().map { rows -> rows.map { it.model() } }
    override suspend fun upsert(p: DrinkPreset) {
        db.withTransaction {
            val others = dao.all().count { it.id != p.id && it.showInNotification }
            require(!p.showInNotification || others < 3) { "At most three notification presets" }
            dao.upsert(p.entity())
        }
    }
    override suspend fun reorder(idsInOrder: List<Long>) {
        db.withTransaction {
            val presets = dao.all()
            require(idsInOrder.size == presets.size && idsInOrder.toSet() == presets.map { it.id }.toSet())
            val byId = presets.associateBy { it.id }
            idsInOrder.forEachIndexed { index, id -> dao.upsert(byId.getValue(id).copy(sortOrder = index)) }
        }
    }
    override suspend fun delete(id: Long) = dao.delete(id)
}
