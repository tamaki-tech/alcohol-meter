package tech.tamaki.alcoholmeter.domain.usecase

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.tamaki.alcoholmeter.domain.model.AppState
import tech.tamaki.alcoholmeter.domain.model.DrinkLog
import tech.tamaki.alcoholmeter.domain.model.HcAvailability
import tech.tamaki.alcoholmeter.domain.model.HcSyncStatus
import tech.tamaki.alcoholmeter.domain.model.PartySession
import tech.tamaki.alcoholmeter.domain.model.SessionWithLogs
import tech.tamaki.alcoholmeter.domain.repository.HealthConnectRepository
import tech.tamaki.alcoholmeter.domain.repository.PartyRepository

class PartyUseCasesTest {
    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val log = DrinkLog("log", "s", now, ZoneOffset.UTC, "beer", 1, 350.0, 5.0, 14.0, 0, null)
    private val session = SessionWithLogs(PartySession("s", now, ZoneOffset.UTC, null, null, null, HcSyncStatus.NOT_SYNCED), listOf(log))
    private inner class FakeParty : PartyRepository {
        var state: AppState = AppState.InParty(session)
        var ended = false
        override fun observeAppState(): Flow<AppState> = flowOf(state)
        override fun observeSession(id: String) = flowOf<SessionWithLogs?>(session)
        override fun observeHistory() = flowOf(emptyList<SessionWithLogs>())
        override fun observeAllLogs() = flowOf(listOf(log))
        override suspend fun startParty() {}
        override suspend fun addDrink(presetId: Long) = log
        override suspend fun deleteDrink(logId: String) {}
        override suspend fun updateDrink(logId: String, presetId: Long?, drankAt: Instant?) {}
        override suspend fun endParty(endAt: Instant) { ended = true }
        override suspend fun closeSummary() {}
        override suspend fun deleteSession(id: String) {}
    }
    private class FakeEffects : PartyEffects {
        var refresh = 0
        var scheduled = 0
        var cancelled = 0
        override suspend fun refreshNotification() { refresh++ }
        override suspend fun scheduleActiveWork() { scheduled++ }
        override fun cancelActiveWork() { cancelled++ }
        override suspend fun scheduleForgotToEnd() { scheduled++ }
        override fun postponeReminder() {}
    }
    private class FakeHealth : HealthConnectRepository {
        var enqueued: String? = null
        var writeAllowed = true
        override suspend fun hasWritePermission() = writeAllowed
        val recoveries = mutableListOf<String>()
        override val requiredPermissions = emptySet<String>()
        override suspend fun availability() = HcAvailability.AVAILABLE
        override suspend fun hasPermissions() = true
        override suspend fun latestWeight(): Pair<Double, Instant>? = null
        override fun permissionContract(): ActivityResultContract<Set<String>, Set<String>> = object : ActivityResultContract<Set<String>, Set<String>>() {
            override fun createIntent(context: Context, input: Set<String>) = Intent()
            override fun parseResult(resultCode: Int, intent: Intent?) = emptySet<String>()
        }
        override fun enqueueSync(sessionId: String) { enqueued = sessionId }
        override fun enqueueRecovery(sessionId: String) { recoveries.add(sessionId) }
    }
    @Test fun recordingRefreshesNotificationAndReschedulesReminder() = runBlocking {
        val effects = FakeEffects()
        assertEquals(log, RecordDrink(FakeParty(), effects)(1))
        assertEquals(1, effects.refresh)
        assertEquals(1, effects.scheduled)
    }
    @Test fun endingCancelsActiveWorkAndEnqueuesSync() = runBlocking {
        val party = FakeParty()
        val effects = FakeEffects()
        val health = FakeHealth()
        EndParty(party, health, effects)(now)
        assertTrue(party.ended)
        assertEquals(1, effects.cancelled)
        assertEquals(1, effects.refresh)
        assertEquals("s", health.enqueued)
    }
    @Test fun startingSchedulesUpdatesAndRefreshesNotification() = runBlocking {
        val effects = FakeEffects()
        StartParty(FakeParty(), effects)()
        assertEquals(1, effects.refresh)
        assertEquals(1, effects.scheduled)
    }
    @Test fun undoRefreshesNotificationAndReschedulesReminder() = runBlocking {
        val effects = FakeEffects()
        val health = FakeHealth()
        UndoDrink(DeleteDrink(FakeParty(), health, effects))(log.id)
        assertEquals(1, effects.refresh)
        assertEquals(1, effects.scheduled)
        assertEquals(null, health.enqueued)
    }
    @Test fun deletingSessionSchedulesHealthSync() = runBlocking {
        val effects = FakeEffects()
        val health = FakeHealth()
        DeleteSession(FakeParty(), health, effects)("s")
        assertEquals("s", health.enqueued)
        assertEquals(1, effects.refresh)
    }
    @Test fun reconcileSchedulesPendingSessionsAndRestoresActiveParty() = runBlocking {
        val pending = object : PendingHealthSync {
            override suspend fun pendingSessionIds() = listOf("s1", "s2")
            override suspend fun hasPendingDeletions() = true
        }
        val health = FakeHealth()
        val effects = FakeEffects()
        ReconcileBackgroundState(pending, health, ObserveHomeState(FakeParty()), effects)()
        assertEquals(listOf("s1", "s2"), health.recoveries)
        assertEquals(1, effects.refresh)
        assertEquals(1, effects.scheduled)
    }
    @Test fun reconcileSchedulesDeletionQueueWhenNoSessionRemains() = runBlocking {
        val pending = object : PendingHealthSync {
            override suspend fun pendingSessionIds() = emptyList<String>()
            override suspend fun hasPendingDeletions() = true
        }
        val health = FakeHealth()
        val party = FakeParty().apply { state = AppState.BeforeParty }
        val effects = FakeEffects()
        ReconcileBackgroundState(pending, health, ObserveHomeState(party), effects)()
        assertEquals(listOf(ReconcileBackgroundState.DELETION_QUEUE_ID), health.recoveries)
        assertEquals(0, effects.refresh)
        assertEquals(0, effects.scheduled)
    }
    @Test fun reconcileDoesNothingWhenNoWorkAndNoActiveParty() = runBlocking {
        val pending = object : PendingHealthSync {
            override suspend fun pendingSessionIds() = emptyList<String>()
            override suspend fun hasPendingDeletions() = false
        }
        val health = FakeHealth()
        val party = FakeParty().apply { state = AppState.BeforeParty }
        val effects = FakeEffects()
        ReconcileBackgroundState(pending, health, ObserveHomeState(party), effects)()
        assertTrue(health.recoveries.isEmpty())
        assertEquals(0, effects.scheduled)
    }
    @Test fun grantingWritePermissionAllowsPendingSessionToBeReconciled() = runBlocking {
        val health = FakeHealth().apply { writeAllowed = false }
        val permissions = HealthPermissions(health)
        val pending = object : PendingHealthSync {
            override suspend fun pendingSessionIds() = listOf("unsent")
            override suspend fun hasPendingDeletions() = false
        }
        permissions.refresh()
        assertEquals(false, permissions.writeGranted.value)
        health.writeAllowed = true
        permissions.refresh()
        ReconcileBackgroundState(pending, health, ObserveHomeState(FakeParty()), FakeEffects())()
        assertEquals(true, permissions.writeGranted.value)
        assertEquals(listOf("unsent"), health.recoveries)
    }
}
