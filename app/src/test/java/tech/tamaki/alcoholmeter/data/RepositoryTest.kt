package tech.tamaki.alcoholmeter.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import tech.tamaki.alcoholmeter.background.HealthSyncWorker
import tech.tamaki.alcoholmeter.data.health.HealthSyncGateway
import tech.tamaki.alcoholmeter.domain.model.DrinkLog
import tech.tamaki.alcoholmeter.domain.model.HcSyncStatus
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tech.tamaki.alcoholmeter.data.local.AlcoholDatabase
import tech.tamaki.alcoholmeter.data.local.PresetEntity
import tech.tamaki.alcoholmeter.data.local.SettingsStore
import tech.tamaki.alcoholmeter.data.repository.RoomPartyRepository
import tech.tamaki.alcoholmeter.data.repository.RoomPresetRepository
import tech.tamaki.alcoholmeter.domain.model.AppState

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RepositoryTest {
    private lateinit var db: AlcoholDatabase
    private lateinit var party: RoomPartyRepository
    private lateinit var presets: RoomPresetRepository
    private lateinit var scope: CoroutineScope
    private lateinit var file: File
    private val now = Instant.parse("2026-01-01T12:00:00Z")
    @Before fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AlcoholDatabase::class.java).build()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        file = File.createTempFile("settings", ".preferences_pb", context.cacheDir).also { it.delete() }
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        party = RoomPartyRepository(db, SettingsStore(store, Clock.fixed(now, ZoneOffset.UTC)), Clock.fixed(now, ZoneOffset.UTC))
        presets = RoomPresetRepository(db)
        db.presetDao().insertAll((1..4).map { PresetEntity(it.toLong(), "drink$it", "d$it", 1, 350.0, 5.0, "beer", 0, it, it <= 3) })
    }
    @After fun cleanup() { db.close(); scope.cancel(); file.delete() }
    @Test fun stateTransitionsAndCloseSummary() = runBlocking {
        assertEquals(AppState.BeforeParty, party.observeAppState().first())
        party.startParty()
        assertTrue(party.observeAppState().first() is AppState.InParty)
        party.endParty(now)
        assertTrue(party.observeAppState().first { it is AppState.Finished } is AppState.Finished)
        party.closeSummary()
        assertEquals(AppState.BeforeParty, party.observeAppState().first { it is AppState.BeforeParty })
    }
    @Test fun startPartyRejectsSecondActiveSession() = runBlocking {
        party.startParty()
        val failure = runCatching { party.startParty() }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
    }
    @Test fun addDrinkPersistsCalculatedAlcohol() = runBlocking {
        party.startParty()
        val added = party.addDrink(1)
        assertEquals(14.0, added.pureAlcoholG, 1e-9)
        assertEquals(added, party.observeAllLogs().first().single())
    }
    @Test fun reorderPreservesRequestedOrder() = runBlocking {
        presets.reorder(listOf(4, 2, 1, 3))
        assertEquals(listOf(4L, 2L, 1L, 3L), presets.observePresets().first().map { it.id })
    }
    @Test fun notificationPresetsCannotExceedThree() = runBlocking {
        val fourth = presets.observePresets().first().last()
        assertTrue(runCatching { presets.upsert(fourth.copy(showInNotification = true)) }.exceptionOrNull() is IllegalArgumentException)
        assertEquals(3, presets.observePresets().first().count { it.showInNotification })
    }
    @Test fun deletingSessionCascadesToLogs() = runBlocking {
        party.startParty()
        val log = party.addDrink(1)
        party.deleteSession(log.sessionId)
        assertTrue(party.observeAllLogs().first().isEmpty())
    }
    @Test fun deletingSyncedDrinkQueuesClientId() = runBlocking {
        party.startParty()
        val log = party.addDrink(1)
        db.partyDao().saveLog(db.partyDao().log(log.id)!!.copy(hcRecordId = "health-id"))
        party.deleteDrink(log.id)
        assertEquals(listOf(log.id), db.partyDao().pendingDeletions().map { it.clientRecordId })
        assertTrue(party.observeAllLogs().first().isEmpty())
    }
    @Test fun deletingSessionQueuesSyncedLogsBeforeCascade() = runBlocking {
        party.startParty()
        val log = party.addDrink(1)
        db.partyDao().saveLog(db.partyDao().log(log.id)!!.copy(hcRecordId = "health-id"))
        party.deleteSession(log.sessionId)
        assertEquals(listOf(log.id), db.partyDao().pendingDeletions().map { it.clientRecordId })
        assertTrue(party.observeAllLogs().first().isEmpty())
    }
    private fun worker(sessionId: String, gateway: HealthSyncGateway): HealthSyncWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val factory = object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
                HealthSyncWorker(appContext, workerParameters, tech.tamaki.alcoholmeter.domain.usecase.SyncHealth(tech.tamaki.alcoholmeter.data.health.RoomHealthSyncOperation(db, party, gateway)))
        }
        return TestListenableWorkerBuilder<HealthSyncWorker>(context)
            .setInputData(workDataOf("sessionId" to sessionId)).setWorkerFactory(factory).build()
    }
    @Test fun syncSuccessMarksSessionSynced() = runBlocking {
        party.startParty()
        val log = party.addDrink(1)
        party.endParty(now)
        val gateway = object : HealthSyncGateway {
            override suspend fun canWriteAlcohol() = true
            override suspend fun insert(logs: List<DrinkLog>) = logs.associate { it.id to "hc-id" }
            override suspend fun delete(ids: List<String>) {}
        }
        assertEquals(ListenableWorker.Result.success(), worker(log.sessionId, gateway).doWork())
        assertEquals(HcSyncStatus.SYNCED, db.partyDao().session(log.sessionId)!!.hcSyncStatus)
        assertEquals("hc-id", db.partyDao().log(log.id)!!.hcRecordId)
    }
    @Test fun syncFailureMarksFailedAndRetries() = runBlocking {
        party.startParty()
        val log = party.addDrink(1)
        party.endParty(now)
        val gateway = object : HealthSyncGateway {
            override suspend fun canWriteAlcohol() = true
            override suspend fun insert(logs: List<DrinkLog>): Map<String, String> = error("failure")
            override suspend fun delete(ids: List<String>) {}
        }
        assertEquals(ListenableWorker.Result.retry(), worker(log.sessionId, gateway).doWork())
        assertEquals(HcSyncStatus.FAILED, db.partyDao().session(log.sessionId)!!.hcSyncStatus)
    }
    @Test fun editingDrinkIncrementsHealthVersionAndMarksSessionDirty() = runBlocking {
        party.startParty()
        val log = party.addDrink(1)
        party.endParty(now)
        db.partyDao().session(log.sessionId)!!.let { db.partyDao().saveSession(it.copy(hcSyncStatus = HcSyncStatus.SYNCED)) }
        party.updateDrink(log.id, 2, null)
        assertEquals(2L, db.partyDao().log(log.id)!!.hcVersion)
        assertEquals(HcSyncStatus.NOT_SYNCED, db.partyDao().session(log.sessionId)!!.hcSyncStatus)
    }
    @Test fun syncDeletesQueuedRecordsBeforeWritingAndClearsQueue() = runBlocking {
        party.startParty()
        val log = party.addDrink(1)
        party.endParty(now)
        db.partyDao().queueDeletion(tech.tamaki.alcoholmeter.data.local.PendingDeletionEntity("deleted"))
        val calls = mutableListOf<String>()
        val gateway = object : HealthSyncGateway {
            override suspend fun canWriteAlcohol() = true
            override suspend fun insert(logs: List<DrinkLog>): Map<String, String> {
                calls.add("insert")
                return logs.associate { it.id to "hc-id" }
            }
            override suspend fun delete(ids: List<String>) { calls.add("delete:" + ids.single()) }
        }
        assertEquals(ListenableWorker.Result.success(), worker(log.sessionId, gateway).doWork())
        assertEquals(listOf("delete:deleted", "insert"), calls)
        assertTrue(db.partyDao().pendingDeletions().isEmpty())
    }
    @Test fun failedDeletionRetainsQueueForRetry() = runBlocking {
        db.partyDao().queueDeletion(tech.tamaki.alcoholmeter.data.local.PendingDeletionEntity("deleted"))
        val gateway = object : HealthSyncGateway {
            override suspend fun canWriteAlcohol() = true
            override suspend fun insert(logs: List<DrinkLog>) = emptyMap<String, String>()
            override suspend fun delete(ids: List<String>) { error("failure") }
        }
        assertEquals(ListenableWorker.Result.retry(), worker("missing", gateway).doWork())
        assertEquals(listOf("deleted"), db.partyDao().pendingDeletions().map { it.clientRecordId })
    }
    @Test fun deletionDuringInsertQueuesClientIdAndRetries() = runBlocking {
        party.startParty()
        val log = party.addDrink(1)
        party.endParty(now)
        val gateway = object : HealthSyncGateway {
            override suspend fun canWriteAlcohol() = true
            override suspend fun insert(logs: List<DrinkLog>): Map<String, String> {
                party.deleteDrink(log.id)
                return mapOf(log.id to "hc-id")
            }
            override suspend fun delete(ids: List<String>) {}
        }
        assertEquals(ListenableWorker.Result.retry(), worker(log.sessionId, gateway).doWork())
        assertEquals(listOf(log.id), db.partyDao().pendingDeletions().map { it.clientRecordId })
        assertEquals(HcSyncStatus.NOT_SYNCED, db.partyDao().session(log.sessionId)!!.hcSyncStatus)
    }
    @Test fun recoveryFindsOnlyEndedUnsyncedSessions() = runBlocking {
        val active = tech.tamaki.alcoholmeter.data.local.SessionEntity("active", now, ZoneOffset.UTC, null, null, null, HcSyncStatus.NOT_SYNCED)
        val pending = active.copy(id = "pending", endedAt = now, endOffset = ZoneOffset.UTC)
        val failed = pending.copy(id = "failed", hcSyncStatus = HcSyncStatus.FAILED)
        val synced = pending.copy(id = "synced", hcSyncStatus = HcSyncStatus.SYNCED)
        listOf(active, pending, failed, synced).forEach { db.partyDao().insertSession(it) }
        assertEquals(setOf("pending", "failed"), db.partyDao().pendingSessionIds().toSet())
    }
    @Test fun recordedPresetIdIsPersisted() = runBlocking {
        party.startParty()
        val log = party.addDrink(1)
        assertEquals(1L, log.presetId)
        assertEquals(1L, party.observeAllLogs().first().single().presetId)
        party.updateDrink(log.id, 2, null)
        assertEquals(2L, party.observeAllLogs().first().single().presetId)
    }
    @Test fun noWritePermissionCompletesWithoutRetryAndKeepsNotSynced() = runBlocking {
        party.startParty()
        val log = party.addDrink(1)
        party.endParty(now)
        val existing = db.partyDao().session(log.sessionId)!!
        db.partyDao().saveSession(existing.copy(hcSyncStatus = HcSyncStatus.FAILED))
        db.partyDao().queueDeletion(tech.tamaki.alcoholmeter.data.local.PendingDeletionEntity("queued"))
        val gateway = object : HealthSyncGateway {
            var permitted = false
            override suspend fun canWriteAlcohol() = permitted
            override suspend fun insert(logs: List<DrinkLog>): Map<String, String> {
                check(permitted)
                return logs.associate { it.id to "hc-id" }
            }
            override suspend fun delete(ids: List<String>) { check(permitted) }
        }
        assertEquals(ListenableWorker.Result.success(), worker(log.sessionId, gateway).doWork())
        assertEquals(HcSyncStatus.NOT_SYNCED, db.partyDao().session(log.sessionId)!!.hcSyncStatus)
        assertEquals(listOf("queued"), db.partyDao().pendingDeletions().map { it.clientRecordId })
        assertTrue(db.partyDao().pendingSessionIds().contains(log.sessionId))
        gateway.permitted = true
        assertEquals(ListenableWorker.Result.success(), worker(log.sessionId, gateway).doWork())
        assertEquals(HcSyncStatus.SYNCED, db.partyDao().session(log.sessionId)!!.hcSyncStatus)
        assertTrue(db.partyDao().pendingDeletions().isEmpty())
    }
}
