package tech.tamaki.alcoholmeter.ui.viewmodel

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.tamaki.alcoholmeter.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.tamaki.alcoholmeter.domain.calc.AbstinenceCalculator
import tech.tamaki.alcoholmeter.domain.calc.BacCalculator
import tech.tamaki.alcoholmeter.domain.model.AppState
import tech.tamaki.alcoholmeter.domain.model.DrinkLog
import tech.tamaki.alcoholmeter.domain.model.DrinkPreset
import tech.tamaki.alcoholmeter.domain.usecase.CloseSummary
import tech.tamaki.alcoholmeter.domain.usecase.EndParty
import tech.tamaki.alcoholmeter.domain.usecase.ObserveHomeState
import tech.tamaki.alcoholmeter.domain.usecase.ObservePartyData
import tech.tamaki.alcoholmeter.domain.usecase.ObservePresets
import tech.tamaki.alcoholmeter.domain.usecase.ObserveSettings
import tech.tamaki.alcoholmeter.domain.usecase.RecordDrink
import tech.tamaki.alcoholmeter.domain.usecase.StartParty
import tech.tamaki.alcoholmeter.domain.usecase.UndoDrink
import tech.tamaki.alcoholmeter.ui.format.formatBac
import tech.tamaki.alcoholmeter.ui.format.formatDate
import tech.tamaki.alcoholmeter.ui.format.formatDuration
import tech.tamaki.alcoholmeter.ui.format.formatGrams
import tech.tamaki.alcoholmeter.ui.format.formatTime
import tech.tamaki.alcoholmeter.ui.screens.home.HomeBeforeUiState
import tech.tamaki.alcoholmeter.ui.screens.home.HomeInPartyUiState
import tech.tamaki.alcoholmeter.ui.screens.home.HomeRecentDrinkUiState
import tech.tamaki.alcoholmeter.ui.screens.summary.EndPartyUiState
import tech.tamaki.alcoholmeter.ui.screens.summary.EndTimeChoice

internal fun matchesPreset(log: DrinkLog, preset: DrinkPreset): Boolean =
    log.presetId == preset.id || (log.presetId == null && log.name == preset.name &&
        log.beverageType == preset.beverageType && log.volumeMl == preset.volumeMl && log.abvPercent == preset.abvPercent)

sealed interface HomeState {
    data object Loading : HomeState
    data class Before(val data: HomeBeforeUiState, val recentSessionId: String?) : HomeState
    data class InParty(val data: HomeInPartyUiState, val confirmation: EndPartyUiState?) : HomeState
    data class Finished(val sessionId: String) : HomeState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val clock: Clock,
    @param:ApplicationContext private val context: Context,
    private val observeHome: ObserveHomeState,
    observeData: ObservePartyData,
    observePresets: ObservePresets,
    observeSettings: ObserveSettings,
    private val startParty: StartParty,
    private val recordDrink: RecordDrink,
    private val undoDrink: UndoDrink,
    private val endParty: EndParty,
    private val closeSummary: CloseSummary,
) : ViewModel() {
    private val undo = MutableStateFlow<DrinkLog?>(null)
    private val confirmation = MutableStateFlow<EndTimeChoice?>(null)
    private var undoJob: Job? = null
    private val seconds = flow { while (true) { emit(clock.instant()); delay(1_000) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), clock.instant())
    private val bac = combine(observeData.logs(), observeSettings.profile, seconds.distinctUntilChanged { a, b -> a.epochSecond / 60 == b.epochSecond / 60 }) { logs, profile, _ ->
        profile?.let { BacCalculator.bacAt(logs, it, clock.instant()) } ?: 0.0
    }
    private val inputs = combine(observeHome(), observeData.logs(), observeData.history(), observePresets(), observeSettings.profile, observeSettings.recordStartDate) { values -> values }
    val uiState = combine(inputs, seconds, undo, confirmation, bac) { values, now, undoLog, choice, currentBac ->
        @Suppress("UNCHECKED_CAST")
        val state = values[0] as AppState
        @Suppress("UNCHECKED_CAST") val logs = values[1] as List<DrinkLog>
        @Suppress("UNCHECKED_CAST") val history = values[2] as List<tech.tamaki.alcoholmeter.domain.model.SessionWithLogs>
        @Suppress("UNCHECKED_CAST") val presets = values[3] as List<tech.tamaki.alcoholmeter.domain.model.DrinkPreset>
        val profile = values[4] as tech.tamaki.alcoholmeter.domain.model.UserProfile?
        val today = AbstinenceCalculator.logicalDate(now, clock.zone.rules.getOffset(now))
        val start = values[5] as? java.time.LocalDate ?: today
        when (state) {
            AppState.BeforeParty -> {
                val stats = AbstinenceCalculator.monthStats(logs, start, today, java.time.YearMonth.from(today))
                val latest = history.firstOrNull()
                val recent = latest?.let { context.getString(R.string.vm_home_recent, formatDate(it.session.startedAt.atZone(clock.zone).toLocalDate()), formatTime(it.session.startedAt, clock.zone), it.logs.size, formatGrams(it.logs.sumOf { log -> log.pureAlcoholG })) }
                val bac = currentBac
                HomeState.Before(HomeBeforeUiState(formatDate(today), AbstinenceCalculator.currentStreak(logs, start, today).toInt(), AbstinenceCalculator.longestStreak(logs, start, today).toInt(), stats.abstinentDays, recent, if (bac > 0) formatBac(bac) else null, profile?.let { BacCalculator.soberAt(logs, it, now)?.let { time -> formatTime(time, clock.zone) } }), latest?.session?.id)
            }
            is AppState.Finished -> HomeState.Finished(state.data.session.id)
            is AppState.InParty -> {
                val session = state.data
                val elapsed = formatDuration(Duration.between(session.session.startedAt, now).seconds)
                val bac = currentBac
                val counts = presets.associate { preset -> preset.id to session.logs.count { log -> matchesPreset(log, preset) } }
                val last = session.logs.maxByOrNull { it.drankAt }
                val lastPreset = last?.presetId ?: presets.firstOrNull { preset -> last != null && preset.name == last.name && preset.beverageType == last.beverageType && preset.volumeMl == last.volumeMl && preset.abvPercent == last.abvPercent }?.id
                val data = HomeInPartyUiState(formatTime(session.session.startedAt, clock.zone), elapsed, session.logs.size, formatBac(bac), formatGrams(session.logs.sumOf { it.pureAlcoholG }), session.logs.sortedByDescending { it.drankAt }.take(3).map { HomeRecentDrinkUiState(it.name, formatTime(it.drankAt, clock.zone), it.colorIndex) }, presets, counts, lastPreset, undoLog?.takeIf { session.logs.lastOrNull()?.id == it.id }?.let { context.getString(R.string.vm_recorded, it.name, session.logs.size) })
                val dialog = choice?.let { EndPartyUiState(session.logs.size, durationText(context, Duration.between(session.session.startedAt, now).seconds), formatTime(now, clock.zone), last?.let { log -> formatTime(log.drankAt, clock.zone) } ?: formatTime(now, clock.zone), it) }
                HomeState.InParty(data, dialog)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState.Loading)
    fun start() { viewModelScope.launch { startParty() } }
    fun record(presetId: Long) { viewModelScope.launch {
        val log = recordDrink(presetId)
        undo.value = log
        undoJob?.cancel()
        undoJob = launch { delay(5_000); if (undo.value?.id == log.id) undo.value = null }
    } }
    fun undoLast() {
        val log = undo.value ?: return
        undo.value = null
        undoJob?.cancel()
        viewModelScope.launch {
            val current = (observeHome().first() as? AppState.InParty)?.data?.logs?.lastOrNull()
            if (current?.id == log.id) undoDrink(log.id)
        }
    }
    fun showEndDialog(lastDrink: Boolean = false) { confirmation.value = if (lastDrink) EndTimeChoice.LastDrink else EndTimeChoice.Now }
    fun dismissEndDialog() { confirmation.value = null }
    fun confirmEnd(choice: EndTimeChoice) {
        val state = uiState.value as? HomeState.InParty ?: return
        val last = state.data.recentDrinks.firstOrNull()
        viewModelScope.launch {
            val time: Instant = if (choice == EndTimeChoice.LastDrink && last != null) {
                (observeStateLastDrink() ?: clock.instant())
            } else clock.instant()
            confirmation.value = null
            endParty(time)
        }
    }
    private suspend fun observeStateLastDrink(): Instant? = (observeHome().first() as? AppState.InParty)?.data?.logs?.maxOfOrNull { it.drankAt }
    fun close() { viewModelScope.launch { closeSummary() } }
}
