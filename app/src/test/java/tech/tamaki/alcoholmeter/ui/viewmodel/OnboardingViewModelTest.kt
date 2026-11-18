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
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.tamaki.alcoholmeter.domain.model.*
import tech.tamaki.alcoholmeter.domain.repository.*
import tech.tamaki.alcoholmeter.domain.usecase.*
import tech.tamaki.alcoholmeter.ui.screens.onboarding.OnboardingSex

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @BeforeEach fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterEach fun cleanup() { Dispatchers.resetMain() }
    @Test fun profileMustHaveWeightAndSexBeforePermissionsStep() = runTest(dispatcher) {
        val settings = object : SettingsRepository {
            override val profile = MutableStateFlow<UserProfile?>(null)
            override val onboardingCompleted = MutableStateFlow(false)
            override val recordStartDate = flowOf<LocalDate?>(null)
            override suspend fun saveProfile(p: UserProfile) { profile.value = p }
            override suspend fun completeOnboarding() { onboardingCompleted.value = true }
        }
        val health = object : HealthConnectRepository {
            override val requiredPermissions = emptySet<String>()
            override suspend fun availability() = HcAvailability.AVAILABLE
            override suspend fun hasPermissions() = false
            override suspend fun latestWeight(): Pair<Double, Instant>? = null
            override fun permissionContract(): ActivityResultContract<Set<String>, Set<String>> = object : ActivityResultContract<Set<String>, Set<String>>() {
                override fun createIntent(context: Context, input: Set<String>) = Intent()
                override fun parseResult(resultCode: Int, intent: Intent?) = emptySet<String>()
            }
            override fun enqueueSync(sessionId: String) = Unit
            override fun enqueueRecovery(sessionId: String) = Unit
        }
        val party = object : PartyRepository {
            override fun observeAppState() = flowOf<AppState>(AppState.BeforeParty)
            override fun observeSession(id: String) = flowOf<SessionWithLogs?>(null)
            override fun observeHistory() = flowOf(emptyList<SessionWithLogs>())
            override fun observeAllLogs() = flowOf(emptyList<DrinkLog>())
            override suspend fun startParty() = Unit
            override suspend fun addDrink(presetId: Long): DrinkLog = error("unused")
            override suspend fun deleteDrink(logId: String) = Unit
            override suspend fun updateDrink(logId: String, presetId: Long?, drankAt: Instant?) = Unit
            override suspend fun endParty(endAt: Instant) = Unit
            override suspend fun closeSummary() = Unit
            override suspend fun deleteSession(id: String) = Unit
        }
        val effects = object : PartyEffects {
            override suspend fun refreshNotification() = Unit
            override suspend fun scheduleActiveWork() = Unit
            override fun cancelActiveWork() = Unit
            override suspend fun scheduleForgotToEnd() = Unit
            override fun postponeReminder() = Unit
        }
        val pending = object : PendingHealthSync {
            override suspend fun pendingSessionIds() = emptyList<String>()
            override suspend fun hasPendingDeletions() = false
        }
        val reconcile = ReconcileBackgroundState(pending, health, ObserveHomeState(party), effects)
        val vm = OnboardingViewModel(CheckHealthAvailability(health), HealthPermissions(health), ObserveSettings(settings), SaveOnboardingProfile(settings), CompleteOnboarding(settings), reconcile, Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC))
        vm.launchState.test {
            assertEquals(LaunchState.Loading, awaitItem())
            assertEquals(LaunchState.Onboarding, awaitItem())
            vm.setWeight("60"); vm.next(); runCurrent()
            assertEquals(1, vm.uiState.value.step)
            vm.setSex(OnboardingSex.Female); vm.next(); runCurrent()
            assertEquals(2, vm.uiState.value.step)
            assertEquals(UserProfile(60.0, Sex.FEMALE), settings.profile.value)
            vm.skip(); vm.skip(); runCurrent()
            assertEquals(LaunchState.Ready, awaitItem())
            assertEquals(true, settings.onboardingCompleted.value)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
