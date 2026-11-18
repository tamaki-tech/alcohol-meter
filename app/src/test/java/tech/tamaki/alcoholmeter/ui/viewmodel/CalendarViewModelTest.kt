package tech.tamaki.alcoholmeter.ui.viewmodel

import app.cash.turbine.test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.tamaki.alcoholmeter.domain.model.AppState
import tech.tamaki.alcoholmeter.domain.model.DrinkLog
import tech.tamaki.alcoholmeter.domain.model.SessionWithLogs
import tech.tamaki.alcoholmeter.domain.model.UserProfile
import tech.tamaki.alcoholmeter.domain.repository.PartyRepository
import tech.tamaki.alcoholmeter.domain.repository.SettingsRepository
import tech.tamaki.alcoholmeter.domain.usecase.ObservePartyData
import tech.tamaki.alcoholmeter.domain.usecase.ObserveSettings

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @BeforeEach fun setup() { Dispatchers.setMain(dispatcher) }
    @AfterEach fun cleanup() { Dispatchers.resetMain() }
    @Test fun monthSelectionRejectsFutureAndCountsFinishedDays() = runTest(dispatcher) {
        val logs = MutableStateFlow(emptyList<DrinkLog>())
        val party = object : PartyRepository {
            override fun observeAppState() = flowOf<AppState>(AppState.BeforeParty)
            override fun observeSession(id: String) = flowOf<SessionWithLogs?>(null)
            override fun observeHistory() = flowOf(emptyList<SessionWithLogs>())
            override fun observeAllLogs() = logs
            override suspend fun startParty() = Unit
            override suspend fun addDrink(presetId: Long): DrinkLog = error("unused")
            override suspend fun deleteDrink(logId: String) = Unit
            override suspend fun updateDrink(logId: String, presetId: Long?, drankAt: Instant?) = Unit
            override suspend fun endParty(endAt: Instant) = Unit
            override suspend fun closeSummary() = Unit
            override suspend fun deleteSession(id: String) = Unit
        }
        val settings = object : SettingsRepository {
            override val profile = flowOf<UserProfile?>(null)
            override val onboardingCompleted = flowOf(true)
            override val recordStartDate = flowOf<LocalDate?>(LocalDate.parse("2026-01-01"))
            override suspend fun saveProfile(p: UserProfile) = Unit
            override suspend fun completeOnboarding() = Unit
        }
        val vm = CalendarViewModel(Clock.fixed(Instant.parse("2026-01-04T12:00:00Z"), ZoneOffset.UTC), ObservePartyData(party), ObserveSettings(settings))
        vm.uiState.test {
            awaitItem()
            val state = awaitItem()
            assertEquals(3, state.monthRestDays)
            vm.selectMonth(YearMonth.of(2026, 2))
            assertEquals(YearMonth.of(2026, 1), vm.uiState.value.month)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
