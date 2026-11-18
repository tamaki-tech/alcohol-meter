package tech.tamaki.alcoholmeter.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tech.tamaki.alcoholmeter.domain.model.*
import tech.tamaki.alcoholmeter.domain.repository.*
import tech.tamaki.alcoholmeter.domain.usecase.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    @Test fun editingDrinkSavesSelectedPresetAndSchedulesSync() = runTest(dispatcher) {
        val at = Instant.parse("2026-01-01T12:00:00Z")
        val drink = DrinkLog("drink", "session", at, ZoneOffset.UTC, "beer", 1, 350.0, 5.0, 14.0, 0, "hc")
        val session = SessionWithLogs(PartySession("session", at.minusSeconds(3600), ZoneOffset.UTC, at.plusSeconds(3600), ZoneOffset.UTC, null, HcSyncStatus.FAILED), listOf(drink))
        val party = object : PartyRepository {
            var updated: Triple<String, Long?, Instant?>? = null
            override fun observeAppState() = flowOf<AppState>(AppState.BeforeParty)
            override fun observeSession(id: String) = flowOf<SessionWithLogs?>(session)
            override fun observeHistory() = flowOf(listOf(session))
            override fun observeAllLogs() = flowOf(listOf(drink))
            override suspend fun startParty() = Unit
            override suspend fun addDrink(presetId: Long): DrinkLog = error("unused")
            override suspend fun deleteDrink(logId: String) = Unit
            override suspend fun updateDrink(logId: String, presetId: Long?, drankAt: Instant?) { updated = Triple(logId, presetId, drankAt) }
            override suspend fun endParty(endAt: Instant) = Unit
            override suspend fun closeSummary() = Unit
            override suspend fun deleteSession(id: String) = Unit
        }
        val presets = object : PresetRepository {
            override fun observePresets() = MutableStateFlow(listOf(DrinkPreset(1, "beer", "beer", 1, 350.0, 5.0, "beer", 0, 0, true), DrinkPreset(2, "wine", "wine", 2, 120.0, 12.0, "wine", 1, 1, false)))
            override suspend fun upsert(p: DrinkPreset) = Unit
            override suspend fun reorder(idsInOrder: List<Long>) = Unit
            override suspend fun delete(id: Long) = Unit
        }
        val settings = object : SettingsRepository {
            override val profile = flowOf<UserProfile?>(UserProfile(60.0, Sex.MALE))
            override val onboardingCompleted = flowOf(true)
            override val recordStartDate = flowOf<LocalDate?>(LocalDate.parse("2026-01-01"))
            override suspend fun saveProfile(p: UserProfile) = Unit
            override suspend fun completeOnboarding() = Unit
        }
        val health = object : HealthConnectRepository {
            var requested: String? = null
            var canWrite = false
            override suspend fun hasWritePermission() = canWrite
            override val requiredPermissions = emptySet<String>()
            override suspend fun availability() = HcAvailability.AVAILABLE
            override suspend fun hasPermissions() = true
            override suspend fun latestWeight(): Pair<Double, Instant>? = null
            override fun permissionContract(): ActivityResultContract<Set<String>, Set<String>> = object : ActivityResultContract<Set<String>, Set<String>>() {
                override fun createIntent(context: Context, input: Set<String>) = Intent()
                override fun parseResult(resultCode: Int, intent: Intent?) = emptySet<String>()
            }
            override fun enqueueSync(sessionId: String) { requested = sessionId }
            override fun enqueueRecovery(sessionId: String) = Unit
        }
        val effects = object : PartyEffects {
            override suspend fun refreshNotification() = Unit
            override suspend fun scheduleActiveWork() = Unit
            override fun cancelActiveWork() = Unit
            override suspend fun scheduleForgotToEnd() = Unit
            override fun postponeReminder() = Unit
        }
        val permissions = HealthPermissions(health)
        val vm = HistoryDetailViewModel(SavedStateHandle(mapOf("sessionId" to "session")), ObservePartyData(party), ObserveSettings(settings), ObservePresets(presets), UpdateDrink(party, health, effects), DeleteDrink(party, health, effects), DeleteSession(party, health, effects), RequestHealthSync(health), permissions, Clock.fixed(at, ZoneOffset.UTC), ApplicationProvider.getApplicationContext())
        vm.uiState.test {
            awaitItem()
            assertEquals(tech.tamaki.alcoholmeter.ui.screens.summary.HealthSyncUiStatus.NotLinked, awaitItem()!!.summary.syncStatus)
            health.canWrite = true
            permissions.refresh(); runCurrent()
            assertEquals(tech.tamaki.alcoholmeter.ui.screens.summary.HealthSyncUiStatus.Failed, expectMostRecentItem()!!.summary.syncStatus)
            vm.edit("drink"); runCurrent()
            val edit = expectMostRecentItem()!!.edit!!
            vm.save(edit.copy(selectedPresetId = 2)); runCurrent()
            assertEquals(Triple("drink", 2L, at), party.updated)
            assertEquals("session", health.requested)
            cancelAndIgnoreRemainingEvents()
        }
    }
    @Test fun summaryIncludesAlcoholFromEarlierSession() {
        val at = Instant.parse("2026-01-01T12:00:00Z")
        val drink = DrinkLog("current", "session", at, ZoneOffset.UTC, "beer", 1, 350.0, 5.0, 14.0, 0, null)
        val prior = drink.copy(id = "prior", sessionId = "earlier", drankAt = at.minusSeconds(600))
        val session = SessionWithLogs(PartySession("session", at, ZoneOffset.UTC, at.plusSeconds(3600), ZoneOffset.UTC, null, HcSyncStatus.SYNCED), listOf(drink))
        val result = summaryState(session, UserProfile(60.0, Sex.MALE), Clock.fixed(at, ZoneOffset.UTC), ApplicationProvider.getApplicationContext(), listOf(prior, drink))
        assertEquals("0.066%", result.maximumBac)
    }
    @Test fun durationUsesLocalizedResources() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("42 分", durationText(context, 42 * 60L))
        assertEquals("2 時間 42 分", durationText(context, 2 * 3600L + 42 * 60L))
    }
}
