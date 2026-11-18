package tech.tamaki.alcoholmeter.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import tech.tamaki.alcoholmeter.domain.model.*
import tech.tamaki.alcoholmeter.domain.repository.*
import tech.tamaki.alcoholmeter.domain.usecase.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val instant = Instant.parse("2026-01-01T12:00:00Z")
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    @Test fun homeShowsPartyAndUndoRemovesLastDrink() = runTest(dispatcher) {
        val party = object : PartyRepository {
            val state = MutableStateFlow<AppState>(AppState.BeforeParty)
            val logs = MutableStateFlow(emptyList<DrinkLog>())
            override fun observeAppState() = state
            override fun observeSession(id: String) = flowOf((state.value as? AppState.InParty)?.data)
            override fun observeHistory() = flowOf(emptyList<SessionWithLogs>())
            override fun observeAllLogs() = logs
            override suspend fun startParty() {
                val session = PartySession("session", instant, ZoneOffset.UTC, null, null, null, HcSyncStatus.NOT_SYNCED)
                state.value = AppState.InParty(SessionWithLogs(session, emptyList()))
            }
            override suspend fun addDrink(presetId: Long): DrinkLog {
                val log = DrinkLog("drink", "session", instant, ZoneOffset.UTC, "beer", 1, 350.0, 5.0, 14.0, 0, null)
                logs.value = listOf(log)
                val current = state.value as AppState.InParty
                state.value = AppState.InParty(current.data.copy(logs = logs.value))
                return log
            }
            override suspend fun deleteDrink(logId: String) {
                logs.value = emptyList()
                val current = state.value as AppState.InParty
                state.value = AppState.InParty(current.data.copy(logs = logs.value))
            }
            override suspend fun updateDrink(logId: String, presetId: Long?, drankAt: Instant?) = Unit
            override suspend fun endParty(endAt: Instant) = Unit
            override suspend fun closeSummary() = Unit
            override suspend fun deleteSession(id: String) = Unit
        }
        val settings = object : SettingsRepository {
            override val profile = flowOf<UserProfile?>(UserProfile(60.0, Sex.MALE))
            override val onboardingCompleted = flowOf(true)
            override val recordStartDate = flowOf<LocalDate?>(LocalDate.parse("2026-01-01"))
            override suspend fun saveProfile(p: UserProfile) = Unit
            override suspend fun completeOnboarding() = Unit
        }
        val presets = object : PresetRepository {
            override fun observePresets() = flowOf(emptyList<DrinkPreset>())
            override suspend fun upsert(p: DrinkPreset) = Unit
            override suspend fun reorder(idsInOrder: List<Long>) = Unit
            override suspend fun delete(id: Long) = Unit
        }
        val effects = object : PartyEffects {
            override suspend fun refreshNotification() = Unit
            override suspend fun scheduleActiveWork() = Unit
            override fun cancelActiveWork() = Unit
            override suspend fun scheduleForgotToEnd() = Unit
            override fun postponeReminder() = Unit
        }
        val health = object : HealthConnectRepository {
            override val requiredPermissions = emptySet<String>()
            override suspend fun availability() = HcAvailability.AVAILABLE
            override suspend fun hasPermissions() = true
            override suspend fun latestWeight(): Pair<Double, Instant>? = null
            override fun permissionContract(): ActivityResultContract<Set<String>, Set<String>> = object : ActivityResultContract<Set<String>, Set<String>>() {
                override fun createIntent(context: Context, input: Set<String>) = Intent()
                override fun parseResult(resultCode: Int, intent: Intent?) = emptySet<String>()
            }
            override fun enqueueSync(sessionId: String) = Unit
            override fun enqueueRecovery(sessionId: String) = Unit
        }
        val vm = HomeViewModel(Clock.fixed(instant, ZoneOffset.UTC), ApplicationProvider.getApplicationContext(), ObserveHomeState(party), ObservePartyData(party), ObservePresets(presets), ObserveSettings(settings), StartParty(party, effects), RecordDrink(party, effects), UndoDrink(DeleteDrink(party, health, effects)), EndParty(party, health, effects), CloseSummary(party))
        vm.uiState.test {
            awaitItem()
            val before = awaitItem() as HomeState.Before
            assertEquals(null, before.data.recentParty)
            vm.start(); runCurrent()
            assertTrue(expectMostRecentItem() is HomeState.InParty)
            vm.record(1); runCurrent()
            assertEquals(1, (expectMostRecentItem() as HomeState.InParty).data.cups)
            vm.undoLast(); runCurrent()
            assertEquals(0, (expectMostRecentItem() as HomeState.InParty).data.cups)
            vm.record(1); runCurrent()
            assertTrue((expectMostRecentItem() as HomeState.InParty).data.undoMessage != null)
            advanceTimeBy(5_000); runCurrent()
            assertEquals(null, (expectMostRecentItem() as HomeState.InParty).data.undoMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }
    @Test fun presetIdentitySurvivesRename() {
        val preset = DrinkPreset(42, "renamed", "renamed", 1, 350.0, 5.0, "beer", 0, 0, false)
        val log = DrinkLog("id", "s", instant, ZoneOffset.UTC, "original", 1, 350.0, 5.0, 14.0, 0, null, presetId = 42)
        assertTrue(matchesPreset(log, preset))
        assertTrue(!matchesPreset(log.copy(presetId = 43), preset))
    }
}
